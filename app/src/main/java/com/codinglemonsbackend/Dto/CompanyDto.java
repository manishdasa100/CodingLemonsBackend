package com.codinglemonsbackend.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CompanyDto {

    @NotBlank(message = "Company name should not be empty")
    private String name;

    @JsonProperty(access = Access.READ_ONLY)
    private String slug;

    //@Pattern(regexp = "^https:\\/\\/(www\\.)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,10}(\\/[a-zA-Z0-9#?&%=._-]*)*$")
    @Pattern(
        regexp = "^https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&=]*)$",
        message = "Please enter a valid website URL (must start with http:// or https://)"
    )
    private String websiteLink;

    @JsonProperty(access = Access.READ_ONLY)
    private String companyLogoUri;

    public CompanyDto(String name, String slug, String websiteLink) {
        this(name, slug, websiteLink, null);
    }
}
