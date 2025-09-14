package com.codinglemonsbackend.Config;

import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;

// import com.codinglemonsbackend.Service.MetricsService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MonitoringConfig {

    // @Autowired
    // private MetricsService metricsService;

    @Bean
    public InfoContributor customInfoContributor() {
        return new InfoContributor() {
            @Override
            public void contribute(Info.Builder builder) {
                Map<String, Object> details = new HashMap<>();
                details.put("app", "CodingLemons Backend");
                details.put("description", "Backend API for CodingLemons platform");
                details.put("startup-time", LocalDateTime.now().toString());
                details.put("java-version", System.getProperty("java.version"));
                details.put("environment", System.getProperty("spring.profiles.active", "default"));
                builder.withDetails(details);
            }
        };
    }

    @Bean
    public HealthIndicator customHealthIndicator() {
        return () -> {
            try {
                long freeMemory = Runtime.getRuntime().freeMemory();
                long totalMemory = Runtime.getRuntime().totalMemory();
                long usedMemory = totalMemory - freeMemory;
                double memoryUsagePercentage = (double) usedMemory / totalMemory * 100;
                
                if (memoryUsagePercentage > 90) {
                    return Health.down()
                            .withDetail("memory-usage", memoryUsagePercentage + "%")
                            .withDetail("reason", "High memory usage")
                            .build();
                }
                
                return Health.up()
                        .withDetail("memory-usage", memoryUsagePercentage + "%")
                        .withDetail("free-memory", freeMemory)
                        .withDetail("total-memory", totalMemory)
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    // User Engagement Metrics
    // @Bean
    // @Qualifier("userRegistrationCounter")
    // public Counter userRegistrationCounter(MeterRegistry meterRegistry) {
    //     return Counter.builder("user.registrations.total")
    //             .description("Total number of user registrations")
    //             .tag("type", "user-registration")
    //             .register(meterRegistry);
    // }

    // @Bean
    // @Qualifier("userLoginCounter")
    // public Counter userLoginCounter(MeterRegistry meterRegistry) {
    //     return Counter.builder("user.logins.total")
    //             .description("Total number of user logins")
    //             .tag("type", "user-login")
    //             .register(meterRegistry);
    
    // }
    // @Bean
    // @Qualifier("userResetPasswordCounter")
    // public Counter userResetPasswordCounter(MeterRegistry meterRegistry) {
    //     return Counter.builder("user.reset-password.total")
    //             .description("Total number of user password resets")
    //             .tag("type", "password-reset")
    //             .register(meterRegistry);
    // }

    // @Bean
    // public Gauge dailyActiveUsersGauge(MeterRegistry meterRegistry) {
    //     return Gauge.builder("user.active.daily")
    //             .description("Daily active users")
    //             .tag("period", "daily")
    //             .register(meterRegistry, metricsService, MetricsService::getDailyActiveUsers);
    // }

    // @Bean
    // public Gauge weeklyActiveUsersGauge(MeterRegistry meterRegistry) {
    //     return Gauge.builder("user.active.weekly")
    //             .description("Weekly active users")
    //             .tag("period", "weekly")
    //             .register(meterRegistry, metricsService, MetricsService::getWeeklyActiveUsers);
    // }

    // @Bean
    // public Gauge monthlyActiveUsersGauge(MeterRegistry meterRegistry) {
    //     return Gauge.builder("user.active.monthly")
    //             .description("Monthly active users")
    //             .tag("period", "monthly")
    //             .register(meterRegistry, metricsService, MetricsService::getMonthlyActiveUsers);
    // }

    @Bean
    public Counter codeExecutionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("code.executions.total")
                .description("Total number of code executions")
                .tag("type", "execution")
                .register(meterRegistry);
    }

    @Bean
    public Counter problemSubmissionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("problem.submissions.total")
                .description("Total number of problem submissions")
                .tag("type", "submission")
                .register(meterRegistry);
    }

    // Application Performance Metrics
    @Bean
    public Timer apiResponseTimer(MeterRegistry meterRegistry) {
        return Timer.builder("api.response.duration")
                .description("API response times")
                .register(meterRegistry);
    }

    @Bean
    public Counter apiRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("api.requests.total")
                .description("Total number of API requests")
                .tag("type", "request")
                .register(meterRegistry);
    }

    @Bean
    public Counter apiErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("api.errors.total")
                .description("Total number of API errors")
                .register(meterRegistry);
    }

    @Bean
    public DistributionSummary requestThroughput(MeterRegistry meterRegistry) {
        return DistributionSummary.builder("api.throughput")
                .description("API request throughput")
                .register(meterRegistry);
    }

    // Database Performance Metrics
    @Bean
    public Timer databaseReadTimer(MeterRegistry meterRegistry) {
        return Timer.builder("database.read.duration")
                .description("Database read operation duration")
                .tag("operation", "read")
                .register(meterRegistry);
    }

    @Bean
    public Timer databaseWriteTimer(MeterRegistry meterRegistry) {
        return Timer.builder("database.write.duration")
                .description("Database write operation duration")
                .tag("operation", "write")
                .register(meterRegistry);
    }

    // S3 Performance Metrics
    @Bean
    public Timer s3UploadTimer(MeterRegistry meterRegistry) {
        return Timer.builder("s3.upload.duration")
                .description("S3 file upload duration")
                .tag("service", "s3")
                .register(meterRegistry);
    }

    @Bean
    public DistributionSummary s3UploadSize(MeterRegistry meterRegistry) {
        return DistributionSummary.builder("s3.upload.size.bytes")
                .description("S3 upload file size in bytes")
                .tag("service", "s3")
                .register(meterRegistry);
    }

    @Bean
    public Counter s3UploadCounter(MeterRegistry meterRegistry) {
        return Counter.builder("s3.uploads.total")
                .description("Total number of S3 uploads")
                .tag("service", "s3")
                .register(meterRegistry);
    }

    // RabbitMQ Performance Metrics
    // @Bean
    // public Timer messageProcessingTimer(MeterRegistry meterRegistry) {
    //     return Timer.builder("rabbitmq.message.processing.duration")
    //             .description("RabbitMQ message processing time")
    //             .tag("service", "rabbitmq")
    //             .register(meterRegistry);
    // }

    // @Bean
    // public Counter messagePublishedCounter(MeterRegistry meterRegistry) {
    //     return Counter.builder("rabbitmq.messages.published.total")
    //             .description("Total messages published to RabbitMQ")
    //             .tag("service", "rabbitmq")
    //             .register(meterRegistry);
    // }

    // @Bean
    // public Counter messageConsumedCounter(MeterRegistry meterRegistry) {
    //     return Counter.builder("rabbitmq.messages.consumed.total")
    //             .description("Total messages consumed from RabbitMQ")
    //             .tag("service", "rabbitmq")
    //             .register(meterRegistry);
    // }

    // @Bean
    // public Gauge messageQueueSizeGauge(MeterRegistry meterRegistry) {
    //     return Gauge.builder("rabbitmq.queue.size")
    //             .description("Number of messages in RabbitMQ queue")
    //             .tag("service", "rabbitmq")
    //             .register(meterRegistry, metricsService, MetricsService::getRabbitMQQueueSize);
    // }

    // @Bean
    // public Timer problemExecutionTimer(MeterRegistry meterRegistry) {
    //     return Timer.builder("problem.execution.duration")
    //             .description("Time taken to execute problem solutions")
    //             .tag("type", "execution")
    //             .register(meterRegistry);
    // }

    // Infrastructure Metrics
    // @Bean
    // public Gauge networkInputGauge(MeterRegistry meterRegistry) {
    //     return Gauge.builder("system.network.input.bytes")
    //             .description("Network input bytes")
    //             .tag("direction", "input")
    //             .register(meterRegistry, metricsService, MetricsService::getNetworkInputBytes);
    // }

    // @Bean
    // public Gauge networkOutputGauge(MeterRegistry meterRegistry) {
    //     return Gauge.builder("system.network.output.bytes")
    //             .description("Network output bytes")
    //             .tag("direction", "output")
    //             .register(meterRegistry, metricsService, MetricsService::getNetworkOutputBytes);
    // }

    // @Bean
    // public Gauge containerCpuUsageGauge(MeterRegistry meterRegistry) {
    //     return Gauge.builder("container.cpu.usage.percent")
    //             .description("Container CPU usage percentage")
    //             .tag("resource", "cpu")
    //             .register(meterRegistry, metricsService, MetricsService::getContainerCpuUsage);
    // }

    // @Bean
    // public Gauge containerMemoryUsageGauge(MeterRegistry meterRegistry) {
    //     return Gauge.builder("container.memory.usage.bytes")
    //             .description("Container memory usage in bytes")
    //             .tag("resource", "memory")
    //             .register(meterRegistry, metricsService, MetricsService::getContainerMemoryUsage);
    // }

    // Metrics implementations are now delegated to MetricsService
}