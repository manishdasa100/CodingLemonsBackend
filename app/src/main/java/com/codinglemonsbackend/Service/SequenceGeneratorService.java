package com.codinglemonsbackend.Service;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Entities.DatabaseSequence;
import com.codinglemonsbackend.Repository.SequenceGeneratorRepository;

@Service
public class SequenceGeneratorService {

    private SequenceGeneratorRepository sequenceGeneratorRepository;

    public SequenceGeneratorService(@Autowired SequenceGeneratorRepository sequenceGeneratorRepository) {
        this.sequenceGeneratorRepository = sequenceGeneratorRepository;
    }
    
    public Integer getSequence(String sequenceName) {
        
        DatabaseSequence counter = sequenceGeneratorRepository.getSequence(sequenceName);

        if (!Objects.isNull(counter)) {
            return counter.getSeq();
        } else {
            sequenceGeneratorRepository.saveSequence(
                new DatabaseSequence(sequenceName, 1)
            );
        }

        return 1;
    }
}
