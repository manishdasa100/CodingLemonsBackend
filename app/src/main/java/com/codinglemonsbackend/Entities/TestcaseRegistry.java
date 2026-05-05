package com.codinglemonsbackend.Entities;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "TestCase")
public class TestcaseRegistry {

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TestcasePair {
        private String input;
        private String expectedOutput;
    }

    @Id
    private Integer problemId;

    private List<TestcasePair> testcases;
}
