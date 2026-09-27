package com.codinglemonsbackend.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Update.Position;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.ProblemOfTheDayEntity;

@Repository
public class ProblemOfTheDayRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    public ProblemOfTheDayEntity getProblemOfTheDay(){

        ProblemOfTheDayEntity potd = mongoTemplate.findById(ProblemOfTheDayEntity.ENTITY_NAME, ProblemOfTheDayEntity.class);

        return potd;
    }

    /**
     * Promotes a new problem and pushes the outgoing one onto the front of the history, trimmed to
     * {@code historySize}, in one atomic update. History used to be rewritten wholesale from a copy
     * read moments earlier, so two writers - the midnight job on two instances, or that job racing
     * an admin override - could each silently drop the other's entry and let a problem come back
     * round early. Mongo now does the prepend and the trim itself, against whatever is actually
     * stored.
     */
    public void saveProblemOfTheDay(Integer newProblemId, Integer outgoingProblemId, int historySize) {
        Update update = new Update().set("problemId", newProblemId);
        if (outgoingProblemId != null) {
            update.push("history").atPosition(Position.FIRST).slice(historySize).each(outgoingProblemId);
        }
        mongoTemplate.upsert(new Query(Criteria.where("_id").is(ProblemOfTheDayEntity.ENTITY_NAME)),
            update, ProblemOfTheDayEntity.class);
    }
}
