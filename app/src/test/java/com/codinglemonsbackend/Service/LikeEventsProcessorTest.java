package com.codinglemonsbackend.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;

import com.codinglemonsbackend.Dto.LikeEvent;
import com.codinglemonsbackend.Repository.LikeRepository;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The batch exists to collapse work, and both halves of that can be wrong independently: which
 * event survives coalescing, and how far the problem's counter moves afterwards. A counter that
 * drifts is unrecoverable without recounting the collection, so it is worth pinning down.
 */
class LikeEventsProcessorTest {

    private static final String STREAM = "stream:like-events";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RedisService redisService;
    private LikeRepository likeRepository;
    private ProblemsRepository problemsRepository;
    private LikeEventsProcessor processor;

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        likeRepository = mock(LikeRepository.class);
        problemsRepository = mock(ProblemsRepository.class);
        // No pending list, so every test starts with only the new entries it queues.
        processor = new LikeEventsProcessor(redisService, likeRepository, problemsRepository,
                objectMapper, STREAM, "group", "consumer", 100, 60L, 5L);
    }

    /** Twenty toggles by one user on one problem must cost exactly one write and one increment. */
    @Test
    void onlyTheLastEventPerUserAndProblemSurvives() {
        givenQueued(like("ada", 1), unlike("ada", 1), like("ada", 1));
        when(likeRepository.findExistingLikeKeys(any())).thenReturn(Set.of());

        processor.drain();

        assertEquals(List.of("ada"), usernamesOf(capturedLikes()));
        assertEquals(List.of(), usernamesOf(capturedDislikes()));
        verify(problemsRepository).incrementLikes(1, 1);
    }

    /**
     * A coalesced batch knows the state a user ended in, not whether it changed anything. Events
     * that agree with what is stored have to write nothing and count nothing - this is also what
     * makes a redelivered batch harmless.
     */
    @Test
    void eventsThatMatchStoredStateChangeNothing() {
        givenQueued(like("ada", 1), unlike("bob", 2));
        // ada's like is already stored; bob never liked problem 2 in the first place.
        when(likeRepository.findExistingLikeKeys(any()))
                .thenReturn(Set.of(LikeRepository.likeKey(1, "ada")));

        processor.drain();

        assertEquals(List.of(), usernamesOf(capturedLikes()));
        assertEquals(List.of(), usernamesOf(capturedDislikes()));
        verify(problemsRepository, never()).incrementLikes(anyInt(), anyInt());
    }

    /** Both users' rows still change, but the problem's count lands where it started. */
    @Test
    void opposingFlipsOnOneProblemCancelOut() {
        givenQueued(like("ada", 1), unlike("bob", 1));
        when(likeRepository.findExistingLikeKeys(any()))
                .thenReturn(Set.of(LikeRepository.likeKey(1, "bob")));

        processor.drain();

        assertEquals(List.of("ada"), usernamesOf(capturedLikes()));
        assertEquals(List.of("bob"), usernamesOf(capturedDislikes()));
        verify(problemsRepository, never()).incrementLikes(anyInt(), anyInt());
    }

    /** Acking leaves the entry in the stream, so every handled id has to be deleted too. */
    @Test
    void handledEntriesAreAcknowledgedAndDeleted() {
        givenQueued(like("ada", 1), like("bob", 2));
        when(likeRepository.findExistingLikeKeys(any())).thenReturn(Set.of());

        processor.drain();

        // Both ids go out in one XACK and one XDEL rather than a pair of calls each.
        verify(redisService).acknowledge(eq(STREAM), eq("group"),
                eq(RecordId.of("0-1")), eq(RecordId.of("0-2")));
        verify(redisService).deleteFromStream(eq(STREAM),
                eq(RecordId.of("0-1")), eq(RecordId.of("0-2")));
        // The count changed, so the formatted count cached for the problem page must go.
        verify(redisService).deleteKey(RedisService.PROBLEM_LIKES_COUNT_CACHE_PREFIX + "1");
    }

    /**
     * The reason this reclaims through XCLAIM instead of re-reading its own pending list: an entry
     * another instance is midway through applying is still pending, and applying one batch twice
     * moves a problem's counter twice. The idle guard is what holds that off.
     */
    @Test
    void entriesStillInFlightElsewhereAreLeftAlone() {
        givenQueued(like("ada", 1));
        when(likeRepository.findExistingLikeKeys(any())).thenReturn(Set.of());
        givenPending(pendingMessage("0-9", Duration.ofSeconds(3), 1));   // retryAfter is 60s

        processor.drain();

        verify(redisService, never()).claimPending(anyString(), anyString(), anyString(),
                any(Duration.class), any(RecordId[].class));
    }

    /** Once it has sat untouched past retryAfter, it is fair game. */
    @Test
    void staleEntriesAreReclaimed() {
        givenQueued(like("ada", 1));
        when(likeRepository.findExistingLikeKeys(any())).thenReturn(Set.of());
        givenPending(pendingMessage("0-9", Duration.ofSeconds(120), 1));
        when(redisService.claimPending(anyString(), anyString(), anyString(),
                any(Duration.class), any(RecordId[].class))).thenReturn(List.of());

        processor.drain();

        verify(redisService).claimPending(eq(STREAM), eq("group"), eq("consumer"),
                any(Duration.class), eq(RecordId.of("0-9")));
    }

    /** A like nobody can apply has to stop coming back, or it costs a reclaim forever. */
    @Test
    void entriesThatExhaustedTheirRetriesAreDropped() {
        givenQueued(like("ada", 1));
        when(likeRepository.findExistingLikeKeys(any())).thenReturn(Set.of());
        givenPending(pendingMessage("0-9", Duration.ofSeconds(120), 5));   // maxDeliveries is 5

        processor.drain();

        verify(redisService).acknowledge(eq(STREAM), eq("group"), eq(RecordId.of("0-9")));
        verify(redisService).deleteFromStream(eq(STREAM), eq(RecordId.of("0-9")));
        verify(redisService, never()).claimPending(anyString(), anyString(), anyString(),
                any(Duration.class), any(RecordId[].class));
    }

    private PendingMessage pendingMessage(String id, Duration idle, long deliveries) {
        return new PendingMessage(RecordId.of(id), Consumer.from("group", "someone"), idle, deliveries);
    }

    private void givenPending(PendingMessage... messages) {
        when(redisService.getPendingMessages(anyString(), anyString(), anyLong()))
                .thenReturn(new PendingMessages("group", List.of(messages)));
    }

    private LikeEvent like(String username, Integer problemId) {
        return new LikeEvent(problemId, username, true);
    }

    private LikeEvent unlike(String username, Integer problemId) {
        return new LikeEvent(problemId, username, false);
    }

    /** Ids ascend so the reader sees them in the order Redis would return them. */
    private void givenQueued(LikeEvent... events) {
        List<MapRecord<String, String, String>> records = new ArrayList<>();
        for (int i = 0; i < events.length; i++) {
            records.add(recordOf("0-" + (i + 1), events[i]));
        }
        when(redisService.readGroup(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(records);
    }

    private MapRecord<String, String, String> recordOf(String id, LikeEvent event) {
        try {
            return StreamRecords.<String, String, String>mapBacked(
                            Map.of("body", objectMapper.writeValueAsString(event)))
                    .withId(RecordId.of(id))
                    .withStreamKey(STREAM);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<LikeEvent> capturedLikes() {
        ArgumentCaptor<Collection<LikeEvent>> likes = ArgumentCaptor.forClass(Collection.class);
        verify(likeRepository).applyLikeBatch(likes.capture(), any());
        return likes.getValue();
    }

    @SuppressWarnings("unchecked")
    private Collection<LikeEvent> capturedDislikes() {
        ArgumentCaptor<Collection<LikeEvent>> dislikes = ArgumentCaptor.forClass(Collection.class);
        verify(likeRepository).applyLikeBatch(any(), dislikes.capture());
        return dislikes.getValue();
    }

    private List<String> usernamesOf(Collection<LikeEvent> events) {
        return events.stream().map(LikeEvent::getUsername).toList();
    }
}
