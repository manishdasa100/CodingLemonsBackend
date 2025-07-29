package com.codinglemonsbackend.Dto;

import java.util.EnumMap;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DriverCodeRegistryDto {    
    private EnumMap<ProgrammingLanguage, String> additions;
    private EnumMap<ProgrammingLanguage, String> updates;
    private Set<ProgrammingLanguage> deletions;
}
