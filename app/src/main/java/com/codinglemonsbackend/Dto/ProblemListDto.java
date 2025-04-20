package com.codinglemonsbackend.Dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_NULL)
public class ProblemListDto {
    
    @JsonProperty(access = Access.READ_ONLY)
    private String id;
    
    @NotEmpty
    @Size(max = 30)
    private String name;

    @NotEmpty
    @Size(min = 3, max = 100)
    private String description;

    @JsonProperty(access = Access.READ_ONLY)
    private List<ProblemDto> problemsData;

    @NotNull
    private Boolean isPublic;
    
    @NotNull
    private Boolean isPinned;

}
