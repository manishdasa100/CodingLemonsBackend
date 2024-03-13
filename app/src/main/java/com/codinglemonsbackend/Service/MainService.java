package com.codinglemonsbackend.Service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDtoWithStatus;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Entities.UserProblemList;
import com.codinglemonsbackend.Exceptions.ResourceAlreadyExistsException;
import com.codinglemonsbackend.Payloads.CodeSubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.ProblemSetResponsePayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Service.Judge0SubmissionServiceImpl.Judge0SubmissionToken;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MainService {

    public ProblemSetResponsePayload getProblemSet(String difficultyStr, String topicsStr, Integer page, Integer size);

    public ProblemDtoWithStatus getProblem(Integer id);

    public void addProblem(ProblemDto problemDto);

    public void clearAllProblems();

    public void addProblemList(UserProblemList problemList) throws ResourceAlreadyExistsException;

    public List<UserProblemList> getUserFavorites();

    // public Mono<Judge0CreateSubmissionResponse> createSubmission(SubmitCodeRequestPayload payload);

    public String submitCode(SubmitCodeRequestPayload payload);

    public CodeSubmissionResponsePayload getSubmission(String submissionId);
    
}
