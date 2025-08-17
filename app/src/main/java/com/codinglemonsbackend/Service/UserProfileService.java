package com.codinglemonsbackend.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Entities.UserProfileEntity;
import com.codinglemonsbackend.Events.UserAccountCreationEvent;
import com.codinglemonsbackend.Events.UserProfileUpdateEvent;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Properties.S3Properties;
import com.codinglemonsbackend.Repository.UserProfileRepository;
import com.codinglemonsbackend.Utils.URIUtils;

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
    private S3Properties s3Properties;

    @Autowired
    private UserRankService userRankService;

    @Autowired
    private CompanyService companyService;

    @Value("${assets.domain}")
    private String ASSETS_DOMAIN;

    private static final String ASSETS_BASE_PATH = "users";

    public UserProfileDto getUserProfile(String username) {
        Optional<UserProfileEntity> profile = userProfileRepository.getUserProfile(username);
        if (profile.isEmpty()) throw new UsernameNotFoundException("User profile not found");
        UserProfileEntity entity = profile.get();
        UserProfileDto userProfile = mapper.map(entity, UserProfileDto.class);
        System.out.println("USER PROFILE: " + userProfile);
        String path = "default";
        String profilePictureId = "default_user_dp.jpg";
        if (entity.getProfilePictureId() != null) {
            path = entity.getUsername();
            profilePictureId = entity.getProfilePictureId();
        }
        String profilePictureUrl = URIUtils.createURI(
            ASSETS_DOMAIN, 
            ASSETS_BASE_PATH, 
            path, 
            profilePictureId).toString();
        userProfile.setProfilePictureUrl(profilePictureUrl);
        userProfile.setRank(userRankService.getRankByName(entity.getRank()).get());
        return userProfile;
    }

    @Async("applicationAsyncExecutor")
    @EventListener
    public void createUserProfile(UserAccountCreationEvent event) {
        System.out.println("Received user account creation event");
        UserEntity newUser = event.getUser();
        createUserProfile(newUser);
    }

    public void createUserProfile(UserEntity user) {
        UserProfileEntity userProfileEntity = UserProfileEntity.builder()
                                                .username(user.getUsername())
                                                .firstName(user.getFirstName())
                                                .lastName(user.getLastName())
                                                .email(user.getEmail())
                                                .score(0)
                                                .rank(userRankService.getInitialRank().getRankName())
                                                .build();
        userProfileRepository.saveUserProfile(userProfileEntity);
    }
    public Boolean updateUserProfile(String username, UserProfileDto newProfile) {

        System.out.println("Received user profile update event");

        UserProfileDto currentProfile = getUserProfile(username);
        
        Map<String, Object> updatePropertiesMap = new HashMap<>();

        if (newProfile.getFirstName() != null && !newProfile.getFirstName().equals(currentProfile.getFirstName())) {
            System.out.println("FIRST NAME");
            String newFirstName = newProfile.getFirstName().trim();
            if (newFirstName.isEmpty()) {
                throw new IllegalArgumentException("First name cannot be empty");
            }
            updatePropertiesMap.put("firstName", newFirstName);
        }

        if (newProfile.getLastName() != null && !newProfile.getLastName().equals(currentProfile.getLastName())) {
            System.out.println("LAST NAME");
            String newLastName = newProfile.getLastName().trim();
            if (newLastName.isEmpty()) {
                newLastName = null; // Allow last name to be set to null
            }
            updatePropertiesMap.put("lastName", newLastName);
        }

        if (newProfile.getEmail() != null && !newProfile.getEmail().equals(currentProfile.getEmail())) {
            System.out.println("EMAIL");
            String newEmail = newProfile.getEmail().trim();
            if (newEmail.isEmpty()) {
                newEmail = null; // Allow email to be set to null
            }
            updatePropertiesMap.put("email", newEmail);
        }

        if (newProfile.getGithubUrl() != null && !newProfile.getGithubUrl().equals(currentProfile.getGithubUrl())) {
            System.out.println("GUTHUB URL");
            String newGithubUrl = newProfile.getGithubUrl().trim();
            if (newGithubUrl.isEmpty()) {
                newGithubUrl = null; // Allow GitHub URL to be set to null
            }
            updatePropertiesMap.put("githubUrl", newGithubUrl);
        }

        if (newProfile.getTwitterUrl() != null && !newProfile.getTwitterUrl().equals(currentProfile.getTwitterUrl())) {
            System.out.println("TWITTER URL");
            String newTwitterUrl = newProfile.getTwitterUrl().trim();
            if (newTwitterUrl.isEmpty()) {
                newTwitterUrl = null; // Allow Twitter URL to be set to null
            }
            updatePropertiesMap.put("twitterUrl", newTwitterUrl);
        }

        if (newProfile.getLinkedinUrl() != null && !newProfile.getLinkedinUrl().equals(currentProfile.getLinkedinUrl())) {
            System.out.println("LINKEDIN URL");
            String newLinkedinUrl = newProfile.getLinkedinUrl().trim();
            if (newLinkedinUrl.isEmpty()) {
                newLinkedinUrl = null; // Allow LinkedIn URL to be set to null
            }
            updatePropertiesMap.put("linkedinUrl", newLinkedinUrl);
        }

        if (newProfile.getAbout() != null && !newProfile.getAbout().equals(currentProfile.getAbout())) {
            System.out.println("ABOUT ME");
            String newAbout = newProfile.getAbout().trim();
            if (newAbout.isEmpty()) {
                newAbout = null; // Allow about section to be set to null
            }
            updatePropertiesMap.put("about", newAbout);
        }

        if (newProfile.getSchool() != null && !newProfile.getSchool().equals(currentProfile.getSchool())) {
            System.out.println("SCHOOL");
            String newSchool = newProfile.getSchool().trim();
            if (newSchool.isEmpty()) {
                newSchool = null; // Allow school to be set to null
            }
            updatePropertiesMap.put("school", newSchool);
        }

        if (newProfile.getLocation() != null && !newProfile.getLocation().equals(currentProfile.getLocation())) {
            System.out.println("LOCATION");
            updatePropertiesMap.put("location", newProfile.getLocation());
        }

        if (newProfile.getCompanySlug() != null && !newProfile.getCompanySlug().equals(currentProfile.getCompanySlug())) {
            System.out.println("COMPANY");
            String companySlug = newProfile.getCompanySlug().trim();
            if (companySlug.isEmpty()) {
                companySlug = null; 
            }else if (!companyService.isValidCompany(companySlug)) {
                throw new IllegalArgumentException("Invalid company slug: " + companySlug);
            }
            updatePropertiesMap.put("companySlug", companySlug);
        }

        if (newProfile.getJobTitle() != null && !newProfile.getJobTitle().equals(currentProfile.getJobTitle())) {
            System.out.println("JOB TITLE");
            String newJobTitle = newProfile.getJobTitle().trim();
            if (newJobTitle.isEmpty()) {
                newJobTitle = null;
            }
            updatePropertiesMap.put("jobTitle", newProfile.getJobTitle());
        }

        if (newProfile.getSkillTags() != null && !Arrays.equals(newProfile.getSkillTags(), currentProfile.getSkillTags())) {
            System.out.println("SKILL TAGS");
            updatePropertiesMap.put("skillTags", newProfile.getSkillTags());
        }
        
        if (updatePropertiesMap.isEmpty()){
            return false; // No updates to apply
        }

        return userProfileRepository.updateUserProfile(username, updatePropertiesMap);
    }

    public void uploadUserProfilePicture(String username, byte[] imageFile) throws FileUploadFailureException{

        String profilePictureId = UUID.randomUUID().toString();
        Boolean s3Uploaded = false;
        try {
            s3Service.putObject(
                s3Properties.getBucket(), 
                ASSETS_BASE_PATH + "/" + username + "/" + profilePictureId, 
                imageFile
            );
            s3Uploaded = true;
            userProfileRepository.updateUserProfile(username, Map.of("profilePictureId", profilePictureId));
        } catch (Exception e) {
            if (s3Uploaded) {
                // Rollback S3
                try {
                    s3Service.deleteObject(s3Properties.getBucket(), "profile-picture/%s/%s".formatted(username, profilePictureId));
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

}
