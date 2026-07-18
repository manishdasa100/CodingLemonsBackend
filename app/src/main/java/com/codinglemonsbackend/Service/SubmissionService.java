package com.codinglemonsbackend.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Entities.SubmissionEntity;
import com.codinglemonsbackend.Entities.UserEntity;
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
                        .dateOfSubmission(Instant.now().toString())
                        .runSucccess(!EnumSet.of(ExecutionStatus.CE, ExecutionStatus.RE, ExecutionStatus.IE).contains(executionReport.status()))
                        .totalTestCases(executionReport.totalTestcases())
                        .totalCorrectOutput(executionReport.totalCorrect())
                        .runtimeMs(executionReport.runtimeMs())
                        .memoryMb(executionReport.memoryMb())
                        .failedTestCase(executionReport.failedTestcase())
                        .status(executionReport.status())
                        .build();
        submissionRepository.saveSubmission(submission);
    }

    public SubmissionDto getUserSubmission(String submissionId){
        UserEntity signedInUser = (UserEntity)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return submissionRepository.getUserSubmissionById(signedInUser.getUsername(), submissionId)
                                        .orElseThrow(() -> new NoSuchElementException(String.format("No user submission found with id %s", submissionId)));
    }

    public List<SubmissionDto> getUserSubmissions(Integer problemId) {
        UserEntity signedInUser = (UserEntity)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<SubmissionEntity> submissions = submissionRepository.getUserSubmissionsByProblemId(signedInUser.getUsername(), problemId);
        List<SubmissionDto> submissionDtos = submissions.stream().map((e) -> modelMapper.map(e, SubmissionDto.class)).toList();
        return submissionDtos;
    }

    public List<SubmissionDto> getRecentUserSubmission(String username, Integer limit) {
        if (limit == null || limit < 0) limit = -1;
        return submissionRepository.getRecentUserSubmissions(username, limit);
    }
}
