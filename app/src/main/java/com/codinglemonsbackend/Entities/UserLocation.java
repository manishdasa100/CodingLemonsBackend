package com.codinglemonsbackend.Entities;

import com.codinglemonsbackend.Validation.CrossFieldValidation;
import com.codinglemonsbackend.Validation.CrossFieldValidation.ValidationType;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@CrossFieldValidation(rules = {
    @CrossFieldValidation.FieldRule(
        field = "city",
        dependsOn = "country",
        type = ValidationType.BOTH_OR_NEITHER,
        message = "Both city and country must be provided together, or both must be empty"
    )
})
public class UserLocation {

    @Size(min = 2, max = 50, message = "City name must be between 2 and 50 characters")
    private String city;

    @Size(min = 4, max = 50, message = "Country name must be between 4 and 50 characters")
    private String country;
}
