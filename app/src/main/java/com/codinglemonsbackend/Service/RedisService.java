package com.codinglemonsbackend.Service;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
    
    private HashOperations<String, String, String> hashOperations;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.hashOperations = redisTemplate.opsForHash();
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
}
