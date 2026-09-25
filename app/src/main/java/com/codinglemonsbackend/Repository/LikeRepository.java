package com.codinglemonsbackend.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.LikeEvent;
import com.codinglemonsbackend.Entities.UserLikedProblems;

@Repository
public class LikeRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    /** Identifies one user's like on one problem within a batch. */
    public static String likeKey(Integer problemId, String username) {
        return problemId + ":" + username;
    }

    /** One index hit on _id, then a membership test inside that user's own document. */
    public boolean isLiked(String username, Integer problemId) {
        Query query = new Query(Criteria.where("_id").is(username)
                .and("likedProblemIds").is(problemId));
        return mongoTemplate.exists(query, UserLikedProblems.class);
    }

    /**
     * What the users in this batch already like. A coalesced batch knows the state each user ended
     * up in, not whether it changed anything, so the counter can only move on pairs that actually
     * flip - and comparing against stored state is also what makes replaying a batch a no-op.
     *
     * Returns each user's whole set, which is a superset of the pairs asked about; the caller only
     * looks up the keys it cares about. Trimming it server side would cost more than shipping it,
     * because the sets are bounded by the problem catalogue.
     */
    public Set<String> findExistingLikeKeys(Collection<LikeEvent> events) {
        if (events.isEmpty()) return Set.of();

        Set<String> usernames = events.stream()
                .map(LikeEvent::getUsername)
                .collect(Collectors.toSet());

        Query query = new Query(Criteria.where("_id").in(usernames));
        query.fields().include("likedProblemIds");

        Set<String> keys = new HashSet<>();
        for (UserLikedProblems liked : mongoTemplate.find(query, UserLikedProblems.class)) {
            if (liked.getLikedProblemIds() == null) continue;
            liked.getLikedProblemIds()
                    .forEach(problemId -> keys.add(likeKey(problemId, liked.getUsername())));
        }
        return keys;
    }

    /**
     * Applies one coalesced batch in a single round trip. $addToSet and $pull are set operations,
     * so a redelivered batch cannot add a like twice or remove one that is already gone - the
     * uniqueness a compound index used to enforce is now a property of the update itself.
     * UNORDERED so one bad pair cannot take the rest of the batch with it.
     */
    public void applyLikeBatch(Collection<LikeEvent> likes, Collection<LikeEvent> dislikes) {
        if (likes.isEmpty() && dislikes.isEmpty()) return;

        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, UserLikedProblems.class);

        // Upsert: a user's first like is also the first time they get a document.
        likes.forEach(event -> bulkOps.upsert(userQuery(event),
                new Update().addToSet("likedProblemIds", event.getProblemId())));

        dislikes.forEach(event -> bulkOps.updateOne(userQuery(event),
                new Update().pull("likedProblemIds", event.getProblemId())));

        bulkOps.execute();
    }

    private Query userQuery(LikeEvent event) {
        return new Query(Criteria.where("_id").is(event.getUsername()));
    }
}
