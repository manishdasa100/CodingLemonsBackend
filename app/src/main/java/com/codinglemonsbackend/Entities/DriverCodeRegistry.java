package com.codinglemonsbackend.Entities;

import java.util.EnumMap;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.ProgrammingLanguage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "DriverCode")
public class DriverCodeRegistry {

    @Transient
    public static final String TYPE = "DRIVERCODE";

    @Id
    private String id;

    private Integer problemId;

    private EnumMap<ProgrammingLanguage, String> driverCodes;
}
