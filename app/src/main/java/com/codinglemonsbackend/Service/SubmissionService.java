package com.codinglemonsbackend.Service;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.modelmapper.ModelMapper;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Entities.SubmissionEntity;
import com.codinglemonsbackend.Repository.SubmissionRepository;

public abstract class SubmissionService {

    private final SubmissionRepository submissionRepository;

    private final ModelMapper modelMapper;

    public SubmissionService(SubmissionRepository submissionRepository, ModelMapper modelMapper) {
        this.submissionRepository = submissionRepository;
        this.modelMapper = modelMapper;
    }

    public abstract String queueSubmission(SubmissionMetadata submissionDto);

    public abstract ExecutionReportDto constructExecutionReport(String report);

    public abstract ExecutorWorkerType getWorkerType();

    public void saveSubmission(ExecutionReportDto executionReport, SubmissionMetadata submissionMetadata) {
        SubmissionEntity submission = SubmissionEntity.builder()
                        .submissionId(executionReport.executionId())
                        .username(submissionMetadata.getUsername())
                        .problemId(submissionMetadata.getProblemId())
                        .language(submissionMetadata.getLanguage())
                        .userCode(submissionMetadata.getUserCode())
                        .dateOfSubmission(java.time.LocalDate.now().toString())
                        .runSucccess(!EnumSet.of(ExecutionStatus.CE, ExecutionStatus.RE, ExecutionStatus.IE).contains(executionReport.status()))
                        .totalTestCases(executionReport.totalTestcases())
                        .totalCorrectOutput(executionReport.totalCorrect())
                        .runtimeMs(executionReport.runtimeMs())
                        .memoryMb(executionReport.memoryMb())
                        .failedTestCase(executionReport.failedTestcase() != null
                                ? executionReport.failedTestcase() : null)
                        .status(executionReport.status())
                        .build();
        submissionRepository.saveSubmission(submission);
    }

    public SubmissionDto getSubmission(String submissionId){
        Optional<SubmissionEntity> submissionEntity = submissionRepository.getSubmission(submissionId);
        if (submissionEntity.isEmpty()) throw new NoSuchElementException("No submission found for id "+submissionId);
        SubmissionDto submissionDto = modelMapper.map(submissionEntity.get(), SubmissionDto.class);
        return submissionDto;        
    }
}
