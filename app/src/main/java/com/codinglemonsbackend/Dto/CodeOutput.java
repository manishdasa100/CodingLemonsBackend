package com.codinglemonsbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeOutput {
    private String error;
    private String stderr;
    private String stdout;
}
