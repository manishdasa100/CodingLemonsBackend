package com.codinglemonsbackend.Config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    public static final String PENDING_SUBMISSIONS = "Queue:Pending_Submissions";

    public static final String LIKE_EVENTS = "Queue:Like_Events";

    public static final String MAINEXCHANGE = "Exchange:Main";

    @Bean
    public Queue pendingSubmssionsQueue(){
        return new Queue(PENDING_SUBMISSIONS);
    }

    @Bean
    public Queue likeEventsQueue(){
        return new Queue(LIKE_EVENTS);
    }

    @Bean
    public DirectExchange mainExchange(){
        return new DirectExchange(MAINEXCHANGE);
    }


    @Bean
    public Binding pendingSubmissionsBinding(DirectExchange exchange){
        return BindingBuilder.bind(pendingSubmssionsQueue()).to(exchange).with(pendingSubmssionsQueue().getName());
    }

    @Bean
    public Binding likeEventsBinding(DirectExchange exchange){
        return BindingBuilder.bind(likeEventsQueue()).to(exchange).with(likeEventsQueue().getName());
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper){
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory, ObjectMapper objectMapper){
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter(objectMapper));
        return rabbitTemplate;
    }
    
}
