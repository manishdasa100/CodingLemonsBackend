package com.codinglemonsbackend.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Service.SequenceService;

@Component
public class ProblemEntityEventListener extends AbstractMongoEventListener<ProblemEntity>{

    private SequenceService sequenceService;

    public ProblemEntityEventListener(@Autowired SequenceService sequenceGeneratorService){
        this.sequenceService = sequenceGeneratorService;
    }

    @Override
    public void onBeforeConvert(BeforeConvertEvent<ProblemEntity> event) {
        System.out.println("---------------------------------------------");
        System.out.println("onBeforeConvert called" + event.getSource().getProblemId());
        System.out.println("---------------------------------------------");
        // if (event.getSource().getProblemId() < 1) {
        event.getSource().setProblemId(sequenceService.getNextSequence(ProblemEntity.SEQUENCE_NAME));
        // }
    }
    
}
