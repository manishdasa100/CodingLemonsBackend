package com.codinglemonsbackend.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Repository.DriverCodeRepository;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.codinglemonsbackend.Repository.TestcaseRepository;

import java.util.Collections;
import java.util.NoSuchElementException;

@Component
public class ProblemArtifactsUpdatedListener {

    @Autowired
    private DriverCodeRepository driverCodeRepository;

    @Autowired
    private TestcaseRepository testcaseRepository;

    @Autowired
    private ProblemsRepository problemsRepository;

    @Async("applicationAsyncExecutor")
    @EventListener
    public void onProblemArtifactsUpdated(ProblemArtifactsUpdatedEvent event) {
        Integer problemId = event.getProblemId();
        boolean hasDriverCode = driverCodeRepository.getByProblemId(problemId).isPresent();
        boolean hasTestcases = testcaseRepository.getByProblemId(problemId).isPresent() 
                    && testcaseRepository.getByProblemId(problemId).get().getJudgeTestcases() != null
                    && !testcaseRepository.getByProblemId(problemId).get().getJudgeTestcases().isEmpty();

        ProblemDto problem = problemsRepository.getProblemById(problemId).orElseThrow(() -> new NoSuchElementException("Problem not found for ID: " + problemId));
        
        var executionLimits = problem.getExecutionLimits();
        boolean isCalibrated = executionLimits != null && executionLimits.getCpuTimeLimit() != null && executionLimits.getMemoryLimit() != null;

        ProblemStatus problemStatus = (hasDriverCode && hasTestcases && isCalibrated)? ProblemStatus.READY : ProblemStatus.DRAFT;

        problemsRepository.updateProblemProperties(
            problemId,
            Collections.singletonMap("status", problemStatus)
        );
    }
}
