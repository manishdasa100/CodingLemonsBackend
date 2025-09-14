package com.codinglemonsbackend.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.codinglemonsbackend.Interceptors.MetricsInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private MetricsInterceptor metricsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(metricsInterceptor);
    }
}