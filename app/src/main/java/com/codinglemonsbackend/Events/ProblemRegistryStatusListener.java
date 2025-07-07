package com.codinglemonsbackend.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Repository.DriverCodeRepositoryService;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.codinglemonsbackend.Repository.TestcaseRepositoryService;

import java.util.Collections;
import java.util.Optional;

@Component
public class ProblemRegistryStatusListener {

    @Autowired
    private DriverCodeRepositoryService driverCodeRepositoryService;

    @Autowired
    private TestcaseRepositoryService testcaseRepositoryService;

    @Autowired
    private ProblemsRepository problemsRepository;

    @EventListener
    public void onProblemRegistryUpdated(ProblemRegistryUpdatedEvent event) {
        Integer problemId = event.getProblemId();
        boolean hasDriverCode = driverCodeRepositoryService.getRegistry(problemId).isPresent();
        boolean hasTestcases = testcaseRepositoryService.getRegistry(problemId).isPresent();
        ProblemStatus problemStatus = ProblemStatus.DRAFT;
        if (hasDriverCode && hasTestcases) {
            problemStatus = ProblemStatus.READY;
        }
        problemsRepository.updateProblemProperties(
            problemId,
            Collections.singletonMap("status", problemStatus),
            ProblemEntity.class
        );
    }
}
