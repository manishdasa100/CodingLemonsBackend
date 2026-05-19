package com.codinglemonsbackend.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Repository.DriverCodeRepository;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.codinglemonsbackend.Repository.TestcaseRepository;

import java.util.Collections;

@Component
public class ProblemRegistryStatusListener {

    @Autowired
    private DriverCodeRepository driverCodeRepository;

    @Autowired
    private TestcaseRepository testcaseRepository;

    @Autowired
    private ProblemsRepository problemsRepository;

    @Async("applicationAsyncExecutor")
    @EventListener
    public void onProblemRegistryUpdated(ProblemRegistryUpdatedEvent event) {
        Integer problemId = event.getProblemId();
        boolean hasDriverCode = driverCodeRepository.getByProblemId(problemId).isPresent();
        boolean hasTestcases = testcaseRepository.getByProblemId(problemId).isPresent();
        ProblemStatus problemStatus = ProblemStatus.DRAFT;
        if (hasDriverCode && hasTestcases) {
            problemStatus = ProblemStatus.READY;
        }
        problemsRepository.updateProblemProperties(
            problemId,
            Collections.singletonMap("status", problemStatus)
        );
    }
}
