package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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

    public Optional<SubmissionEntity> getUserSubmissionById(String username, String submissionId){

        Criteria criteria = new Criteria().andOperator(
            Criteria.where("submissionId").is(submissionId),
            Criteria.where("username").is(username)
        );
        
        Query query = new Query(criteria);

        SubmissionEntity submission = mongoTemplate.findOne(query, SubmissionEntity.class);

        return Optional.ofNullable(submission);
    }

    public List<SubmissionEntity> getUserSubmissionsByProblemId(String username, Integer problemId) {
        Criteria criteria = new Criteria().andOperator(
            Criteria.where("username").is(username),
            Criteria.where("problemId").is(problemId)
        );

        Query query = new Query(criteria)
            .with(Sort.by(Sort.Direction.DESC, "dateOfSubmission"));

        List<SubmissionEntity> submission = mongoTemplate.find(query, SubmissionEntity.class);
        return submission;
    }

    public List<SubmissionEntity> getRecentUserSubmissions(String username, Integer limit) {
        Query query = new Query(Criteria.where("username").is(username))
            .with(Sort.by(Sort.Direction.DESC, "dateOfSubmission"))
            .limit(limit);

        return mongoTemplate.find(query, SubmissionEntity.class);
    }
}
