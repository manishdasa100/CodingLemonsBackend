package com.codinglemonsbackend.Service;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Config.CustomCacheConfig;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.ProblemOfTheDayMetadata;
import com.codinglemonsbackend.Repository.ProblemOfTheDayRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@CacheConfig(cacheNames = CustomCacheConfig.DEFAULT_CACHE)
@Slf4j
public class ProblemOfTheDayService {

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private ProblemRepositoryService problemRepositoryService;

    @Autowired
    private ProblemOfTheDayRepository problemOfTheDayRepository;
    
    @Cacheable(cacheNames = CustomCacheConfig.PROBLEM_OF_THE_DAY_CACHE)
    public ProblemDto getProblemOfTheDay(){

        System.out.println("CACHE MISS");

        ProblemOfTheDayMetadata problemMetadata = problemOfTheDayRepository.getProblemOfTheDayMetadata();

        if(Objects.isNull(problemMetadata)){
            throw new NoSuchElementException("Problem of the day not set");
        }
        
        ProblemDto problemOfTheDay = problemRepositoryService.getProblem(problemMetadata.getProblemId());

        return problemOfTheDay;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @CacheEvict(cacheNames = CustomCacheConfig.PROBLEM_OF_THE_DAY_CACHE)
    public void setProblenOftheDay(){

        Integer randomProblemId = getRandomProblemId();

        problemOfTheDayRepository.saveProblemOfTheDayMetadata(randomProblemId);

    }

    private Integer getRandomProblemId(){

        Random random = new Random();

        Integer totalProblems = sequenceService.getCurrentSequence(ProblemEntity.SEQUENCE_NAME);

        Integer randomProblemId;

        //Check for repeat problemId
        ProblemOfTheDayMetadata prevProbleOfTheDayMetadata = problemOfTheDayRepository.getProblemOfTheDayMetadata();

        do{
            randomProblemId = random.nextInt(1, totalProblems+1);
        } while(randomProblemId == prevProbleOfTheDayMetadata.getProblemId());

        return randomProblemId;
    }
}
