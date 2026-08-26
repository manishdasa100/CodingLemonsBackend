package com.codinglemonsbackend.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ExecutionStatus;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromptCatalog {

    private final ResourcePatternResolver resourcePatternResolver;
    
    private final Map<ExecutionStatus, List<String>> templates = new EnumMap<>(ExecutionStatus.class);

    @PostConstruct
    void loadTemplate() throws IOException {
        Map<ExecutionStatus, SortedMap<Integer, String>> byLevel = new EnumMap<>(ExecutionStatus.class);

        for (Resource r : resourcePatternResolver.getResources("classpath:ai/prompts/*.st")) {
            String name = r.getFilename().replace(".st", "");
            int dash = name.indexOf('-');
            ExecutionStatus status = ExecutionStatus.valueOf(name.substring(0, dash).toUpperCase());
            int level = Integer.parseInt(name.substring(dash+1));
            byLevel.computeIfAbsent(status, k -> new TreeMap<>())
            .put(level, r.getContentAsString(StandardCharsets.UTF_8));
        }
        byLevel.forEach((status, levels) -> {
            if (levels.firstKey() != 1 || levels.lastKey() != levels.size()) throw new IllegalStateException("Prompt levels for "+ status+ " must be 1...n with no gaps, found "+levels.keySet());
            templates.put(status, List.copyOf(levels.values()));
        });

        if (templates.isEmpty()) throw new IllegalStateException("No prompt templates found on classpath:ai/prompts/*.st");
    }

    public boolean supports(ExecutionStatus status) { 
        return templates.containsKey(status); 
    }

    public int maxLevel(ExecutionStatus status) { 
        return templates.get(status).size(); 
    }

    public String render(ExecutionStatus status, int level, Map<String, Object> vars) {
        return new PromptTemplate(templates.get(status).get(level-1)).render(vars);
    }

}
