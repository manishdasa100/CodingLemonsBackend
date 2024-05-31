package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.UserProblemList;

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

    public List<UserProblemList> getAllProblemListsOfUser(String username){

        Query query = new Query(Criteria.where("creator").is(username));
        
        List<UserProblemList> userProblemLists = mongoTemplate.find(query, UserProblemList.class);

        return userProblemLists;
    }

    public void saveProblemList(UserProblemList problemList){
        mongoTemplate.save(problemList);
    }

    public void updateProblemList(String listId, String name, String description){
        
        UserProblemList userProblemList = mongoTemplate.findById(listId, UserProblemList.class);
        
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

        UserProblemList userProblemList = mongoTemplate.findById(listId, UserProblemList.class);

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
