package com.codinglemonsbackend.Service;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Config.CustomCacheConfig;
import com.codinglemonsbackend.Dto.CodeRunResultDto;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Entities.ProblemExecutionDetails;
import com.codinglemonsbackend.Entities.ProblemListEntity;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;
import com.codinglemonsbackend.Exceptions.ProfilePictureUploadFailureException;
import com.codinglemonsbackend.Payloads.LikeRequest;
import com.codinglemonsbackend.Payloads.SubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Payloads.UpdateProblemListRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class MainServiceImpl implements MainService{

    private static final Integer MAX_PROBLEMSET_SIZE = 100;
    private static final Integer DEFAULT_PROBLEMSET_SIZE = 10;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ProblemRepositoryService problemRepositoryService;

    @Autowired
    private UserProblemListRepositoryService userProblemListRepositoryService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private ProblemOfTheDayService problemOfTheDayService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelMapper modelMapper;

    private final String PENDING_SUBMISSION_REDIS_KEY = "submissions:pending";

    public final String CODERUN_RESULTS = "coderun:results";

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

    public ProblemSet getProblemSet(String difficultyStr, String topicsStr, String companiesStr, Integer page, Integer size) {

        if (page < 0) page = 0;
        if (size <= 0) size = DEFAULT_PROBLEMSET_SIZE; 
        if (size > MAX_PROBLEMSET_SIZE) size = MAX_PROBLEMSET_SIZE;

        ProblemSet problemSet = null;

        if (StringUtils.isBlank(difficultyStr) && StringUtils.isBlank(topicsStr) && StringUtils.isBlank(companiesStr)) problemSet = problemRepositoryService.getAllProblems(page, size);
        else problemSet = problemRepositoryService.getFilteredProblems(difficultyStr, topicsStr, companiesStr, page, size);

        List<Set<Integer>> acceptedAndAttemptedProblemIds = getAcceptedAndAttemptedProblemIdsOfUser();

        Set<Integer> acceptedProblemIds = acceptedAndAttemptedProblemIds.get(0);

        Set<Integer> attemptedProblemIds = acceptedAndAttemptedProblemIds.get(1);

        List<ProblemDto> problemDtos = problemSet.getProblems();

        List<ProblemDto> problemsWithStatus = problemDtos.stream().map((e) -> {
            ProblemStatus status = null;
            if (acceptedProblemIds.contains(e.getId())) status = ProblemStatus.ACC;
            else if (attemptedProblemIds.contains(e.getId())) status = ProblemStatus.ATT;
            else status = ProblemStatus.NATT;
            e.setStatus(status);
            return e;
        }).collect(Collectors.toList());

        problemSet.setProblems(problemsWithStatus);

        return problemSet;
    }

    @Override
    public ProblemDto getProblem(Integer id) {
        
        ProblemDto problemDto = problemRepositoryService.getProblem(id);

        List<Set<Integer>> acceptedAndAttemptedProblemIds = getAcceptedAndAttemptedProblemIdsOfUser();

        Set<Integer> acceptedProblemIds = acceptedAndAttemptedProblemIds.get(0);

        Set<Integer> attemptedProblemIds = acceptedAndAttemptedProblemIds.get(1);

        ProblemStatus status = null;

        if (acceptedProblemIds.contains(id)) status = ProblemStatus.ACC;
        else if (attemptedProblemIds.contains(id)) status = ProblemStatus.ATT;
        else status = ProblemStatus.NATT;

        problemDto.setStatus(status);

        return problemDto;
    }
    

    @Override
    public void addProblemList(ProblemListDto problemListDto) throws DuplicateResourceException {
        UserEntity currentSignedInUserEntity = getCurrentlySignedInUser();
        ProblemListEntity enitityToSave = modelMapper.map(problemListDto, ProblemListEntity.class);
        enitityToSave.setCreator(currentSignedInUserEntity.getUsername());
        userProblemListRepositoryService.saveProblemList(enitityToSave);
    }

    public int addProblemToList(String listId, Set<Integer> problemIds) {
        Set<Integer> validProblemIds = problemRepositoryService.getProblemsByIds(new ArrayList<>(problemIds))
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

    @Override
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

        Boolean problemExist = redisService.hashKeyExists(CustomCacheConfig.PROBLEM_LIKES_CACHE, Integer.toString(problemId)) || problemRepositoryService.problemExists(problemId);
        
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

    @Override
    public String submitCode(SubmitCodeRequestPayload payload) {

        ProblemExecutionDetails executionDetails = problemRepositoryService.getExecutionDetails(payload.getProblemId());

        SubmissionMetadata submissionMetadata = SubmissionMetadata.builder()
                                                .problemId(payload.getProblemId())
                                                .executionDetails(executionDetails)
                                                .language(payload.getLanguage())
                                                .username(getCurrentlySignedInUser().getUsername())
                                                .userCode(payload.getUserCode())
                                                .isRunCode(payload.getIsRunCode())
                                                .build();
        
        String submissionToken = submissionService.submitCode(submissionMetadata);

        redisService.storeHash(PENDING_SUBMISSION_REDIS_KEY, submissionToken, PendingOrdersStatus.QUEUED.toString());

        return submissionToken;

    }

    @Override
    public SubmissionResponsePayload<?> getSubmission(String submissionId) throws FailedSubmissionException {

        // First check if the submission is present in redis hash, if yes then return the status as pending
        // else search the database for the submission and return 

        if (redisService.hashKeyExists(PENDING_SUBMISSION_REDIS_KEY, submissionId)) {
            String status = redisService.getHashValue(PENDING_SUBMISSION_REDIS_KEY, submissionId);
            if (status.equals(PendingOrdersStatus.FAILED.toString()) || status.equals(PendingOrdersStatus.HAULTED.toString())) {
                redisService.deleteHashEntry(PENDING_SUBMISSION_REDIS_KEY, submissionId);
                log.info("Submission {} for username {} {}", submissionId, getCurrentlySignedInUser(), status);
                throw new FailedSubmissionException("Submission " + status);
            } 
            return new SubmissionResponsePayload<SubmissionDto>(status, null);
        }

        // Check if the submission is in code run hashc
        if (redisService.hashKeyExists(CODERUN_RESULTS, submissionId)) {
            String result = redisService.getHashValue(CODERUN_RESULTS, submissionId);
            try {
                CodeRunResultDto codeRunResult = objectMapper.readValue(result, CodeRunResultDto.class);
                redisService.deleteHashEntry(CODERUN_RESULTS, submissionId);
                return new SubmissionResponsePayload<CodeRunResultDto>("Completed", codeRunResult);
            } catch (Exception e) {
                e.printStackTrace();
            }    
        }

        SubmissionDto submissionDto = submissionService.getSubmission(submissionId);

        return new SubmissionResponsePayload<SubmissionDto>("Completed", submissionDto);
    }


    @Override
    public ProblemDto getProblemOfTheDay() {
        
        ProblemDto problemOfTheDay = problemOfTheDayService.getProblemOfTheDay();

        if (Objects.isNull(problemOfTheDay)) throw new NoSuchElementException("Problem of the day not set");

        return problemOfTheDay;
    }

    @Override
    public boolean updateUserProfile(UserProfileDto newUserProfile) {

        System.out.println("UPDATING USER PROFILE");

        UserEntity user = (UserEntity)getCurrentlySignedInUser();

        UserProfileDto userProfile = userProfileService.getUserProfile(user.getUsername());

        Boolean updateStatus = userProfileService.updateUserProfile(userProfile, newUserProfile);

        if (newUserProfile.getFirstName() != null || newUserProfile.getLastName() != null || newUserProfile.getEmail() != null) {
            System.out.println("UPDATING USER DETAILS");
            userService.updateUserDetails(UserDto.builder()
                                            .username(user.getUsername())
                                            .firstName(newUserProfile.getFirstName())
                                            .lastName(newUserProfile.getLastName())
                                            .email(newUserProfile.getEmail())
                                            .build(),
                                            modelMapper.map(user, UserDto.class));
        }

        return updateStatus;
    } 

    @Override
    public void uploadUserProfilePicture(MultipartFile file) throws ProfilePictureUploadFailureException {

        UserEntity user = getCurrentlySignedInUser();

        userProfileService.uploadUserProfilePicture(user.getUsername(), file);
    }

    @Override
    public byte[] getUserProfilePicture() {

        UserEntity user = getCurrentlySignedInUser();

        UserProfileDto userProfileDto = userProfileService.getUserProfile(user.getUsername());

        byte[] profilePicture = userProfileService.getUserProfilePicture(userProfileDto);

        return profilePicture;
    
    }

    @Override
    public UserProfileDto getUserProfile(String username) {

        UserProfileDto userProfileDto = userProfileService.getUserProfile(username);

        return userProfileDto;
    }
 
}
