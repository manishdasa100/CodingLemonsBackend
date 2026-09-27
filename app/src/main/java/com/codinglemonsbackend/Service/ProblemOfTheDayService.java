package com.codinglemonsbackend.Service;

import java.time.LocalDate;
import java.time.ZoneId;
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
    /** One constant for the cron and for the date the election key is keyed on - two spellings drift. */
    private static final String POTD_ZONE = "Asia/Kolkata";
    /** Long enough to outlast the slowest instance's attempt, short enough to be gone by tomorrow. */
    private static final long ELECTION_TTL_SECONDS = 3600;

    @Autowired
    private ProblemOfTheDayRepository problemOfTheDayRepository;

    @Autowired
    private ProblemsRepository problemsRepository;

    @Autowired
    private RedisService redisService;

    @PostConstruct
    public void initializeProblemOfTheDay() {
        log.info("Checking if Problem of the Day is initialized...");
        if (problemOfTheDayRepository.getProblemOfTheDay() == null) {
            log.info("No POTD found on startup, selecting initial problem of the day");
            try {
                if (winsElection()) selectAndSaveProblemOfTheDay();
            } catch (Exception e) {
                // Including an unreachable Redis. A @PostConstruct that throws would stop the app
                // from starting, and a missing POTD is not worth that.
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
    @Scheduled(cron = "0 0 0 * * *", zone = POTD_ZONE)
    @CacheEvict(cacheNames = RedisService.PROBLEM_OF_THE_DAY_CACHE)
    public void scheduledSetProblemOfTheDay() {
        if (!winsElection()) {
            log.debug("Another instance is setting today's problem of the day");
            return;
        }
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

    /**
     * Whether this instance is the one that sets today's problem.
     *
     * Every instance's scheduler fires at midnight, so without this each would pick its own random
     * problem and overwrite the last: five selections, four of them thrown away, and the slowest
     * write deciding the day. SET NX is the whole election - the test and the set are one command,
     * so of any number of instances issuing it at once exactly one is told the key was absent. The
     * key is dated and expires on its own rather than needing to be released.
     *
     * An unreachable Redis fails the job rather than falling back to letting everyone run, because
     * that fallback is precisely the overwriting this guard exists to remove and it would kick in
     * at the worst possible moment. Yesterday's problem stays live, which is a cosmetic loss, and
     * the error says what to do about it.
     */
    private boolean winsElection() {
        String key = RedisService.POTD_ELECTION_PREFIX + LocalDate.now(ZoneId.of(POTD_ZONE));
        try {
            return Boolean.TRUE.equals(redisService.setIfAbsent(key, "1", ELECTION_TTL_SECONDS));
        } catch (RuntimeException e) {
            log.error("Cannot elect an instance to set the problem of the day, so it will not change today. Set it with the admin override once Redis is reachable.", e);
            throw e;
        }
    }

    // #5 — admin can manually override the POTD at any time
    @CacheEvict(cacheNames = RedisService.PROBLEM_OF_THE_DAY_CACHE)
    public void overrideProblemOfTheDay(Integer problemId) {
        if (!problemsRepository.isPublishedProblem(problemId)) {
            throw new IllegalArgumentException("Problem " + problemId + " does not exist or is not published");
        }
        ProblemOfTheDayEntity current = problemOfTheDayRepository.getProblemOfTheDay();
        saveWithHistory(problemId, current == null ? null : current.getProblemId());
        log.info("Problem of the day manually overridden to problem {}", problemId);
    }

    // #4 — shared selection + history logic used by both scheduler and admin override
    private void selectAndSaveProblemOfTheDay() {
        // Read once. The outgoing problem is all the write needs from it; the database prepends it
        // to the history itself, so there is nothing here to go stale between the read and the save.
        ProblemOfTheDayEntity current = problemOfTheDayRepository.getProblemOfTheDay();
        Integer outgoing = current == null ? null : current.getProblemId();

        problemsRepository.getRandomPublishedProblemId(buildExcludeList(current)).ifPresentOrElse(
            newProblemId -> saveWithHistory(newProblemId, outgoing),
            () -> {
                // Fallback: history may cover all published problems — retry with only current excluded
                log.warn("No available published problems with full history exclusion. Retrying with minimal exclusion.");
                List<Integer> minimal = outgoing != null ? List.of(outgoing) : List.of();
                problemsRepository.getRandomPublishedProblemId(minimal).ifPresentOrElse(
                    newProblemId -> saveWithHistory(newProblemId, outgoing),
                    () -> log.warn("No published problems available at all. POTD unchanged.")
                );
            }
        );
    }

    private void saveWithHistory(Integer newProblemId, Integer outgoingProblemId) {
        problemOfTheDayRepository.saveProblemOfTheDay(newProblemId, outgoingProblemId, HISTORY_SIZE);
    }

    private List<Integer> buildExcludeList(ProblemOfTheDayEntity current) {
        if (current == null) return List.of();
        List<Integer> exclude = new ArrayList<>();
        if (current.getProblemId() != null) exclude.add(current.getProblemId());
        if (current.getHistory() != null) exclude.addAll(current.getHistory());
        return exclude;
    }
}
