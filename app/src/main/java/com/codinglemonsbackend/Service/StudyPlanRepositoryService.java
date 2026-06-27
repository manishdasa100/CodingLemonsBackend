package com.codinglemonsbackend.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Entities.ProblemListEntity;
import com.codinglemonsbackend.Entities.UserStudyPlanProgress;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;
import com.codinglemonsbackend.Repository.ProblemListRepository;
import com.codinglemonsbackend.Repository.StudyPlanProgressRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudyPlanRepositoryService {
    
    private final StudyPlanProgressRepository studyPlanRepository;

    private final ProblemListRepository problemListRepository;

    public void createProgress(String listId, String owner) {
        if (listId == null || listId.isEmpty() || owner == null || owner.isEmpty()) return;
        UserStudyPlanProgress studyPlanProgress = new UserStudyPlanProgress(
            listId, 
            owner,
            LocalDate.now()
        );
        studyPlanRepository.saveStudyPlanprogress(studyPlanProgress);
    }

    public void resetProgress(String listId, String owner) {
        UserStudyPlanProgress userStudyPlanProgress = this.getStudyPlanProgress(listId, owner);
        userStudyPlanProgress.setSolvedProblemIds(Collections.emptySet());
        userStudyPlanProgress.setDateOfActivation(LocalDate.now());
        studyPlanRepository.saveStudyPlanprogress(userStudyPlanProgress);
    }

    public void deleteProgress(String listId, String owner) {
        studyPlanRepository.deleteStudyPlanProgress(listId, owner);
    }

    public UserStudyPlanProgress getStudyPlanProgress(String listId, String owner) {
        UserStudyPlanProgress userStudyPlanProgress = studyPlanRepository.getStudyPlanProgress(listId, owner)
                                                        .orElseThrow(()-> new NoSuchElementException(String.format("No study plan progress found corresponding to list %s", listId)));
        return userStudyPlanProgress;
    }

    @Async("applicationAsyncExecutor")
    @EventListener
    public void addProblemIdToProgress(SubmitCodeCompletedEvent event) {
        Integer problemId = event.getSubmissionMetadata().getProblemId();
        String listId = event.getSubmissionMetadata().getListId();
        String owner = event.getSubmissionMetadata().getUsername();
        if (listId == null) return;
        
        Optional<ProblemListEntity> listEntityOptional = problemListRepository.getUserProblemListEntityById(listId);
        Optional<UserStudyPlanProgress> studyPlanProgressOptional = studyPlanRepository.getStudyPlanProgress(listId, owner);
        
        if (listEntityOptional.isEmpty() || studyPlanProgressOptional.isEmpty()) return;
        
        ProblemListEntity listEntity = listEntityOptional.get();
        
        if (!listEntity.getProblemIds().contains(problemId)) return;
       
        studyPlanRepository.addProblemIdToProgress(listId, owner, problemId);
    }
}
