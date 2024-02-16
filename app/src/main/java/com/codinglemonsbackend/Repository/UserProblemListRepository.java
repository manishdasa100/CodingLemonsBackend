package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

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

    public void updateProblemList(String id){

    }

    public Boolean deleteProblemList(String id){
        
        return true;
    }
}
