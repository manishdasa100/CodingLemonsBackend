package com.codinglemonsbackend.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Service.SequenceGeneratorService;

@Component
public class ProblemEntityEventListener extends AbstractMongoEventListener<ProblemEntity>{

    private SequenceGeneratorService sequenceGenerator;

    public ProblemEntityEventListener(@Autowired SequenceGeneratorService sequenceGeneratorService){
        this.sequenceGenerator = sequenceGeneratorService;
    }

    @Override
    public void onBeforeConvert(BeforeConvertEvent<ProblemEntity> event) {
        System.out.println("---------------------------------------------");
        System.out.println("onBeforeConvert called" + event.getSource().getProblemId());
        System.out.println("---------------------------------------------");
        // if (event.getSource().getProblemId() < 1) {
        event.getSource().setProblemId(sequenceGenerator.getSequence(ProblemEntity.SEQUENCE_NAME));
        // }
    }
    
}
