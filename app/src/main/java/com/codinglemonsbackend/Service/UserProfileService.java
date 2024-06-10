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
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Entities.UserProfileEntity;
import com.codinglemonsbackend.Exceptions.ProfilePictureUploadFailureException;
import com.codinglemonsbackend.Properties.S3Buckets;
import com.codinglemonsbackend.Repository.UserProfileRepository;

@Service
public class UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ModelMapper mapper;
    
    @Autowired
    private S3Service s3Service;

    @Autowired
    private S3Buckets s3Buckets;

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
        if (newProfile.getAboutMe() != null && !newProfile.getAboutMe().equals(currentProfile.getAboutMe())) {
            System.out.println("ABOUT ME");
            updatePropertiesMap.put("aboutMe", newProfile.getAboutMe());
        }
        if (newProfile.getSchool() != null && !newProfile.getSchool().equals(currentProfile.getSchool())) {
            System.out.println("SCHOOL");
            updatePropertiesMap.put("school", newProfile.getSchool());
        }
        if (newProfile.getCountry() != null && !newProfile.getCountry().equals(currentProfile.getCountry())) {
            System.out.println("COUNTRY");
            updatePropertiesMap.put("country", newProfile.getCountry());
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

        Boolean profileUpdateStatus = userProfileRepository.updateUserProfile(username, updatePropertiesMap);

        return profileUpdateStatus;
    }

    public void uploadUserProfilePicture(String username, MultipartFile file) throws ProfilePictureUploadFailureException{

        String profilePictureId = UUID.randomUUID().toString();

        try {
            s3Service.putObject(
                s3Buckets.getCustomer(), 
                "profile-picture/%s/%s".formatted(username, profilePictureId), 
                file.getBytes()
            );
        } catch (Exception e) {
            throw new ProfilePictureUploadFailureException("Profile picture upload failed");
        }

        userProfileRepository.updateUserProfilePictureId(username, profilePictureId);
    }

    public byte[] getUserProfilePicture(UserProfileDto userProfile) {

        String profilePictureId = userProfile.getProfilePictureId();

        byte[] userProfilePicture = s3Service.getObject(
            s3Buckets.getCustomer(), 
            "profile-picture/%s/%s".formatted(userProfile.getUsername(), profilePictureId)
        );

        return userProfilePicture;
    }
}
