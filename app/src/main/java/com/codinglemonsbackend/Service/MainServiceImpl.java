package com.codinglemonsbackend.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.BadgeDto;
import com.codinglemonsbackend.Dto.CurrentUserDto;
import com.codinglemonsbackend.Dto.UserStreakDto;
import com.codinglemonsbackend.Dto.UserSubmissionStatusDto;
import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.EarnedBadgeDto;
import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDto.Difficulty;
import com.codinglemonsbackend.Dto.ProblemExecutionDetails;
import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Dto.ProblemOfTheDayDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.ProblemsPage;
import com.codinglemonsbackend.Dto.StudyPlanOperation;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Dto.UserSubmissionStatus;
import com.codinglemonsbackend.Entities.SubmissionEntity;
import com.codinglemonsbackend.Entities.Topic;
import com.codinglemonsbackend.Entities.UserWorkExperience;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Entities.UserStreakEntity;
import com.codinglemonsbackend.Entities.UserStudyPlanProgress;
import com.codinglemonsbackend.Repository.SubmissionRepository;
import com.codinglemonsbackend.Repository.TopicRepository;
import com.codinglemonsbackend.Repository.UserProfileRepository;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;
import com.codinglemonsbackend.Events.UserProfileUpdateEvent;
import com.codinglemonsbackend.Entities.ProblemListEntity;
import com.codinglemonsbackend.Entities.UserSubmissionStatusEntity;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Payloads.LikeRequest;
import com.codinglemonsbackend.Payloads.LikesData;
import com.codinglemonsbackend.Payloads.SubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Payloads.UpdateProblemListRequest;
import com.codinglemonsbackend.Utils.ImageUtils;
import com.codinglemonsbackend.Utils.ImageUtils.ImageDimension;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.slugify.Slugify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.constraints.NotBlank;

@Service
@Slf4j
@RequiredArgsConstructor
public class MainServiceImpl{

    private static final Integer MAX_PROBLEMSET_SIZE = 100;
    private static final Integer DEFAULT_PROBLEMSET_SIZE = 10;

    private final MeterRegistry meterRegistry;

    private final Counter codeExecutionCounter;

    private final Counter problemSubmissionCounter;

    private final LikeService likeService;

    private final UserProfileService userProfileService;

    private final ProblemRepositoryService problemRepositoryService;

    private final ProblemListRepositoryService problemListRepositoryService;

    private final StudyPlanRepositoryService studyPlanRepositoryService;

    private final UserStreakService userStreakService;

    private final SubmissionService submissionService;

    private final SubmissionServiceRegistry submissionServiceRegistry;

    private final ProblemOfTheDayService problemOfTheDayService;

    private final UserSubmissionStatusService userSubmissionStatusService;

    private final BadgeService badgeService;

    private final CompanyService companyService;

    private final TopicRepository topicRepository;

    private final UserProfileRepository userProfileRepository;

    private final RedisService redisService;

    private final ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper;

    private final ModelMapper modelMapper;

    private final String PENDING_SUBMISSION_REDIS_KEY_PREFIX = "submission:report";

    private UserEntity getCurrentlySignedInUser(){
        return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private List<Set<Integer>> getSubmissionStatusofUser(){
        String username = getCurrentlySignedInUser().getUsername();
        UserSubmissionStatusEntity submissionStatus = userSubmissionStatusService.getSubmissionStatus(username);
        if (submissionStatus == null) {
            return List.of(Set.of(), Set.of());
        }
        return List.of(submissionStatus.getSolvedProblemIds(), submissionStatus.getAttemptedProblemIds());
    }

    public ProblemsPage getProblemSet(String difficultyStr, String topicsStr, String companysStr, Integer pageNo, Integer size, Boolean isAdmin) {

        if (pageNo < 0) pageNo = 0;
        if (size <= 0) size = DEFAULT_PROBLEMSET_SIZE; 
        if (size > MAX_PROBLEMSET_SIZE) size = MAX_PROBLEMSET_SIZE;

        ProblemsPage problemPage = null;

        if (StringUtils.isBlank(difficultyStr) && StringUtils.isBlank(topicsStr) && StringUtils.isBlank(companysStr)) problemPage = problemRepositoryService.getAllProblems(pageNo, size, isAdmin);
        else problemPage = problemRepositoryService.getFilteredProblems(difficultyStr, topicsStr, companysStr, pageNo, size, isAdmin);

        List<Set<Integer>> acceptedAndAttempted = getSubmissionStatusofUser();
        Set<Integer> acceptedIds = acceptedAndAttempted.get(0);
        Set<Integer> attemptedIds = acceptedAndAttempted.get(1);

        List<ProblemDto> problemsWithStatus = problemPage.entities().stream().map(e -> {
            UserSubmissionStatus status;
            if (acceptedIds.contains(e.getId())) status = UserSubmissionStatus.ACC;
            else if (attemptedIds.contains(e.getId())) status = UserSubmissionStatus.ATT;
            else status = UserSubmissionStatus.NATT;
            e.setUserSubmissionStatus(status);
            return e;
        }).collect(Collectors.toList());

        return new ProblemsPage(problemPage.total(), problemsWithStatus);
    }

    public ProblemDto getProblem(Integer id) {
        ProblemDto problemDto = problemRepositoryService.getProblemById(id);

        List<Set<Integer>> acceptedAndAttempted = getSubmissionStatusofUser();
        Set<Integer> acceptedIds = acceptedAndAttempted.get(0);
        Set<Integer> attemptedIds = acceptedAndAttempted.get(1);

        UserSubmissionStatus status;
        if (acceptedIds.contains(problemDto.getId())) status = UserSubmissionStatus.ACC;
        else if (attemptedIds.contains(problemDto.getId())) status = UserSubmissionStatus.ATT;
        else status = UserSubmissionStatus.NATT;

        problemDto.setUserSubmissionStatus(status);

        //redisService.storeValue(RedisService.PROBLEM_LIKES_COUNT_CACHE_PREFIX+Integer.toString(id), Integer.toString(problemDto.getLikes()), 300);

        return problemDto;
    }
    
    public LikesData getProblemLikesData(Integer id) {
        // Get the problem like count from redis db. 
        // If not present then get it from mongodb database and store it in redis db
        // Convert the count integer to string. If like count is in thousands then divide it by 1000/ if in millions then divide it by 1000000
        String problemLikes = null;

        if (redisService.keyExist(RedisService.PROBLEM_LIKES_COUNT_CACHE_PREFIX+Integer.toString(id))) {
            problemLikes = redisService.getValue(RedisService.PROBLEM_LIKES_COUNT_CACHE_PREFIX+Integer.toString(id));
        } else {
            Integer likesCount = getProblem(id).getLikes();
            problemLikes = formatLikeCount(likesCount);
            redisService.storeValue(RedisService.PROBLEM_LIKES_COUNT_CACHE_PREFIX+Integer.toString(id), problemLikes, 300);
        }
        
        // Get the problem like status for the currently signed in user and cache the result in redis
        String username = getCurrentlySignedInUser().getUsername();
        Boolean likeStatus = null;
        String redisLikeStatusKey = RedisService.USER_LIKE_STATUS_CACHE_PREFIX+username;

        if (redisService.hashKeyExists(redisLikeStatusKey, Integer.toString(id))) {
            likeStatus = Boolean.parseBoolean(redisService.getHashValue(redisLikeStatusKey, Integer.toString(id)));
        } else {
            likeStatus = likeService.getLikeStatus(username, id);
            redisService.storeHash(redisLikeStatusKey, Integer.toString(id), likeStatus.toString(), 300);
        }
        
        return new LikesData(problemLikes, likeStatus);
    }
        
    private String formatLikeCount(Integer likeCount) {
        if (likeCount < 1000) return Integer.toString(likeCount);
        if (likeCount < 1000000) return Integer.toString(likeCount/1000) + "K";
        return Integer.toString(likeCount/1000000) + "M";
    }

    public void createProblemList(ProblemListDto problemListDto) throws DuplicateResourceException {
        UserEntity currentSignedInUserEntity = getCurrentlySignedInUser();
        ProblemListEntity enitityToSave = modelMapper.map(problemListDto, ProblemListEntity.class);
        enitityToSave.setCreator(currentSignedInUserEntity.getUsername());
        problemListRepositoryService.saveProblemList(enitityToSave);
    }

    public void addProblemToList(String listId, Set<Integer> problemIds) {
        Set<Integer> validProblemIds = problemRepositoryService.getProblemsByIds(new ArrayList<>(problemIds), false)
                                        .stream()
                                        .map(ProblemDto::getId)
                                        .collect(Collectors.toSet());

        if (validProblemIds.isEmpty()) {
            throw new IllegalArgumentException("No valid problem ids found");
        }

        problemListRepositoryService.addProblemToProblemList(listId, validProblemIds);
    }

    public void removeProblemFromList(String listId, Set<Integer> problemIds) {
        problemListRepositoryService.removeProblemFromProblemList(listId, problemIds);
    }

    public void updateProblemList(UpdateProblemListRequest newListDetails) {
        problemListRepositoryService.updateProblemList(newListDetails);
    }

    public List<ProblemListDto> getUserFavorites(String username) {
        return problemListRepositoryService.getProblemLists(username);
    }

    public ProblemListDto getAProblemList(String username, String name) {
        return problemListRepositoryService.getAProblemList(username, name);
    }

    public List<ProblemListDto> getAllGlobalProblemLists() {
        return problemListRepositoryService.getAllGlobalProblemLists();
    }

    public void likeProblem(LikeRequest request) throws DuplicateResourceException {
        Integer problemId = request.getProblemId();
        
        // Check if the problemId exists

        Boolean problemExist = redisService.keyExist(RedisService.PROBLEM_LIKES_COUNT_CACHE_PREFIX+Integer.toString(problemId)) || problemRepositoryService.problemExists(problemId);
        
        if (!problemExist) {
            throw new NoSuchElementException("Problem with id " + problemId + " not found");
        }
        
        System.out.println("Problem id exists");
        String username = getCurrentlySignedInUser().getUsername();
        Boolean isLike = request.getIsLike();
        if (isLike) {
            likeService.likeProblem(username, problemId);
        } else {
            likeService.dislikeProblem(username, problemId);
        }
         
    }

    public String submitCode(SubmitCodeRequestPayload payload, String listId) {
        ProblemDto problemDto = getProblem(payload.getProblemId());
        if (problemDto.getStatus() != ProblemStatus.PUBLISHED) {
            return "Problem is not published";
        }
        
        // Track code execution metrics
        if (payload.getIsRunCode()) {
            codeExecutionCounter.increment();
            log.info("Code execution tracked for user: {} problem: {}", 
                    getCurrentlySignedInUser().getUsername(), payload.getProblemId());
        } else {
            problemSubmissionCounter.increment();
            log.info("Problem submission tracked for user: {} problem: {}", 
                    getCurrentlySignedInUser().getUsername(), payload.getProblemId());
        }
        
        ProblemExecutionDetails executionDetails = ProblemExecutionDetails.builder()
                                                .cpuTimeLimit(problemDto.getCpuTimeLimit())
                                                .memoryLimit(problemDto.getMemoryLimit())
                                                .stackLimit(problemDto.getStackLimit())
                                                .build();

        SubmissionMetadata submissionMetadata = SubmissionMetadata.builder()
                                                .problemId(payload.getProblemId())
                                                .solutionPoints(problemDto.getDifficulty().getPoints())
                                                .difficulty(problemDto.getDifficulty())
                                                .executionDetails(executionDetails)
                                                .language(payload.getLanguage())
                                                .username(getCurrentlySignedInUser().getUsername())
                                                .userCode(payload.getUserCode())
                                                .isRunCode(payload.getIsRunCode())
                                                .b64Encoded(payload.getB64Encoded())
                                                .listId(listId)
                                                .build();

        String submissionJobId = submissionService.queueSubmission(submissionMetadata);
        log.info("Storing submission {} in Redis with key: {}", submissionJobId, PENDING_SUBMISSION_REDIS_KEY_PREFIX);

        try {
            String metadataJson = objectMapper.writeValueAsString(submissionMetadata);
            redisService.storeHash(PENDING_SUBMISSION_REDIS_KEY_PREFIX + ":" + submissionJobId, "status", PendingOrdersStatus.QUEUED.name(), 3600);
            redisService.storeHash(PENDING_SUBMISSION_REDIS_KEY_PREFIX + ":" + submissionJobId, "submissionMetadata", metadataJson, 3600);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize submission metadata for job {}. Error: {}", submissionJobId, e);
            throw new RuntimeException("Failed to store submission metadata in Redis", e);
        }

        return submissionJobId;
    }

    public SubmissionResponsePayload check(String submissionJobId) {
        
        String redisKey = PENDING_SUBMISSION_REDIS_KEY_PREFIX + ":" + submissionJobId;

        if (!redisService.keyExist(redisKey)) {
            log.info("No pending submission found in Redis for id {}", submissionJobId);
            throw new NoSuchElementException("No pending submission found for id " + submissionJobId);
        }

        PendingOrdersStatus status = PendingOrdersStatus.valueOf(
            redisService.getHashValue(redisKey, "status")
        );

        ExecutionReportDto executionReport = null;

        if (status.equals(PendingOrdersStatus.COMPLETED)) {
            // Read all required fields from Redis in one pass
            String executionReportJson = redisService.getHashValue(redisKey, "executionReport");
            String metadataJson        = redisService.getHashValue(redisKey, "submissionMetadata");
            ExecutorWorkerType workerType = ExecutorWorkerType.valueOf(
                redisService.getHashValue(redisKey, "workerType")
            );

            SubmissionMetadata submissionMetadata;
            try {
                submissionMetadata = objectMapper.readValue(metadataJson, SubmissionMetadata.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize submission metadata for job {}", submissionJobId, e);
                throw new RuntimeException("Failed to read submission metadata from Redis", e);
            }

            // Route to the executor-specific service to normalize the raw report
            executionReport = submissionServiceRegistry.getService(workerType)
                                             .constructExecutionReport(executionReportJson);

            // Persist submission to db for submit code
            if (!submissionMetadata.getIsRunCode()) {
                submissionService.saveSubmission(executionReport, submissionMetadata);
                log.info("Persisted submission {} for user {}", submissionJobId,
                        submissionMetadata.getUsername());
                Boolean isNewSolve = false;
                if (executionReport.status().equals(ExecutionStatus.ACC)) {
                    isNewSolve = userSubmissionStatusService.addToSolvedAndRemoveFromAttempted(
                            submissionMetadata.getUsername(), submissionMetadata.getProblemId(),
                            submissionMetadata.getDifficulty().name(),
                            submissionMetadata.getLanguage().name().toLowerCase());
                } else {
                    userSubmissionStatusService.addToAttemptedIfNotSolved(
                            submissionMetadata.getUsername(), submissionMetadata.getProblemId());
                }
                eventPublisher.publishEvent(new SubmitCodeCompletedEvent(this, executionReport, submissionMetadata, isNewSolve));
            }
            redisService.deleteKey(redisKey);
            return new SubmissionResponsePayload(PendingOrdersStatus.COMPLETED, executionReport);

        } else if(status.equals(PendingOrdersStatus.FAILED)){
            ExecutorWorkerType workerType = ExecutorWorkerType.valueOf(
                redisService.getHashValue(redisKey, "workerType")
            );
            log.error("Submission {} has failed. Worker type: {}", submissionJobId, workerType.name());
            redisService.deleteKey(redisKey);
        }
            
        return new SubmissionResponsePayload(status, null);
    }

    public SubmissionDto getSubmission(String submissionId) {
        return submissionService.getUserSubmission(submissionId);
    }

    public List<SubmissionDto> getSubmissionsOfUserForProblem(Integer problemId){
        return submissionService.getUserSubmissions(problemId);
    }

    public List<SubmissionDto> getRecentSubmissions(Integer limit) {
        return submissionService.getRecentUserSubmission(limit);
    }

    public ProblemOfTheDayDto getProblemOfTheDay() {
        return problemOfTheDayService.getProblemOfTheDay();
    }

    public Boolean updateUserProfile(UserProfileDto newUserProfile) {

        System.out.println("UPDATING USER PROFILE");

        UserEntity user = (UserEntity)getCurrentlySignedInUser();

        System.out.println("Received user profile: " + newUserProfile.toString());

        Boolean profileUpdated = userProfileService.updateUserProfile(user.getUsername(), newUserProfile);

        UserProfileUpdateEvent profileUpdateEvent = new UserProfileUpdateEvent(this, user.getUsername(), newUserProfile);

        eventPublisher.publishEvent(profileUpdateEvent);

        return profileUpdated;
    } 

    public void uploadUserProfilePicture(MultipartFile file) throws IOException, FileUploadFailureException {
        UserEntity user = getCurrentlySignedInUser();
        byte[] resizedImage = ImageUtils.resizeImage(file, ImageDimension.SQUARE);
        userProfileService.uploadUserProfilePicture(user.getUsername(), resizedImage);
    }

    public CurrentUserDto getCurrentUserInfo() {
        UserEntity user = getCurrentlySignedInUser();
        return userProfileService.getCurrentUserInfo(user.getUsername());
    }

    public UserProfileDto getUserProfile(String username) {
        UserProfileDto userProfileDto = userProfileService.getUserProfile(username);
        UserEntity currentlySignInUser = getCurrentlySignedInUser();
        if (!currentlySignInUser.getUsername().equals(username)) {
            userProfileDto.setProfileOwner(false);
        } else {
            userProfileDto.setProfileOwner(true);
        }
        return userProfileDto;
    }

    public List<Topic> getTopics() {
        return topicRepository.getAllTopicTags();
    }

    public List<CompanyDto> getCompanies() {
        return companyService.getAllCompanies();
    }

    public Map<String, List<EarnedBadgeDto>> getUserBadges(String username) {
        List<String> earnedBadgeIds = userProfileRepository.getUserProfile(username)
                .map(p -> p.getEarnedBadgeIds())
                .orElse(List.of());
        return badgeService.getEarnedBadges(earnedBadgeIds);
    }

    public CompanyDto getCompanyDetails(String companySlug) {
        return companyService.getCompanyDetailsBySlug(companySlug);
    }

    public Map<String, Integer> getProblemCountByDifficulty() {
        return problemRepositoryService.getPublishedProblemCountByDifficulty();
    }

    public UserSubmissionStatusDto getSubmissionStatusDto() {
        String username = getCurrentlySignedInUser().getUsername();
        return userSubmissionStatusService.getSubmissionStatusDto(username);
    }

    public UserStreakDto getUserStreak() {
        UserEntity currentSignedInUser = getCurrentlySignedInUser();
        String username = currentSignedInUser.getUsername();
        UserStreakEntity streak = userStreakService.getStreak(username);
        List<String> earnedBadgeIds = userProfileRepository.getEarnedBadgeIds(username);
        BadgeDto highestStreakBadge = badgeService.getHighestStreakBadge(earnedBadgeIds);
        return new UserStreakDto(
                streak.getUsername(),
                streak.getStreakDays(),
                streak.getLastSubmissionDate(),
                streak.getHighestStreakDays(),
                streak.getHighestStreakDate(),
                highestStreakBadge
        );
    }

    public void performStudyPlanOperation(String listId, StudyPlanOperation operation) throws OperationNotSupportedException {
        
        UserEntity signedInUser = (UserEntity)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        ProblemListDto listDto = problemListRepositoryService.getProblemListById(listId);
        
        if (!isStudyPlanOperationAllowed(listDto, operation, signedInUser.getUsername())) {
            throw new OperationNotSupportedException(String.format("The study plan you are tying to %s is not allowed", operation.name().toLowerCase()));    
        }

        switch (operation) {
            case ACTIVATE:
                studyPlanRepositoryService.createProgress(listId, signedInUser.getUsername());
                break;
            case DEACTIVATE:
                studyPlanRepositoryService.deleteProgress(listId, signedInUser.getUsername());
                break;
            case RESET:
                studyPlanRepositoryService.resetProgress(listId, signedInUser.getUsername());
                break;
            default:
                break;
        }
    }

    public UserStudyPlanProgress getStudyPlanProgress(String listId) {
        UserEntity signedInUser = getCurrentlySignedInUser();
        return studyPlanRepositoryService.getStudyPlanProgress(listId, signedInUser.getUsername());
    }
 
    private Boolean isStudyPlanOperationAllowed(ProblemListDto listdto, StudyPlanOperation operation, String signedInUser) throws OperationNotSupportedException {
        boolean isStudyPlan = listdto.getIsStudyPlan();
        if (!isStudyPlan) return false;

        boolean isActive;
        try{
            studyPlanRepositoryService.getStudyPlanProgress(listdto.getId(), signedInUser);
            isActive = true;
        } catch (NoSuchElementException e) {
            isActive = false;
        }

        boolean isPublic = listdto.getIsPublic();

        if (operation == StudyPlanOperation.ACTIVATE) {
            if (isActive) return false;
            else return isPublic || listdto.getCreator().equals(signedInUser);
        }

        return isActive;
    }

}
