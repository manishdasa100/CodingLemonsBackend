package com.codinglemonsbackend.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

import com.codinglemonsbackend.Dto.CurrentUserDto;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Entities.UserProfileEntity;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.SubmissionStats;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;
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
    private BadgeService badgeService;

    @Autowired
    private CompanyService companyService;

    @Value("${assets.domain}")
    private String ASSETS_DOMAIN;

    private static final String ASSETS_BASE_PATH = "users";

    public CurrentUserDto getCurrentUserInfo(String username) {
        UserProfileEntity entity = userProfileRepository.getCurrentUserInfo(username)
                .orElseThrow(() -> new UsernameNotFoundException("User profile not found for username: " + username));
        String path = entity.getProfilePictureId() != null
                ? "%s/%s".formatted(username, entity.getProfilePictureId())
                : "default/default_user_dp.jpg";
        String profilePictureUrl = URIUtils.createURI(ASSETS_DOMAIN, ASSETS_BASE_PATH, path).toString();
        return CurrentUserDto.builder()
                .username(username)
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .profilePictureUrl(profilePictureUrl)
                .build();
    }

    public UserProfileDto getUserProfile(String username) {
        UserProfileEntity entity = userProfileRepository.getUserProfile(username).orElseThrow(() -> new UsernameNotFoundException("User profile not found for username: " + username));
        UserProfileDto userProfile = mapper.map(entity, UserProfileDto.class);
        String path = entity.getProfilePictureId() != null
                ? "%s/%s".formatted(username, entity.getProfilePictureId())
                : "default/default_user_dp.jpg";
        String profilePictureUrl = URIUtils.createURI(
            ASSETS_DOMAIN, 
            ASSETS_BASE_PATH, 
            path).toString();
        userProfile.setProfilePictureUrl(profilePictureUrl);
        userProfile.setRank(userRankService.getRankByName(entity.getRank()).get());
        List<String> earnedBadgeIds = entity.getEarnedBadgeIds() != null ? entity.getEarnedBadgeIds() : List.of();
        userProfile.setEarnedBadges(badgeService.getEarnedBadges(earnedBadgeIds));
        return userProfile;
    }

    @Async("applicationAsyncExecutor")
    @EventListener
    public void onSubmitCodeCompleted(SubmitCodeCompletedEvent event) {
        if (event.getExecutionReport().status() != ExecutionStatus.ACC) return;
        String username = event.getSubmissionMetadata().getUsername();
        Integer points = event.getSubmissionMetadata().getSolutionPoints();
        userProfileRepository.incrementScore(username, points);
        log.info("Score updated for user {} by {} points", username, points);
    }

    @Async("applicationAsyncExecutor")
    @EventListener
    public void createUserProfile(UserAccountCreationEvent event) {
        System.out.println("Received user account creation event");
        UserDto newUser = event.getUser();
        createUserProfile(newUser);
    }

    public void createUserProfile(UserDto user) {
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

        if (newProfile.getCity()!= null && !newProfile.getCity().equals(currentProfile.getCity())) {
            System.out.println("CITY");
            updatePropertiesMap.put("city", newProfile.getCity());
        }

        if (newProfile.getCountry()!= null && !newProfile.getCountry().equals(currentProfile.getCountry())) {
            System.out.println("COUNTRY");
            updatePropertiesMap.put("country", newProfile.getCountry());
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
