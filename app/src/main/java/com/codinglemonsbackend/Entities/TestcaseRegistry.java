package com.codinglemonsbackend.Entities;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
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

    @Builder.Default
    private List<TestcasePair> judgeTestcases = new ArrayList<>();

    @Builder.Default
    private List<TestcasePair> calibrationTestcases = new ArrayList<>();
}
