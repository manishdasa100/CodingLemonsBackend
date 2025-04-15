package com.codinglemonsbackend.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Entities.UserProfileEntity;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Properties.S3Buckets;
import com.codinglemonsbackend.Repository.UserProfileRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ModelMapper mapper;
    
    @Autowired
    private S3Service s3Service;

    @Autowired
    private S3Buckets s3Buckets;

    @Autowired
    private UserRankService userRankService;

    public UserProfileDto getUserProfile(String username) {
        Optional<UserProfileEntity> userProfileEntity = userProfileRepository.getUserProfile(username);
        if (userProfileEntity.isEmpty()) throw new UsernameNotFoundException("User profile not found");
        return mapper.map(userProfileEntity.get(), UserProfileDto.class); 
    }

    public void createAndSaveUserProfile(UserEntity user) {
        UserProfileDto userProfileDto = UserProfileDto.builder()
                                        .username(user.getUsername())
                                        .firstName(user.getFirstName())
                                        .lastName(user.getLastName())
                                        .email(user.getEmail())
                                        .score(0)
                                        .ranking(userRankService.getInitialRank().getRankName())
                                        .build();
        UserProfileEntity userProfileEntity = mapper.map(userProfileDto, UserProfileEntity.class);
        userProfileRepository.saveUserProfile(userProfileEntity);
    }

    public boolean updateUserProfile(UserProfileDto currentProfile, UserProfileDto newProfile) {

        String username = currentProfile.getUsername();
        
        Map<String, Object> updatePropertiesMap = new HashMap<>();

        if (newProfile.getFirstName() != null && !newProfile.getFirstName().equals(currentProfile.getFirstName())) {
            System.out.println("FIRST NAME");
            updatePropertiesMap.put("firstName", newProfile.getFirstName());
        }
        if (newProfile.getLastName() != null && !newProfile.getLastName().equals(currentProfile.getLastName())) {
            System.out.println("LAST NAME");
            updatePropertiesMap.put("lastName", newProfile.getLastName());
        }
        if (newProfile.getEmail() != null && !newProfile.getEmail().equals(currentProfile.getEmail())) {
            System.out.println("EMAIL");
            updatePropertiesMap.put("email", newProfile.getEmail());
        }
        if (newProfile.getGithubUrl() != null && !newProfile.getGithubUrl().equals(currentProfile.getGithubUrl())) {
            System.out.println("GUTHUB URL");
            updatePropertiesMap.put("githubUrl", newProfile.getGithubUrl());
        }
        if (newProfile.getTwitterUrl() != null && !newProfile.getTwitterUrl().equals(currentProfile.getTwitterUrl())) {
            System.out.println("TWITTER URL");
            updatePropertiesMap.put("twitterUrl", newProfile.getTwitterUrl());
        }
        if (newProfile.getLinkedinUrl() != null && !newProfile.getLinkedinUrl().equals(currentProfile.getLinkedinUrl())) {
            System.out.println("LINKEDIN URL");
            updatePropertiesMap.put("linkedinUrl", newProfile.getLinkedinUrl());
        }
        if (newProfile.getAbout() != null && !newProfile.getAbout().equals(currentProfile.getAbout())) {
            System.out.println("ABOUT ME");
            updatePropertiesMap.put("about", newProfile.getAbout());
        }
        if (newProfile.getSchool() != null && !newProfile.getSchool().equals(currentProfile.getSchool())) {
            System.out.println("SCHOOL");
            updatePropertiesMap.put("school", newProfile.getSchool());
        }
        if (newProfile.getLocation() != null && !newProfile.getLocation().equals(currentProfile.getLocation())) {
            System.out.println("LOCATION");
            updatePropertiesMap.put("location", newProfile.getLocation());
        }
        if (newProfile.getCompany() != null && !newProfile.getCompany().equals(currentProfile.getCompany())) {
            System.out.println("COMPANY");
            updatePropertiesMap.put("company", newProfile.getCompany());
        }
        if (newProfile.getJobTitle() != null && !newProfile.getJobTitle().equals(currentProfile.getJobTitle())) {
            System.out.println("JOB TITLE");
            updatePropertiesMap.put("jobTitle", newProfile.getJobTitle());
        }
        if (newProfile.getSkillTags() != null && !Arrays.equals(newProfile.getSkillTags(), currentProfile.getSkillTags())) {
            System.out.println("SKILL TAGS");
            updatePropertiesMap.put("skillTags", newProfile.getSkillTags());
        }

        Boolean profileUpdateStatus = false;
        
        if (!updatePropertiesMap.isEmpty()){
            profileUpdateStatus = userProfileRepository.updateUserProfile(username, updatePropertiesMap);
        }

        return profileUpdateStatus;
    }

    public void uploadUserProfilePicture(String username, byte[] imageFileBytes) throws FileUploadFailureException{

        String profilePictureId = UUID.randomUUID().toString();
        Boolean s3Uploaded = false;
        try {
            s3Service.putObject(
                s3Buckets.getImages(), 
                "profile-picture/%s/%s".formatted(username, profilePictureId), 
                imageFileBytes
            );
            s3Uploaded = true;
            userProfileRepository.updateUserProfile(username, Map.of("profilePictureId", profilePictureId));
        } catch (Exception e) {
            if (s3Uploaded) {
                // Rollback S3
                try {
                    s3Service.deleteObject(s3Buckets.getImages(), "profile-picture/%s/%s".formatted(username, profilePictureId));
                } catch (Exception deleteException) {
                    log.error("Failed to delete orphaned profile picture with id {} from S3 with message", profilePictureId, deleteException);
                }

                log.error("Failed to update profilePictureId for user:{} in database", username, e);
                throw e;
            } else {
                log.error("Failed to upload profile picture to S3 for user:{}",username, e);
                throw new FileUploadFailureException("Profile picture upload to S3 failed with message: " + e.getMessage());
            }
        }

    }

    public byte[] getUserProfilePicture(UserProfileDto userProfile) {

        String profilePictureId = userProfile.getProfilePictureId();

        byte[] userProfilePicture = s3Service.getObject(
            s3Buckets.getImages(), 
            "profile-picture/%s/%s".formatted(userProfile.getUsername(), profilePictureId)
        );

        return userProfilePicture;
    }
}
