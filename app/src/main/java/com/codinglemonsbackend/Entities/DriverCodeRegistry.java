package com.codinglemonsbackend.Entities;

import java.util.EnumMap;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.SupportedLanguage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "DriverCode")
public class DriverCodeRegistry {

    @Id
    private Integer problemId;

    private EnumMap<SupportedLanguage, String> driverCodes;
}
