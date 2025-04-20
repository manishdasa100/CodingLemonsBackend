package com.codinglemonsbackend.Service;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Entities.DatabaseSequence;
import com.codinglemonsbackend.Repository.SequenceGeneratorRepository;

@Service
public class SequenceService {

    private SequenceGeneratorRepository sequenceGeneratorRepository;

    public SequenceService(@Autowired SequenceGeneratorRepository sequenceGeneratorRepository) {
        this.sequenceGeneratorRepository = sequenceGeneratorRepository;
    }
    
    public Integer getNextSequence(String sequenceName) {
        
        DatabaseSequence counter = sequenceGeneratorRepository.getNextSequence(sequenceName);

        if (!Objects.isNull(counter)) {
            return counter.getSeq();
        } else {
            sequenceGeneratorRepository.saveSequence(
                new DatabaseSequence(sequenceName, 1)
            );
        }

        return 1;
    }

    public Integer getCurrentSequence(String sequenceName) {

        DatabaseSequence seq = sequenceGeneratorRepository.getCurrentSequence(sequenceName);

        if (!Objects.isNull(seq)) {
            return seq.getSeq();
        }

        return -1;
    }
}
