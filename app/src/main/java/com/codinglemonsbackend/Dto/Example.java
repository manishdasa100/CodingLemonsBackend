package com.codinglemonsbackend.Dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Example implements Serializable{
    
    private String input;

    private String output;

    private String explanation;
}
