package com.codinglemonsbackend.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Entities.UserProblemList;
import com.codinglemonsbackend.Exceptions.ResourceAlreadyExistsException;
import com.codinglemonsbackend.Repository.UserProblemListRepository;

@Service
public class UserProblemListRepositoryService {

    public static final String SOLVED_PROBLEM_LIST = "Solved";
    
    public static final String ATTEMPTED_PROBLEM_LIST = "Attempted";

    @Autowired
    private UserProblemListRepository userProblemListRepository;

    public List<UserProblemList> geAlltProblemListOfUser(String username) {

        List<UserProblemList> userProblemLists = userProblemListRepository.getAllProblemListsOfUser(username);

        // if(userProblemList.isEmpty()) throw new ResourceNotFoundException("Requested problem list not found");

        return userProblemLists;
    }

    public void createDefaultProblemList(String username){
        
        UserProblemList solvedProblemList = UserProblemList.builder()
                                                .name(SOLVED_PROBLEM_LIST)
                                                .description("Your solved problems")
                                                .problemIds(new ArrayList<Integer>())
                                                .creator(username)
                                                .build();

        UserProblemList attemptedProblemList = UserProblemList.builder()
                                                .name(ATTEMPTED_PROBLEM_LIST)
                                                .description("Your attempted problems")
                                                .problemIds(new ArrayList<Integer>())
                                                .creator(username)
                                                .build();

        try {
            saveProblemList(solvedProblemList);
            saveProblemList(attemptedProblemList);
        } catch (ResourceAlreadyExistsException e) {
            e.printStackTrace();
        }
    }
    
    public void saveProblemList(UserProblemList newProblemList) throws ResourceAlreadyExistsException{
        
        //check if the list exists, if not save or else throw error
        List<UserProblemList> allProblemListsOfUser = geAlltProblemListOfUser(newProblemList.getCreator());

        Boolean problemListAlreadyPresent = allProblemListsOfUser.stream().anyMatch(userProblemList -> userProblemList.getName().equalsIgnoreCase(newProblemList.getName()));
    
        if(problemListAlreadyPresent) throw new ResourceAlreadyExistsException("Problem list already exist");

        userProblemListRepository.saveProblemList(newProblemList);
    }

    public void updateProblemList(String id){

    }

    public Boolean deleteProblemList(String id){
        
        return true;
    }

}
