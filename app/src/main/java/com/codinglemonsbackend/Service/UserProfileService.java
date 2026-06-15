package com.codinglemonsbackend.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.CurrentUserDto;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Dto.UserWorkExperienceDto;
import com.codinglemonsbackend.Entities.UserLocation;
import com.codinglemonsbackend.Entities.UserProfileEntity;
import com.codinglemonsbackend.Entities.UserWorkExperience;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;
import com.codinglemonsbackend.Events.UserAccountCreationEvent;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Properties.S3Properties;
import com.codinglemonsbackend.Repository.UserProfileRepository;
import com.codinglemonsbackend.Utils.URIUtils;
import com.github.slugify.Slugify;

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

    @Autowired
    private Slugify slugify;

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
        if (entity.getWorkExperience() != null && !entity.getWorkExperience().isEmpty()) {
            userProfile.setWorkExperience(toWorkExperienceDtos(entity.getWorkExperience()));
        }
        return userProfile;
    }

    private List<UserWorkExperienceDto> toWorkExperienceDtos(List<UserWorkExperience> experiences) {
        List<String> slugs = experiences.stream()
                .map(UserWorkExperience::getCompanySlug)
                .collect(Collectors.toList());

        Map<String, CompanyDto> companyMap = companyService.getCompaniesBySlugMap(slugs);

        return experiences.stream().map(exp -> {
            CompanyDto companyDto = companyMap.getOrDefault(
                exp.getCompanySlug(),
                new CompanyDto(exp.getCompanyName(), exp.getCompanySlug(), null)
            );
            return new UserWorkExperienceDto(companyDto, exp.getJobTitle(), exp.getStartYear(), exp.getEndYear());
        }).collect(Collectors.toList());
    }

    @Async("applicationAsyncExecutor")
    @EventListener
    public void onSubmitCodeCompleted(SubmitCodeCompletedEvent event) {
        if (event.getExecutionReport().status() != ExecutionStatus.ACC || !event.getIsNewSolve()) return;
        String username = event.getSubmissionMetadata().getUsername();
        Integer points = event.getSubmissionMetadata().getSolutionPoints();
        userProfileRepository.incrementScoreAndAddLanguageSkill(username, points, event.getSubmissionMetadata().getLanguage().name());
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
            String newFirstName = newProfile.getFirstName().trim();
            if (newFirstName.isEmpty()) {
                throw new IllegalArgumentException("First name cannot be empty");
            }
            updatePropertiesMap.put("firstName", newFirstName);
        }

        if (newProfile.getLastName() != null && !newProfile.getLastName().equals(currentProfile.getLastName())) {
            String newLastName = newProfile.getLastName().trim();
            if (newLastName.isEmpty()) {
                newLastName = null; // Allow last name to be set to null
            }
            updatePropertiesMap.put("lastName", newLastName);
        }

        if (newProfile.getEmail() != null && !newProfile.getEmail().equals(currentProfile.getEmail())) {
            String newEmail = newProfile.getEmail().trim();
            if (newEmail.isEmpty()) {
                newEmail = null; // Allow email to be set to null
            }
            updatePropertiesMap.put("email", newEmail);
        }

        if (newProfile.getGithubUrl() != null && !newProfile.getGithubUrl().equals(currentProfile.getGithubUrl())) {
            String newGithubUrl = newProfile.getGithubUrl().trim();
            if (newGithubUrl.isEmpty()) {
                newGithubUrl = null; // Allow GitHub URL to be set to null
            }
            updatePropertiesMap.put("githubUrl", newGithubUrl);
        }

        if (newProfile.getTwitterUrl() != null && !newProfile.getTwitterUrl().equals(currentProfile.getTwitterUrl())) {
            String newTwitterUrl = newProfile.getTwitterUrl().trim();
            if (newTwitterUrl.isEmpty()) {
                newTwitterUrl = null; // Allow Twitter URL to be set to null
            }
            updatePropertiesMap.put("twitterUrl", newTwitterUrl);
        }

        if (newProfile.getLinkedinUrl() != null && !newProfile.getLinkedinUrl().equals(currentProfile.getLinkedinUrl())) {
            String newLinkedinUrl = newProfile.getLinkedinUrl().trim();
            if (newLinkedinUrl.isEmpty()) {
                newLinkedinUrl = null; // Allow LinkedIn URL to be set to null
            }
            updatePropertiesMap.put("linkedinUrl", newLinkedinUrl);
        }

        if (newProfile.getUserOccupation() != null && !newProfile.getUserOccupation().equals(currentProfile.getUserOccupation())) {
            String newUserOccupation = newProfile.getUserOccupation().trim();
            if (newUserOccupation.isEmpty()) {
                newUserOccupation = null; // Allow user occupation to be set to null
            }
            updatePropertiesMap.put("userOccupation", newUserOccupation);
        }

        if (newProfile.getSchool() != null && !newProfile.getSchool().equals(currentProfile.getSchool())) {
            String newSchool = newProfile.getSchool().trim();
            if (newSchool.isEmpty()) {
                newSchool = null; // Allow school to be set to null
            }
            updatePropertiesMap.put("school", newSchool);
        }

        if (newProfile.getLocation() != null && !newProfile.getLocation().equals(currentProfile.getLocation())) {
            UserLocation newLocation = newProfile.getLocation().getCountry() == null
                    ? null
                    : newProfile.getLocation();
            updatePropertiesMap.put("location", newLocation);
        }

        if (newProfile.getSkillTags() != null && !newProfile.getSkillTags().equals(currentProfile.getSkillTags())) {
            updatePropertiesMap.put("skillTags", newProfile.getSkillTags());
        }

        if (newProfile.getWorkExperience() != null) {
            List<UserWorkExperience> newWorkExperience = newProfile.getWorkExperience().stream()
                    .map(dto -> new UserWorkExperience(
                            dto.getCompanyName().trim(),
                            slugify.slugify(dto.getCompanyName().trim()),
                            dto.getJobTitle().trim(),
                            dto.getStartYear(),
                            dto.getEndYear()
                    )).collect(Collectors.toList());
            updatePropertiesMap.put("workExperience", newWorkExperience);
        }
        
        if (updatePropertiesMap.isEmpty()){
            return false; // No updates to apply
        }

        return userProfileRepository.updateUserProfile(username, updatePropertiesMap);
    }

    public void addWorkExperience(String username, UserWorkExperience experience) {
        userProfileRepository.upsertWorkExperience(username, experience);
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
