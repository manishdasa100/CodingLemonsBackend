package com.codinglemonsbackend.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemOfTheDayDto;
import com.codinglemonsbackend.Entities.ProblemOfTheDayEntity;
import com.codinglemonsbackend.Repository.ProblemOfTheDayRepository;
import com.codinglemonsbackend.Repository.ProblemsRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@CacheConfig(cacheNames = RedisService.DEFAULT_CACHE)
@Slf4j
public class ProblemOfTheDayService {

    private static final int HISTORY_SIZE = 30;
    private static final int MAX_SCHEDULER_ATTEMPTS = 3;

    @Autowired
    private ProblemOfTheDayRepository problemOfTheDayRepository;

    @Autowired
    private ProblemsRepository problemsRepository;

    @PostConstruct
    public void initializeProblemOfTheDay() {
        log.info("Checking if Problem of the Day is initialized...");
        if (problemOfTheDayRepository.getProblemOfTheDay() == null) {
            log.info("No POTD found on startup, selecting initial problem of the day");
            try {
                selectAndSaveProblemOfTheDay();
            } catch (Exception e) {
                log.error("Failed to initialize POTD on startup: {}", e.getMessage(), e);
            }
            return;
        }
        log.info("POTD already initialized");
    }

    @Cacheable(cacheNames = RedisService.PROBLEM_OF_THE_DAY_CACHE)
    public ProblemOfTheDayDto getProblemOfTheDay() {
        log.debug("POTD cache miss, loading from database");

        ProblemOfTheDayEntity metadata = problemOfTheDayRepository.getProblemOfTheDay();

        if (metadata == null) {
            throw new NoSuchElementException("Problem of the day not set");
        }

        ProblemDto problem = problemsRepository.getProblemsByIds(List.of(metadata.getProblemId()), false).stream().findFirst()
            .orElseThrow(() -> new NoSuchElementException("Problem of the day references a non-existent problem: " + metadata.getProblemId()));

        return new ProblemOfTheDayDto(problem.getId(), problem.getTitle(), problem.getDifficulty(), problem.getTopics());
    }

    // #3 — explicit UTC timezone so behaviour is environment-independent
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    @CacheEvict(cacheNames = RedisService.PROBLEM_OF_THE_DAY_CACHE)
    public void scheduledSetProblemOfTheDay() {
        // #2 — retry up to MAX_SCHEDULER_ATTEMPTS times before giving up
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_SCHEDULER_ATTEMPTS; attempt++) {
            try {
                selectAndSaveProblemOfTheDay();
                log.info("Problem of the day updated successfully on attempt {}/{}", attempt, MAX_SCHEDULER_ATTEMPTS);
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} to set POTD failed: {}", attempt, MAX_SCHEDULER_ATTEMPTS, e.getMessage());
            }
        }
        log.error("All {} attempts to set POTD failed. Previous problem remains active.", MAX_SCHEDULER_ATTEMPTS, lastException);
    }

    // #5 — admin can manually override the POTD at any time
    @CacheEvict(cacheNames = RedisService.PROBLEM_OF_THE_DAY_CACHE)
    public void overrideProblemOfTheDay(Integer problemId) {
        if (!problemsRepository.isPublishedProblem(problemId)) {
            throw new IllegalArgumentException("Problem " + problemId + " does not exist or is not published");
        }
        saveWithHistory(problemId);
        log.info("Problem of the day manually overridden to problem {}", problemId);
    }

    // #4 — shared selection + history logic used by both scheduler and admin override
    private void selectAndSaveProblemOfTheDay() {
        List<Integer> excludeIds = buildExcludeList();

        problemsRepository.getRandomPublishedProblemId(excludeIds).ifPresentOrElse(
            this::saveWithHistory,
            () -> {
                // Fallback: history may cover all published problems — retry with only current excluded
                log.warn("No available published problems with full history exclusion. Retrying with minimal exclusion.");
                ProblemOfTheDayEntity current = problemOfTheDayRepository.getProblemOfTheDay();
                List<Integer> minimal = (current != null && current.getProblemId() != null)
                    ? List.of(current.getProblemId()) : List.of();
                problemsRepository.getRandomPublishedProblemId(minimal).ifPresentOrElse(
                    this::saveWithHistory,
                    () -> log.warn("No published problems available at all. POTD unchanged.")
                );
            }
        );
    }

    private void saveWithHistory(Integer newProblemId) {
        List<Integer> excludeIds = buildExcludeList();
        List<Integer> newHistory = excludeIds.size() > HISTORY_SIZE
            ? new ArrayList<>(excludeIds.subList(0, HISTORY_SIZE))
            : new ArrayList<>(excludeIds);
        problemOfTheDayRepository.saveProblemOfTheDay(newProblemId, newHistory);
    }

    private List<Integer> buildExcludeList() {
        ProblemOfTheDayEntity current = problemOfTheDayRepository.getProblemOfTheDay();
        if (current == null) return List.of();
        List<Integer> exclude = new ArrayList<>();
        if (current.getProblemId() != null) exclude.add(current.getProblemId());
        if (current.getHistory() != null) exclude.addAll(current.getHistory());
        return exclude;
    }
}
