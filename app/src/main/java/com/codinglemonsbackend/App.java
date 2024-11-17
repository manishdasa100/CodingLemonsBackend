package com.codinglemonsbackend;

import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.codinglemonsbackend.Properties.S3Buckets;
import com.codinglemonsbackend.Service.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.slugify.Slugify;

@SpringBootApplication
@EnableCaching
public class App {

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
        return new ObjectMapper();    
    }

    @Bean
    public Slugify getSlugifyInstance() {
        return Slugify.builder().build();
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    /*@Bean
    CommandLineRunner runner(S3Service s3Service, S3Buckets s3Buckets){
        return args -> {
            s3Service.putObject(s3Buckets.getCustomer(), "foo/bar", "HELLO WORLD".getBytes());
            byte[] obj = s3Service.getObject(s3Buckets.getCustomer(), "foo/bar");
            System.out.println(new String(obj));
        };
    }*/
}
