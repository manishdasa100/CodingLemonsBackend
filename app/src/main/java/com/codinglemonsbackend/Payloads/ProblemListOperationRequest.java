package com.codinglemonsbackend.Payloads;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProblemListOperationRequest {
    
    @NotBlank
    private String id;

    @NotEmpty
    private Set<Integer> problemIds;
}
