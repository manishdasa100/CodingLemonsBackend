package com.codinglemonsbackend.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Entities.ProblemListEntity;
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
        List<ProblemListEntity> userProblemListEntities = problemListRepository.getAllProblemListsOfUser(username);

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
        return problemListRepository.getUserProblemListDetails(creator, name)
            .orElseThrow(() -> new NoSuchElementException(String.format("The list with name %s does not exist!!", name)));
    }

    public void saveProblemList(ProblemListEntity newProblemList) throws DuplicateResourceException {
        problemListRepository.saveProblemList(newProblemList);
    }

    public void addProblemToProblemList(String listId, Set<Integer> validProblemIds) {
        problemListRepository.addProblemToProblemList(listId, validProblemIds);
    }

    public void removeProblemFromProblemList(String listId, Set<Integer> problemIdsToRemove) {
        problemListRepository.removeProblemFromProblemList(listId, problemIdsToRemove);
    }

    public Map<String, Object> updateProblemList(String listId, UpdateProblemListRequest newListDetails) {
        ObjectId objectId;

        try {
            objectId = new ObjectId(listId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid problem list id");
        }

        ProblemListEntity listEntity = problemListRepository.getUserProblemListEntityById(objectId)
            .orElseThrow(() -> new NoSuchElementException(String.format("The requested list id %s not found!!", listId)));

        UserEntity signedInUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

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
            return problemListRepository.updateProblemList(objectId, fieldsToUpdate, listEntity);
        }

        return new HashMap<>();
    }

    public Boolean deleteProblemList(String id) {
        return true;
    }
}
