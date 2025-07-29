package com.codinglemonsbackend.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.DriverCodeRegistryDto;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;
import com.codinglemonsbackend.Dto.RegistryOperationResult;
import com.codinglemonsbackend.Entities.DriverCodeRegistry;
import com.codinglemonsbackend.Exceptions.RegistryConversionException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class DriverCodeRepositoryService implements IRegistryService<DriverCodeRegistry> {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getRegistryType(){
        return DriverCodeRegistry.TYPE;
    }

    @Override
    public Optional<DriverCodeRegistry> getRegistry(String registryId) {
        DriverCodeRegistry driverCodeRegistry = mongoTemplate.findById(registryId, DriverCodeRegistry.class);
        return Optional.ofNullable(driverCodeRegistry);
    }

    @Override
    public Optional<DriverCodeRegistry> getRegistry(Integer problemId) {
        Query query = new Query(Criteria.where("problemId").is(problemId));
        DriverCodeRegistry driverCodeRegistry = mongoTemplate.findOne(query, DriverCodeRegistry.class);
        return Optional.ofNullable(driverCodeRegistry);
    }

    @Override
    public void saveRegistry(DriverCodeRegistry registry) {
        mongoTemplate.save(registry);
    }
    
    @Override
    public RegistryOperationResult addItemsInRegistry(Integer problemId, Object data) {
        DriverCodeRegistryDto driverCodeRegistryDto = convertToDto(data);
        EnumMap<ProgrammingLanguage, String> itemsToAdd = driverCodeRegistryDto.getAdditions();
        if (itemsToAdd == null || itemsToAdd.isEmpty()) {
            throw new IllegalArgumentException("No items to add in the registry");
        }
        Optional<DriverCodeRegistry> optional = getRegistry(problemId);
        try {
            DriverCodeRegistry registry = optional.get();
            EnumMap<ProgrammingLanguage, String> currentItems = registry.getDriverCodes();
            Set<ProgrammingLanguage> newEntries = itemsToAdd.keySet().stream()
                                                        .filter(e -> !currentItems.containsKey(e))
                                                        .collect(Collectors.toSet());
            Set<ProgrammingLanguage> duplicateEntries = new HashSet<>(itemsToAdd.keySet());
            duplicateEntries.removeAll(newEntries);
            newEntries.forEach(entry -> currentItems.put(entry, itemsToAdd.get(entry)));
            registry.setDriverCodes(currentItems);
            saveRegistry(registry);
            return createRegistryOperationResponse(problemId, registry.getId(), duplicateEntries);
        } catch (NoSuchElementException e) {
            String id = UUID.randomUUID().toString();
            DriverCodeRegistry driverCodeRegistry = new DriverCodeRegistry(id, problemId, itemsToAdd);
            saveRegistry(driverCodeRegistry);
            return createRegistryOperationResponse(problemId, id, Collections.emptyList());
        } 
    }

    @Override
    public RegistryOperationResult updateItemsInRegistry(String registryId, Object data) {
        DriverCodeRegistryDto driverCodeRegistryDto = convertToDto(data);
        EnumMap<ProgrammingLanguage, String> updateEntries = driverCodeRegistryDto.getUpdates();
        if (updateEntries == null || updateEntries.isEmpty()) {
            throw new IllegalArgumentException("No items to update in the registry");
        }
        Optional<DriverCodeRegistry> optional = getRegistry(registryId);
        try {
            DriverCodeRegistry registry = optional.get();
            EnumMap<ProgrammingLanguage, String> currentItems = registry.getDriverCodes();
            
            // Find languages that don't exist in current registry
            Set<ProgrammingLanguage> entriesToIgnore = updateEntries.keySet().stream()
                .filter(lang -> updateEntries.get(lang) == null || !currentItems.containsKey(lang))
                .collect(Collectors.toSet());

            Set<ProgrammingLanguage> entriesToUpdate = new HashSet<>(updateEntries.keySet());
            entriesToUpdate.removeAll(entriesToIgnore);

            // Update existing items
            entriesToUpdate.forEach(entry -> currentItems.put(entry, updateEntries.get(entry)));
            
            registry.setDriverCodes(currentItems);
            saveRegistry(registry);

            return createRegistryOperationResponse(registry.getProblemId(), registry.getId(), entriesToIgnore);

        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("No registry found with id: " + registryId);
        }
    }

    @Override
    public RegistryOperationResult removeItemFromRegistry(String registryId, Object data) {
        DriverCodeRegistryDto driverCodeRegistryDto = convertToDto(data);
        Set<ProgrammingLanguage> keysToRemove = driverCodeRegistryDto.getDeletions();
        if (keysToRemove == null || keysToRemove.isEmpty()) {
            throw new IllegalArgumentException("No items to remove from the registry");
        }
        Optional<DriverCodeRegistry> optional = getRegistry(registryId);
        try {
            DriverCodeRegistry driverCodeRegistry = optional.get();
            EnumMap<ProgrammingLanguage, String> registry = driverCodeRegistry.getDriverCodes();

            // Find languages that don't exist in current registry
            Set<ProgrammingLanguage> languagesNotFound = new HashSet<>(keysToRemove);
            languagesNotFound.removeAll(registry.keySet());
            
            // Remove existing items
            keysToRemove.forEach(registry::remove);

            if (registry.isEmpty()) {
                deleteRegistry(registryId);
            } else {
                saveRegistry(driverCodeRegistry);
            }
            
            return createRegistryOperationResponse(driverCodeRegistry.getProblemId(), driverCodeRegistry.getId(), languagesNotFound);

        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("No registry found with id: " + registryId);
        }
    }

    @Override
    public RegistryOperationResult deleteRegistry(String registryId) {
        Query query = new Query(Criteria.where("id").is(registryId));
        DriverCodeRegistry driverCodeRegistry = mongoTemplate.findOne(query, DriverCodeRegistry.class);
        if (driverCodeRegistry == null) {
            throw new NoSuchElementException("No registry found for id: " + registryId);
        }
        mongoTemplate.remove(query, DriverCodeRegistry.class);
        return new RegistryOperationResult(
            driverCodeRegistry.getProblemId(), 
            registryId, 
            null, 
            "Registry deleted successfully");
    }

    private DriverCodeRegistryDto convertToDto(Object data) {
        try {
            return objectMapper.convertValue(data, DriverCodeRegistryDto.class);
        } catch (IllegalArgumentException e) {
            throw new RegistryConversionException("Failed to convert request data: " + e.getMessage(), e.getCause());
        } catch (Exception e) {
            throw new RegistryConversionException("Unexpected error during data conversion: " + e.getMessage(), e.getCause());
        }
    }

    private RegistryOperationResult createRegistryOperationResponse(Integer problemId, String registryId, Collection<ProgrammingLanguage> ignoredLanguages) {
        String allItemsProcessedMessage = "All driver codes were successfully processed";
        boolean allItemsProcessed = ignoredLanguages.isEmpty();
        String message = allItemsProcessed ? 
            allItemsProcessedMessage : 
            String.format("Ignored driver codes for %s ", ignoredLanguages);

        return new RegistryOperationResult(problemId, registryId, allItemsProcessed, message);
    }
}
