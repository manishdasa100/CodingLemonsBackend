package com.codinglemonsbackend.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.Difficulty;
import com.codinglemonsbackend.Dto.Example;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;
import com.codinglemonsbackend.Entities.Company;
import com.codinglemonsbackend.Entities.Topic;
import com.codinglemonsbackend.Repository.CompanyRepository;
import com.codinglemonsbackend.Repository.TopicRepository;
import com.codinglemonsbackend.Service.CompanyService;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProblemEntityDeserializer extends JsonDeserializer<ProblemDto>{

 
    private TopicRepository topicRepository;


    private CompanyService companyService;

    @Override
    public ProblemDto deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        ObjectNode node = p.getCodec().readTree(p);
        
        return ProblemDto.builder()
                .title(extractTitle(node))
                .description(extractDescription(node))
                .constraints(extractConstraints(node))
                .examples(extractExamples(node))
                .difficulty(extractDifficulty(node))
                .cpuTimeLimit(extractCpuTimeLimit(node))
                .memoryLimit(extractMemoryLimit(node))
                .stackLimit(extractStackLimit(node))
                .topics(extractTopics(node))
                .companies(extractCompanies(node))
                .codeSnippets(extractCodeSnippets(node))
                .build();
    }

    public String extractTitle(JsonNode node) {
        if (!node.has("title") || !node.get("title").isTextual()) {
            throw new IllegalArgumentException("Title must be a non-blank string");
        }
        String title = node.get("title").asText();
        if (title.isBlank()) {
            throw new IllegalArgumentException("Title must be a non-blank string");
        }
        return title;
    }

    public String extractDescription(JsonNode node) {
        if (!node.has("description") || !node.get("description").isTextual()) {
            throw new IllegalArgumentException("Description must be a non-blank string");
        }
        String description = node.get("description").asText();
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description must be a non-blank string");
        }
        return description;
    }

    public List<String> extractConstraints(JsonNode node) {
        if (!node.has("constraints") || !node.get("constraints").isArray()) {
            throw new IllegalArgumentException("Constraints must be an array");
        }
        JsonNode constraintsNode = node.get("constraints");
        List<String> constraints = new ArrayList<>();
        for (JsonNode item : constraintsNode) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("Found invalid constraint");
            }
            constraints.add(item.asText());
        }
        return constraints;
    }

    public List<Example> extractExamples(JsonNode node) {
        if (!node.has("examples") || !node.get("examples").isArray()) {
            throw new IllegalArgumentException("Examples must be an array");
        }
        JsonNode examplesNode = node.get("examples");
        List<Example> examples = new ArrayList<>();
        for (JsonNode item : examplesNode) {
            if (!item.has("input") || !item.has("output") || !item.has("explanation")) {
                throw new IllegalArgumentException("Found invalid Example");
            }
            Example exampleItem = Example.builder()
                    .input(item.get("input").asText())
                    .output(item.get("output").asText())
                    .explanation(item.get("explanation").asText())
                    .build();
            examples.add(exampleItem);
        }
        return examples;
    }

    public Difficulty extractDifficulty(JsonNode node) {
        if (!node.has("difficulty") || !node.get("difficulty").isTextual()) {
            throw new IllegalArgumentException("Invalid difficulty value");
        }
        try {
            return Difficulty.valueOf(node.get("difficulty").asText(null));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid difficulty value");
        }
    }

    public Float extractCpuTimeLimit(JsonNode node) {
        if (!node.has("cpuTimeLimit") || !node.get("cpuTimeLimit").isNumber()) {
            throw new IllegalArgumentException("CPU time limit must be a number");
        }
        float cpuTimeLimit = node.get("cpuTimeLimit").floatValue();
        if (cpuTimeLimit < 0.1f || cpuTimeLimit > 5.0f) {
            throw new IllegalArgumentException("CPU time limit must be between 0.1 and 5.0 seconds");
        }
        return cpuTimeLimit;
    }

    public Float extractMemoryLimit(JsonNode node) {
        if (!node.has("memoryLimit") || !node.get("memoryLimit").isNumber()) {
            throw new IllegalArgumentException("Memory limit must be a number");
        }
        float memoryLimit = node.get("memoryLimit").floatValue();
        if (memoryLimit < 100.0f || memoryLimit > 128000.0f) {
            throw new IllegalArgumentException("Memory limit must be between 100.0 and 128000.0 MB");
        }
        return memoryLimit;
    }

    public Integer extractStackLimit(JsonNode node) {
        if (!node.has("stackLimit") || !node.get("stackLimit").isNumber()) {
            throw new IllegalArgumentException("Stack limit must be a number");
        }
        int stackLimit = node.get("stackLimit").intValue();
        if (stackLimit < 1024 || stackLimit > 40000) {
            throw new IllegalArgumentException("Stack limit must be between 1024 and 40000 KB");
        }
        return stackLimit;
    }

    public Set<Topic> extractTopics(JsonNode node) {
        if (!node.has("topicSlugs") || !node.get("topicSlugs").isArray()) {
            throw new IllegalArgumentException("Topics must be an array");
        }
        JsonNode topicSlugsNode = node.get("topicSlugs");
        List<String> topicSlugList= new ArrayList<>();
        for (JsonNode item : topicSlugsNode) {
            if (item.isTextual()) {
                topicSlugList.add(item.asText());
            } else if (item.has("slug") && item.get("slug").isTextual()) {
                topicSlugList.add(item.get("slug").asText());
            } else {
                throw new IllegalArgumentException("Topic slug not present");
            }
        }
        return topicRepository.getValidTags(topicSlugList);
    }

    public Set<CompanyDto> extractCompanies(JsonNode node) {
        if (!node.has("companySlugs") || !node.get("companySlugs").isArray()) {
            throw new IllegalArgumentException("Companies must be an array");
        }
        List<String> companySlugList = new ArrayList<>();
        for (JsonNode item : node.get("companySlugs")) {
            if (item.isTextual()) {
                companySlugList.add(item.asText());
            } else if (item.has("slug") && item.get("slug").isTextual()) {
                companySlugList.add(item.get("slug").asText());
            } else {
                throw new IllegalArgumentException("Company slug not present");
            }
        }
        return companyService.getValidTags(companySlugList);
    }

    public Map<ProgrammingLanguage, String> extractCodeSnippets(JsonNode node) {
        if (!node.has("codeSnippets") || !node.get("codeSnippets").isObject()) {
            throw new IllegalArgumentException("Driver codes must be an object");
        }
        Map<ProgrammingLanguage, String> codeSnippets = new HashMap<>();
        JsonNode codeSnippetsNode = node.get("codeSnippets");
        codeSnippetsNode.fields().forEachRemaining(entry -> {
            try {
                ProgrammingLanguage lang = ProgrammingLanguage.valueOf(entry.getKey());
                if (entry.getValue().isTextual()) {
                    codeSnippets.put(lang, entry.getValue().asText());
                } else {
                    throw new IllegalArgumentException("Code snippet value must be a string");
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid programming language: " + entry.getKey());
            }
        });
        return codeSnippets;
    }

    public ProblemStatus extractStatus(JsonNode node) {
        if (!node.has("status") || !node.get("status").isTextual()) {
            throw new IllegalArgumentException("Status must be a string");
        }
        try {
            return ProblemStatus.valueOf(node.get("status").asText());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value");
        }
    }
}
