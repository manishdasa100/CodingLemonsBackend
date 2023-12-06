package com.codinglemonsbackend.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Payloads.ProblemSetResponse;
import com.codinglemonsbackend.Repository.ProblemsRepository;

@Service
@CacheConfig(cacheNames = "problemSet")
public class ProblemRepositoryService {
    
    @Autowired
    private ProblemsRepository problemsRepository;

    @Cacheable
    private ProblemSetResponse getAllProblems(Integer page, Integer size) {
        return problemsRepository.findAll(page, size);
    }

    @CacheEvict(allEntries = true)
    public void addProblem(ProblemDto problemDto) {
        System.out.println("---------------------------------------------");
        System.out.println("Problem entity from Repo service: " + problemDto.toString());
        System.out.println("---------------------------------------------");
        ProblemEntity entity = ProblemEntity.builder()
                                    .title(problemDto.getTitle())
                                    .description(problemDto.getDescription())
                                    .testCases(problemDto.getTestCases())
                                    .testCaseOutputs(problemDto.getTestCaseOutputs())
                                    .difficulty(problemDto.getDifficulty())
                                    .driverCodes(problemDto.getDriverCodes())
                                    .optimalSolutions(problemDto.getOptimalSolutions())
                                    .topics(problemDto.getTopics())
                                    .acceptance(0)
                                    .build();
        problemsRepository.addProblem(entity);
    }

    // Caching does not fit here as it is very less probable that a same problem will be accessed multiple times by multiple users
    public ProblemEntity getProblem(Integer id) {
        return problemsRepository.getProblemById(id).orElseThrow();
    }

    public void removeAllProblems() {
        problemsRepository.removeAllProblems();
    }

    // public ProblemSetResponse getFilteredProblems(String difficultyStr, String topics, int page, int size) {
    //     String[] topicsArray = topics.trim().split(",");
    //     Difficulty difficulty = Difficulty.valueOf(difficultyStr);
    //     System.out.println(difficulty + "   " + Arrays.toString(topicsArray));
    //     return problemsRepository.getFilteredProblems(difficulty, topicsArray, page, size);
    // }

    public ProblemSetResponse getProblems(String difficultyStr, String topicsStr, int page, int size){
        
        if ((difficultyStr == null || difficultyStr.length() == 0) && (topicsStr == null || topicsStr.length() == 0)) {
            
            // no filtering needed based on topics and/or difficulty
            return getAllProblems(page, size);
        }

        String[] topicsArray = null;

        List<Difficulty> difficulties = null;

        if (difficultyStr != null) {
            String[] difficultiesArray = difficultyStr.trim().split(",");
            difficulties = List.of(difficultiesArray).stream().map(e -> Difficulty.valueOf(e)).collect(Collectors.toList());
        }
        if (topicsStr != null) {
            topicsArray = topicsStr.trim().split(",");
        }

        return problemsRepository.getFilteredProblems(difficulties, topicsArray, page, size);
    }
}
