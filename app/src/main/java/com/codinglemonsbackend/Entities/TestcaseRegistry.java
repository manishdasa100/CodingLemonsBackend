package com.codinglemonsbackend.Entities;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "TestCase")
public class TestcaseRegistry {

    @Transient
    public static final String TYPE = "TESTCASE";

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TestcasePair{
        private String input;
        private String output;
    }

    @Id
    private String id;

    private Integer problemId;

    private List<TestcasePair> testcases;
}
