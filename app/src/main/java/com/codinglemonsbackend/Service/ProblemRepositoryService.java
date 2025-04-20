package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.codinglemonsbackend.Config.CustomCacheConfig;
import com.codinglemonsbackend.Dto.Difficulty;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Entities.CompanyTag;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.ProblemExecutionDetails;
import com.codinglemonsbackend.Entities.TopicTag;
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
    private CompanyRepository companyRepository;

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
    public ProblemEntity addProblem(ProblemDto problemDto, ProblemExecutionDetails executionDetails) throws Exception {
        System.out.println("---------------------------------------------");
        System.out.println("Problem entity from Repo service: " + problemDto.toString());
        System.out.println("---------------------------------------------");

        ProblemEntity entity = modelMapper.map(problemDto, ProblemEntity.class);

        ProblemEntity savedEntity = problemsRepository.addProblem(entity, executionDetails);

        return savedEntity;
    }

    public Boolean problemExists(Integer problemId) {
        return problemsRepository.problemExists(problemId);
    }
    // Caching does not fit here as it is very less probable that a same problem will be accessed multiple times by multiple users
    public ProblemDto getProblem(Integer id) {
        
        Optional<ProblemEntity> probEntity = problemsRepository.getProblemById(id);

        if(probEntity.isEmpty()) throw new NoSuchElementException("Problem Id "+ id + " does not exist");

        ProblemDto problemDto = modelMapper.map(probEntity, ProblemDto.class);

        return problemDto;
    }

    public List<ProblemDto> getProblemsByIds(List<Integer> problemIds) {
        List<ProblemEntity> probEntities = problemsRepository.getProblemsByIds(problemIds);
        List<ProblemDto> problemDtos = probEntities.stream().map(probEntity -> modelMapper.map(probEntity, ProblemDto.class)).collect(Collectors.toList());
        return problemDtos;
    }

    public ProblemExecutionDetails getExecutionDetails(Integer id) {
        Optional<ProblemExecutionDetails> executionDetails = problemsRepository.getExecutionDetails(id);
        if (executionDetails.isEmpty()) throw new NoSuchElementException("No execution details found for id " + id);
        return executionDetails.get();
    }

    @CacheEvict(cacheNames = RedisService.ALL_PROBLEMS_CACHE)
    public long updateProblem(Integer problemId, ProblemUpdateDto updateMetadata) {

        ProblemDto problemDto = getProblem(problemId);

        ProblemExecutionDetails oldExecutionDetails = getExecutionDetails(problemId);

        System.out.println("Hello Hello");

        Map<String, Object> problemDetailsFieldsToUpdate = new HashMap<>();

        Map<String, Object> executionDetailsFieldsToUpdate = new HashMap<>();

        if (StringUtils.isNotBlank(updateMetadata.getTitle())  && !updateMetadata.getTitle().equals(problemDto.getTitle())) {
            problemDetailsFieldsToUpdate.put("title", updateMetadata.getTitle());
        }

        if (StringUtils.isNotBlank(updateMetadata.getDescription()) && !updateMetadata.getDescription().equals(problemDto.getDescription())) {
            problemDetailsFieldsToUpdate.put("description", updateMetadata.getDescription());
        }

        if (!CollectionUtils.isEmpty(updateMetadata.getConstraints()) && !updateMetadata.getConstraints().equals(problemDto.getConstraints())) {
            problemDetailsFieldsToUpdate.put("constraints", updateMetadata.getConstraints());
        }

        if (!CollectionUtils.isEmpty(updateMetadata.getExamples()) && !updateMetadata.getExamples().equals(problemDto.getExamples())) {
            problemDetailsFieldsToUpdate.put("examples", updateMetadata.getExamples());
        }

        if (ObjectUtils.isNotEmpty(updateMetadata.getDifficulty()) && !updateMetadata.getDifficulty().equals(problemDto.getDifficulty())) {
            problemDetailsFieldsToUpdate.put("difficulty", updateMetadata.getDifficulty());
        }

        if (!CollectionUtils.isEmpty(updateMetadata.getTopicSlugs()) && !updateMetadata.getTopicSlugs().equals(problemDto.getTopics().stream().map(TopicTag::getSlug).collect(Collectors.toSet()))) {
            Set<TopicTag> validTopics = topicRepository.getValidTags(updateMetadata.getTopicSlugs());
            if (!validTopics.isEmpty()) problemDetailsFieldsToUpdate.put("topics", validTopics);    
        }

        if (!CollectionUtils.isEmpty(updateMetadata.getCompanySlugs()) && !updateMetadata.getCompanySlugs().equals(problemDto.getCompanies().stream().map(CompanyTag::getSlug).collect(Collectors.toSet()))) {
            Set<CompanyTag> validCompanies = companyRepository.getValidTags(updateMetadata.getCompanySlugs());
            if (!validCompanies.isEmpty()) problemDetailsFieldsToUpdate.put("companies", validCompanies);
        }

        if (!CollectionUtils.isEmpty(updateMetadata.getCodeSnippets()) && !updateMetadata.getCodeSnippets().equals(problemDto.getCodeSnippets())) {
            problemDetailsFieldsToUpdate.put("codeSnippets", updateMetadata.getCodeSnippets());
        }

        if (updateMetadata.getStackLimit()!= null && !updateMetadata.getStackLimit().equals(oldExecutionDetails.getStackLimit())) {
            executionDetailsFieldsToUpdate.put("stackLimit", updateMetadata.getStackLimit());
        }

        if (updateMetadata.getMemoryLimit()!= null && !updateMetadata.getMemoryLimit().equals(oldExecutionDetails.getMemoryLimit())) {
            executionDetailsFieldsToUpdate.put("memoryLimit", updateMetadata.getMemoryLimit());
        }

        if (updateMetadata.getCpuTimeLimit()!= null && !updateMetadata.getCpuTimeLimit().equals(oldExecutionDetails.getCpuTimeLimit())) {
            executionDetailsFieldsToUpdate.put("cpuTimeLimit", updateMetadata.getCpuTimeLimit());
        }

        if (!CollectionUtils.isEmpty(updateMetadata.getTestCasesWithExpectedOutputs()) && !updateMetadata.getTestCasesWithExpectedOutputs().equals(oldExecutionDetails.getTestCasesWithExpectedOutputs())){
            executionDetailsFieldsToUpdate.put("testCasesWithExpectedOutputs", updateMetadata.getTestCasesWithExpectedOutputs());
        }

        if (!CollectionUtils.isEmpty(updateMetadata.getDriverCodes()) && !updateMetadata.getDriverCodes().equals(oldExecutionDetails.getDriverCodes())) {
            executionDetailsFieldsToUpdate.put("driverCodes", updateMetadata.getDriverCodes());
        } 

        long updatedDocumentsCount = 0;

        if (!problemDetailsFieldsToUpdate.isEmpty()) {
            System.out.println("UPDATING PROBLEM DETAILS");
            updatedDocumentsCount = problemsRepository.updateProblemProperties(problemId, problemDetailsFieldsToUpdate, ProblemEntity.class);
        } 

        if (!executionDetailsFieldsToUpdate.isEmpty()) {
            System.out.println("UPDATING EXECUTION DETAILS");
            updatedDocumentsCount += problemsRepository.updateProblemProperties(problemId, executionDetailsFieldsToUpdate, ProblemExecutionDetails.class);
        }

        return updatedDocumentsCount;
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
