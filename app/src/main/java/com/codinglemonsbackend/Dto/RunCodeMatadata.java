package com.codinglemonsbackend.Dto;

import java.util.List;

import com.codinglemonsbackend.Entities.ProgrammingLanguage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunCodeMatadata {
    private String code;
    private ProgrammingLanguage language;
    private List<String> testCases;
}
