package com.codinglemonsbackend.Repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.Submission;

@Repository
public class SubmissionRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    public String saveSubmission(Submission submission){
        
        Submission savedSubmission = mongoTemplate.save(submission);

        return savedSubmission.getSubmissionId();
    }

    public Optional<Submission> getSubmission(String submissionId){

        Query query = new Query(Criteria.where("submissionId").is(submissionId));

        Submission submission = mongoTemplate.findOne(query, Submission.class);

        return Optional.ofNullable(submission);
    }
}
