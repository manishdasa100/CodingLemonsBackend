package com.codinglemonsbackend.Service;

import java.util.List;
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
import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Repository.ProblemsRepository;

@Service
@CacheConfig(cacheNames = CustomCacheConfig.DEFAULT_CACHE)
public class ProblemRepositoryService {

    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private ProblemsRepository problemsRepository;

    @Cacheable
    public ProblemSet getAllProblems(Integer page, Integer size) {
        return problemsRepository.findAll(page, size);
    }

    public ProblemSet getFilteredProblems(String difficultyStr, String topicsStr, int page, int size){
        
        // if ((difficultyStr == null || difficultyStr.length() == 0) && (topicsStr == null || topicsStr.length() == 0)) {
            
        //     // no filtering needed based on topics and/or difficulty
        //     return getAllProblems(page, size);
        // }

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

    @CacheEvict(allEntries = true)
    public void addProblem(ProblemDto problemDto) {
        System.out.println("---------------------------------------------");
        System.out.println("Problem entity from Repo service: " + problemDto.toString());
        System.out.println("---------------------------------------------");
        ProblemEntity entity = modelMapper.map(problemDto, ProblemEntity.class);
        entity.setAcceptance(0);
        problemsRepository.addProblem(entity);
    }

    // Caching does not fit here as it is very less probable that a same problem will be accessed multiple times by multiple users
    public ProblemDto getProblem(Integer id) {
        
        Optional<ProblemEntity> probEntity = problemsRepository.getProblemById(id);

        if(probEntity.isEmpty()) throw new NoSuchElementException("Problem Id "+ id + " not present");

        ProblemDto problemDto = modelMapper.map(probEntity, ProblemDto.class);

        return problemDto;
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

}
