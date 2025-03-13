package com.codinglemonsbackend.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

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

    public static final String PROBLEM_LIKES_COUNT_PREFIX = "LIKES_COUNT:";

    public static final String USER_LIKES_CACHE_PREFIX = "USER_LIKES:";

    public static final String USER_DISLIKES_CACHE_PREFIX = "USER_DISLIKES:";

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

    public void storeHash(String key, String hashKey, String value) {
        hashOperations.put(key, hashKey, value);
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

    public void storeValue(String key, String value, long timeout) {
        stringOperations.set(key, value);
        redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
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
}
