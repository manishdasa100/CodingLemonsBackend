package com.codinglemonsbackend.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Location {

    @NotNull
    @Size(min = 2, message = "City name must have atleast two characters")
    private String city;

    @NotNull
    @Size(min = 4, message = "City name must have atleast three characters")
    private String country;
}
