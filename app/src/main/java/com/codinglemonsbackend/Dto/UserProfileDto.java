package com.codinglemonsbackend.Dto;

import com.codinglemonsbackend.Entities.SkillTags;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileDto {
    
    @JsonProperty(access = Access.READ_ONLY)
    private String username;

    private String firstName;

    private String lastName;

    @Pattern(regexp = "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$", message = "Not a valid email address")
    private String email;

    @Pattern(regexp = "^https:\\/\\/(www\\.)?github\\.com\\/[a-zA-Z0-9_-]+\\/?$", message = "Github url not valid")
    private String githubUrl;

    @Pattern(regexp = "^https:\\/\\/(www\\.)?x\\.com\\/[a-zA-Z0-9_-]+\\/?$", message = "Twitter url not valid")
    private String twitterUrl;

    @Pattern(regexp = "^https:\\/\\/www\\.linkedin\\.com\\/(?:in|pub|public-profile\\/in|public-profile\\/pub)\\/(?:[\\w]+-[\\w]+-[\\w]+)\\/?", message = "LinkedIn url not vlaid")
    private String linkedinUrl;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer score;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer ranking;

    @JsonIgnore
    private String profilePictureId;

    @Size(min = 5, max = 500)
    private String about;

    @Size(min = 10, max = 100)
    private String school;

    @Valid
    private Location location;

    private String company;

    @Size(min = 4, max = 40)
    private String jobTitle;

    private SkillTags[] skillTags;
}
