package com.codinglemonsbackend.Config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MetricsFilter extends OncePerRequestFilter {

    private final MeterRegistry meterRegistry;

    //private final AtomicInteger activeRequests;

    public MetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        //this.activeRequests = new AtomicInteger(0);

         // Register active requests gauge
        //meterRegistry.gauge("server.active.requests", Tags.empty(), activeRequests);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
    
        //activeRequests.incrementAndGet();

        try {
            filterChain.doFilter(request, response);
        } finally {
            String method = request.getMethod();
            String pathPattern = getPathPattern(request);
            String status = String.valueOf(response.getStatus());
            String remoteAddr = request.getRemoteAddr();
            
            Tags tags = Tags.of(
                "method", method,
                "path", pathPattern,
                "status", status,
                "remoteAddr", remoteAddr
            );
            
            meterRegistry.counter("api.requestsssss.total", tags).increment();
            //activeRequests.decrementAndGet();
        }
    }

    private String getPathPattern(HttpServletRequest request) {
        // For actuator endpoints, use the actual URI
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/actuator/")) {
            return requestURI;
        }
        
        // For application controller endpoints, use controller pattern if available
        Object controllerPattern = request.getAttribute("CONTROLLER_PATTERN");
        if (controllerPattern != null) {
            return controllerPattern.toString();
        }
        
        // Fallback for requests without controller pattern (likely invalid JWT)
        return "INVALID JWT";
    }
}