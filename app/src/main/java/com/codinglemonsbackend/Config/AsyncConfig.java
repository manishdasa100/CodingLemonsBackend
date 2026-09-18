package com.codinglemonsbackend.Config;

import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;

import io.micrometer.core.instrument.MeterRegistry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Resolved lazily: this class is an {@link AsyncConfigurer}, which Spring needs very early,
     * and injecting the registry directly would drag the whole metrics stack up with it.
     */
    private final ObjectProvider<MeterRegistry> meterRegistry;

    public AsyncConfig(ObjectProvider<MeterRegistry> meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean("applicationAsyncExecutor")
    public TaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // Minimum threads
        executor.setMaxPoolSize(20);        // Maximum threads
        executor.setQueueCapacity(100);     // Queue size before creating new threads
        executor.setThreadNamePrefix("Application-Async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * An {@code @Async} method returning void has nowhere to hand an exception: without a handler
     * here Spring drops it on the floor. Every listener on SubmitCodeCompletedEvent is one of
     * those, so a throw meant a score was never awarded, a streak never advanced or a badge never
     * granted, with nothing anywhere to say so.
     *
     * This makes the failure loud, not survivable - the work is still lost. Anything that must not
     * be lost belongs in {@link com.codinglemonsbackend.Service.ExecutionResultProcessor}, which
     * runs on the results stream and gets redelivered.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            String task = method.getDeclaringClass().getSimpleName() + "." + method.getName();
            log.error("Async task {} failed for [{}] - its work was skipped and will not be retried",
                    task, describe(params), throwable);
            meterRegistry.ifAvailable(registry ->
                    registry.counter("async.task.failures.total", "task", task).increment());
        };
    }

    /**
     * Never calls toString() on an argument it does not recognise. SubmissionMetadata is a @Data
     * class holding the user's source code, so the obvious implementation would paste every failed
     * submission into the logs; unknown types contribute their name only.
     */
    private static String describe(Object[] params) {
        return Arrays.stream(params)
                .map(param -> {
                    if (param instanceof SubmitCodeCompletedEvent event) {
                        SubmissionMetadata metadata = event.getSubmissionMetadata();
                        return "job=%s user=%s problem=%s".formatted(metadata.getSubmissionJobId(),
                                metadata.getUsername(), metadata.getProblemId());
                    }
                    return param == null ? "null" : param.getClass().getSimpleName();
                })
                .collect(Collectors.joining(", "));
    }
}
