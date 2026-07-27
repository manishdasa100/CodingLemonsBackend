package com.codinglemonsbackend.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.SupportedLanguage;
import com.codinglemonsbackend.Entities.DriverCodeRegistry;
import com.codinglemonsbackend.Repository.DriverCodeRepository;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.codinglemonsbackend.Repository.TestcaseRepository;

import java.util.Collections;
import java.util.List;
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
        boolean hasAllDriverCode = driverCodeRepository.getByProblemId(problemId)
                                    .map(DriverCodeRegistry::getDriverCodes)
                                    .filter(dc -> dc.keySet().containsAll(List.of(SupportedLanguage.values())))
                                    .isPresent();

        boolean hasEnoughTestcases = testcaseRepository.getByProblemId(problemId)
                                    .map(t -> t.getJudgeTestcases()).map(List::size).orElse(0) >= 10;

        boolean hasExecutionLimit = problemsRepository.getProblemById(problemId)
                                    .map(ProblemDto::getExecutionLimits)
                                    .map(limits -> limits.getCpuTimeLimit() != null && limits.getMemoryLimit() != null)
                                    .orElse(false);
        
        ProblemStatus problemStatus = (hasAllDriverCode && hasEnoughTestcases && hasExecutionLimit)? ProblemStatus.READY : ProblemStatus.DRAFT;

        problemsRepository.updateProblemProperties(
            problemId,
            Collections.singletonMap("status", problemStatus)
        );
    }
}
