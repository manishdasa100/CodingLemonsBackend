package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.LikeEvent;
import com.codinglemonsbackend.Entities.UserLike;
import com.mongodb.bulk.BulkWriteResult;

@Repository
public class LikeRepository {

    @Autowired
    private MongoTemplate mongoTemplate;
    
    public Optional<UserLike> findByUsernameAndProblemId(String username, Integer problemId){

        Criteria criteria  = new Criteria().andOperator(
            Criteria.where("problemId").is(problemId),
            Criteria.where("username").is(username)
        );

        Query query = new Query(criteria);

        UserLike userLike = mongoTemplate.findOne(query, UserLike.class);

        return Optional.ofNullable(userLike);
    }

    public BulkWriteResult bulkInsert(List<LikeEvent> likeEvents){
        
        // List<LikeEvent> userLikeEvents = likeEvents.stream()
        //                             .filter(LikeEvent::getIsLike)
        //                             .collect(Collectors.toList());

        // List<LikeEvent> userDislikeEvents = likeEvents.stream()
        //                             .filter(likeEvent -> !likeEvent.getIsLike())
        //                             .collect(Collectors.toList());
        
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.ORDERED, UserLike.class);

        likeEvents.stream()
                    .forEach(event -> {
                        if (event.getIsLike()) bulkOps.insert(new UserLike(event.getProblemId(), event.getUsername(), event.getCreatedAt()));
                        else bulkOps.remove(new Query(Criteria.where("problemId").is(event.getProblemId()).and("username").is(event.getUsername()))); 
                    });
        
        // userLikeEvents.stream().map(event -> new UserLike(event.getProblemId(), event.getUsername(), event.getCreatedAt()))
        //     .forEach(bulkOps::insert);

        // userDislikeEvents.stream().map(event -> new Query(Criteria.where("problemId").is(event.getProblemId()).and("username").is(event.getUsername())))
        //     .forEach(bulkOps::remove);
        
        
        BulkWriteResult result = bulkOps.execute();    
        
        return result;
    }
}
