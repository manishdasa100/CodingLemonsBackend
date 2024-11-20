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
import org.apache.commons.lang3.StringUtils;
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
import com.codinglemonsbackend.Entities.CompanyTag;
import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.TopicTag;
import com.codinglemonsbackend.Repository.CompanyRepository;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.codinglemonsbackend.Repository.TopicRepository;
import com.mongodb.client.result.DeleteResult;

@Service
@CacheConfig(cacheNames = CustomCacheConfig.DEFAULT_CACHE)
public class ProblemRepositoryService {

    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private ProblemsRepository problemsRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Cacheable(cacheNames = CustomCacheConfig.ALL_PROBLEMS_CACHE)
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
        if (StringUtils.isNotEmpty(topicsStr)) {
            topicSlugs = Arrays.stream(topicsStr.trim().split(","))
                            .map(String::trim)
                            .filter(e -> !e.isEmpty())
                            .toArray(String[]::new);
        }
        if (StringUtils.isNotEmpty(companiesStr)) {
            companySlugs = Arrays.stream(companiesStr.trim().split(","))
                            .map(String::trim)
                            .filter(e -> !e.isEmpty())
                            .toArray(String[]::new);
        }

        return problemsRepository.getProblems(difficulties, topicSlugs, companySlugs, page, size);
    }

    @CacheEvict(cacheNames = CustomCacheConfig.ALL_PROBLEMS_CACHE)
    public void addProblem(ProblemDto problemDto) throws Exception {
        System.out.println("---------------------------------------------");
        System.out.println("Problem entity from Repo service: " + problemDto.toString());
        System.out.println("---------------------------------------------");
        
        // Verify if the topic tags and company tags are present in database before adding the problem
        Set<TopicTag> topicTags = problemDto.getTopics();
        Set<String> topicSlugs = topicTags.stream().map(TopicTag::getSlug).collect(Collectors.toSet());

        if (problemDto.getCompanies() != null) {
            Set<CompanyTag> companyTags = problemDto.getCompanies();
            Set<String> companySlugs = companyTags.stream().map(CompanyTag::getSlug).collect(Collectors.toSet());
            Set<CompanyTag> validCompanies = companyRepository.getValidTags(companySlugs);
            problemDto.setCompanies(validCompanies);
        }

        Set<TopicTag> validTopics = topicRepository.getValidTags(topicSlugs);
        
        if(validTopics.isEmpty()) throw new Exception("No matching topics were found. Please provide valid topics."); 

        problemDto.setTopics(validTopics);

        ProblemEntity entity = modelMapper.map(problemDto, ProblemEntity.class);
        problemsRepository.addProblem(entity);
    }

    // Caching does not fit here as it is very less probable that a same problem will be accessed multiple times by multiple users
    public ProblemDto getProblem(Integer id) {
        
        Optional<ProblemEntity> probEntity = problemsRepository.getProblemById(id);

        if(probEntity.isEmpty()) throw new NoSuchElementException("Problem Id "+ id + " does not exist");

        ProblemDto problemDto = modelMapper.map(probEntity, ProblemDto.class);

        return problemDto;
    }

    public List<ProblemDto> getProblemsByIds(Integer[] problemIds) {
        List<ProblemEntity> probEntities = problemsRepository.getProblemsByIds(problemIds);
        List<ProblemDto> problemDtos = probEntities.stream().map(probEntity -> modelMapper.map(probEntity, ProblemDto.class)).collect(Collectors.toList());
        return problemDtos;
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

        if (updateMetadata.getCompanies()!= null && !updateMetadata.getCompanies().equals(problemDto.getCompanies())) {
            updatePropertiesMap.put("companyTags", updateMetadata.getCompanies());
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
