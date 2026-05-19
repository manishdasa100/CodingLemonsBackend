package com.codinglemonsbackend.Entities;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.SubmissionStats;

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

    @Transient
    public static final String ENTITY_COLLECTION_NAME = "UserProfile"; 
    
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

    private String city;

    private String country;

    private String companySlug;

    private String jobTitle;

    private SkillTags[] skillTags;

    @Builder.Default
    private List<String> earnedBadgeIds = new ArrayList<>();
}
