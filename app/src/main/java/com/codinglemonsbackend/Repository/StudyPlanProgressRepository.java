package com.codinglemonsbackend.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.UserStudyPlanProgress;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class StudyPlanProgressRepository {

    private final MongoTemplate mongoTemplate;
    
    public void saveStudyPlanprogress(UserStudyPlanProgress studyPlanProgress){
        if (studyPlanProgress == null) return; 
        mongoTemplate.save(studyPlanProgress);
    }

    public void deleteStudyPlanProgress(String listId, String owner) {
        if (listId == null || listId.isEmpty() || owner == null || owner.isEmpty()) return; 
        Criteria criteria = new Criteria().andOperator(
            Criteria.where("listId").is(listId),
            Criteria.where("owner").is(owner)
        );
        Query query = new Query(criteria);
        mongoTemplate.remove(query, UserStudyPlanProgress.class);
    }

    public Optional<UserStudyPlanProgress> getStudyPlanProgress(String listId, String owner) {
        if (listId == null || listId.isEmpty() || owner == null || owner.isEmpty()) return Optional.empty(); 
        Criteria criteria = new Criteria().andOperator(
            Criteria.where("listId").is(listId),
            Criteria.where("owner").is(owner)
        );
        Query query = new Query(criteria);
        UserStudyPlanProgress progress = mongoTemplate.findOne(query, UserStudyPlanProgress.class);
        return Optional.ofNullable(progress);
    }

    public void addProblemIdToProgress(String listId, String owner, Integer problemId) {
        Criteria criteria = new Criteria().andOperator(
            Criteria.where("owner").is(owner),
            Criteria.where("listId").is(listId)
        );

        Query query = new Query(criteria);

        Update update = new Update().addToSet("solvedProblemIds", problemId);

        mongoTemplate.updateFirst(query, update, UserStudyPlanProgress.class);
    }
}
