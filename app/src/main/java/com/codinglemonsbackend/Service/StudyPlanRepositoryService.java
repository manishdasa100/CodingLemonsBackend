package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Entities.UserStudyPlanProgress;
import com.codinglemonsbackend.Repository.StudyPlanProgressRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudyPlanRepositoryService {
    
    private final StudyPlanProgressRepository studyPlanRepository;

    public void createProgress(String listId, String owner) {
        if (listId == null || listId.isEmpty() || owner == null || owner.isEmpty()) return;
        UserStudyPlanProgress studyPlanProgress = new UserStudyPlanProgress(listId, owner);
        studyPlanRepository.saveStudyPlanprogress(studyPlanProgress);
    }

    public void resetProgress(String listId, String owner) {
        UserStudyPlanProgress userStudyPlanProgress = this.getStudyPlanProgress(listId, owner);
        userStudyPlanProgress.setSolvedProblemIds(List.of());
        studyPlanRepository.saveStudyPlanprogress(userStudyPlanProgress);
    }

    public void deleteProgress(String listId, String owner) {
        studyPlanRepository.deleteStudyPlanProgress(listId, owner);
    }

    public UserStudyPlanProgress getStudyPlanProgress(String listId, String owner) {
        UserStudyPlanProgress userStudyPlanProgress = studyPlanRepository.getStudyPlanProgress(listId, owner)
                                                        .orElseThrow(()-> new NoSuchElementException(String.format("No study plan progress found corresponding to list {}", listId)));
        return userStudyPlanProgress;
    }
}
