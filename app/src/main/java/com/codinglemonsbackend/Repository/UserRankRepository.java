package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
        UserRank savedUserRank = mongoTemplate.save(newUserRank);
        return savedUserRank;
    }

    public void updateUserRank(String rankName, Map<String, Object> updateProperies) {
        Update update = new Update();
        for (String key : updateProperies.keySet()) {
            update.set(key, updateProperies.get(key));
        }
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(rankName)), update, UserRank.class);
    }

    public Long deleteUserRank(String rankName) {
        Query query = new Query(Criteria.where("_sid").is(rankName));
        return mongoTemplate.remove(query, UserRank.class).getDeletedCount();
    }

}
