package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.CodeRunResultDto;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDtoWithStatus;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Entities.UserProblemList;
import com.codinglemonsbackend.Exceptions.ResourceAlreadyExistsException;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;
import com.codinglemonsbackend.Exceptions.ProfilePictureUploadFailureException;
import com.codinglemonsbackend.Payloads.ProblemSetResponsePayload;
import com.codinglemonsbackend.Payloads.ProblemUpdateRequestPayload;
import com.codinglemonsbackend.Payloads.SubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Payloads.UserUpdateRequestPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class MainServiceImpl implements MainService{

    private static final Integer PROBLEMSETMAXSIZE = 100;

    @Autowired
    private UserService userService;

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

    private List<List<Integer>> getAcceptedAndAttemptedProblemIdsOfUser(){
        
        UserEntity currentSignedInUserEntity = getCurrentlySignedInUser();

        String username = currentSignedInUserEntity.getUsername();

        UserProblemList acceptedProblemList = userProblemListRepositoryService.geAlltProblemListOfUser(username).stream().filter(problemList -> problemList.getName().equals(UserProblemListRepositoryService.SOLVED_PROBLEM_LIST)).findFirst().get();
        UserProblemList atteptedProblemList = userProblemListRepositoryService.geAlltProblemListOfUser(username).stream().filter(problemList -> problemList.getName().equals(UserProblemListRepositoryService.ATTEMPTED_PROBLEM_LIST)).findFirst().get();
        
        return List.of(
            acceptedProblemList.getProblemIds(), 
            atteptedProblemList.getProblemIds()
        );
        // return userProblemListRepositoryService.getProblemList(
        //     userProblemListRepositoryService.SOLVED_PROBLEM_LIST, 
        //     currentSignedInUserEntity.getUsername()
        // ).getProblemIds();
    }

    public ProblemSetResponsePayload getProblemSet(String difficultyStr, String topicsStr, Integer page, Integer size) {

        if (size > PROBLEMSETMAXSIZE) size = PROBLEMSETMAXSIZE;

        System.out.println(difficultyStr + " " + topicsStr);

        ProblemSet problemSet = null;

        if (difficultyStr == null && topicsStr == null) problemSet = problemRepositoryService.getAllProblems(page, size);
        
        problemSet = problemRepositoryService.getFilteredProblems(difficultyStr, topicsStr, page, size);

        List<List<Integer>> acceptedAndAttemptedProblemIds = getAcceptedAndAttemptedProblemIdsOfUser();

        List<Integer> acceptedProblemIds = acceptedAndAttemptedProblemIds.get(0);

        List<Integer> attemptedProblemIds = acceptedAndAttemptedProblemIds.get(1);

        List<ProblemDto> problemDtos = problemSet.getProblems();

        List<ProblemDtoWithStatus> problemSetWithStatus = problemDtos.stream().map((e)->{
                                    ProblemStatus status = null;
                                    if (acceptedProblemIds.contains(e.getProblemId())) status = ProblemStatus.ACC;
                                    else if (attemptedProblemIds.contains(e.getProblemId())) status = ProblemStatus.ATT;
                                    else status = ProblemStatus.NATT;
                                    return ProblemDtoWithStatus.builder().status(status).problem(e).build();
                                }).collect(Collectors.toList());

        return ProblemSetResponsePayload.builder().total(problemSet.getTotal()).problems(problemSetWithStatus).build();
    }

    @Override
    public ProblemDtoWithStatus getProblem(Integer id) {
        
        ProblemDto problemDto = problemRepositoryService.getProblem(id);

        List<List<Integer>> acceptedAndAttemptedProblemIds = getAcceptedAndAttemptedProblemIdsOfUser();

        List<Integer> acceptedProblemIds = acceptedAndAttemptedProblemIds.get(0);

        List<Integer> attemptedProblemIds = acceptedAndAttemptedProblemIds.get(1);

        ProblemStatus status = null;

        if (acceptedProblemIds.contains(id)) status = ProblemStatus.ACC;
        else if (attemptedProblemIds.contains(id)) status = ProblemStatus.ATT;
        else status = ProblemStatus.NATT;

        return ProblemDtoWithStatus.builder().status(status).problem(problemDto).build();
    }

    @Override
    public void addProblem(ProblemDto problemDto) {
        System.out.println("--------PROBLEM DTO--------------");
        System.out.println(problemDto);
        problemRepositoryService.addProblem(problemDto);
    }

    @Override
    public void updateProblem(Integer problemId, ProblemUpdateDto updateMetadata) {
        problemRepositoryService.updateProblem(problemId, updateMetadata);
    }

    @Override
    public void deleteProblemById(Integer problemId) {
        problemRepositoryService.deleteProblemById(problemId);
    }

    @Override
    public void clearAllProblems() {
        problemRepositoryService.removeAllProblems();
    }

    @Override
    public void addProblemList(UserProblemList problemList) throws ResourceAlreadyExistsException {
        UserEntity currentSignedInUserEntity = getCurrentlySignedInUser();
        problemList.setCreator(currentSignedInUserEntity.getUsername());
        userProblemListRepositoryService.saveProblemList(problemList);
    }

    @Override
    public List<UserProblemList> getUserFavorites() {
        return userProblemListRepositoryService.geAlltProblemListOfUser(getCurrentlySignedInUser().getUsername());
    }

    @Override
    public String submitCode(SubmitCodeRequestPayload payload) {

        ProblemDto problemDto = getProblem(payload.getProblemId()).getProblem();

        SubmissionMetadata submissionMetadata = SubmissionMetadata.builder()
                                                .problemDto(problemDto)
                                                .language(payload.getLanguage())
                                                .username(getCurrentlySignedInUser().getUsername())
                                                .userCode(payload.getUserCode())
                                                .isRunCode(payload.getIsRunCode())
                                                .build();

        //Mono<List<Judge0SubmissionToken>> submissionTokens = submissionService.submitCode(submissionMetadata);
        String submissionToken = submissionService.submitCode(submissionMetadata);

        redisService.storeHash(PENDING_SUBMISSION_REDIS_KEY, submissionToken, PendingOrdersStatus.QUEUED.toString());

        //submissionTokens.subscribe(e -> {for(Judge0SubmissionToken token:e){System.out.println(token.getToken());}});
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

        // Check if the submission is in code run hash
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

    // @Override
    // public boolean updateUserDetails(UserUpdateRequestPayload updateRequest) {

    //     UserEntity user = (UserEntity)getCurrentlySignedInUser();

    //     UserProfileDto userProfileDto = userProfileService.getUserProfile(user.getUsername());

    //     return userService.updateUserDetails(user, updateRequest);
    // } 
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
    public UserProfileDto getUserProfile() {

        UserEntity user = getCurrentlySignedInUser();

        UserProfileDto userProfileDto = userProfileService.getUserProfile(user.getUsername());

        return userProfileDto;
    }
 
}
