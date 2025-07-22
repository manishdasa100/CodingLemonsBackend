package com.codinglemonsbackend.Events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;

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
        Optional<ProblemEntity> lastProblemEntity = problemsRepository.getLasEntity();
        if (lastProblemEntity.isPresent()){
            entityToSave.setPreviousProblemId(lastProblemEntity.get().getId());
            //adminService.updateProblem(lastProblemEntity.get().getId(), ProblemUpdateDto.builder().nextProblemId(entityToSave.getId()).build());
            problemsRepository.updateProblemProperties(lastProblemEntity.get().getId(), Collections.singletonMap("nextProblemId", entityToSave.getId()));
        }
    }

    @Override
    public void onBeforeDelete(BeforeDeleteEvent<ProblemEntity> event){
        System.out.println("BEFORE DELETE EVENT CALLED");
        Integer problemId = event.getSource().getInteger("_id");
        System.out.println(problemId);
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
