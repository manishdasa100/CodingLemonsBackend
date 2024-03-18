package com.codinglemonsbackend.Config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CustomCacheConfig {
    
    public static final String DEFAULT_CACHE = "DEFAULT";

    public static final String ALL_PROBLEMS_CACHE = "ALL PROBLEMS";

    public static final String PROBLEM_OF_THE_DAY_CACHE = "PROBLEM OF THE DAY";
    
    // @Bean
    // @Primary
    // public CacheManager customCacheManager(){
    //     return new ConcurrentMapCacheManager(DEFAULT_CACHE);
    // }
}
