package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;
import com.codinglemonsbackend.Exceptions.ProfilePictureUploadFailureException;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Payloads.LikeRequest;
import com.codinglemonsbackend.Payloads.SubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Payloads.UpdateProblemListRequest;


public interface MainService {

    public ProblemSet getProblemSet(String difficultyStr, String topicsStr, String companiesStr, Integer page, Integer size);

    public ProblemDto getProblem(Integer id);

    public void addProblemList(ProblemListDto problemListDto) throws DuplicateResourceException;

    public int addProblemToList(String listId, Set<Integer> problemIds);

    public Map<String, Object> updateProblemList(String listId, UpdateProblemListRequest updateRequest);

    public List<ProblemListDto> getUserFavorites(String username);

    public ProblemListDto getUserProblemList(String username, String name);

    public void likeProblem(LikeRequest likeRequest) throws DuplicateResourceException;

    public String submitCode(SubmitCodeRequestPayload payload);

    public SubmissionResponsePayload<?> getSubmission(String submissionId) throws FailedSubmissionException;

    public ProblemDto getProblemOfTheDay();

    public boolean updateUserProfile(UserProfileDto newUserProfile);

    public UserProfileDto getUserProfile(String username);

    public void uploadUserProfilePicture(MultipartFile file) throws ProfilePictureUploadFailureException;

    public byte[] getUserProfilePicture();
    
}
