package com.codinglemonsbackend.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.LikeEvent;
import com.codinglemonsbackend.Repository.LikeRepository;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Drains queued like and dislike events in batches.
 *
 * Liking is the cheapest thing a user can do and the easiest to repeat, so the events are queued
 * rather than written per click and collapsed before they reach the database: within one batch
 * only a user's final state on a given problem survives, and a problem's counter moves once by the
 * net change rather than once per event. Toggling a like twenty times costs one write.
 *
 * Deliberately not a StreamMessageListenerContainer - that delivers one record at a time, which is
 * the opposite of what batching needs.
 */
@Slf4j
@Component
public class LikeEventsProcessor {

    private final RedisService redisService;
    private final LikeRepository likeRepository;
    private final ProblemsRepository problemsRepository;
    private final ObjectMapper objectMapper;
    private final String likeEventsStream;
    private final String consumerGroup;
    private final String consumerName;
    private final int batchSize;
    private final Duration retryAfter;
    private final long maxDeliveries;

    public LikeEventsProcessor(
            RedisService redisService,
            LikeRepository likeRepository,
            ProblemsRepository problemsRepository,
            ObjectMapper objectMapper,
            @Value("${queue.like-events.stream}") String likeEventsStream,
            @Value("${queue.like-events.group:backend-like-processors}") String consumerGroup,
            @Value("${queue.like-events.consumer:backend-like-processor}") String consumerName,
            @Value("${like.batch.max-size:100}") int batchSize,
            @Value("${queue.like-events.retry-after-seconds:60}") long retryAfterSeconds,
            @Value("${queue.like-events.max-deliveries:2}") long maxDeliveries) {
        this.redisService = redisService;
        this.likeRepository = likeRepository;
        this.problemsRepository = problemsRepository;
        this.objectMapper = objectMapper;
        this.likeEventsStream = likeEventsStream;
        this.consumerGroup = consumerGroup;
        // Identifies this instance in XINFO CONSUMERS. It carries no correctness weight: stale
        // work is found through the group's pending list, which spans every consumer in it.
        this.consumerName = consumerName;
        this.batchSize = batchSize;
        this.retryAfter = Duration.ofSeconds(retryAfterSeconds);
        this.maxDeliveries = maxDeliveries;
    }

    @PostConstruct
    void start() {
        ensureConsumerGroup();
    }

    private void ensureConsumerGroup() {
        try {
            redisService.createConsumerGroup(likeEventsStream, consumerGroup);
        } catch (Exception e) {
            // The next tick re-attempts, so a Redis outage at boot does not take the app down.
            log.error("Could not create consumer group {} on {}", consumerGroup, likeEventsStream, e);
        }
    }

    @Scheduled(fixedDelayString = "${queue.like-events.interval-ms:10000}")
    public void drain() {
        List<MapRecord<String, String, String>> records;
        try {
            records = read();
        } catch (Exception e) {
            log.warn("Could not read like events from {}: {}", likeEventsStream, e.getMessage());
            ensureConsumerGroup();
            return;
        }
        if (records.isEmpty()) return;

        List<RecordId> handled = new ArrayList<>(records.size());
        // Stream ids are monotonic and assigned by Redis, so iteration order settles which of a
        // user's events on a problem wins. The event's own createdAt cannot: it is stamped by
        // whichever app instance took the click, and two clocks disagree.
        Map<String, LikeEvent> winners = new LinkedHashMap<>();

        for (MapRecord<String, String, String> record : records) {
            handled.add(record.getId());
            String body = record.getValue().get("body");
            if (body == null) {
                log.warn("Like event {} has no 'body' field - discarding it", record.getId());
                continue;
            }
            try {
                LikeEvent event = objectMapper.readValue(body, LikeEvent.class);
                winners.put(LikeRepository.likeKey(event.getProblemId(), event.getUsername()), event);
            } catch (Exception e) {
                // Deterministic: the same bytes will not parse on a retry either, so it is acked
                // below with the rest rather than left to come back forever.
                log.error("Discarding unreadable like event {}", record.getId(), e);
            }
        }

        try {
            apply(winners.values());
        } catch (RuntimeException e) {
            // Left unacked, so the next tick picks them up from this consumer's pending list.
            log.error("Could not apply a batch of {} like events - leaving them pending",
                    winners.size(), e);
            return;
        }

        retire(handled);
        log.info("Applied {} like events as {} distinct changes", handled.size(), winners.size());
    }

    /**
     * Stale leftovers first, then new arrivals, so both coalesce together. Reclaimed entries carry
     * lower stream ids than anything new, which keeps the combined list in id order and so keeps
     * "last event wins" meaning the same thing it always did.
     */
    private List<MapRecord<String, String, String>> read() {
        List<MapRecord<String, String, String>> records = new ArrayList<>(reclaimStale());
        records.addAll(redisService.readGroup(likeEventsStream, consumerGroup, consumerName, batchSize));
        return records;
    }

    /**
     * Entries an earlier batch left unacknowledged, once they have sat untouched for retryAfter.
     *
     * The idle guard is what makes this safe on more than one instance: XCLAIM hands over an entry
     * only if its idle time has passed, and resets that clock as it does, so of two instances
     * reaching for the same entry exactly one comes away with it. Reading a consumer's own pending
     * list has no such guard - it returns work another instance may be midway through applying,
     * and two applies of one batch would move a problem's counter twice.
     */
    private List<MapRecord<String, String, String>> reclaimStale() {
        PendingMessages pending =
                redisService.getPendingMessages(likeEventsStream, consumerGroup, batchSize);
        if (pending == null || pending.isEmpty()) return List.of();

        List<RecordId> stale = new ArrayList<>();
        List<RecordId> exhausted = new ArrayList<>();
        for (PendingMessage message : pending) {
            if (message.getElapsedTimeSinceLastDelivery().compareTo(retryAfter) < 0) continue;
            if (message.getTotalDeliveryCount() >= maxDeliveries) exhausted.add(message.getId());
            else stale.add(message.getId());
        }

        if (!exhausted.isEmpty()) {
            // A like nobody can apply is not worth a dead letter queue, but it must stop coming
            // back - otherwise it costs a reclaim every retryAfter for the life of the stream.
            log.error("Discarding {} like events that failed {} times", exhausted.size(), maxDeliveries);
            retire(exhausted);
        }
        if (stale.isEmpty()) return List.of();

        log.warn("Reclaiming {} like events left pending by an earlier batch", stale.size());
        List<MapRecord<String, String, String>> claimed = redisService.claimPending(
                likeEventsStream, consumerGroup, consumerName, retryAfter, stale.toArray(RecordId[]::new));
        return claimed == null ? List.of() : claimed;
    }

    /** Acking clears the pending entry; the record itself only leaves the stream on delete. */
    private void retire(List<RecordId> ids) {
        if (ids.isEmpty()) return;
        RecordId[] done = ids.toArray(RecordId[]::new);
        redisService.acknowledge(likeEventsStream, consumerGroup, done);
        redisService.deleteFromStream(likeEventsStream, done);
    }

    private void apply(Collection<LikeEvent> winners) {
        if (winners.isEmpty()) return;

        Set<String> alreadyLiked = likeRepository.findExistingLikeKeys(winners);

        List<LikeEvent> toLike = new ArrayList<>();
        List<LikeEvent> toUnlike = new ArrayList<>();
        Map<Integer, Integer> deltaByProblem = new HashMap<>();

        for (LikeEvent event : winners) {
            boolean stored = alreadyLiked.contains(
                    LikeRepository.likeKey(event.getProblemId(), event.getUsername()));
            boolean wanted = Boolean.TRUE.equals(event.getIsLike());

            // Only a pair that actually flips is written, and only a flip moves the counter.
            if (wanted && !stored) {
                toLike.add(event);
                deltaByProblem.merge(event.getProblemId(), 1, Integer::sum);
            } else if (!wanted && stored) {
                toUnlike.add(event);
                deltaByProblem.merge(event.getProblemId(), -1, Integer::sum);
            }
        }

        likeRepository.applyLikeBatch(toLike, toUnlike);

        // Only the problem's own count needs clearing. Each user's like status was already set to
        // its final value when they clicked, and the database has just been brought into line with
        // it - there is nothing left that disagrees.
        deltaByProblem.forEach((problemId, delta) -> {
            if (delta == 0) return;   // a like and an unlike by different users cancelled out
            problemsRepository.incrementLikes(problemId, delta);
            redisService.deleteKey(RedisService.PROBLEM_LIKES_COUNT_CACHE_PREFIX + problemId);
        });
    }
}
