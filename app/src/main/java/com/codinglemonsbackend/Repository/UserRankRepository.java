package com.codinglemonsbackend.Repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.UserRank;


@Repository
public class UserRankRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    // public List<UserRank> loadUserRanksSortedAscByMilestonePoints() {
    //     List<UserRank> allRanks = getAllRanks();
    //     allRanks.sort((rank1, rank2) -> rank1.getMilestonePoints() - rank2.getMilestonePoints());
    //     return allRanks;
    // }
    
    public List<UserRank> getAllRanks() {
        return mongoTemplate.findAll(UserRank.class);
    }
    
    public UserRank saveUserRank(UserRank newUserRank) {
        throw new RuntimeException("UserRank save to db failed");
        //UserRank savedUserRank = mongoTemplate.save(newUserRank);
        //return savedUserRank;
    }

    public void updateUserRank(String rankID) {
        // Implement the update logic here
    }

    public Long deleteUserRank(String rankId) {
        Query query = new Query(Criteria.where("id").is(rankId));
        return mongoTemplate.remove(query, UserRank.class).getDeletedCount();
    }


}
