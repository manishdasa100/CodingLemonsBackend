package com.codinglemonsbackend.Events;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;

import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.codinglemonsbackend.Service.AdminServiceImpl;
import com.codinglemonsbackend.Service.MainService;
import com.codinglemonsbackend.Service.SequenceService;

@Component
public class ProblemEntityEventListener extends AbstractMongoEventListener<ProblemEntity>{

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private ProblemsRepository problemsRepository;

    @Autowired
    private AdminServiceImpl adminService;

    public ProblemEntityEventListener(SequenceService sequenceGeneratorService){
        this.sequenceService = sequenceGeneratorService;
    }

    @Override
    public void onBeforeConvert(BeforeConvertEvent<ProblemEntity> event) {
        System.out.println("---------------------------------------------");
        System.out.println("onBeforeConvert called" + event.getSource().getId());
        System.out.println("---------------------------------------------");
        // if (event.getSource().getProblemId() < 1) {
        ProblemEntity entityToSave = event.getSource();
        entityToSave.setId(sequenceService.getNextSequence(ProblemEntity.SEQUENCE_NAME));
        entityToSave.setSubmissionCount(0);
        entityToSave.setAcceptedCount(0);
        Optional<ProblemEntity> lastProblemEntity = problemsRepository.getLasEntity();
        if (lastProblemEntity.isPresent()){
            entityToSave.setPreviousProblemId(lastProblemEntity.get().getId());
            adminService.updateProblem(lastProblemEntity.get().getId(), ProblemUpdateDto.builder().nextProblemId(entityToSave.getId()).build());
        }
        // }
    }

    @Override
    public void onBeforeDelete(BeforeDeleteEvent<ProblemEntity> event){
        System.out.println("BEFORE DELETE EVENT CALLED");
        Integer problemId = event.getSource().getInteger("_id");
        System.out.println(problemId);
        Optional<ProblemEntity> entityToDelete = problemsRepository.getProblemById(problemId);
        if (entityToDelete.isPresent()){
            Integer previousProblemId = entityToDelete.get().getPreviousProblemId();
            Integer nextProblemId = entityToDelete.get().getNextProblemId();
            if (previousProblemId!= null) {
                // Updating the previous problem's nextProblemId to deleted problem's nextProblemId
                Map<String, Object> propertiesMap = new HashMap<>();
                propertiesMap.put("nextProblemId", entityToDelete.get().getNextProblemId());
                problemsRepository.updateProblem(previousProblemId, propertiesMap);
            }
            if (nextProblemId != null) {
                // Updating the next problem's previousProblemId to deleted problem's previousProblemId
                Map<String, Object> propertiesMap = new HashMap<>();
                propertiesMap.put("previousProblemId", entityToDelete.get().getPreviousProblemId());
                problemsRepository.updateProblem(nextProblemId, propertiesMap);
            }

        }
    }
    
}
