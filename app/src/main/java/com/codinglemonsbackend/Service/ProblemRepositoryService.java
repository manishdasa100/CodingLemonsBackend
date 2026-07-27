package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ProblemDto.Example;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDto.Difficulty;
import com.codinglemonsbackend.Dto.ProblemsPage;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.SupportedLanguage;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.ProblemExecutionLimits;
import com.codinglemonsbackend.Entities.Topic;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.codinglemonsbackend.Repository.TopicRepository;
import com.mongodb.client.result.DeleteResult;


@Service
@CacheConfig(cacheNames = RedisService.DEFAULT_CACHE)
public class ProblemRepositoryService {

    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private ProblemsRepository problemsRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private CompanyService companyService;

    @Cacheable(cacheNames = RedisService.ALL_PROBLEMS_CACHE, condition = "!#isAdmin")
    public ProblemsPage getAllProblems(Integer page, Integer size, Boolean isAdmin) {
        System.out.println("CACHE MISS");
        return problemsRepository.getProblems(null, null, null, page, size, isAdmin);
    }

    public ProblemsPage getFilteredProblems(String difficultyStr, String topicsStr, String companiesStr, int page, int size, Boolean isAdmin) {

        Difficulty[] difficulties = null;
        String[] topicSlugs = null;
        String[] companySlugs = null;

        if (StringUtils.isNotBlank(difficultyStr)) {
            String[] difficultiesArray = Arrays.stream(difficultyStr.trim().split(","))
                                        .map(String::trim)
                                        .filter(e -> !e.isEmpty())
                                        .toArray(String[]::new);
            difficulties = Arrays.stream(difficultiesArray).map(String::toUpperCase)
                           .filter(e -> EnumUtils.isValidEnum(Difficulty.class, e))
                           .map(Difficulty::valueOf)
                           .toArray(Difficulty[]::new);
        }
        if (StringUtils.isNotBlank(topicsStr)) {
            topicSlugs = Arrays.stream(topicsStr.trim().split(","))
                            .map(String::trim)
                            .filter(e -> !e.isEmpty())
                            .toArray(String[]::new);
        }
        if (StringUtils.isNotBlank(companiesStr)) {
            companySlugs = Arrays.stream(companiesStr.trim().split(","))
                            .map(String::trim)
                            .filter(e -> !e.isEmpty())
                            .toArray(String[]::new);
        }

        return problemsRepository.getProblems(difficulties, topicSlugs, companySlugs, page, size, isAdmin);
    }

    @CacheEvict(cacheNames = RedisService.ALL_PROBLEMS_CACHE)
    public ProblemEntity addProblem(ProblemDto problemDto) throws Exception {
        ProblemEntity entity = modelMapper.map(problemDto, ProblemEntity.class);
        ProblemEntity savedEntity = problemsRepository.addProblem(entity);
        return savedEntity;
    }

    public Boolean problemExists(Integer problemId) {
        return problemsRepository.problemExists(problemId);
    }

    public ProblemDto getProblemById(Integer id) {
        return problemsRepository.getProblemById(id).orElseThrow(
            () -> new NoSuchElementException("Problem Id "+ id + " does not exist")
        );
    }

    public List<ProblemDto> getProblemsByIds(List<Integer> problemIds, Boolean isAdmin) {
        return problemsRepository.getProblemsByIds(problemIds, isAdmin);
    }

    @CacheEvict(cacheNames = RedisService.ALL_PROBLEMS_CACHE)
    public long updateProblem(Integer problemId, ProblemUpdateDto problemUpdateDto) {
        
        if (!problemsRepository.problemExists(problemId)) {
            throw new NoSuchElementException("Problem Id "+ problemId + " does not exist");
        }
        
        Map<String, Object> updatesMetadata = problemUpdateDto.getUpdates();
        if (updatesMetadata == null) {
            throw new IllegalArgumentException("No updates provided");
        }

        Map<String, Object> validUpdates = new HashMap<>();

        if (updatesMetadata.containsKey("title")) {
            Object titleObj = updatesMetadata.get("title");
            if (titleObj instanceof String && StringUtils.isNotBlank((String) titleObj)) {
                validUpdates.put("title", titleObj);
            } else {
                throw new IllegalArgumentException("Title must be a non-blank string");
            }
        }

        if (updatesMetadata.containsKey("description")) {
            Object descObj = updatesMetadata.get("description");
            if (descObj instanceof String && StringUtils.isNotBlank((String) descObj)) {
                validUpdates.put("description", descObj);
            } else {
                throw new IllegalArgumentException("Description must be a non-blank string");
            }
        }

        if (updatesMetadata.containsKey("difficulty")) {
            Object diffObj = updatesMetadata.get("difficulty");
            if (diffObj instanceof String) {
                try{
                    validUpdates.put("difficulty", Difficulty.valueOf((String) diffObj));
                }catch(IllegalArgumentException e){
                    throw new IllegalArgumentException("Invalid difficulty value. Difficulty must be one of the following: " + Arrays.toString(Difficulty.values()));
                }
            } else {
                throw new IllegalArgumentException("Difficulty must be a string and one of the following: " + Arrays.toString(Difficulty.values()));
            }
        }

        if (updatesMetadata.containsKey("constraints")) {
            Object constraintsObj = updatesMetadata.get("constraints");
            if (constraintsObj instanceof List<?>) {
                List<?> constraintsList = (List<?>) constraintsObj;
                if (!constraintsList.isEmpty() && constraintsList.stream().allMatch(item -> item instanceof String && StringUtils.isNotBlank((String)item))) {
                    @SuppressWarnings("unchecked")
                    List<String> constraints = (List<String>) constraintsList;
                    validUpdates.put("constraints", constraints);
                } else {
                    throw new IllegalArgumentException("Constraints must be a non-empty list of non-blank strings");
                }
            } else {
                throw new IllegalArgumentException("Constraints must be a List");
            }
        }

        if (updatesMetadata.containsKey("examples")) {
            Object examplesObj = updatesMetadata.get("examples");
            if (examplesObj instanceof List<?>) {
                List<?> examplesList = (List<?>) examplesObj;
                if (!examplesList.isEmpty()) {
                    List<Example> examples = new ArrayList<>();
                    for (Object exampleObj : examplesList) {
                        if (!(exampleObj instanceof Map<?, ?>)) {
                            throw new IllegalArgumentException("Invalid example format");
                        } 
                        Map<?, ?> exampleMap = (Map<?, ?>) exampleObj;
                        if (!exampleMap.containsKey("input") || !exampleMap.containsKey("output")) {
                            throw new IllegalArgumentException("Invalid example format. Example must contain input, output and explanation(optional)");
                        }
                        
                        Object inputObj = exampleMap.get("input"); 
                        Object outputObj = exampleMap.get("output");
                        Object explanationObj = exampleMap.get("explanation");
                        if (!(inputObj instanceof String) || StringUtils.isBlank((String)inputObj)
                            || !(outputObj instanceof String) || StringUtils.isBlank((String)outputObj)
                            || (explanationObj != null && (!(explanationObj instanceof String) || StringUtils.isBlank((String)explanationObj)))) {
                            throw new IllegalArgumentException("Invalid example format. Input, output and explanation must be non-blank strings");
                        }
                        Example example = new Example(((String) inputObj), ((String) outputObj), ((String) explanationObj));
                        examples.add(example);
                    }
                    validUpdates.put("examples", examples);
                } else {
                    throw new IllegalArgumentException("Examples list must be non-empty");
                }
            } else {
                throw new IllegalArgumentException("Examples must be a List");
            }
        }

        if (updatesMetadata.containsKey("codeSnippets")) {
            Object snippetsObj = updatesMetadata.get("codeSnippets");
            if (snippetsObj instanceof Map<?, ?>) {
                Map<?, ?> snippetsMap = (Map<?, ?>) snippetsObj;
                if (!snippetsMap.isEmpty()) {
                    Map<SupportedLanguage, String> codeSnippets = new HashMap<>();
                    for (Map.Entry<?, ?> entry : snippetsMap.entrySet()) {
                        try{
                            SupportedLanguage language = SupportedLanguage.valueOf((String) entry.getKey());
                            String code = (String) entry.getValue();
                            if (StringUtils.isBlank(code)) {
                                throw new IllegalArgumentException(String.format("Code snippet for %s must be a non-empty String", language));
                            }
                            codeSnippets.put(language, code);
                        }catch(IllegalArgumentException e){
                            throw new IllegalArgumentException("Invalid Programming language or code snippet found. Actual error: " + e.getMessage());
                        } catch(ClassCastException e){
                            throw new IllegalArgumentException("Code snippet must be a String");
                        }
                    }
                    validUpdates.put("codeSnippets", codeSnippets);
                } else {
                    throw new IllegalArgumentException("Code snippets must be a non-empty map of ProgrammingLanguage to String");
                }
            } else {
                throw new IllegalArgumentException("Code snippets must be a Map");
            }
        }

        if (updatesMetadata.containsKey("topics")) {
            Object topicsObj = updatesMetadata.get("topics");
            if (topicsObj instanceof List<?>) {
                List<?> topicsList = (List<?>) topicsObj;
                if (!topicsList.isEmpty() && topicsList.stream().allMatch(item -> item instanceof String)) {
                    @SuppressWarnings("unchecked")
                    List<String> newTopics = (List<String>) topicsList;
                    Set<Topic> newValidTopicTags = topicRepository.getValidTags(new HashSet<>(newTopics));
                    if (newValidTopicTags.size() != newTopics.size()) {
                        throw new IllegalArgumentException("Invalid topic slugs. Some topics do not exist");
                    }
                    validUpdates.put("topics", newValidTopicTags.stream().map(Topic::getSlug).collect(Collectors.toSet()));
                } else {
                    throw new IllegalArgumentException("Topics must be a non-empty set of strings");
                }
            } else {
                throw new IllegalArgumentException("Topics must be a Set");
            }
        }

        if (updatesMetadata.containsKey("companies")) {
            Object companiesObj = updatesMetadata.get("companies");
            if (companiesObj instanceof List<?>) {
                List<?> companiesList = (List<?>) companiesObj;
                if (!companiesList.isEmpty() && companiesList.stream().allMatch(item -> item instanceof String)) {
                    @SuppressWarnings("unchecked")
                    List<String> newCompanies = (List<String>) companiesList;
                    Set<CompanyDto> newValidCompanyTags = companyService.getValidTags(new HashSet<>(newCompanies));
                    if (newValidCompanyTags.size() != newCompanies.size()) {
                        throw new IllegalArgumentException("Invalid company slugs. Some companies do not exist");
                    }
                    validUpdates.put("companies", newValidCompanyTags.stream().map(CompanyDto::getSlug).collect(Collectors.toSet()));
                } else {
                    throw new IllegalArgumentException("Companies must be a non-empty set of strings");
                }
            } else {
                throw new IllegalArgumentException("Companies must be a Set");
            }
        }

        if (updatesMetadata.containsKey("status")) {
            Object statusObj = updatesMetadata.get("status");
            if (statusObj instanceof String) {
                try{
                    validUpdates.put("status", ProblemStatus.valueOf((String) statusObj));
                }catch(IllegalArgumentException e){
                    throw new IllegalArgumentException("Invalid status value. Status must be one of the following: " + Arrays.toString(ProblemStatus.values()));
                }
            } else {
                throw new IllegalArgumentException("Status must be a string and one of the following: " + Arrays.toString(ProblemStatus.values()));
            }
        }

        if (validUpdates.isEmpty()) {
            throw new IllegalArgumentException("No valid updates provided");
        }

        return problemsRepository.updateProblemProperties(problemId, validUpdates);
    }

    public void calibrateProblem(Integer problemId, ProblemExecutionLimits executionLimits) {
        if (!problemsRepository.problemExists(problemId)) {
            throw new NoSuchElementException("Problem Id "+ problemId + " does not exist");
        }
        problemsRepository.updateProblemProperties(
            problemId, 
            Map.of(
                "executionLimits", executionLimits
            )
        );
    }

    @Async("applicationAsyncExecutor")
    @EventListener
    public void onSubmitCodeCompleted(SubmitCodeCompletedEvent event) {
        Integer problemId = event.getSubmissionMetadata().getProblemId();
        boolean accepted = event.getExecutionReport().status() == ExecutionStatus.ACC;
        problemsRepository.incrementProblemStats(problemId, accepted);
    }

    @Cacheable(cacheNames = RedisService.PROBLEM_COUNT_BY_DIFFICULTY_CACHE)
    public Map<String, Integer> getPublishedProblemCountByDifficulty() {
        return problemsRepository.getPublishedCountByDifficulty();
    }

    @CacheEvict(cacheNames = {RedisService.ALL_PROBLEMS_CACHE, RedisService.PROBLEM_COUNT_BY_DIFFICULTY_CACHE})
    public void deleteProblemById(Integer problemId){

        DeleteResult result = problemsRepository.removeProblemById(problemId);

        if (result.getDeletedCount()<1) throw new NoSuchElementException("Problem Id "+ problemId + " not present");
    }

    @CacheEvict(cacheNames = RedisService.ALL_PROBLEMS_CACHE)
    public void removeAllProblems() {
        problemsRepository.removeAllProblems();
    }

}
