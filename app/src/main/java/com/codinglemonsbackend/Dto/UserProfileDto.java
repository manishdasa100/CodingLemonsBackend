package com.codinglemonsbackend.Dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codinglemonsbackend.Entities.UserLocation;
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
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class UserProfileDto {

    @JsonProperty(access = Access.READ_ONLY)
    private String username;

    private String firstName;

    private String lastName;
    
    @Size(min = 5, max = 20, 
    message = "User occupation must be between 5 and 20 characters")
    private String userOccupation;

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

    private String school;

    @Valid
    private UserLocation location;

    @Valid
    private List<UserWorkExperienceDto> workExperience;

    private Set<String> skillTags;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean profileOwner;

    @JsonProperty(access = Access.READ_ONLY)
    private Map<String, List<EarnedBadgeDto>> earnedBadges;
}
