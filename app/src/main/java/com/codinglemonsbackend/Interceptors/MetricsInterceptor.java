package com.codinglemonsbackend.Interceptors;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MetricsInterceptor implements HandlerInterceptor{

    private final MeterRegistry meterRegistry;
    // private final Counter totalRequestsCounter;
    private final AtomicInteger activeRequests;
    
    public MetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.activeRequests = new AtomicInteger(0);

        // Register active requests gauge
        meterRegistry.gauge("server.active.requests", Tags.empty(), activeRequests);
        
        // Register total requests counter
        // this.totalRequestsCounter = Counter.builder("api.requests.total")
        //         .description("Total number of API requests")
        //         .register(meterRegistry);
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        activeRequests.incrementAndGet();
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            String controllerPattern = extractControllerPattern(handlerMethod);
            request.setAttribute("CONTROLLER_PATTERN", controllerPattern);
        }
        // request.setAttribute("startTime", System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        
        activeRequests.decrementAndGet();
        
        // long startTime = (Long) request.getAttribute("startTime");
        // long duration = System.nanoTime() - startTime;

        // String method = request.getMethod();
        // String status = String.valueOf(response.getStatus());
        // String uri = request.getRequestURI();

        // totalRequestsCounter.increment();

        // TODO: Add endpoint-specific metrics here
        // Example: Counter for specific endpoints, response times by endpoint, etc.
        
        // Optional: Record response time by endpoint if needed
        // meterRegistry.timer("api.response.duration", Tags.of("status", status, "method", method, "uri", sanitizeUri(uri)))
        //              .record(duration, TimeUnit.NANOSECONDS);
    }

    private String extractControllerPattern(HandlerMethod handlerMethod) {
        try {
            // Get class-level @RequestMapping
            RequestMapping classMapping = handlerMethod.getBeanType().getAnnotation(RequestMapping.class);
            String basePath = "";
            if (classMapping != null && classMapping.value().length > 0) {
                basePath = classMapping.value()[0];
            }

            // Get method-level @RequestMapping
            RequestMapping methodMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
            String methodPath = "";
            if (methodMapping != null && methodMapping.value().length > 0) {
                methodPath = methodMapping.value()[0];
            }

            // Handle other mapping annotations
            if (methodMapping == null) {
                methodPath = extractFromOtherMappings(handlerMethod);
            }

            String fullPattern = basePath + methodPath;
            return fullPattern.isEmpty() ? "/unknown" : fullPattern;
            
        } catch (Exception e) {
            return "/error";
        }
    }

    private String extractFromOtherMappings(HandlerMethod handlerMethod) {
        // Handle @GetMapping, @PostMapping, etc.
        if (handlerMethod.getMethodAnnotation(GetMapping.class) != null) {
            GetMapping mapping = handlerMethod.getMethodAnnotation(GetMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        
        if (handlerMethod.getMethodAnnotation(PostMapping.class) != null) {
            PostMapping mapping = handlerMethod.getMethodAnnotation(PostMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        
        if (handlerMethod.getMethodAnnotation(PutMapping.class) != null) {
            PutMapping mapping = handlerMethod.getMethodAnnotation(PutMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        
        if (handlerMethod.getMethodAnnotation(DeleteMapping.class) != null) {
            DeleteMapping mapping = handlerMethod.getMethodAnnotation(DeleteMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        
        if (handlerMethod.getMethodAnnotation(PatchMapping.class) != null) {
            PatchMapping mapping = handlerMethod.getMethodAnnotation(PatchMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        
        return "";
    }

}
