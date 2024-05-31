package com.codinglemonsbackend.Service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDtoWithStatus;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Entities.UserProblemList;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;
import com.codinglemonsbackend.Exceptions.ProfilePictureUploadFailureException;
import com.codinglemonsbackend.Exceptions.ResourceAlreadyExistsException;
import com.codinglemonsbackend.Payloads.SubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.ProblemSetResponsePayload;
import com.codinglemonsbackend.Payloads.ProblemUpdateRequestPayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Payloads.UserUpdateRequestPayload;


public interface MainService {

    public ProblemSetResponsePayload getProblemSet(String difficultyStr, String topicsStr, Integer page, Integer size);

    public ProblemDtoWithStatus getProblem(Integer id);

    public void addProblem(ProblemDto problemDto);

    public void updateProblem(Integer problemId, ProblemUpdateDto updateMetadata);

    public void deleteProblemById(Integer problemId);

    public void clearAllProblems();

    public void addProblemList(UserProblemList problemList) throws ResourceAlreadyExistsException;

    public List<UserProblemList> getUserFavorites();

    // public Mono<Judge0CreateSubmissionResponse> createSubmission(SubmitCodeRequestPayload payload);

    public String submitCode(SubmitCodeRequestPayload payload);

    public SubmissionResponsePayload<?> getSubmission(String submissionId) throws FailedSubmissionException;

    public ProblemDto getProblemOfTheDay();

    public boolean updateUserInfo(UserUpdateRequestPayload updateRequest);

    public UserDto getUserInfo();

    public void uploadUserProfilePicture(MultipartFile file) throws ProfilePictureUploadFailureException;

    public byte[] getUserProfilePicture();
    
}
