package com.codinglemonsbackend.Config;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;

@Configuration
public class CustomCacheConfig {
    
    public static final String DEFAULT_CACHE = "Deafult Cache";
    @Bean
    @Primary
    public CacheManager customCacheManager(){
        return new ConcurrentMapCacheManager(DEFAULT_CACHE);
    }
}
