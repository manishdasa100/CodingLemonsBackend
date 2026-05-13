package com.codinglemonsbackend.Repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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

    public void saveProblemOfTheDay(Integer problemId, List<Integer> history) {
        Query query = new Query(Criteria.where("_id").is(ProblemOfTheDayEntity.ENTITY_NAME));
        Update update = new Update()
            .set("problemId", problemId)
            .set("history", history);
        mongoTemplate.upsert(query, update, ProblemOfTheDayEntity.class);
    }
}
