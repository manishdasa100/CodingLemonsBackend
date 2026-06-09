package com.codinglemonsbackend.Entities;

import com.codinglemonsbackend.Validation.ValidYearRange;
import com.codinglemonsbackend.Validation.PastOrCurrentYear;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ValidYearRange
public class UserWorkExperience {

    @NotBlank(message = "Company name cannot be blank")
    private String companyName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String companySlug;

    @Size(min = 5, max = 40)
    private String jobTitle;

    @NotNull(message = "Start year is required")
    @PastOrCurrentYear
    private Integer startYear;

    private Integer endYear;
}
