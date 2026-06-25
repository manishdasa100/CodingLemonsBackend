package com.codinglemonsbackend.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Dto.StudyPlanOperation;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.ProblemListEntity;
import com.codinglemonsbackend.Entities.StudyPlanDifficultyTier;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Payloads.UpdateProblemListRequest;
import com.codinglemonsbackend.Repository.ProblemListRepository;


@Service
public class ProblemListRepositoryService {

    @Autowired
    private ProblemListRepository problemListRepository;

    @Autowired
    private ModelMapper modelMapper;

    public List<ProblemListDto> getAllGlobalProblemLists() {
        return getProblemLists("global");
    }

    public List<ProblemListDto> getProblemLists(String username) {

        UserEntity signedInUser = getCurrentlySignedInUser();

        Boolean publicOnlyList = false;

        if (!username.equals(signedInUser.getUsername())) {
            publicOnlyList = true;
        }

        List<ProblemListEntity> userProblemListEntities = problemListRepository.getAllProblemListsOfUser(username, publicOnlyList);

        if (userProblemListEntities.isEmpty()) {
            String message = "No problem lists found for user " + username;
            if (username.equals("global")) {
                message = "No global problem lists found";
            }
            throw new NoSuchElementException(message);
        }

        List<ProblemListDto> userProblemListDtos = userProblemListEntities.stream()
                .map(entity -> {
                    ProblemListDto dto = modelMapper.map(entity, ProblemListDto.class);
                    dto.setTotalProblems(entity.getProblemIds().size());
                    return dto;    
                })
                .collect(Collectors.toList());

        return userProblemListDtos;
    }

    public ProblemListDto getAProblemList(String creator, String name) {
        UserEntity signedInUser = getCurrentlySignedInUser();
        
        Boolean publicOnlyList = false;

        if (!creator.equals(signedInUser.getUsername())) {
            publicOnlyList = true;
        }
        return problemListRepository.getUserProblemListDetails(creator, name, publicOnlyList)
            .orElseThrow(() -> new NoSuchElementException(String.format("The list with name %s does not exist!!", name)));
    }

    public ProblemListDto getProblemListById(String listId) {
        ProblemListEntity listDto = problemListRepository.getUserProblemListEntityById(listId)
                                .orElseThrow(()-> new NoSuchElementException(String.format("The list with id {} not found", listId)));
        return modelMapper.map(listDto, ProblemListDto.class);
    }

    public void saveProblemList(ProblemListEntity newProblemList) throws DuplicateResourceException {
        problemListRepository.saveProblemList(newProblemList);
    }

    public void addProblemToProblemList(String listId, Set<Integer> validProblemIds) {
        ProblemListEntity listEntity = problemListRepository.getUserProblemListEntityById(listId)
                                        .orElseThrow(()->new NoSuchElementException(String.format("List with id {} not found", listId)));
        
        if(!isUserAuthorizedToModifyList(listEntity)) {
            throw new AccessDeniedException("You are not authorized to update this list");
        }

        problemListRepository.addProblemToProblemList(listId, validProblemIds);
    }

    public void removeProblemFromProblemList(String listId, Set<Integer> problemIdsToRemove) {
        ProblemListEntity listEntity = problemListRepository.getUserProblemListEntityById(listId)
                                            .orElseThrow(()->new NoSuchElementException(String.format("List with id {} not found", listId)));

        if (!isUserAuthorizedToModifyList(listEntity)) {
            throw new AccessDeniedException("You are not authorized to modify this list.");
        }

        problemListRepository.removeProblemFromProblemList(listId, problemIdsToRemove);
    }

    public void updateProblemList(UpdateProblemListRequest listUpdaterequest) {

        String listId = listUpdaterequest.getId();

        ProblemListEntity listEntity = problemListRepository.getUserProblemListEntityById(listId)
                                        .orElseThrow(()->new NoSuchElementException(String.format("List with id {} not found", listId)));

        if(!isUserAuthorizedToModifyList(listEntity)) {
            throw new AccessDeniedException("You are not authorized to update this list");
        } 

        Map<String, Object> fieldsToUpdate = new HashMap<>();

        if (StringUtils.isNotBlank(listUpdaterequest.getName())) {
            fieldsToUpdate.put("name", listUpdaterequest.getName());
        }

        if (StringUtils.isNotBlank(listUpdaterequest.getDescription())) {
            fieldsToUpdate.put("description", listUpdaterequest.getDescription());
        }

        if (Objects.nonNull(listUpdaterequest.getIsStudyPlan())) {
            StudyPlanDifficultyTier difficultyTier = null;
            Integer timelineDays = null;
            if (listUpdaterequest.getIsStudyPlan()) {
                difficultyTier = listUpdaterequest.getDifficultyTier();
                timelineDays = listUpdaterequest.getTimelineDays();
            }
            fieldsToUpdate.put("isStudyPlan", listUpdaterequest.getIsStudyPlan());
            fieldsToUpdate.put("difficultyTier", difficultyTier);
            fieldsToUpdate.put("timelineDays", timelineDays);
        }

        if (Objects.nonNull(listUpdaterequest.getIsPublic())) {
            fieldsToUpdate.put("isPublic", listUpdaterequest.getIsPublic());
        }

        if (Objects.nonNull(listUpdaterequest.getIsPinned())) {
            fieldsToUpdate.put("isPinned", listUpdaterequest.getIsPinned());
        }

        if (!fieldsToUpdate.isEmpty()) {
            problemListRepository.updateProblemList(listUpdaterequest.getId(), fieldsToUpdate);
        }
    }

    // public void activateOrDeactivateStudyPlan(String listId, StudyPlanOperation operation) throws OperationNotSupportedException {
    //     ProblemListEntity listEntity = problemListRepository.getUserProblemListEntityById(listId)
    //                                                        .orElseThrow(() -> new NoSuchElementException(String.format("List with id {} not found", listId)));
        
    //     if (!isStudyPlanOperationAllowed(listEntity, operation)) {
    //         throw new OperationNotSupportedException(String.format("The study plan you are tying to {} is not allowed", operation.name().toLowerCase()));    
    //     } 
    //     problemListRepository.activateOrDeactivateStudyPlan(listId, operation.equals(StudyPlanOperation.ACTIVATE));
    // }

    public Boolean deleteProblemList(String id) {
        return true;
    }

    private boolean isUserAuthorizedToModifyList(ProblemListEntity listEntity) {
        UserEntity signedInUser = getCurrentlySignedInUser();
        boolean isAdmin = signedInUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("SUPERADMIN"));
        boolean isPublicList = "global".equals(listEntity.getCreator());

        if (isPublicList) {
            return isAdmin;
        } else {
            return listEntity.getCreator().equals(signedInUser.getUsername());
        }
    }

    // private Boolean isStudyPlanOperationAllowed(ProblemListEntity listEntity, StudyPlanOperation operation) throws OperationNotSupportedException {
    //     UserEntity signedInUser = getCurrentlySignedInUser();

    //     boolean isStudyPlan = listEntity.getIsStudyPlan();
    //     boolean isAleadyActive = listEntity.getIsActive();
    //     boolean isOwner = listEntity.getCreator().equals(signedInUser.getUsername());
        
    //     if (isStudyPlan) {
    //         if (operation.equals(StudyPlanOperation.ACTIVATE)) return isOwner && !isAleadyActive;
    //         return isOwner && isAleadyActive;
    //     }

    //     return false;
    // }

    private UserEntity getCurrentlySignedInUser() {
        return (UserEntity)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}
