package com.codinglemonsbackend.Repository;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.Hint;
import com.codinglemonsbackend.Entities.UserProblemHintEntity;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserProblemHintRepository {

    private final MongoTemplate mongoTemplate;
    
    public Optional<UserProblemHintEntity> find(String username, Integer problemId) {
        UserProblemHintEntity userProblemHintEntity = mongoTemplate.findOne(queryFor(username, problemId), UserProblemHintEntity.class);
        return Optional.ofNullable(userProblemHintEntity);
    }

    public void appendHint(String username, Integer problemId, ExecutionStatus status, Hint hint) {
        this.appendHint(username, problemId, status, hint, null);
    }

    // Upserts the (user, problem) document and appends the hint to hints.{status}.
    // historyCap null = keep every hint (TLE/MLE); non-null = keep only the last N (WA/CE/RE).
    // Takes the cap as a param so the repo never hardcodes the retention number —
    // TRANSIENT_HISTORY_CAP stays declared in exactly one place (AIHintService).
    public void appendHint(String username, Integer problemId, ExecutionStatus status, Hint hint, Integer historyCap) {
        Update.PushOperatorBuilder push = new Update().push("hints." + status.name());
        Update update = (historyCap == null) ? push.each(hint) : push.slice(-historyCap).each(hint);
        mongoTemplate.upsert(queryFor(username, problemId), update, UserProblemHintEntity.class);
    }

    private Query queryFor(String username, Integer problemId) {
        return new Query(new Criteria().andOperator(
            Criteria.where("username").is(username),
            Criteria.where("problemId").is(problemId)
        ));
    }
}
