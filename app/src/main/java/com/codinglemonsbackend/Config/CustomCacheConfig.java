package com.codinglemonsbackend.Config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class CustomCacheConfig {
    
    public static final String DEFAULT_CACHE = "DEFAULT";

    public static final String ALL_PROBLEMS_CACHE = "ALL PROBLEMS";

    public static final String PROBLEM_OF_THE_DAY_CACHE = "PROBLEM OF THE DAY";

    public static final String PROBLEM_LIKES_CACHE = "PROBLEM LIKES";

    public static final String USER_LIKES_CACHE_PREFIX = "USER_LIKES:";

    public static final String USER_DISLIKES_CACHE_PREFIX = "USER_DISLIKES:";
    
    @Bean
    public CacheManager customCacheManager(RedisConnectionFactory redisConnectionFactory){
        //return new ConcurrentMapCacheManager(DEFAULT_CACHE);
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                                .entryTtl(Duration.ofMinutes(30))
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> customConfigs = new HashMap<>();
        customConfigs.put(ALL_PROBLEMS_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)));
        customConfigs.put(PROBLEM_OF_THE_DAY_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(customConfigs)
                .build();
    }
}
