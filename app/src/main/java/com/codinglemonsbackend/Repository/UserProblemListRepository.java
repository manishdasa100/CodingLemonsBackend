package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.VariableOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.ProblemListEntity;

@Repository
public class UserProblemListRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String COLLECTION_NAME = "UserProblemList";

    // public Optional<UserProblemList> getUserProblemList(String name, String username){

    //     Query query = new Query(new Criteria().andOperator(
    //         Criteria.where("creator").is(username),
    //         Criteria.where("name").is(name)
    //     ));
        
    //     UserProblemList userProblemList = mongoTemplate.findOne(query, UserProblemList.class);

    //     return Optional.ofNullable(userProblemList);
    // }

    // private String[] projectionFields = {"title", "difficulty", "acceptedCount", "submissionCount"};

    public List<ProblemListDto> getAllProblemListsOfUser(String username){

        MatchOperation matchOperation = Aggregation.match(
                Criteria.where("creator").is(username)       // Example condition to filter by name
        );

        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from("problems")                            
                .localField("problemIds")                    
                .foreignField("_id")                          
                .as("problemsData");                           


        ProjectionOperation projectFields = Aggregation.project()
            .and( 
                VariableOperators.Map.itemsOf("problemsData")
                    .as("e")
                    .andApply(ctx -> new Document("_id", "$$e._id").append("title", "$$e.title").append("description", "$$e.description")))
            .as("problemsData")
            .andInclude("name", "description");


        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation,
                lookupOperation,
                projectFields
        );

        AggregationResults<ProblemListDto> results = mongoTemplate.aggregate(aggregation, "UserProblemList", ProblemListDto.class);

        return results.getMappedResults();
    }

    public void saveProblemList(ProblemListEntity problemList){
        mongoTemplate.save(problemList);
    }

    public void updateProblemList(String listId, String name, String description){
        
        ProblemListEntity userProblemList = mongoTemplate.findById(listId, ProblemListEntity.class);
        
        if (!Objects.isNull(userProblemList)) {
            
            if (!Objects.isNull(name)){
                userProblemList.setName(name);
            }

            if (!Objects.isNull(description)) {
                userProblemList.setDescription(description);
            }

            saveProblemList(userProblemList);

        } else {
            throw new NoSuchElementException("No problem list found with id " + listId);
        }
    }

    public void addProblemToProblemList(String listId, Integer problemId){

        ProblemListEntity userProblemList = mongoTemplate.findById(listId, ProblemListEntity.class);

        Boolean probleExists = mongoTemplate.exists(new Query(Criteria.where("problemId").is(problemId)), ProblemEntity.class, "problems");

        if (!Objects.isNull(userProblemList) && probleExists) {
            userProblemList.getProblemIds().add(problemId);
            saveProblemList(userProblemList);
        } else {
            throw new NoSuchElementException("Provide correct problem list id and problem id");
        }
    }

    public Boolean deleteProblemList(String id){
        
        return true;
    }
}
