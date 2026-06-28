package com.codinglemonsbackend.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.VariableOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Entities.ProblemEntity;
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

        query.fields()
            .include("problemId", "language", "dateOfSubmission", "runtimeMs", "memoryMb", "status");

        List<SubmissionEntity> submission = mongoTemplate.find(query, SubmissionEntity.class);
        return submission;
    }

    public List<SubmissionDto> getRecentUserSubmissions(String username, Integer limit) {

        MatchOperation matchOperation = Aggregation.match(Criteria.where("username").is(username));

        LookupOperation lookupOperation = LookupOperation.newLookup()
            .from(ProblemEntity.ENTITY_COLLECTION_NAME)
            .localField("problemId")
            .foreignField("_id")
            .as("problemData");

        ProjectionOperation projectFields = Aggregation.project()
            .and(
                ArrayOperators.ArrayElemAt.arrayOf(
                    VariableOperators.Map.itemsOf("problemData")
                        .as("e")
                        .andApply(ctx -> new Document("_id", "$$e._id")
                                            .append("title", "$$e.title")
                                            .append("difficulty", "$$e.difficulty")
                                        ))
                    .elementAt(0)
            ).as("problemData")
            .andInclude("problemId", "language", "dateOfSubmission", "status");

        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(matchOperation);
        operations.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "dateOfSubmission")));
        if (limit != -1) {
            operations.add(Aggregation.limit(limit));
        }
        operations.add(lookupOperation);
        operations.add(projectFields);

        Aggregation aggregation = Aggregation.newAggregation(operations);

        return mongoTemplate.aggregate(aggregation, SubmissionEntity.class, SubmissionDto.class).getMappedResults();
    }
}
