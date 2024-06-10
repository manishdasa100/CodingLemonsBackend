package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Config.CustomCacheConfig;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Payloads.ProblemUpdateRequestPayload;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.mongodb.client.result.DeleteResult;

@Service
@CacheConfig(cacheNames = CustomCacheConfig.DEFAULT_CACHE)
public class ProblemRepositoryService {

    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private ProblemsRepository problemsRepository;

    @Cacheable(cacheNames = CustomCacheConfig.ALL_PROBLEMS_CACHE)
    public ProblemSet getAllProblems(Integer page, Integer size) {
        System.out.println("CACHE MISS");
        return problemsRepository.findAll(page, size);
    }

    public ProblemSet getFilteredProblems(String difficultyStr, String topicsStr, int page, int size){
        
        String[] topicsArray = null;

        List<Difficulty> difficulties = null;

        if (difficultyStr != null) {
            String[] difficultiesArray = difficultyStr.trim().split(",");
            difficulties = List.of(difficultiesArray).stream().map(e -> Difficulty.valueOf(e)).collect(Collectors.toList());
        }
        if (topicsStr != null) {
            topicsArray = topicsStr.trim().split(",");
        }

        return problemsRepository.getProblems(difficulties, topicsArray, page, size);
    }

    @CacheEvict(cacheNames = CustomCacheConfig.ALL_PROBLEMS_CACHE)
    public void addProblem(ProblemDto problemDto) {
        System.out.println("---------------------------------------------");
        System.out.println("Problem entity from Repo service: " + problemDto.toString());
        System.out.println("---------------------------------------------");
        ProblemEntity entity = modelMapper.map(problemDto, ProblemEntity.class);
        entity.setSubmissionCount(0);
        entity.setAcceptedCount(0);
        problemsRepository.addProblem(entity);
    }

    // Caching does not fit here as it is very less probable that a same problem will be accessed multiple times by multiple users
    public ProblemDto getProblem(Integer id) {
        
        Optional<ProblemEntity> probEntity = problemsRepository.getProblemById(id);

        if(probEntity.isEmpty()) throw new NoSuchElementException("Problem Id "+ id + " not present");

        ProblemDto problemDto = modelMapper.map(probEntity, ProblemDto.class);

        return problemDto;
    }

    @CacheEvict(cacheNames = CustomCacheConfig.ALL_PROBLEMS_CACHE)
    public void updateProblem(Integer problemId, ProblemUpdateDto updateMetadata) {

        ProblemDto problemDto = getProblem(problemId);

        Map<String, Object> updatePropertiesMap = new HashMap<>();

        if (updateMetadata.getTitle()!= null  && !updateMetadata.getTitle().equals(problemDto.getTitle())) {
            updatePropertiesMap.put("title", updateMetadata.getTitle());
        }

        if (updateMetadata.getDescription()!= null && !updateMetadata.getDescription().equals(problemDto.getDescription())) {
            updatePropertiesMap.put("description", updateMetadata.getDescription());
        }

        if (updateMetadata.getConstraints() != null && !updateMetadata.getConstraints().equals(problemDto.getConstraints())) {
            updatePropertiesMap.put("constraints", updateMetadata.getConstraints());
        }

        if (updateMetadata.getExamples()!= null && !updateMetadata.getExamples().equals(problemDto.getExamples())) {
           updatePropertiesMap.put("examples", updateMetadata.getExamples());
        }

        if (updateMetadata.getDifficulty()!= null && !updateMetadata.getDifficulty().equals(problemDto.getDifficulty())) {
            updatePropertiesMap.put("difficulty", updateMetadata.getDifficulty());
        }

        if (updateMetadata.getTopics()!= null && !updateMetadata.getTopics().equals(problemDto.getTopics())) {
            updatePropertiesMap.put("topics", updateMetadata.getTopics());
        }

        if (updateMetadata.getCompanyTags()!= null && !updateMetadata.getCompanyTags().equals(problemDto.getCompanyTags())) {
            updatePropertiesMap.put("companyTags", updateMetadata.getCompanyTags());
        }

        if (updateMetadata.getStackLimit()!= null && !updateMetadata.getStackLimit().equals(problemDto.getStackLimit())) {
            updatePropertiesMap.put("stackLimit", updateMetadata.getStackLimit());
        }

        if (updateMetadata.getMemoryLimit()!= null && !updateMetadata.getMemoryLimit().equals(problemDto.getMemoryLimit())) {
            updatePropertiesMap.put("memoryLimit", updateMetadata.getMemoryLimit());
        }

        if (updateMetadata.getCpuTimeLimit()!= null && !updateMetadata.getCpuTimeLimit().equals(problemDto.getCpuTimeLimit())) {
            updatePropertiesMap.put("cpuTimeLimit", updateMetadata.getCpuTimeLimit());
        }

        if (updateMetadata.getTestCasesWithExpectedOutputs()!= null && !updateMetadata.getTestCasesWithExpectedOutputs().equals(problemDto.getTestCasesWithExpectedOutputs())) {
            updatePropertiesMap.put("testCasesWithExpectedOutputs", updateMetadata.getTestCasesWithExpectedOutputs());
        }

        if (updateMetadata.getCodeSnippets()!= null && !updateMetadata.getCodeSnippets().equals(problemDto.getCodeSnippets())) {
            updatePropertiesMap.put("codeSnippets", updateMetadata.getCodeSnippets());
        }

        if (updateMetadata.getDriverCodes()!= null && !updateMetadata.getDriverCodes().equals(problemDto.getDriverCodes())) {
            updatePropertiesMap.put("driverCodes", updateMetadata.getDriverCodes());
        }

        if (updateMetadata.getNextProblemId() != null && !updateMetadata.getNextProblemId().equals(problemDto.getNextProblemId())) { 
            updatePropertiesMap.put("nextProblemId", updateMetadata.getNextProblemId());
        }

        if (updateMetadata.getPreviousProblemId() != null && !updateMetadata.getPreviousProblemId().equals(problemDto.getPreviousProblemId())) { 
            updatePropertiesMap.put("previousProblemId", updateMetadata.getPreviousProblemId());
        }

        if (!updatePropertiesMap.isEmpty()) {
            System.out.println("THERE IS SOMETHING TO UPDATE");
            problemsRepository.updateProblem(problemId, updatePropertiesMap);
        } else {
            System.out.println("THERE IS NOTHING TO UPDATE");
        }
    }

    @CacheEvict(cacheNames = CustomCacheConfig.ALL_PROBLEMS_CACHE)
    public void deleteProblemById(Integer problemId){

        DeleteResult result = problemsRepository.removeProblemById(problemId);

        if (result.getDeletedCount()<1) throw new NoSuchElementException("Problem Id "+ problemId + " not present");
    }

    @CacheEvict(cacheNames = CustomCacheConfig.ALL_PROBLEMS_CACHE)
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
