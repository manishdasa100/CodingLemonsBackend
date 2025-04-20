package com.codinglemonsbackend.Payloads;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateProblemListRequest {
    
    @Size(min = 1, max = 15)
    private String name;

    @Size(min = 3, max = 50)
    private String description;

    private Boolean isPublic;

    private Boolean isPinned;
}
