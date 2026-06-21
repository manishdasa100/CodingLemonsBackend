package com.codinglemonsbackend.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
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

    public String getValue(String key) {
        return stringOperations.get(key);
    }

    public Boolean keyExist(String key){
        return redisTemplate.hasKey(key);
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

    public String addToStream(String streamKey, Map<String, String> fields) {
        RecordId recordId = redisTemplate.opsForStream().add(streamKey, fields);
        return recordId == null ? null : recordId.getValue();
    }
}
