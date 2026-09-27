package com.codinglemonsbackend.Repository;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.DatabaseSequence;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor 
public class SequenceGeneratorRepository {

    public static final String COLLECTION_NAME = "database_sequences";

    private final MongoTemplate mongoTemplate;

    // saveSequence is gone with the caller that used it - the upsert below is the only way a
    // counter comes into existence now, which is the point.

    /**
     * Never returns null: upsert creates the counter at 1 on its first use, so allocating an id is
     * one atomic round trip whether or not the sequence already exists. Without the upsert the
     * first call came back null and the caller had to create the counter itself - two concurrent
     * first calls both did, and both handed out 1.
     */
    public DatabaseSequence getNextSequence(String sequenceName) {

        Query query = new Query(Criteria.where("id").is(sequenceName));

        DatabaseSequence counter = mongoTemplate.findAndModify(
            query,
            new Update().inc("seq", 1),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            DatabaseSequence.class,
            COLLECTION_NAME);

        return counter;

    }

    public DatabaseSequence getCurrentSequence(String sequenceName) {

        DatabaseSequence dbSeq = mongoTemplate.findById(sequenceName, DatabaseSequence.class, COLLECTION_NAME);

        return dbSeq;
    }
}
