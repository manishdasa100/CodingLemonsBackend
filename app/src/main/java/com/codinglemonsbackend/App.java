package com.codinglemonsbackend;

import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.github.slugify.Slugify;

import org.springframework.http.converter.json.SpringHandlerInstantiator;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class App {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public HandlerInstantiator springHandlerInstantiator() {
        return new SpringHandlerInstantiator(applicationContext.getAutowireCapableBeanFactory());
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebClient.Builder getWebClient() {
        return WebClient.builder();
    }

    @Bean
    public ModelMapper getModelMapper(){
        return new ModelMapper();
    }

    @Bean
    public ObjectMapper getObjectMapper(){
        ObjectMapper objectMapper =  new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setHandlerInstantiator(springHandlerInstantiator());
        return objectMapper;
    }

    @Bean
    public Slugify getSlugifyInstance() {
        return Slugify.builder().build();
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    // @Bean
    // CommandLineRunner runner(Map<String, ChatModel> chatModels){
    //     return args -> {
    //         System.out.println("===================AI DEBUG WINDOW===================");
    //         log.info("Available Chat Models:{}", chatModels.size());
    //         chatModels.forEach((name, model) -> {
    //             log.info("Model Name: {}, Model Details: {}", name, model);
    //         });

    //         chatModels.forEach((name, model) -> {
    //             try {
    //                 ChatResponse response = model.call(new Prompt("Reply with exactly: OK"));
    //                 log.info("Model Name: {}, Reply: {}", name, response.getResult().getOutput().getText());
    //                 log.info("Model Name: {}, Usage Details: {}", name, response.getMetadata().getUsage());
    //             } catch (Exception e) {
    //                 log.error("Exception for model {}: {}: {}", name, e.getClass().getSimpleName(), e.getMessage());
    //             }
    //         });
    //     };
    // }
}
