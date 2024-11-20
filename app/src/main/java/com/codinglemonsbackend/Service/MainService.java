package com.codinglemonsbackend.Service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDtoWithStatus;
import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.ProblemListEntity;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;
import com.codinglemonsbackend.Exceptions.ProfilePictureUploadFailureException;
import com.codinglemonsbackend.Exceptions.ResourceAlreadyExistsException;
import com.codinglemonsbackend.Payloads.SubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.ProblemSetResponsePayload;
import com.codinglemonsbackend.Payloads.ProblemUpdateRequestPayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Payloads.UserUpdateRequestPayload;


public interface MainService {

    public ProblemSet getProblemSet(String difficultyStr, String topicsStr, String companiesStr, Integer page, Integer size);

    public ProblemDto getProblem(Integer id);

    public void addProblemList(ProblemListEntity problemList) throws ResourceAlreadyExistsException;

    public List<ProblemListDto> getUserFavorites();

    // public Mono<Judge0CreateSubmissionResponse> createSubmission(SubmitCodeRequestPayload payload);

    public String submitCode(SubmitCodeRequestPayload payload);

    public SubmissionResponsePayload<?> getSubmission(String submissionId) throws FailedSubmissionException;

    public ProblemDto getProblemOfTheDay();

    //public boolean updateUserDetails(UserUpdateRequestPayload updateRequest);
    public boolean updateUserProfile(UserProfileDto newUserProfile);

    public UserProfileDto getUserProfile();

    public void uploadUserProfilePicture(MultipartFile file) throws ProfilePictureUploadFailureException;

    public byte[] getUserProfilePicture();
    
}
