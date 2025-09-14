package com.codinglemonsbackend.Health;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.AMQP;

@Component
public class RabbitMQHealthIndicator implements HealthIndicator {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Health health() {
        try {
            // Try to execute a simple operation to test connectivity
            Boolean connectionActive = rabbitTemplate.execute(channel -> {
                try {
                    // Just check if we can get channel info
                    return channel != null && channel.isOpen();
                } catch (Exception e) {
                    return false;
                }
            });
            
            // If we got here, connection is working
            if (connectionActive != null && connectionActive) {
                return Health.up()
                        .withDetail("connection", "active")
                        .withDetail("status", "connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("connection", "inactive")
                        .withDetail("status", "disconnected")
                        .build();
            }
                    
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "connection failed")
                    .build();
        }
    }
}