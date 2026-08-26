package com.codinglemonsbackend.Config;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codinglemonsbackend.Dto.AIProvider;
import com.codinglemonsbackend.Properties.AIHintProperties;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(AIHintProperties.class)
@Slf4j
public class AIHintConfig {
    
    @Bean
    List<AIProvider> orderedChatModels(Map<String, ChatModel> models, AIHintProperties props) {
        log.info("Configuring ordered chat clients");
        List<AIProvider> orderedAIProviders = props.getProviderPriority().stream()
            .map(name -> {
                ChatModel model = models.get(name);
                if (model == null) throw new IllegalStateException("ai.hint.provider-priority references unknown chat model '" + name
                        + "'. Available: " + models.keySet());
                return new AIProvider(name, ChatClient.create(model));
            })
            .toList();
        if (orderedAIProviders.isEmpty()) throw new IllegalStateException("ai.hint.provider-priority is empty; must specify at least one chat model");
        log.info("AI hint provider order: {}", orderedAIProviders.stream().map(AIProvider::name).toList());
        return orderedAIProviders;
    } 
}
