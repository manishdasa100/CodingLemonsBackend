package com.codinglemonsbackend.Repository;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.DriverCodeRegistryDto;
import com.codinglemonsbackend.Dto.SupportedLanguage;
import com.codinglemonsbackend.Dto.RegistryOperationResult;
import com.codinglemonsbackend.Entities.DriverCodeRegistry;

@Repository
public class DriverCodeRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Optional<DriverCodeRegistry> getByProblemId(Integer problemId) {
        Query query = new Query(Criteria.where("problemId").is(problemId));
        return Optional.ofNullable(mongoTemplate.findOne(query, DriverCodeRegistry.class));
    }

    public RegistryOperationResult syncItems(Integer problemId, DriverCodeRegistryDto dto) {
        Set<SupportedLanguage> deletions = dto.getDeletions();
        EnumMap<SupportedLanguage, String> updates = dto.getUpdates();
        EnumMap<SupportedLanguage, String> additions = dto.getAdditions();

        boolean hasAnyOperation =
                (deletions != null && !deletions.isEmpty()) ||
                (updates != null && !updates.isEmpty()) ||
                (additions != null && !additions.isEmpty());

        if (!hasAnyOperation) {
            throw new IllegalArgumentException("No operations provided. Supply at least one of: additions, updates, deletions");
        }

        DriverCodeRegistry registry = getByProblemId(problemId).orElse(null);

        Set<String> ignoredDeletions = new HashSet<>();
        List<SupportedLanguage> ignoredUpdates = new ArrayList<>();
        List<SupportedLanguage> ignoredAdditions = new ArrayList<>();

        // 1. Deletions
        if (deletions != null && !deletions.isEmpty()) {
            if (registry == null) {
                deletions.stream().map(SupportedLanguage::name).forEach(ignoredDeletions::add);
            } else {
                EnumMap<SupportedLanguage, String> current = registry.getDriverCodes();
                deletions.stream().filter(lang -> !current.containsKey(lang))
                        .map(SupportedLanguage::name)
                        .forEach(ignoredDeletions::add);
                deletions.forEach(current::remove);
            }
        }

        // 2. Updates
        if (updates != null && !updates.isEmpty()) {
            if (registry == null) {
                ignoredUpdates.addAll(updates.keySet());
            } else {
                EnumMap<SupportedLanguage, String> current = registry.getDriverCodes();
                updates.keySet().stream().filter(lang -> !current.containsKey(lang)).forEach(ignoredUpdates::add);
                updates.entrySet().stream()
                        .filter(e -> current.containsKey(e.getKey()))
                        .forEach(e -> current.put(e.getKey(), e.getValue()));
            }
        }

        // 3. Additions
        if (additions != null && !additions.isEmpty()) {
            if (registry == null) {
                registry = new DriverCodeRegistry(problemId, new EnumMap<>(additions));
            } else {
                EnumMap<SupportedLanguage, String> current = registry.getDriverCodes();
                additions.keySet().stream().filter(current::containsKey).forEach(ignoredAdditions::add);
                additions.entrySet().stream()
                        .filter(e -> !current.containsKey(e.getKey()))
                        .forEach(e -> current.put(e.getKey(), e.getValue()));
            }
        }

        if (registry != null) {
            if (registry.getDriverCodes().isEmpty()) {
                mongoTemplate.remove(new Query(Criteria.where("_id").is(problemId)), DriverCodeRegistry.class);
            } else {
                mongoTemplate.save(registry);
            }
        }

        boolean allProcessed = ignoredDeletions.isEmpty() && ignoredUpdates.isEmpty() && ignoredAdditions.isEmpty();
        return new RegistryOperationResult(problemId, allProcessed, ignoredAdditions, ignoredUpdates, ignoredDeletions);
    }

    public RegistryOperationResult deleteByProblemId(Integer problemId) {
        Query query = new Query(Criteria.where("_id").is(problemId));
        if (!mongoTemplate.exists(query, DriverCodeRegistry.class)) {
            throw new NoSuchElementException("No driver code registry found for problem: " + problemId);
        }
        mongoTemplate.remove(query, DriverCodeRegistry.class);
        return new RegistryOperationResult(problemId, true, null, null, null);
    }
}
