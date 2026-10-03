package com.codinglemonsbackend.Service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.codinglemonsbackend.Entities.ProblemOfTheDayEntity;
import com.codinglemonsbackend.Repository.ProblemOfTheDayRepository;
import com.codinglemonsbackend.Repository.ProblemsRepository;

/**
 * Every instance's scheduler fires at midnight, so the problem of the day was selected once per
 * instance and the slowest write decided the day. Asserting on the stored problem cannot catch that
 * - the end state is a valid problem either way. What has to be asserted is how many selections ran.
 */
class ProblemOfTheDayElectionTest {

    @Test
    void onlyOneInstanceOfFiveSelectsTodaysProblem() {
        RedisService redisService = mock(RedisService.class);
        ProblemsRepository problemsRepository = mock(ProblemsRepository.class);
        ProblemOfTheDayRepository potdRepository = mock(ProblemOfTheDayRepository.class);

        // One Redis, so exactly one SET NX on the dated key succeeds.
        when(redisService.setIfAbsent(anyString(), anyString(), anyLong()))
                .thenReturn(true, false, false, false, false);
        when(problemsRepository.getRandomPublishedProblemId(any())).thenReturn(Optional.of(7));

        for (int instance = 0; instance < 5; instance++) {
            instanceOf(potdRepository, problemsRepository, redisService).scheduledSetProblemOfTheDay();
        }

        verify(problemsRepository, times(1)).getRandomPublishedProblemId(any());
        verify(potdRepository, times(1)).saveProblemOfTheDay(eq(7), isNull(), anyInt());
    }

    @Test
    void redisBeingDownLeavesYesterdaysProblemInPlace() {
        RedisService redisService = mock(RedisService.class);
        ProblemsRepository problemsRepository = mock(ProblemsRepository.class);
        ProblemOfTheDayRepository potdRepository = mock(ProblemOfTheDayRepository.class);

        when(redisService.setIfAbsent(anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("connection refused"));

        ProblemOfTheDayService service = instanceOf(potdRepository, problemsRepository, redisService);
        assertThrows(RuntimeException.class, service::scheduledSetProblemOfTheDay);

        // Proceeding unelected would put back exactly the overwriting the election removes, and do
        // it while the system is already unhealthy. A stale problem for a day is the cheaper loss.
        verify(problemsRepository, never()).getRandomPublishedProblemId(any());
        verify(potdRepository, never()).saveProblemOfTheDay(anyInt(), any(), anyInt());
    }

    @Test
    void onlyOneInstanceOfFiveInitializesAnEmptyCollectionAtBoot() {
        RedisService redisService = mock(RedisService.class);
        ProblemsRepository problemsRepository = mock(ProblemsRepository.class);
        ProblemOfTheDayRepository potdRepository = mock(ProblemOfTheDayRepository.class);

        when(redisService.setIfAbsent(anyString(), anyString(), anyLong()))
                .thenReturn(true, false, false, false, false);
        when(potdRepository.getProblemOfTheDay()).thenReturn(null);
        when(problemsRepository.getRandomPublishedProblemId(any())).thenReturn(Optional.of(7));

        for (int instance = 0; instance < 5; instance++) {
            instanceOf(potdRepository, problemsRepository, redisService).initializeProblemOfTheDay();
        }

        verify(problemsRepository, times(1)).getRandomPublishedProblemId(any());
    }

    @Test
    void anOrdinaryBootDoesNotTakeTheDaysElectionKey() {
        RedisService redisService = mock(RedisService.class);
        ProblemsRepository problemsRepository = mock(ProblemsRepository.class);
        ProblemOfTheDayRepository potdRepository = mock(ProblemOfTheDayRepository.class);

        ProblemOfTheDayEntity existing = new ProblemOfTheDayEntity();
        existing.setProblemId(5);
        when(potdRepository.getProblemOfTheDay()).thenReturn(existing);

        instanceOf(potdRepository, problemsRepository, redisService).initializeProblemOfTheDay();

        // Electing before the null check would let a boot minutes before midnight hold the key the
        // cron then needs, and today's problem would never change.
        verifyNoInteractions(redisService);
    }

    private static ProblemOfTheDayService instanceOf(ProblemOfTheDayRepository potdRepository,
            ProblemsRepository problemsRepository, RedisService redisService) {
        ProblemOfTheDayService service = new ProblemOfTheDayService();
        ReflectionTestUtils.setField(service, "problemOfTheDayRepository", potdRepository);
        ReflectionTestUtils.setField(service, "problemsRepository", problemsRepository);
        ReflectionTestUtils.setField(service, "redisService", redisService);
        return service;
    }
}
