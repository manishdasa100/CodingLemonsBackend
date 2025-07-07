package com.codinglemonsbackend.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.TestcaseRegistryDto;
import com.codinglemonsbackend.Dto.RegistryOperationResult;
import com.codinglemonsbackend.Entities.TestcaseRegistry.TestcasePair;
import com.codinglemonsbackend.Entities.TestcaseRegistry;
import com.codinglemonsbackend.Exceptions.RegistryConversionException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class TestcaseRepositoryService implements IRegistryService<TestcaseRegistry> {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private TestcaseRegistryDto convertToDto(Object data) {
        try {
            return objectMapper.convertValue(data, TestcaseRegistryDto.class);
        } catch (IllegalArgumentException e) {
            throw new RegistryConversionException("Failed to convert request data to testcase. Please ensure all required fields are present and have correct types.", e);
        } catch (Exception e) {
            throw new RegistryConversionException("Unexpected error during data conversion: " + e.getMessage(), e);
        }
    }

    private void validateTestcasePair(TestcasePair testcase) {
        if (testcase == null) {
            throw new IllegalArgumentException("Testcase pair cannot be null");
        }
        if (testcase.getInput() == null) {
            throw new IllegalArgumentException("Testcase input cannot be null");
        }
        if (testcase.getOutput() == null) {
            throw new IllegalArgumentException("Testcase output cannot be null");
        }
    }

    private RegistryOperationResult createRegistryOperationResponse(Integer problemId, String registryId, Collection<String> ignoredInputs) {
        String allItemsAddedMessage = "All testcases were successfully processed";
        boolean allItemsAdded = ignoredInputs.isEmpty();
        String message = allItemsAdded ? 
            allItemsAddedMessage : 
            String.format("Ignored inputs %s ", ignoredInputs);

        return new RegistryOperationResult(problemId, registryId, allItemsAdded, message);
    }

    @Override
    public String getRegistryType() {
        return TestcaseRegistry.TYPE;
    }

    @Override
    public Optional<TestcaseRegistry> getRegistry(String registryId) {
        TestcaseRegistry driverCodeRegistry = mongoTemplate.findById(registryId, TestcaseRegistry.class);
        return Optional.ofNullable(driverCodeRegistry);
    }

    @Override
    public Optional<TestcaseRegistry> getRegistry(Integer problemId) {
        Query query = new Query(Criteria.where("problemId").is(problemId));
        TestcaseRegistry driverCodeRegistry = mongoTemplate.findOne(query, TestcaseRegistry.class);
        return Optional.ofNullable(driverCodeRegistry);
    }

    @Override
    public void saveRegistry(TestcaseRegistry registry) {
        mongoTemplate.save(registry);
    }


    @Override
    public RegistryOperationResult addItemsInRegistry(Integer problemId, Object data) {
        TestcaseRegistryDto testcaseRegistryDto = convertToDto(data);
        List<TestcasePair> entries = testcaseRegistryDto.getAdditions();
        
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("No items found to add in the registry");
        }

        entries.forEach(this::validateTestcasePair);

        Optional<TestcaseRegistry> optional = getRegistry(problemId);

        try {
            TestcaseRegistry testcaseRegistry = optional.get();
            Set<String> existingInputs = testcaseRegistry.getTestcases().stream()
                    .map(TestcasePair::getInput)
                    .collect(Collectors.toSet());
            
            List<String> duplicateInputs = entries.stream()
                    .map(TestcasePair::getInput)
                    .filter(existingInputs::contains)
                    .collect(Collectors.toList());

            List<TestcasePair> newEntries = entries.stream()
                    .filter(entry -> !existingInputs.contains(entry.getInput()))
                    .collect(Collectors.toList());

            testcaseRegistry.getTestcases().addAll(newEntries);
            saveRegistry(testcaseRegistry);

            RegistryOperationResult response = createRegistryOperationResponse(problemId, testcaseRegistry.getId(), duplicateInputs);
            return response;

        } catch (NoSuchElementException e) {
            String id = UUID.randomUUID().toString();
            TestcaseRegistry newTestcaseRegistry = new TestcaseRegistry(id, problemId, new ArrayList<>(entries));
            saveRegistry(newTestcaseRegistry);
            RegistryOperationResult response = createRegistryOperationResponse(problemId, id, Collections.emptyList());
            return response;
        }
    }

    @Override
    public RegistryOperationResult updateItemsInRegistry(String registryId, Object data) {
        TestcaseRegistryDto testcaseRegistryDto = convertToDto(data);
        List<TestcasePair> entries = testcaseRegistryDto.getUpdates();
        
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("No items found to update in the registry");
        }

        // Validate each testcase pair
        entries.forEach(this::validateTestcasePair);

        Optional<TestcaseRegistry> optional = getRegistry(registryId);

        try {
            TestcaseRegistry testcaseRegistry = optional.get();
            List<TestcasePair> currentTestcases = testcaseRegistry.getTestcases();

            // Get sets of inputs for efficient lookup
            Set<String> currentTestcaseInputs = currentTestcases.stream()
                    .map(TestcasePair::getInput)
                    .collect(Collectors.toSet());

            // Create a map of input to TestcasePair for quick updates
            Map<String, TestcasePair> updatesMap = entries.stream()
                    .collect(Collectors.toMap(
                        TestcasePair::getInput,
                        Function.identity()
                    ));

            // Find inputs that don't exist in current testcases
            Set<String> inputsNotFound = new HashSet<>(updatesMap.keySet());
            inputsNotFound.removeAll(currentTestcaseInputs);


            // Update existing testcases in-place
            currentTestcases.stream()
            .filter(testcasePair -> updatesMap.containsKey(testcasePair.getInput()))
            .forEach(testcasePair -> testcasePair.setOutput(updatesMap.get(testcasePair.getInput()).getOutput()));
            

            saveRegistry(testcaseRegistry);

            RegistryOperationResult response = createRegistryOperationResponse(testcaseRegistry.getProblemId(), testcaseRegistry.getId(), inputsNotFound);
            return response;

        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("No registry found with registryId: " + registryId);
        }
    }

    @Override
    public RegistryOperationResult removeItemFromRegistry(String registryId, Object data) {
        TestcaseRegistryDto testcaseRegistryDto = convertToDto(data);
        Set<String> keysToRemove = testcaseRegistryDto.getDeletions();
        
        if (keysToRemove == null || keysToRemove.isEmpty()) {
            throw new IllegalArgumentException("No items found to remove from the registry");
        }

        Optional<TestcaseRegistry> optional = getRegistry(registryId);
        
        try {
            TestcaseRegistry testcaseRegistry = optional.get();
            List<TestcasePair> currentTestcases = new ArrayList<>(testcaseRegistry.getTestcases());
            
            // Track which inputs actually exist in the registry
            Set<String> existingInputs = currentTestcases.stream()
                    .map(TestcasePair::getInput)
                    .collect(Collectors.toSet());
            
            // Find inputs that don't exist in current testcases
            Set<String> inputsNotFound = new HashSet<>(keysToRemove);
            inputsNotFound.removeAll(existingInputs);
            
            // Remove the testcases
            boolean removed = currentTestcases.removeIf(e -> keysToRemove.contains(e.getInput()));

            System.out.println("removed: " + removed);

            if (currentTestcases.isEmpty()) {
                deleteRegistry(registryId);
            } else {
                testcaseRegistry.setTestcases(currentTestcases);
                saveRegistry(testcaseRegistry);
            }

            RegistryOperationResult response = createRegistryOperationResponse(testcaseRegistry.getProblemId(), testcaseRegistry.getId(), inputsNotFound);
            return response;
            
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("No registry found for id: " + registryId);
        }
    }

    @Override
    public RegistryOperationResult deleteRegistry(String registryId) {
        Query query = new Query(Criteria.where("id").is(registryId));
        TestcaseRegistry testcaseRegistry = mongoTemplate.findOne(query, TestcaseRegistry.class);
        if (testcaseRegistry == null) {
            throw new NoSuchElementException("No registry found for id: " + registryId);
        }
        mongoTemplate.remove(query, TestcaseRegistry.class);
        return new RegistryOperationResult(
            testcaseRegistry.getProblemId(), 
            registryId, 
            null, 
            "Registry deleted successfully");
    }
}
