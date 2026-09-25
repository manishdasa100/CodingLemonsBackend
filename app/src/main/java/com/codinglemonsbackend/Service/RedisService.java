package com.codinglemonsbackend.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
    public static final String DEFAULT_CACHE = "DEFAULT";
    public static final String ALL_PROBLEMS_CACHE = "ALL PROBLEMS";
    public static final String PROBLEM_OF_THE_DAY_CACHE = "PROBLEM OF THE DAY";
    public static final String PROBLEM_LIKES_COUNT_CACHE_PREFIX = "PROBLEM_LIKES_COUNT:";
    /**
     * One hash per user, problem id to "1" or "0". It is both the answer to "has this user liked
     * it" and the record of a click the batch has not stored yet - those were two structures
     * saying the same thing, and keeping them apart meant every click had to write both.
     */
    public static final String USER_LIKE_STATUS_PREFIX = "LIKE_STATUS:";
    public static final String SUBMISSION_DEDUP_KEY = "submission:dedup:";
    public static final String PROBLEM_COUNT_BY_DIFFICULTY_CACHE = "PROBLEM:COUNT:BY:DIFFICULTY";
    public static final String USER_RANKS = "USER_RANKS";
    public static final String AI_HINT_COOLDOWN_PREFIX = "ai:hint:cooldown:";
    public static final String AI_HINT_QUOTA_PREFIX = "ai:hint:quota:";
    private RedisTemplate<String, String> redisTemplate;
    private HashOperations<String, String, String> hashOperations;
    private ValueOperations<String, String> stringOperations;
    private SetOperations<String, String> setOperations;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
        this.stringOperations = redisTemplate.opsForValue();
        this.setOperations = redisTemplate.opsForSet();
    }

    public void storeHash(String key, String hashKey, String value, long timeout) {
        hashOperations.put(key, hashKey, value);
        redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * Same as {@link #storeHash} but in one round trip instead of two. Written against the raw
     * connection because that is the only way to pipeline; the template serializes keys, values
     * and hash fields as plain strings, so these are the same bytes the typed operations produce.
     */
    public void storeHashPipelined(String key, String hashKey, String value, long ttlSeconds) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);
            connection.hashCommands().hSet(rawKey,
                    hashKey.getBytes(StandardCharsets.UTF_8),
                    value.getBytes(StandardCharsets.UTF_8));
            connection.keyCommands().expire(rawKey, ttlSeconds);
            return null;
        });
    }
    
    public void incrementHashValue(String key, String hashKey, long value) {
        hashOperations.increment(key, hashKey, value);
    }

    public void decrementHashValue(String key, String hashKey, long value) {
        hashOperations.increment(key, hashKey, -value);
    }

    public String getHashValue(String key, String hashKey) {
        return hashOperations.get(key, hashKey);
    }

    public void deleteHashEntry(String key, String hashKey) {
        hashOperations.delete(key, hashKey);
    }

    public boolean hashKeyExists(String key, String hashKey){
        return hashOperations.hasKey(key, hashKey);
    }

    public Map<String, String> getHashEntries(String key) {
        return hashOperations.entries(key);
    }

    public void storeValue(String key, String value, long timeout) {
        stringOperations.set(key, value);
        redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * Atomically sets {@code key} to {@code value} with the given TTL only if the key
     * does not already exist. Returns true if the key was set (new), false if it already
     * existed (duplicate).
     */
    public Boolean setIfAbsent(String key, String value, long ttlSeconds) {
        return stringOperations.setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public Boolean putHashIfAbsent(String key, String hashKey, String value) {
        return hashOperations.putIfAbsent(key, hashKey, value);
    }

    public String getValue(String key) {
        return stringOperations.get(key);
    }

    public Boolean keyExist(String key){
        return redisTemplate.hasKey(key);
    }

    public Long increment(String key, long delta) {
        return stringOperations.increment(key, delta);
    }

    public void addToSet(String key, String... values) {
        setOperations.add(key, values);
    }
    
    public void removeFromSet(String key, String... values) {
        setOperations.remove(key, values);
    }

    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    private StreamOperations<String, String, String> streamOps() {
        return redisTemplate.opsForStream();
    }

    public String addToStream(String streamKey, Map<String, String> fields) {
        RecordId recordId = streamOps().add(streamKey, fields);
        return recordId == null ? null : recordId.getValue();
    }

    public Set<String> getSetMembers(String key) {
        Set<String> members = setOperations.members(key);
        return members == null ? Set.of() : members;
    }

    public Long incrementWithTtl(String key, long delta, long ttlSeconds) {
        Long value = stringOperations.increment(key, delta);
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        return value;
    }

    /**
     * Creates the consumer group, also creating the stream if it does not exist yet. Redis
     * errors with BUSYGROUP when the group is already there, which is the normal case on
     * every boot after the first.
     */
    public void createConsumerGroup(String streamKey, String group) {
        try {
            streamOps().createGroup(streamKey, ReadOffset.from("0"), group);
        } catch (RedisSystemException e) {
            String message = e.getMostSpecificCause().getMessage();
            if (message == null || !message.contains("BUSYGROUP")) throw e;
        }
    }

    /**
     * Reads up to {@code count} entries never yet delivered to this group. Unlike a listener
     * container, which hands over one record at a time, this returns the whole slice - which is
     * what lets a consumer coalesce a batch before touching the database.
     */
    public List<MapRecord<String, String, String>> readGroup(
            String streamKey, String group, String consumer, int count) {
        List<MapRecord<String, String, String>> records = streamOps().read(
                Consumer.from(group, consumer),
                StreamReadOptions.empty().count(count),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
        return records == null ? List.of() : records;
    }

    public void acknowledge(String streamKey, String group, RecordId... recordIds) {
        streamOps().acknowledge(streamKey, group, recordIds);
    }

    /**
     * Acknowledging an entry only clears it from the group's pending list - the entry itself stays
     * in the stream forever. Deleting it after the ack is what stops the stream growing without
     * bound, and it can never drop unprocessed work because only acked ids are passed here.
     */
    public Long deleteFromStream(String streamKey, RecordId... recordIds) {
        return streamOps().delete(streamKey, recordIds);
    }

    public PendingMessages getPendingMessages(String streamKey, String group, long count) {
        return streamOps().pending(streamKey, group, Range.unbounded(), count);
    }

    /**
     * Takes ownership of entries another consumer left pending for longer than {@code minIdle},
     * returning them for reprocessing.
     */
    public List<MapRecord<String, String, String>> claimPending(
            String streamKey, String group, String consumer, Duration minIdle, RecordId... recordIds) {
        return streamOps().claim(streamKey, group, consumer, minIdle, recordIds);
    }

    public void setExpiry(String key, Instant instant) {
        redisTemplate.expireAt(key, instant);
    }

    public Long getExpiry(String key) {
        return redisTemplate.getExpire(key);
    }
}
