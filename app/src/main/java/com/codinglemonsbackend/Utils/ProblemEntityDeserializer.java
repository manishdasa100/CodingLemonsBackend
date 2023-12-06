package com.codinglemonsbackend.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Entities.Difficulty;
// import com.codinglemonsbackend.Entities.Difficulty;
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

        List<String> testCases = new ArrayList<String>();
        for (JsonNode item: node.get("testCases")){
            testCases.add(item.asText());
        }

        List<String> testCaseOutputs = new ArrayList<String>();
        for (JsonNode item: node.get("testCaseOutputs")){
            testCaseOutputs.add(item.asText());
        }

        List<String> topics = new ArrayList<String>();
        for (JsonNode item: node.get("topics")){
            topics.add(item.asText());
        }

        Map<ProgrammingLanguage, String> driverCodes = new HashMap<>();
        ObjectNode driverCodesNode = (ObjectNode)node.get("driverCodes");
        for (ProgrammingLanguage language : ProgrammingLanguage.values()) {
            if (driverCodesNode.has(language.name())) {
                driverCodes.put(language, driverCodesNode.get(language.name()).asText());
            }
        }

        Map<ProgrammingLanguage, String> optimalSolutions = new HashMap<>();
        ObjectNode optimalSolutionsNode = (ObjectNode)node.get("optimalSolutions");
        for (ProgrammingLanguage language : ProgrammingLanguage.values()) {
            if (optimalSolutionsNode.has(language.name())) {
                optimalSolutions.put(language, optimalSolutionsNode.get(language.name()).asText());
            }
        }

        ProblemDto problemDto = ProblemDto.builder()
                                    .title(node.get("title").asText())
                                    .description(node.get("description").asText())
                                    .difficulty(problemDifficulty)
                                    .testCases(testCases)
                                    .testCaseOutputs(testCaseOutputs)
                                    .driverCodes(driverCodes)
                                    .optimalSolutions(optimalSolutions)
                                    .topics(topics)
                                    .build();

        return problemDto;
    }
    
}
