package com.codinglemonsbackend.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import jakarta.servlet.Filter;

@Configuration
public class FilterRegistrationConfig {
    
    @Autowired
    @Qualifier("metricsFilter")
    private Filter metricsFilter;

    // @Autowired
    // @Qualifier("mdcFilter")
    // private Filter mdcFilter;

    @Bean
    public FilterRegistrationBean<Filter> metricsFilterRegistration() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(metricsFilter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // Run first
        return registrationBean;
    }

    // @Bean
    // public FilterRegistrationBean<Filter> mdcFilterRegistration() {
    //     FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    //     registrationBean.setFilter(mdcFilter);
    //     registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 2); // Run second
    //     return registrationBean;
    // }
}
