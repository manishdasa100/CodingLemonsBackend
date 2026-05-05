package com.codinglemonsbackend.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.RegistryOperationResult;
import com.codinglemonsbackend.Dto.TestcaseRegistryDto;
import com.codinglemonsbackend.Entities.TestcaseRegistry;
import com.codinglemonsbackend.Entities.TestcaseRegistry.TestcasePair;

@Repository
public class TestcaseRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Optional<TestcaseRegistry> getByProblemId(Integer problemId) {
        return Optional.ofNullable(mongoTemplate.findById(problemId, TestcaseRegistry.class));
    }

    public RegistryOperationResult syncItems(Integer problemId, TestcaseRegistryDto dto) {
        Set<String> deletions = dto.getDeletions();
        List<TestcasePair> updates = dto.getUpdates();
        List<TestcasePair> additions = dto.getAdditions();

        boolean hasAnyOperation =
                (deletions != null && !deletions.isEmpty()) ||
                (updates != null && !updates.isEmpty()) ||
                (additions != null && !additions.isEmpty());

        if (!hasAnyOperation) {
            throw new IllegalArgumentException("No operations provided. Supply at least one of: additions, updates, deletions");
        }

        if (additions != null) additions.forEach(this::validateTestcasePair);
        if (updates != null) updates.forEach(this::validateTestcasePair);

        TestcaseRegistry registry = getByProblemId(problemId).orElse(null);

        Set<String> ignoredDeletions = new HashSet<>();
        List<String> ignoredUpdates = new ArrayList<>();
        List<String> ignoredAdditions = new ArrayList<>();

        // 1. Additions
        if (additions != null && !additions.isEmpty()) {
            if (registry == null) {
                registry = new TestcaseRegistry(problemId, new ArrayList<>(additions));
            } else {
                Set<String> existingInputs = registry.getTestcases().stream()
                        .map(TestcasePair::getInput).collect(Collectors.toSet());
                additions.stream()
                        .filter(e -> existingInputs.contains(e.getInput()))
                        .map(TestcasePair::getInput)
                        .forEach(ignoredAdditions::add);
                additions.stream()
                        .filter(e -> !existingInputs.contains(e.getInput()))
                        .forEach(registry.getTestcases()::add);
            }
        }

        // 2. Updates
        if (updates != null && !updates.isEmpty()) {
            if (registry == null) {
                updates.stream().map(TestcasePair::getInput).forEach(ignoredUpdates::add);
            } else {
                Map<String, TestcasePair> updatesMap = updates.stream()
                        .collect(Collectors.toMap(TestcasePair::getInput, Function.identity()));
                Set<String> notFound = new HashSet<>(updatesMap.keySet());
                registry.getTestcases().stream()
                        .filter(tc -> updatesMap.containsKey(tc.getInput()))
                        .forEach(tc -> {
                            tc.setExpectedOutput(updatesMap.get(tc.getInput()).getExpectedOutput());
                            notFound.remove(tc.getInput());
                        });
                ignoredUpdates.addAll(notFound);
            }
        }

        // 3. Deletions
        if (deletions != null && !deletions.isEmpty()) {
            if (registry == null) {
                ignoredDeletions.addAll(deletions);
            } else {
                List<TestcasePair> current = registry.getTestcases();
                Set<String> existingInputs = current.stream().map(TestcasePair::getInput).collect(Collectors.toSet());
                deletions.stream().filter(k -> !existingInputs.contains(k)).forEach(ignoredDeletions::add);
                current.removeIf(e -> deletions.contains(e.getInput()));
            }
        }

        if (registry != null) {
            if (registry.getTestcases().isEmpty()) {
                mongoTemplate.remove(new Query(Criteria.where("_id").is(problemId)), TestcaseRegistry.class);
            } else {
                mongoTemplate.save(registry);
            }
        }

        boolean allProcessed = ignoredDeletions.isEmpty() && ignoredUpdates.isEmpty() && ignoredAdditions.isEmpty();
        return new RegistryOperationResult(problemId, allProcessed, ignoredAdditions, ignoredUpdates, ignoredDeletions);
    }

    public RegistryOperationResult deleteByProblemId(Integer problemId) {
        Query query = new Query(Criteria.where("_id").is(problemId));
        if (!mongoTemplate.exists(query, TestcaseRegistry.class)) {
            throw new NoSuchElementException("No testcase registry found for problem: " + problemId);
        }
        mongoTemplate.remove(query, TestcaseRegistry.class);
        return new RegistryOperationResult(problemId, true, null, null, null);
    }

    private void validateTestcasePair(TestcasePair testcase) {
        if (testcase == null || testcase.getInput() == null || testcase.getExpectedOutput() == null) {
            throw new IllegalArgumentException("Testcase must have non-null input and expectedOutput");
        }
    }
}
