package com.codinglemonsbackend.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.ProblemOfTheDayMetadata;

import java.util.Objects;

@Repository
public class ProblemOfTheDayRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    public ProblemOfTheDayMetadata getProblemOfTheDayMetadata(){

        ProblemOfTheDayMetadata problemOfTheDayMetadata = mongoTemplate.findById(ProblemOfTheDayMetadata.ENTITY_NAME, ProblemOfTheDayMetadata.class);

        return problemOfTheDayMetadata;
    }

    public void saveProblemOfTheDayMetadata(Integer problemId){

        System.out.println("SAVING PROBLEM OF THE DAY WITH ID: " + problemId);

        Query query = new Query(Criteria.where("id").is(ProblemOfTheDayMetadata.ENTITY_NAME));

        ProblemOfTheDayMetadata metadata = mongoTemplate.findAndModify(query, new Update().set("problemId", problemId), ProblemOfTheDayMetadata.class);

        if(Objects.isNull(metadata)) {

            ProblemOfTheDayMetadata problemOfTheDayMetadata = new ProblemOfTheDayMetadata();
            problemOfTheDayMetadata.setId(ProblemOfTheDayMetadata.ENTITY_NAME);
            problemOfTheDayMetadata.setProblemId(problemId);

            mongoTemplate.save(
                problemOfTheDayMetadata, 
                ProblemOfTheDayMetadata.ENTITY_NAME
            );
        } 
    }
}
