package com.codinglemonsbackend.Dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TestcasesDto {
    
    Map<String, String> testcases;
}
