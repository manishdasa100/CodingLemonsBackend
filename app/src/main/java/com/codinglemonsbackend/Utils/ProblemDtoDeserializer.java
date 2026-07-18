package com.codinglemonsbackend.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ProblemDto.Example;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDto.Difficulty;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;
import com.codinglemonsbackend.Repository.TopicRepository;
import com.codinglemonsbackend.Service.CompanyService;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class ProblemDtoDeserializer extends JsonDeserializer<ProblemDto>{

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
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
            if (!item.has("input") || !item.has("output")) {
                throw new IllegalArgumentException("Found invalid Example");
            }
            Example exampleItem = Example.builder()
                    .input(item.get("input").asText())
                    .output(item.get("output").asText())
                    .explanation(item.has("explanation") ? item.get("explanation").asText() : null)
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

    public Set<String> extractTopics(JsonNode node) {
        if (!node.has("topics") || !node.get("topics").isArray()) {
            throw new IllegalArgumentException("Topics must be an array");
        }
        Set<String> topicSlugs = new HashSet<>();
        for (JsonNode item : node.get("topics")) {
            if (item.isTextual()) {
                topicSlugs.add(item.asText());
            } else if (item.has("slug") && item.get("slug").isTextual()) {
                topicSlugs.add(item.get("slug").asText());
            } else {
                throw new IllegalArgumentException("Topic slug not present");
            }
        }
        return topicRepository.getValidTags(topicSlugs).stream()
                .map(t -> t.getSlug())
                .collect(Collectors.toSet());
    }

    public Set<String> extractCompanies(JsonNode node) {
        if (!node.has("companies") || !node.get("companies").isArray()) {
            throw new IllegalArgumentException("Companies must be an array");
        }
        Set<String> companySlugs = new HashSet<>();
        for (JsonNode item : node.get("companies")) {
            if (item.isTextual()) {
                companySlugs.add(item.asText());
            } else if (item.has("slug") && item.get("slug").isTextual()) {
                companySlugs.add(item.get("slug").asText());
            } else {
                throw new IllegalArgumentException("Company slug not present");
            }
        }
        return companyService.getValidTags(companySlugs).stream()
                .map(c -> c.getSlug())
                .collect(Collectors.toSet());
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
}
