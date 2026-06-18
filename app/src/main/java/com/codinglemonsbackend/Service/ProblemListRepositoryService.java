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

    public void updateProblemList(UpdateProblemListRequest newListDetails) {

        Map<String, Object> fieldsToUpdate = new HashMap<>();

        if (StringUtils.isNotBlank(newListDetails.getName())) {
            fieldsToUpdate.put("name", newListDetails.getName());
        }

        if (StringUtils.isNotBlank(newListDetails.getDescription())) {
            fieldsToUpdate.put("description", newListDetails.getDescription());
        }

        if (Objects.nonNull(newListDetails.getIsStudyPlan())) {
            StudyPlanDifficultyTier difficultyTier = null;
            Integer timelineDays = null;
            if (newListDetails.getIsStudyPlan()) {
                difficultyTier = newListDetails.getDifficultyTier();
                timelineDays = newListDetails.getTimelineDays();
            }
            fieldsToUpdate.put("isStudyPlan", newListDetails.getIsStudyPlan());
            fieldsToUpdate.put("difficultyTier", difficultyTier);
            fieldsToUpdate.put("timelineDays", timelineDays);
        }

        if (Objects.nonNull(newListDetails.getIsPublic())) {
            fieldsToUpdate.put("isPublic", newListDetails.getIsPublic());
        }

        if (Objects.nonNull(newListDetails.getIsPinned())) {
            fieldsToUpdate.put("isPinned", newListDetails.getIsPinned());
        }

        if (!fieldsToUpdate.isEmpty()) {
            problemListRepository.updateProblemList(newListDetails.getId(), fieldsToUpdate);
        }
    }

    public Boolean deleteProblemList(String id) {
        return true;
    }
}
