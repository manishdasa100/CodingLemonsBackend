package com.codinglemonsbackend.Service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.codinglemonsbackend.Dto.LikeEvent;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Repository.LikeRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Liking is queued rather than written, so this class only has to answer one question - does this
 * user currently like the problem - and keep that answer right between the click and the batch.
 * A single hash field per user and problem holds it, which is why a click is one read and one
 * write rather than a set to add to, a set to remove from and a cache to invalidate.
 */
@Slf4j
@Service
public class LikeService {

    private static final String LIKED = "1";
    private static final String NOT_LIKED = "0";

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${queue.like-events.stream}")
    private String likeEventsStream;

    @Value("${like.status.ttl-seconds:900}")
    private long statusTtlSeconds;

    /** Liking something already liked is a no-op - the click only tells us what we already know. */
    public void likeProblem(String username, Integer problemId) {
        if (getLikeStatus(username, problemId)) return;
        queue(username, problemId, true);
        recordStatus(username, problemId, true);
    }

    public void dislikeProblem(String username, Integer problemId) throws DuplicateResourceException {
        if (!getLikeStatus(username, problemId)) return;
        queue(username, problemId, false);
        recordStatus(username, problemId, false);
    }

    /**
     * Whether the user currently likes the problem, counting clicks the batch has not stored yet.
     * A miss is filled from the database, so the next read - and the next click's duplicate check -
     * costs a single Redis call.
     */
    public Boolean getLikeStatus(String username, Integer problemId) {
        String field = Integer.toString(problemId);
        String known = redisService.getHashValue(statusKey(username), field);
        if (known != null) return LIKED.equals(known);

        boolean stored = likeRepository.isLiked(username, problemId);
        recordStatus(username, problemId, stored);
        return stored;
    }

    /**
     * Written only once the queue has taken the event, because the queued event is the durable
     * copy. Marking the user first and then failing to queue would show a like that never happens
     * and block the retry, since the duplicate check above reads this very field.
     */
    private void recordStatus(String username, Integer problemId, boolean liked) {
        redisService.storeHashPipelined(statusKey(username), Integer.toString(problemId),
                liked ? LIKED : NOT_LIKED, statusTtlSeconds);
    }

    private void queue(String username, Integer problemId, boolean isLike) {
        LikeEvent likeEvent = new LikeEvent(problemId, username, isLike);
        try {
            redisService.addToStream(likeEventsStream,
                    Map.of("body", objectMapper.writeValueAsString(likeEvent)));
        } catch (Exception e) {
            log.error("Could not queue a {} by {} on problem {}",
                    isLike ? "like" : "dislike", username, problemId, e);
            throw new RuntimeException("Failed to send like event to queue", e);
        }
    }

    private String statusKey(String username) {
        return RedisService.USER_LIKE_STATUS_PREFIX + username;
    }
}
