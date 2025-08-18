package com.codinglemonsbackend.Dto;

import com.codinglemonsbackend.Entities.SkillTags;
import com.codinglemonsbackend.Validation.CrossFieldValidation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@CrossFieldValidation(
    rules = {
        @CrossFieldValidation.FieldRule(
            field = "companySlug",
            dependsOn = "jobTitle", 
            type = CrossFieldValidation.ValidationType.BOTH_OR_NEITHER,
            message = "Company slug and job title must be provided together"
        ),
        @CrossFieldValidation.FieldRule(
            field = "country",
            dependsOn = "city",
            type = CrossFieldValidation.ValidationType.REQUIRED_IF_NOT_EMPTY,
            message = "Country must be provided if city is specified"
        )
    }
)
public class UserProfileDto {

    // public static record Location(
    //     @NotBlank(message = "City cannot be blank")
    //     @Size(min = 2, message = "City name must have at least two characters")
    //     String city,
        
    //     @NotBlank(message = "Country cannot be blank")
    //     @Size(min = 4, message = "Country name must have at least four characters")
    //     String country
    // ) {}
    
    @JsonProperty(access = Access.READ_ONLY)
    private String username;

    private String firstName;

    private String lastName;

    @Email(message = "Email must be a valid email address")
    private String email;

    @Pattern(regexp = "^https:\\/\\/(www\\.)?github\\.com\\/[a-zA-Z0-9_-]+\\/?$", message = "Github profile url not valid")
    private String githubUrl;

    @Pattern(regexp = "^https:\\/\\/(www\\.)?(x|twitter)\\.com\\/[a-zA-Z0-9_-]+\\/?$", message = "Twitter profile url not valid")
    private String twitterUrl;

    @Pattern(regexp = "^https:\\/\\/www\\.linkedin\\.com\\/(in|pub)\\/[a-zA-Z0-9_-]+\\/?$", message = "LinkedIn profile url not vlaid")
    private String linkedinUrl;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer score;

    @JsonProperty(access = Access.READ_ONLY)
    private UserRankDto rank;

    @JsonProperty(access = Access.READ_ONLY)
    private String profilePictureUrl;

    @Size(min = 20, max = 500, 
    message = "About section must be between 20 and 500 characters")
    private String about;

    private String school;

    // @Valid
    // private Location location;

    @Size(min = 2, max = 50, 
    message = "City name must be between 2 and 50 characters")
    private String city;

    @Size(min = 4, max = 50, 
    message = "Country name must be between 2 and 50 characters")
    private String country;

    private String companySlug;

    @Size(min = 5, max = 40)
    private String jobTitle;

    private SkillTags[] skillTags;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean profileOwner;
}
