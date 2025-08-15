package com.codinglemonsbackend.Entities;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "UserStreaks")
public class UserStreakEntity {

    @Id
    private String username;
    private Integer streakDays;
    private LocalDate lastSubmissionDate; 
}
