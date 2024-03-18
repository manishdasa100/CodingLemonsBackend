package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDtoWithStatus;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Entities.UserProblemList;
import com.codinglemonsbackend.Exceptions.ResourceAlreadyExistsException;
import com.codinglemonsbackend.Payloads.CodeSubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.ProblemSetResponsePayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class MainServiceImpl implements MainService{

    private static final Integer PROBLEMSETMAXSIZE = 100;

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

    private final String PENDING_SUBMISSION_REDIS_KEY = "submissions:pending";

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
                                                .build();

        //Mono<List<Judge0SubmissionToken>> submissionTokens = submissionService.submitCode(submissionMetadata);
        String submissionToken = submissionService.submitCode(submissionMetadata);

        redisService.storeHash(PENDING_SUBMISSION_REDIS_KEY, submissionToken, PendingOrdersStatus.QUEUED.toString());

        //submissionTokens.subscribe(e -> {for(Judge0SubmissionToken token:e){System.out.println(token.getToken());}});
        return submissionToken;

    }

    @Override
    public CodeSubmissionResponsePayload getSubmission(String submissionId) {

        // First check if the submission is present in redis hash, if yes then return the status as pending
        // else search the database for the submission and return 

        if (redisService.hashKeyExists(PENDING_SUBMISSION_REDIS_KEY, submissionId)) {
            String status = redisService.getHashValue(PENDING_SUBMISSION_REDIS_KEY, submissionId);
            if (status.equals(PendingOrdersStatus.FAILED.toString()) || status.equals(PendingOrdersStatus.HAULTED.toString())) {
                redisService.deleteHashEntry(PENDING_SUBMISSION_REDIS_KEY, submissionId);
                log.info("Submission {} for username {} {}", submissionId, getCurrentlySignedInUser(), status);
                throw new IllegalStateException("Submission " + status);
            } 
            return CodeSubmissionResponsePayload.builder().status(status).build();
        }

        SubmissionDto submissionDto = submissionService.getSubmission(submissionId);

        return CodeSubmissionResponsePayload.builder().status("Completed").submission(submissionDto).build();
    }

    @Override
    public ProblemDto getProblemOfTheDay() {
        
        ProblemDto problemOfTheDay = problemOfTheDayService.getProblemOfTheDay();

        if (Objects.isNull(problemOfTheDay)) throw new NoSuchElementException("Problem of the day not set");

        return problemOfTheDay;
    } 
 
}
