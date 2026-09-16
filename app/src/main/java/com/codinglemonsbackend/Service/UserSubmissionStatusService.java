package com.codinglemonsbackend.Service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Dto.UserSubmissionStatusDto;
import com.codinglemonsbackend.Entities.UserSubmissionStatusEntity;
import com.codinglemonsbackend.Repository.UserSubmissionStatusRepository;

@Service
public class UserSubmissionStatusService {

    @Autowired
    private UserSubmissionStatusRepository userSubmissionStatusRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createForUser(String username) {
        userSubmissionStatusRepository.createForUser(username);
    }

    /**
     * Moves problemId to the solved set and removes it from attempted.
     *
     * @return true if this is a new solve (problem was not previously in solved set)
     */
    public boolean addToSolvedAndRemoveFromAttempted(SubmissionMetadata metadata) {
        String username = metadata.getUsername();
        Integer problemId = metadata.getProblemId();
        String difficulty = metadata.getDifficulty().name();
        String language = metadata.getLanguage().name().toLowerCase();
        return userSubmissionStatusRepository.addToSolvedAndRemoveFromAttempted(username, problemId, difficulty, language);
    }

    public UserSubmissionStatusDto getSubmissionStatusDto(String username) {
        UserSubmissionStatusEntity entity = userSubmissionStatusRepository.getSubmissionStatusDto(username);
        if (entity == null) return new UserSubmissionStatusDto(username, Map.of(), Map.of());
        Map<String, Integer> counts = entity.getSolvedCountByDifficulty() != null
                ? entity.getSolvedCountByDifficulty()
                : Map.of();
        Map<String, Integer> languageCounts = entity.getSolvedCountByLanguage() != null
                ? entity.getSolvedCountByLanguage()
                : Map.of();
        return new UserSubmissionStatusDto(username, counts, languageCounts);
    }

    /**
     * Adds problemId to the attempted set only if it is not already in the solved set.
     */
    public void addToAttemptedIfNotSolved(SubmissionMetadata metadata) {
        String username = metadata.getUsername();
        Integer problemId = metadata.getProblemId();
        userSubmissionStatusRepository.addToAttemptedIfNotSolved(username, problemId);
    }

    public UserSubmissionStatusEntity getSubmissionStatus(String username) {
        return userSubmissionStatusRepository.getByUsername(username);
    }
}
