package com.codinglemonsbackend.Config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.codinglemonsbackend.Service.RedisService;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CustomCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                                .entryTtl(Duration.ofMinutes(30))
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> customConfigs = new HashMap<>();
        customConfigs.put(RedisService.ALL_PROBLEMS_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)));
        customConfigs.put(RedisService.PROBLEM_OF_THE_DAY_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(customConfigs)
                .build();
    }

    @Bean
    public CachingConfigurer cachingConfigurer() {
        return new CachingConfigurer() {
            @Override
            public CacheErrorHandler errorHandler() {
                return new CacheErrorHandler() {
                    @Override
                    public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                        log.warn("Cache GET failed on cache '{}', key '{}': {}", cache.getName(), key, e.getMessage());
                    }
                    @Override
                    public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                        log.warn("Cache PUT failed on cache '{}', key '{}': {}", cache.getName(), key, e.getMessage());
                    }
                    @Override
                    public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                        log.warn("Cache EVICT failed on cache '{}', key '{}': {}", cache.getName(), key, e.getMessage());
                    }
                    @Override
                    public void handleCacheClearError(RuntimeException e, Cache cache) {
                        log.warn("Cache CLEAR failed on cache '{}': {}", cache.getName(), e.getMessage());
                    }
                };
            }
        };
    }
    
}
