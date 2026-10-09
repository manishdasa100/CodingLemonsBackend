package com.codinglemonsbackend.Filters;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component 
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("web.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long start = System.nanoTime();
        MDC.put("requestId", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());
        // Not X-Forwarded-For: its first entry is whatever the client chose to send. Behind
        // Cloudflare, server.forward-headers-strategy=native (Day 13) makes this the real client.
        MDC.put("clientIp", request.getRemoteAddr());
        response.setHeader("X-Request-ID", requestId);

        boolean completed = false;
        try {
            chain.doFilter(request, response);
            completed = true;
        } finally {
            // An exception escaping the chain only becomes a 500 after this filter has returned.
            int status = completed ? response.getStatus() : 500;
            MDC.put("status", String.valueOf(status));
            MDC.put("durationMs", String.valueOf((System.nanoTime() - start) / 1_000_000));
            ACCESS_LOG.info("{} {} {}", request.getMethod(), request.getRequestURI(), status);
            MDC.clear();
        }
    }
}
