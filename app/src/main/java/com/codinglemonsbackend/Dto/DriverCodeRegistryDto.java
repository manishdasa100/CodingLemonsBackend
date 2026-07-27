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
    private EnumMap<SupportedLanguage, String> additions;
    private EnumMap<SupportedLanguage, String> updates;
    private Set<SupportedLanguage> deletions;
}
