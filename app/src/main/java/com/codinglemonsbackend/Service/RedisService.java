package com.codinglemonsbackend.Service;

import java.util.Date;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private RedisTemplate<String, String> redisTemplate;
    
    private HashOperations<String, String, String> hashOperations;

    private ValueOperations<String, String> stringOperations;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
        this.stringOperations = redisTemplate.opsForValue();
    }

    public void storeHash(String key, String hashKey, String value) {
        hashOperations.put(key, hashKey, value);
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

    public void storeValue(String key, String value) {
        stringOperations.set(key, value);
    }

    public void storeValue(String key, String value, Date expiry) {
        stringOperations.set(key, value);
        redisTemplate.expireAt(key, expiry);
    }

    public String getValue(String key) {
        return stringOperations.get(key);
    }

    public Boolean keyExist(String key){
        return redisTemplate.hasKey(key);
    }
}
