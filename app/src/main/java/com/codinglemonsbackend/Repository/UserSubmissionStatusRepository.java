package com.codinglemonsbackend.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.UserSubmissionStatusEntity;
import com.mongodb.client.result.UpdateResult;

@Repository
public class UserSubmissionStatusRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void createForUser(String username) {
        UserSubmissionStatusEntity entity = UserSubmissionStatusEntity.builder()
                .username(username)
                .build();
        mongoTemplate.save(entity);
    }

    /**
     * Atomically adds problemId to solvedProblemIds, removes it from attemptedProblemIds,
     * and increments the per-difficulty counter — all in a single DB round-trip.
     * The $ne guard ensures the update only fires when this is a genuinely new solve,
     * so $inc never double-counts.
     *
     * @return true if the document was modified (i.e. this is a new solve)
     */
    public boolean addToSolvedAndRemoveFromAttempted(String username, Integer problemId, String difficulty) {
        Query query = Query.query(
                Criteria.where("username").is(username)
                        .and("solvedProblemIds").nin(problemId)
        );
        Update update = new Update()
                .addToSet("solvedProblemIds", problemId)
                .pull("attemptedProblemIds", problemId)
                .inc("solvedCountByDifficulty." + difficulty, 1);
        UpdateResult result = mongoTemplate.updateFirst(query, update, UserSubmissionStatusEntity.class);
        return result.getModifiedCount() > 0;
    }

    /**
     * Adds problemId to attemptedProblemIds only when it is not already present
     * in solvedProblemIds. The "not in solved" guard is expressed in the query
     * filter so the whole operation is atomic — no separate read needed.
     */
    public void addToAttemptedIfNotSolved(String username, Integer problemId) {
        Query query = Query.query(
                Criteria.where("username").is(username)
                        .and("solvedProblemIds").nin(problemId)
        );
        Update update = new Update().addToSet("attemptedProblemIds", problemId);
        mongoTemplate.updateFirst(query, update, UserSubmissionStatusEntity.class);
    }

    public UserSubmissionStatusEntity getByUsername(String username) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("username").is(username)),
                UserSubmissionStatusEntity.class
        );
    }

    public UserSubmissionStatusEntity getSubmissionStatusDto(String username) {
        Query query = Query.query(Criteria.where("username").is(username));
        query.fields().include("solvedCountByDifficulty");
        return mongoTemplate.findOne(query, UserSubmissionStatusEntity.class);
    }
}
