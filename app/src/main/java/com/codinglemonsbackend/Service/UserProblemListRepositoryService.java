package com.codinglemonsbackend.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Entities.ProblemListEntity;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Events.UserAccountCreationEvent;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Payloads.UpdateProblemListRequest;
import com.codinglemonsbackend.Repository.UserProblemListRepository;


@Service
public class UserProblemListRepositoryService {

    public static final String SOLVED_PROBLEM_LIST = "Solved";
    
    public static final String ATTEMPTED_PROBLEM_LIST = "Attempted";

    @Autowired
    private UserProblemListRepository userProblemListRepository;

    public List<ProblemListEntity> getUserProblemLists(String username) {

        List<ProblemListEntity> userProblemLists = userProblemListRepository.getAllProblemListsOfUser(username);

        //if(userProblemLists.isEmpty()) throw new ResourceNotFoundException("Requested problem list not found"); 

        return userProblemLists;
    }

    public ProblemListDto getUserProblemList(String creator, String name) {
        Optional<ProblemListDto> problemListDto = userProblemListRepository.getUserProblemListDetails(creator, name);
        if (problemListDto.isEmpty()) {
            throw new NoSuchElementException("The list you are searching does not exist!!");
        } 
        return problemListDto.get();
    }

    @Async
    @EventListener
    public void createDefaultProblemList(UserAccountCreationEvent event) {

        String username = event.getUser().getUsername();
        
        ProblemListEntity solvedProblemList = ProblemListEntity.builder()
                                                .name(SOLVED_PROBLEM_LIST)
                                                .description("Your solved problems")
                                                .problemIds(new HashSet<Integer>())
                                                .creator(username) 
                                                .isPublic(false)
                                                .isPinned(false)
                                                .build();

        ProblemListEntity attemptedProblemList = ProblemListEntity.builder()
                                                .name(ATTEMPTED_PROBLEM_LIST)
                                                .description("Your attempted problems")
                                                .problemIds(new HashSet<Integer>())
                                                .creator(username)
                                                .isPublic(false)
                                                .isPinned(false)
                                                .build();

        try {
            saveProblemList(solvedProblemList);
            saveProblemList(attemptedProblemList);
        } catch (DuplicateResourceException e) {
            e.printStackTrace();
        }
    }
    
    public void saveProblemList(ProblemListEntity newProblemList) throws DuplicateResourceException{
        userProblemListRepository.saveProblemList(newProblemList);
    }

    public int addProblemToProblemList(String listId, Set<Integer> validProblemIds){
        return userProblemListRepository.addProblemToProblemList(listId, validProblemIds);
    }

    public Map<String, Object> updateProblemList(String listId, UpdateProblemListRequest newListDetails){
        ObjectId objectId;
        
        try {
            objectId = new ObjectId(listId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid problem list id");
        }

        Optional<ProblemListEntity> optionalListEntity = userProblemListRepository.getUserProblemListEntityById(objectId);

        if (optionalListEntity.isEmpty()) {
            throw new NoSuchElementException(String.format("The requested list id %s not found!!", listId));
        
        }
        ProblemListEntity listEntity = optionalListEntity.get();
        
        UserEntity signedInUser= (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!listEntity.getCreator().equals(signedInUser.getUsername())) {
            throw new AccessDeniedException("You are not allowed to update this list.");
        }

        Map<String, Object> fieldsToUpdate = new HashMap<>();

        if (StringUtils.isNotBlank(newListDetails.getName()) && !newListDetails.getName().equals(listEntity.getName())) {
            fieldsToUpdate.put("name", newListDetails.getName());
        }

        if (StringUtils.isNotBlank(newListDetails.getDescription()) && !newListDetails.getDescription().equals(listEntity.getDescription())) {
            fieldsToUpdate.put("description", newListDetails.getDescription());
        }

        if (Objects.nonNull(newListDetails.getIsPublic()) && !newListDetails.getIsPublic().equals(listEntity.getIsPublic())) {
            fieldsToUpdate.put("isPublic", newListDetails.getIsPublic());
        }

        if (Objects.nonNull(newListDetails.getIsPinned()) && !newListDetails.getIsPinned().equals(listEntity.getIsPinned())) {
            fieldsToUpdate.put("isPinned", newListDetails.getIsPinned());
        }

        if (!fieldsToUpdate.isEmpty()) {
            return userProblemListRepository.updateProblemList(objectId, fieldsToUpdate, listEntity);
        }

        return new HashMap<>();
    }

    public Boolean deleteProblemList(String id){
        
        return true;
    }

}
