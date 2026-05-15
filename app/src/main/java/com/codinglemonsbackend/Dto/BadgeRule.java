package com.codinglemonsbackend.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BadgeRule {

    @NotNull
    private BadgeRuleType type;

    @NotNull
    @Min(1)
    private Integer threshold;
}
