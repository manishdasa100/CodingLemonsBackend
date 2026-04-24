package com.codinglemonsbackend.Repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.SubmissionEntity;

@Repository
public class SubmissionRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    public String saveSubmission(@NonNull SubmissionEntity submission){
        
        SubmissionEntity savedSubmission = mongoTemplate.save(submission);

        return savedSubmission.getSubmissionId();
    }

    public Optional<SubmissionEntity> getSubmission(String submissionId){

        Query query = new Query(Criteria.where("submissionId").is(submissionId));

        SubmissionEntity submission = mongoTemplate.findOne(query, SubmissionEntity.class);

        return Optional.ofNullable(submission);
    }
}
