package com.codinglemonsbackend.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDto.Difficulty;
import com.codinglemonsbackend.Dto.ProblemExecutionDetails;
import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Dto.UserSubmissionStatus;
import com.codinglemonsbackend.Entities.SubmissionEntity;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Entities.UserStreakEntity;
import com.codinglemonsbackend.Repository.SubmissionRepository;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;
import com.codinglemonsbackend.Events.UserProfileUpdateEvent;
import com.codinglemonsbackend.Entities.ProblemListEntity;
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

import lombok.extern.slf4j.Slf4j;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
@Slf4j
public class MainServiceImpl{

    private static final Integer MAX_PROBLEMSET_SIZE = 100;
    private static final Integer DEFAULT_PROBLEMSET_SIZE = 10;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Counter codeExecutionCounter;

    @Autowired
    private Counter problemSubmissionCounter;

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ProblemRepositoryService problemRepositoryService;

    @Autowired
    private UserProblemListRepositoryService userProblemListRepositoryService;

    @Autowired
    private UserStreakService userStreakService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private SubmissionServiceRegistry submissionServiceRegistry;

    @Autowired
    private ProblemOfTheDayService problemOfTheDayService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelMapper modelMapper;

    private final String PENDING_SUBMISSION_REDIS_KEY_PREFIX = "submission:report";

    private UserEntity getCurrentlySignedInUser(){
        return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private List<Set<Integer>> getAcceptedAndAttemptedProblemIdsOfUser(){
        
        UserEntity currentSignedInUserEntity = getCurrentlySignedInUser();

        String username = currentSignedInUserEntity.getUsername();

        ProblemListEntity acceptedProblemList = userProblemListRepositoryService.getUserProblemLists(username).stream().filter(problemList -> problemList.getName().equals(UserProblemListRepositoryService.SOLVED_PROBLEM_LIST)).findFirst().get();
        ProblemListEntity atteptedProblemList = userProblemListRepositoryService.getUserProblemLists(username).stream().filter(problemList -> problemList.getName().equals(UserProblemListRepositoryService.ATTEMPTED_PROBLEM_LIST)).findFirst().get();
        
        return List.of(
            acceptedProblemList.getProblemIds(), 
            atteptedProblemList.getProblemIds()
        );
        // return userProblemListRepositoryService.getProblemList(
        //     userProblemListRepositoryService.SOLVED_PROBLEM_LIST, 
        //     currentSignedInUserEntity.getUsername()
        // ).getProblemIds();
    }

    public ProblemSet getProblemSet(String difficultyStr, String topicsStr, String companysStr, Integer page, Integer size, Boolean isAdmin) {

        if (page < 0) page = 0;
        if (size <= 0) size = DEFAULT_PROBLEMSET_SIZE; 
        if (size > MAX_PROBLEMSET_SIZE) size = MAX_PROBLEMSET_SIZE;

        ProblemSet problemSet = null;

        if (StringUtils.isBlank(difficultyStr) && StringUtils.isBlank(topicsStr) && StringUtils.isBlank(companysStr)) problemSet = problemRepositoryService.getAllProblems(page, size, isAdmin);
        else problemSet = problemRepositoryService.getFilteredProblems(difficultyStr, topicsStr, companysStr, page, size, isAdmin);

        List<ProblemDto> problemDtos = problemSet.getProblems();

        List<ProblemDto> problemsWithStatus = problemDtos.stream().map((e) -> {
            UserSubmissionStatus status = getUserSubmissionStatus(e.getId());
            e.setUserSubmissionStatus(status);
            return e;
        }).collect(Collectors.toList());

        problemSet.setProblems(problemsWithStatus);

        return problemSet;
    }

    public ProblemDto getProblem(Integer id) {
        ProblemDto problemDto = problemRepositoryService.getProblemById(id);

        UserSubmissionStatus status = getUserSubmissionStatus(id);
        problemDto.setUserSubmissionStatus(status);

        redisService.storeValue(RedisService.PROBLEM_LIKES_COUNT_CACHE_PREFIX+Integer.toString(id), Integer.toString(problemDto.getLikes()), 300);

        return problemDto;
    }

    private UserSubmissionStatus getUserSubmissionStatus(Integer problemId) {
        List<Set<Integer>> acceptedAndAttemptedProblemIds = getAcceptedAndAttemptedProblemIdsOfUser();
        Set<Integer> acceptedProblemIds = acceptedAndAttemptedProblemIds.get(0);
        Set<Integer> attemptedProblemIds = acceptedAndAttemptedProblemIds.get(1);

        if (acceptedProblemIds.contains(problemId)) return UserSubmissionStatus.ACC;
        else if (attemptedProblemIds.contains(problemId)) return UserSubmissionStatus.ATT;
        else return UserSubmissionStatus.NATT;
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

    public void addProblemList(ProblemListDto problemListDto) throws DuplicateResourceException {
        UserEntity currentSignedInUserEntity = getCurrentlySignedInUser();
        ProblemListEntity enitityToSave = modelMapper.map(problemListDto, ProblemListEntity.class);
        enitityToSave.setCreator(currentSignedInUserEntity.getUsername());
        userProblemListRepositoryService.saveProblemList(enitityToSave);
    }

    public int addProblemToList(String listId, Set<Integer> problemIds) {
        Set<Integer> validProblemIds = problemRepositoryService.getProblemsByIds(new ArrayList<>(problemIds), false)
                                        .stream()
                                        .map(ProblemDto::getId)
                                        .collect(Collectors.toSet());

        if (validProblemIds.isEmpty()) {
            throw new IllegalArgumentException("No valid problem ids found");
        }

        return userProblemListRepositoryService.addProblemToProblemList(listId, validProblemIds);
    }

    public Map<String, Object> updateProblemList(String listId, UpdateProblemListRequest newListDetails) {
        return userProblemListRepositoryService.updateProblemList(listId, newListDetails);
    }

    public List<ProblemListDto> getUserFavorites(String username) {
        List<ProblemListDto> userProblemListDtos =  userProblemListRepositoryService.getUserProblemLists(username)
                .stream()
                .map(entity -> modelMapper.map(entity, ProblemListDto.class))
                .collect(Collectors.toList());

        if (userProblemListDtos.isEmpty()) {
            throw new NoSuchElementException("No problem lists found for user " + username);
        }

        return userProblemListDtos;
    }

    public ProblemListDto getUserProblemList(String username, String name) {
        return userProblemListRepositoryService.getUserProblemList(username, name);
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
    public String submitCode(SubmitCodeRequestPayload payload) {
        ProblemDto problemDto = getProblem(payload.getProblemId());
        System.out.println("Problem status: " + problemDto.getStatus());
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
                                                .executionDetails(executionDetails)
                                                .language(payload.getLanguage())
                                                .username(getCurrentlySignedInUser().getUsername())
                                                .userCode(payload.getUserCode())
                                                .isRunCode(payload.getIsRunCode())
                                                .b64Encoded(payload.getB64Encoded())
                                                .build();

        String submissionJobId = submissionService.queueSubmission(submissionMetadata);
        log.info("Storing submission {} in Redis with key: {}", submissionJobId, PENDING_SUBMISSION_REDIS_KEY_PREFIX);

        try {
            String metadataJson = objectMapper.writeValueAsString(submissionMetadata);
            redisService.storeHash(PENDING_SUBMISSION_REDIS_KEY_PREFIX + ":" + submissionJobId, "status", PendingOrdersStatus.QUEUED.name(), 3600);
            redisService.storeHash(PENDING_SUBMISSION_REDIS_KEY_PREFIX + ":" + submissionJobId, "submissionMetadata", metadataJson, 3600);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize submission metadata for job {}", submissionJobId, e);
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
                eventPublisher.publishEvent(new SubmitCodeCompletedEvent(this, executionReport, submissionMetadata));
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
            
        // QUEUED — tell the caller to poll again
        return new SubmissionResponsePayload(status, null);
    
    }

    public SubmissionDto getSubmission(String submissionId) throws FailedSubmissionException {
        SubmissionDto submissionDto = submissionService.getSubmission(submissionId);
        return submissionDto;
    }

    public ProblemDto getProblemOfTheDay() {
        
        ProblemDto problemOfTheDay = problemOfTheDayService.getProblemOfTheDay();

        if (Objects.isNull(problemOfTheDay)) throw new NoSuchElementException("Problem of the day not set");

        return problemOfTheDay;
    }

    public Boolean updateUserProfile(UserProfileDto newUserProfile) {

        System.out.println("UPDATING USER PROFILE");

        UserEntity user = (UserEntity)getCurrentlySignedInUser();

        System.out.println("Received user profile: " + newUserProfile.toString());

        Boolean profileUpdated = userProfileService.updateUserProfile(user.getUsername(), newUserProfile);

        UserProfileUpdateEvent profileUpdateEvent = new UserProfileUpdateEvent(this, user.getUsername(), newUserProfile);

        eventPublisher.publishEvent(profileUpdateEvent);

        return profileUpdated;

        // UserProfileDto userProfile = userProfileService.getUserProfile(user.getUsername());

        // Boolean updateStatus = userProfileService.updateUserProfile(userProfile, newUserProfile);

        // if (newUserProfile.getFirstName() != null || newUserProfile.getLastName() != null || newUserProfile.getEmail() != null) {
        //     System.out.println("UPDATING USER DETAILS");
        //     userService.updateUserDetails(UserDto.builder()
        //                                     .username(user.getUsername())
        //                                     .firstName(newUserProfile.getFirstName())
        //                                     .lastName(newUserProfile.getLastName())
        //                                     .email(newUserProfile.getEmail())
        //                                     .build(),
        //                                     modelMapper.map(user, UserDto.class));
        // }
    } 

    public void uploadUserProfilePicture(MultipartFile file) throws IOException, FileUploadFailureException {
        UserEntity user = getCurrentlySignedInUser();
        byte[] resizedImage = ImageUtils.resizeImage(file, ImageDimension.SQUARE);
        userProfileService.uploadUserProfilePicture(user.getUsername(), resizedImage);
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

    public CompanyDto getCompanyDetails(String companySlug) {
        return companyService.getCompanyDetailsBySlug(companySlug);
    }

    public UserStreakEntity getUserStreak() {
        UserEntity currentSignedInUser = getCurrentlySignedInUser();
        String username = currentSignedInUser.getUsername();
        UserStreakEntity userStreakEntity = userStreakService.getStreak(username);
        return userStreakEntity;
    }
 
}
