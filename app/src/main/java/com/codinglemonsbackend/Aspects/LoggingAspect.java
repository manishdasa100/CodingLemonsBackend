package com.codinglemonsbackend.Aspects;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Autowired
    private MeterRegistry meterRegistry;

    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    // private final ConcurrentMap<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();
    private final AtomicLong lastRequestTime = new AtomicLong(System.currentTimeMillis());

    // @Pointcut("execution(* com.codinglemonsbackend.Controller.*.*(..))")
    // public void controllerMethods() {}

    // @Pointcut("execution(* com.codinglemonsbackend.Service.*.*(..))")
    // public void serviceMethods() {}

    @Pointcut("execution(* com.codinglemonsbackend.Repository.*.*(..))")
    public void repositoryMethods() {}

    // @Around("controllerMethods()")
    // public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    //     String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + 
    //                        joinPoint.getSignature().getName();
        
    //     // Get HTTP request details
    //     ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    //     HttpServletRequest request = null;
    //     HttpServletResponse response = null;
        
    //     final String endpoint;
    //     final String httpMethod;
        
    //     if (attributes != null) {
    //         request = attributes.getRequest();
    //         response = attributes.getResponse();
    //         endpoint = request.getRequestURI();
    //         httpMethod = request.getMethod();
    //     } else {
    //         endpoint = "unknown";
    //         httpMethod = "unknown";
    //     }
        
    //     // API Request Counter
    //     Counter apiRequestCounter = counters.computeIfAbsent("api.requests", name ->
    //         Counter.builder("api.request.total")
    //                .description("Total API requests")
    //                .tag("endpoint", endpoint)
    //                .tag("method", httpMethod)
    //                .register(meterRegistry));
    //     apiRequestCounter.increment();

    //     // API Response Timer
    //     Timer apiResponseTimer = timers.computeIfAbsent("api.response." + endpoint, name ->
    //         Timer.builder("api.response.duration")
    //              .description("API response duration")
    //              .tag("endpoint", endpoint)
    //              .tag("method", httpMethod)
    //              .register(meterRegistry));

    //     // Method execution timer
    //     // Timer methodTimer = timers.computeIfAbsent(methodName, name -> 
    //     //     Timer.builder("method.execution.time")
    //     //          .description("Method execution time")
    //     //          .tag("class", "controller")
    //     //          .tag("method", name)
    //     //          .register(meterRegistry));

    //     // Counter counter = counters.computeIfAbsent(methodName, name ->
    //     //     Counter.builder("method.calls.total")
    //     //            .description("Total method calls")
    //     //            .tag("class", "controller")
    //     //            .tag("method", name)
    //     //            .register(meterRegistry));

    //     Timer.Sample sample = Timer.start(meterRegistry);
    //     // counter.increment();
        
    //     log.info("Entering controller method: {} with args: {} on endpoint: {} {}", 
    //             methodName, 
    //             Arrays.toString(joinPoint.getArgs()),
    //             httpMethod,
    //             endpoint);

    //     try {
    //         Object result = joinPoint.proceed();
            
    //         // Track response status
    //         final int statusCode;
    //         if (response != null) {
    //             statusCode = response.getStatus();
    //         } else {
    //             statusCode = -1;
    //         }
            
    //         // Track error rates
            
    //         Counter responseStatusCounter = counters.computeIfAbsent("api.response.status." + statusCode, key ->
    //             Counter.builder("api.response.status.total")
    //                     .description("API response by status code")
    //                     .tag("status_code", String.valueOf(statusCode))
    //                     .tag("endpoint", endpoint)
    //                     .tag("method", httpMethod)
    //                     .register(meterRegistry));
    //         responseStatusCounter.increment();
        
            
    //         log.info("Controller method {} executed successfully with status: {}", methodName, statusCode);
    //         return result;
    //     } catch (Exception e) {
    //         // Track exceptions
    //         // Counter exceptionCounter = errorCounters.computeIfAbsent("api.exceptions." + e.getClass().getSimpleName(), key ->
    //         //     Counter.builder("api.exceptions.total")
    //         //            .description("API exceptions by type")
    //         //            .tag("exception_type", e.getClass().getSimpleName())
    //         //            .tag("endpoint", endpoint)
    //         //            .tag("method", httpMethod)
    //         //            .register(meterRegistry));
    //         // exceptionCounter.increment();
            
    //         log.error("Controller method {} threw exception: {}", methodName, e.getMessage());
    //         throw e;
    //     } finally {
    //         // sample.stop(methodTimer);
    //         sample.stop(apiResponseTimer);
    //         updateThroughputMetrics();
    //     }
    // }

    @Around("repositoryMethods()")
    public Object logRepositoryExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + 
                           joinPoint.getSignature().getName();
        
        // Determine if it's a read or write operation
        final String operationType;
        String method = joinPoint.getSignature().getName().toLowerCase();
        if (method.startsWith("save") || method.startsWith("insert") || method.startsWith("update") || 
            method.startsWith("delete") || method.startsWith("create")) {
            operationType = "write";
        } else {
            operationType = "read";
        }
        
        Timer timer = timers.computeIfAbsent("database." + operationType + "." + methodName, name -> 
            Timer.builder("database." + operationType + ".duration")
                 .description("Database " + operationType + " operation duration")
                 .tag("operation", operationType)
                 .tag("repository", joinPoint.getSignature().getDeclaringType().getSimpleName())
                 .tag("method", joinPoint.getSignature().getName())
                 .register(meterRegistry));

        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            return joinPoint.proceed();
        } finally {
            sample.stop(timer);
        }
    }

    // @AfterThrowing(pointcut = "controllerMethods()", throwing = "ex")
    // public void logException(JoinPoint joinPoint, Throwable ex) {
    //     String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + 
    //                        joinPoint.getSignature().getName();
    //     String requestId = MDC.get("requestId");
        
    //     Counter errorCounter = Counter.builder("method.error.total")
    //                                 .description("Total method errors")
    //                                 .tag("method", methodName)
    //                                 .tag("exception_type", ex.getClass().getSimpleName())
    //                                 .register(meterRegistry);
    //     errorCounter.increment();
        
    //     log.error("Method {} failed with exception. RequestId: {}", methodName, requestId, ex);
    // }

    private void updateThroughputMetrics() {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastRequestTime.getAndSet(currentTime);
        long currentCount = requestCount.incrementAndGet();
        
        // Update throughput every 10 seconds
        if (currentTime - lastTime >= 10000) {
            DistributionSummary throughput = DistributionSummary.builder("api.throughput")
                    .description("API request throughput per 10 seconds")
                    .register(meterRegistry);
            
            double requestsPerSecond = (double) currentCount / ((currentTime - lastTime) / 1000.0);
            throughput.record(requestsPerSecond);
            
            requestCount.set(0);
        }
    }
}