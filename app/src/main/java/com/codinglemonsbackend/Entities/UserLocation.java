package com.codinglemonsbackend.Entities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserLocation {
    
    @Size(min = 2, max = 50, message = "City name must be between 2 and 50 characters") 
    private String city;

    @NotBlank(message = "Country cannot be blank")
    @Size(min = 4, max = 50, message = "Country name must be between 4 and 50 characters")
    private String country;
}
