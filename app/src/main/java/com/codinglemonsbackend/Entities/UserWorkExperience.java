package com.codinglemonsbackend.Entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserWorkExperience {

    private String companyName;

    private String companySlug;

    private String jobTitle;

    private Integer startYear;

    private Integer endYear;
}
