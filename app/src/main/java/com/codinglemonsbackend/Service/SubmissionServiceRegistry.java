package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ExecutorWorkerType;

/**
 * Registry that maps each ExecutorWorkerType to its corresponding SubmissionService.
 * Spring auto-populates the list with all SubmissionService beans, so adding a new
 * executor only requires creating a new @Service subclass — no changes needed here.
 */
@Component
public class SubmissionServiceRegistry {

    private final Map<ExecutorWorkerType, SubmissionService> registry;

    public SubmissionServiceRegistry(List<SubmissionService> services) {
        this.registry = services.stream()
                .collect(Collectors.toMap(
                        SubmissionService::getWorkerType,
                        Function.identity()
                ));
    }

    public SubmissionService getService(ExecutorWorkerType workerType) {
        SubmissionService service = registry.get(workerType);
        if (service == null) {
            throw new IllegalArgumentException(
                    "No SubmissionService registered for worker type: " + workerType);
        }
        return service;
    }
}
