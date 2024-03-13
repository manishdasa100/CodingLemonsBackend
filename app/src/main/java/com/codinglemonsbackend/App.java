package com.codinglemonsbackend;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

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

    @Bean
    public WebClient.Builder getWebClient() {
        return WebClient.builder();
    }

    @Bean
    public ModelMapper getModelMapper(){
        return new ModelMapper();
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
