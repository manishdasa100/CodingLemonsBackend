package com.codinglemonsbackend.Events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;

import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.codinglemonsbackend.Service.SequenceService;

@Component
public class ProblemEntityEventListener extends AbstractMongoEventListener<ProblemEntity>{

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private ProblemsRepository problemsRepository;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<ProblemEntity> event) {
        ProblemEntity entityToSave = event.getSource();
        entityToSave.setId(sequenceService.getNextSequence(ProblemEntity.SEQUENCE_NAME));
        entityToSave.setSubmissionCount(0);
        entityToSave.setAcceptedCount(0);
        entityToSave.setLikes(0);
        entityToSave.setStatus(ProblemStatus.DRAFT);
        problemsRepository.getLasEntity()
            .ifPresent(last -> entityToSave.setPreviousProblemId(last.getId()));
    }

    /**
     * Links the previous problem forward, once there is something to link to. Doing it alongside
     * previousProblemId above meant the pointer was written before the insert: a failed insert left
     * the previous problem claiming a nextProblemId no document answers to, and walking the list
     * from there ran into nothing.
     *
     * If this update is the one that fails the list is merely missing a forward link, which is
     * recoverable by re-deriving it from the previousProblemId chain.
     */
    @Override
    public void onAfterSave(AfterSaveEvent<ProblemEntity> event) {
        ProblemEntity saved = event.getSource();
        if (saved.getPreviousProblemId() == null) return;
        problemsRepository.updateProblemProperties(saved.getPreviousProblemId(),
            Collections.singletonMap("nextProblemId", saved.getId()));
    }

    @Override
    public void onBeforeDelete(BeforeDeleteEvent<ProblemEntity> event){
        Integer problemId = event.getSource().getInteger("_id");
        Optional<ProblemDto> entityToDelete = problemsRepository.getProblemById(problemId);
        if (entityToDelete.isPresent()){
            Integer previousProblemId = entityToDelete.get().getPreviousProblemId();
            Integer nextProblemId = entityToDelete.get().getNextProblemId();
            if (previousProblemId!= null) {
                // Updating the previous problem's nextProblemId to deleted problem's nextProblemId
                Map<String, Object> propertiesMap = new HashMap<>();
                propertiesMap.put("nextProblemId", entityToDelete.get().getNextProblemId());
                problemsRepository.updateProblemProperties(previousProblemId, propertiesMap);
            }
            if (nextProblemId != null) {
                // Updating the next problem's previousProblemId to deleted problem's previousProblemId
                Map<String, Object> propertiesMap = new HashMap<>();
                propertiesMap.put("previousProblemId", entityToDelete.get().getPreviousProblemId());
                problemsRepository.updateProblemProperties(nextProblemId, propertiesMap);
            }

        }
    }
    
}
