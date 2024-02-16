package com.codinglemonsbackend.Service;

import java.util.List;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDtoWithStatus;
import com.codinglemonsbackend.Entities.UserProblemList;
import com.codinglemonsbackend.Exceptions.ResourceAlreadyExistsException;
import com.codinglemonsbackend.Payloads.ProblemSetResponsePayload;

public interface MainService {

    public ProblemSetResponsePayload getProblemSet(String difficultyStr, String topicsStr, Integer page, Integer size);

    public ProblemDtoWithStatus getProblem(Integer id);

    public void addProblem(ProblemDto problemDto);

    public void clearAllProblems();

    public void addProblemList(UserProblemList problemList) throws ResourceAlreadyExistsException;

    public List<UserProblemList> getUserFavorites();
    
}
