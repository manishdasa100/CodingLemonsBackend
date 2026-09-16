package com.codinglemonsbackend.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.HashOperations;
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
    public static final String USER_PENDING_LIKES_PREFIX = "PENDING_LIKES:";
    public static final String USER_PENDING_DISLIKES_PREFIX = "PENDING_DISLIKES:";
    public static final String USER_LIKE_STATUS_CACHE_PREFIX = "LIKE_STATUS_CACHE:";
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

    public Boolean isSetMember(String key, String value) {
        return setOperations.isMember(key, value);
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

    public void acknowledge(String streamKey, String group, RecordId recordId) {
        streamOps().acknowledge(streamKey, group, recordId);
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
