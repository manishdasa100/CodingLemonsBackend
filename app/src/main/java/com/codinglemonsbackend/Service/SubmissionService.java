package com.codinglemonsbackend.Service;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.modelmapper.ModelMapper;

import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Entities.Submission;
import com.codinglemonsbackend.Repository.SubmissionRepository;

public abstract class SubmissionService {

    private final SubmissionRepository submissionRepository;

    private final ModelMapper modelMapper;

    public SubmissionService(SubmissionRepository submissionRepository, ModelMapper modelMapper) {
        this.submissionRepository = submissionRepository;
        this.modelMapper = modelMapper;
    }

    public abstract <T> T submitCode(SubmissionMetadata submissionDto);

    public SubmissionDto getSubmission(String submissionId){
        Optional<Submission> submissionEntity = submissionRepository.getSubmission(submissionId);
        if (submissionEntity.isEmpty()) throw new NoSuchElementException("No submission found for id "+submissionId);
        SubmissionDto submissionDto = modelMapper.map(submissionEntity.get(), SubmissionDto.class);
        return submissionDto;        
    }
}
