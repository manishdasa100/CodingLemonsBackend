package com.codinglemonsbackend.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserRankDto {
    
    @NotNull
    @Size(min = 3, max = 20)
    private String rankName;

    @NotNull
    @Min(0)
    private Integer milestonePoints;

    @JsonProperty(access = Access.READ_ONLY)
    private String rankBadgeUrl;
}
