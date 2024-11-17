package com.codinglemonsbackend.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.codinglemonsbackend.Dto.Example;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProgrammingLanguage;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProblemEntityDeserializer extends JsonDeserializer<ProblemDto>{

    @Override
    public ProblemDto deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        
        ObjectNode node = p.getCodec().readTree(p);

        Difficulty problemDifficulty;

        try{
            problemDifficulty = Difficulty.valueOf(node.get("difficulty").asText(null));
        }catch(IllegalArgumentException e){
            problemDifficulty = null;
        }

        System.out.println("DIFFICULTY OF THE PROBLEM ADDED "+problemDifficulty);

        Set<String> constraints = new HashSet<String>();
        for (JsonNode item: node.get("constraints")){
            constraints.add(item.asText());
        }

        Set<Example> examples = new HashSet<Example>();
        for (JsonNode item: node.get("examples")){
            Example exampleItem = Example.builder()
                                .input(item.get("input").asText())
                                .output(item.get("output").asText())
                                .explanation(item.get("explanation").asText())
                                .build();
            examples.add(exampleItem);
        }

        LinkedHashMap<String, String> testCases = new LinkedHashMap<>();
        node.get("testCasesWithExpectedOutputs").fields().forEachRemaining((entry) -> {
            testCases.put(entry.getKey(), entry.getValue().asText());
        });

        Set<String> topics = new HashSet<String>();
        for (JsonNode item: node.get("topics")){
            topics.add(item.asText());
        }

        Set<String> companyTags = new HashSet<String>();
        for (JsonNode item: node.get("companyTags")){
            companyTags.add(item.asText());
        }

        Float cpuTimeLimit = node.get("cpuTimeLimit").floatValue();
        
        Float memoryLimit = node.get("memoryLimit").floatValue();

        Integer stackLimit = node.get("stackLimit").asInt();

        Map<ProgrammingLanguage, String> driverCodes = new HashMap<>();
        ObjectNode driverCodesNode = (ObjectNode)node.get("driverCodes");
        for (ProgrammingLanguage language : ProgrammingLanguage.values()) {
            if (driverCodesNode.has(language.name())) {
                driverCodes.put(language, driverCodesNode.get(language.name()).asText());
            }
        }

        ProblemDto problemDto = ProblemDto.builder()
                                    .title(node.get("title").asText())
                                    .description(node.get("description").asText())
                                    .constraints(constraints)
                                    .examples(examples)
                                    .difficulty(problemDifficulty)
                                    .testCasesWithExpectedOutputs(testCases)
                                    .cpuTimeLimit(cpuTimeLimit)
                                    .memoryLimit(memoryLimit)
                                    .stackLimit(stackLimit)
                                    .driverCodes(driverCodes)
                                    // .topics(topics)
                                    // .companyTags(companyTags)
                                    .build();

        System.out.println("Deserialization completed");

        return problemDto;
    }
    
}
