package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.Difficulty;
import com.codinglemonsbackend.Dto.Example;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;
import com.codinglemonsbackend.Entities.Company;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.Topic;
import com.codinglemonsbackend.Repository.CompanyRepository;
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

    @Cacheable(cacheNames = RedisService.ALL_PROBLEMS_CACHE)
    public ProblemSet getAllProblems(Integer page, Integer size) {
        System.out.println("CACHE MISS");
        return problemsRepository.findAll(page, size);
    }

    public ProblemSet getFilteredProblems(String difficultyStr, String topicsStr, String companiesStr, int page, int size){
        
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

        return problemsRepository.getProblems(difficulties, topicSlugs, companySlugs, page, size);
    }

    @CacheEvict(cacheNames = RedisService.ALL_PROBLEMS_CACHE)
    public ProblemEntity addProblem(ProblemDto problemDto) throws Exception {
        ProblemEntity entity = modelMapper.map(problemDto, ProblemEntity.class);
        Set<String> topicSlugs = problemDto.getTopics().stream().map(topic -> topic.getSlug()).collect(Collectors.toSet());
        Set<String> companySlugs = problemDto.getCompanies().stream().map(company -> company.getSlug()).collect(Collectors.toSet());
        entity.setTopicSlugs(topicSlugs);
        entity.setCompanySlugs(companySlugs);
        ProblemEntity savedEntity = problemsRepository.addProblem(entity);
        return savedEntity;
    }

    public Boolean problemExists(Integer problemId) {
        return problemsRepository.problemExists(problemId);
    }

    public ProblemDto getProblemById(Integer id) {
        
        Optional<ProblemDto> probEntity = problemsRepository.getProblemById(id);

        if(probEntity.isEmpty()) throw new NoSuchElementException("Problem Id "+ id + " does not exist");

        return probEntity.get();
    }

    public List<ProblemDto> getProblemsByIds(List<Integer> problemIds) {
        List<ProblemEntity> probEntities = problemsRepository.getProblemsByIds(problemIds);
        List<ProblemDto> problemDtos = probEntities.stream().map(probEntity -> modelMapper.map(probEntity, ProblemDto.class)).collect(Collectors.toList());
        return problemDtos;
    }


    @CacheEvict(cacheNames = RedisService.ALL_PROBLEMS_CACHE)
    public long updateProblem(Integer problemId, ProblemUpdateDto problemUpdateDto) {
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
                    Map<ProgrammingLanguage, String> codeSnippets = new HashMap<>();
                    for (Map.Entry<?, ?> entry : snippetsMap.entrySet()) {
                        try{
                            ProgrammingLanguage language = ProgrammingLanguage.valueOf((String) entry.getKey());
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

        if (updatesMetadata.containsKey("topicSlugs")) {
            Object topicsObj = updatesMetadata.get("topicSlugs");
            if (topicsObj instanceof List<?>) {
                List<?> topicsList = (List<?>) topicsObj;
                if (!topicsList.isEmpty() && topicsList.stream().allMatch(item -> item instanceof String)) {
                    @SuppressWarnings("unchecked")
                    List<String> newTopics = (List<String>) topicsList;
                    Set<Topic> newValidTopicTags = topicRepository.getValidTags(newTopics);
                    if (newValidTopicTags.size() != newTopics.size()) {
                        throw new IllegalArgumentException("Invalid topic slugs. Some topics do not exist");
                    }
                    validUpdates.put("topicSlugs", newValidTopicTags.stream().map(Topic::getSlug).collect(Collectors.toSet()));
                } else {
                    throw new IllegalArgumentException("Topics must be a non-empty set of strings");
                }
            } else {
                throw new IllegalArgumentException("Topics must be a Set");
            }
        }

        if (updatesMetadata.containsKey("companySlugs")) {
            Object companiesObj = updatesMetadata.get("companySlugs");
            if (companiesObj instanceof List<?>) {
                List<?> companiesList = (List<?>) companiesObj;
                if (!companiesList.isEmpty() && companiesList.stream().allMatch(item -> item instanceof String)) {
                    @SuppressWarnings("unchecked")
                    List<String> newCompanies = (List<String>) companiesList;
                    Set<CompanyDto> newValidCompanyTags = companyService.getValidTags(newCompanies);
                    if (newValidCompanyTags.size() != newCompanies.size()) {
                        throw new IllegalArgumentException("Invalid company slugs. Some companies do not exist");
                    }
                    validUpdates.put("companySlugs", newValidCompanyTags.stream().map(CompanyDto::getSlug).collect(Collectors.toSet()));
                } else {
                    throw new IllegalArgumentException("Companies must be a non-empty set of strings");
                }
            } else {
                throw new IllegalArgumentException("Companies must be a Set");
            }
        }

        if (updatesMetadata.containsKey("cpuTimeLimit")) {
            Object cpuTimeLimitObj = updatesMetadata.get("cpuTimeLimit");
            if (cpuTimeLimitObj instanceof Number) {
                float cpuTimeLimit = ((Number) cpuTimeLimitObj).floatValue();
                if (cpuTimeLimit < 0.1f || cpuTimeLimit > 5.0f) {
                    throw new IllegalArgumentException("CPU time limit must be between 0.1 and 5.0 seconds");
                }
                validUpdates.put("cpuTimeLimit", cpuTimeLimit);
            } else {
                throw new IllegalArgumentException("CPU time limit must be a number");
            }
        }

        if (updatesMetadata.containsKey("memoryLimit")) {
            Object memoryLimitObj = updatesMetadata.get("memoryLimit");
            if (memoryLimitObj instanceof Number) {
                float memoryLimit = ((Number) memoryLimitObj).floatValue();
                if (memoryLimit < 100.0f || memoryLimit > 128000.0f) {
                    throw new IllegalArgumentException("Memory limit must be between 100.0 and 128000.0 MB");
                }
                validUpdates.put("memoryLimit", memoryLimit);
            } else {
                throw new IllegalArgumentException("Memory limit must be a number");
            }
        }

        if (updatesMetadata.containsKey("stackLimit")) {
            Object stackLimitObj = updatesMetadata.get("stackLimit");
            if (stackLimitObj instanceof Number) {
                int stackLimit = ((Number) stackLimitObj).intValue();
                if (stackLimit < 1024 || stackLimit > 40000) {
                    throw new IllegalArgumentException("Stack limit must be between 1024 and 40000 KB");
                }
                validUpdates.put("stackLimit", stackLimit);
            } else {
                throw new IllegalArgumentException("Stack limit must be a number");
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

    @CacheEvict(cacheNames = RedisService.ALL_PROBLEMS_CACHE)
    public void deleteProblemById(Integer problemId){

        DeleteResult result = problemsRepository.removeProblemById(problemId);

        if (result.getDeletedCount()<1) throw new NoSuchElementException("Problem Id "+ problemId + " not present");
    }

    @CacheEvict(cacheNames = RedisService.ALL_PROBLEMS_CACHE)
    public void removeAllProblems() {
        problemsRepository.removeAllProblems();
    }

    // public ProblemSetResponse getFilteredProblems(String difficultyStr, String topics, int page, int size) {
    //     String[] topicsArray = topics.trim().split(",");
    //     Difficulty difficulty = Difficulty.valueOf(difficultyStr);
    //     System.out.println(difficulty + "   " + Arrays.toString(topicsArray));
    //     return problemsRepository.getFilteredProblems(difficulty, topicsArray, page, size);
    // }

}
