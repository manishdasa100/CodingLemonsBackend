package com.codinglemonsbackend.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.DatabaseSequence;

@Repository
public class SequenceGeneratorRepository {

    private MongoTemplate mongoTemplate;

    public SequenceGeneratorRepository(@Autowired MongoTemplate mongoTemplate){
        this.mongoTemplate = mongoTemplate;
    }
    
    public void saveSequence(DatabaseSequence sequence) {
        mongoTemplate.save(sequence, "database_sequences");
    }

    public DatabaseSequence getSequence(String sequenceName) {
        Query query = new Query(Criteria.where("id").is(sequenceName));

        DatabaseSequence counter = mongoTemplate.findAndModify(
            query, 
            new Update().inc("seq", 1),
            FindAndModifyOptions.options().returnNew(true), 
            DatabaseSequence.class, 
            "database_sequences");

        return counter;

    }
}
