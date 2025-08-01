package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.Location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "UserProfile")
public class UserProfileEntity {
    
    @Id
    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String profilePictureId;

    private String githubUrl;

    private String twitterUrl;

    private String linkedinUrl;

    private Integer score;

    private String rank;

    private String avatarUrl;

    private String about;

    private String school;

    private Location location;

    private String company;

    private String jobTitle;

    private SkillTags[] skillTags;
}
