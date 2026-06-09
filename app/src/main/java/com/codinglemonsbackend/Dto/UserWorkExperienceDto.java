package com.codinglemonsbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserWorkExperienceDto {
    
    private CompanyDto companySlug;

    private String jobTitle;

    private Integer startYear;

    private Integer endYear;
}
