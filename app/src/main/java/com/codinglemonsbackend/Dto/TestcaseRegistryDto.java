package com.codinglemonsbackend.Dto;

import java.util.List;
import java.util.Set;

import com.codinglemonsbackend.Entities.TestcaseRegistry.TestcasePair;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TestcaseRegistryDto { 
    private List<TestcasePair> additions;
    private List<TestcasePair> updates;
    private Set<String> deletions;
}
