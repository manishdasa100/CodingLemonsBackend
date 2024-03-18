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

    public static final String COLLECTION_NAME = "database_sequences";

    private MongoTemplate mongoTemplate;

    public SequenceGeneratorRepository(@Autowired MongoTemplate mongoTemplate){
        this.mongoTemplate = mongoTemplate;
    }
    
    public void saveSequence(DatabaseSequence sequence) {
        mongoTemplate.save(sequence, COLLECTION_NAME);
    }

    public DatabaseSequence getNextSequence(String sequenceName) {
        
        Query query = new Query(Criteria.where("id").is(sequenceName));

        DatabaseSequence counter = mongoTemplate.findAndModify(
            query, 
            new Update().inc("seq", 1),
            FindAndModifyOptions.options().returnNew(true), 
            DatabaseSequence.class, 
            COLLECTION_NAME);

        return counter;

    }

    public DatabaseSequence getCurrentSequence(String sequenceName) {

        DatabaseSequence dbSeq = mongoTemplate.findById(sequenceName, DatabaseSequence.class, COLLECTION_NAME);

        return dbSeq;
    }
}
