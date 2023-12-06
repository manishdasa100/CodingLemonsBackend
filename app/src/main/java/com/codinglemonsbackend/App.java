package com.codinglemonsbackend;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import com.codinglemonsbackend.Service.CodeRunner;
import com.codinglemonsbackend.Service.JavaRunner;
import com.codinglemonsbackend.Service.PythonRunner;

@SpringBootApplication
@EnableCaching
public class App {

    // @Bean
    // public JavaRunner getJavaRunner() {
    //     return new JavaRunner();
    // }

    // @Bean
    // public PythonRunner getPythonRunner() {
    //     return new PythonRunner();
    // }

    // @Bean
    // public List<CodeRunner> getCodeRunners(){
    //     return Arrays.asList(
    //         new JavaRunner(), 
    //         new PythonRunner()
    //     );
    // }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
