package com.codinglemonsbackend.Payloads;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProblemListPayload {

    @NotEmpty
    private String name;

    @NotEmpty
    private String description;

    @NotEmpty
    private List<Integer> problemIds;
}
