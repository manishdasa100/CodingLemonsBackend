package com.codinglemonsbackend.Dto;

import com.codinglemonsbackend.Validation.PastOrCurrentYear;
import com.codinglemonsbackend.Validation.ValidYearRange;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserWorkExperienceDto {
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CompanyDto company;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Company name cannot be blank")
    private String companyName;

    @NotBlank(message = "Job title cannot be blank")
    @Size(min = 5, max = 40, message = "Job title must be between 5 and 40 characters")
    private String jobTitle;

    @NotNull
    @PastOrCurrentYear
    private Integer startYear;

    private Integer endYear;

    public UserWorkExperienceDto(CompanyDto company, String jobTitle, Integer startYear, Integer endYear) {
        this.company = company;
        this.jobTitle = jobTitle;
        this.startYear = startYear;
        this.endYear = endYear;
    }
}
