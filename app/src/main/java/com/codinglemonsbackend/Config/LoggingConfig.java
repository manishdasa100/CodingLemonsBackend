package com.codinglemonsbackend.Config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Configuration
public class LoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(false); // Don't log request body for security
        loggingFilter.setIncludeHeaders(false); // Don't log headers for security
        loggingFilter.setMaxPayloadLength(1000);
        return loggingFilter;
    }

    @Bean
    public Filter mdcFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                
                try {
                    String requestId = UUID.randomUUID().toString().substring(0, 8);
                    String userAgent = httpRequest.getHeader("User-Agent");
                    String remoteAddr = getClientIpAddress(httpRequest);
                    
                    MDC.put("requestId", requestId);
                    MDC.put("method", httpRequest.getMethod());
                    MDC.put("uri", httpRequest.getRequestURI());
                    MDC.put("userAgent", userAgent != null ? userAgent : "unknown");
                    MDC.put("clientIp", remoteAddr);
                    
                    // Add request ID to response header for tracing
                    httpResponse.setHeader("X-Request-ID", requestId);
                    
                    chain.doFilter(request, response);
                    
                    MDC.put("statusCode", String.valueOf(httpResponse.getStatus()));
                    
                } finally {
                    MDC.clear();
                }
            }
            
            private String getClientIpAddress(HttpServletRequest request) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                
                return request.getRemoteAddr();
            }
        };
    }
}