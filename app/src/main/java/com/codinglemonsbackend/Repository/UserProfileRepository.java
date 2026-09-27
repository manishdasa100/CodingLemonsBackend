package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.UserProfileEntity;
import com.codinglemonsbackend.Entities.UserWorkExperience;
import com.mongodb.client.result.UpdateResult;

@Repository
public class UserProfileRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    public Optional<UserProfileEntity> getUserProfile(String username) {
        UserProfileEntity userProfileEntity = mongoTemplate.findById(username, UserProfileEntity.class);
        return Optional.ofNullable(userProfileEntity);
    }

    public List<String> getEarnedBadgeIds(String username) {
        Query query = new Query(Criteria.where("_id").is(username));
        query.fields().include("earnedBadgeIds");
        UserProfileEntity entity = mongoTemplate.findOne(query, UserProfileEntity.class);
        if (entity == null || entity.getEarnedBadgeIds() == null) return List.of();
        return entity.getEarnedBadgeIds();
    }

    public Optional<UserProfileEntity> getCurrentUserInfo(String username) {
        Query query = new Query(Criteria.where("_id").is(username));
        query.fields().include("firstName", "lastName", "profilePictureId");
        UserProfileEntity userProfileEntity = mongoTemplate.findOne(query, UserProfileEntity.class);
        return Optional.ofNullable(userProfileEntity);
    }

    public void saveUserProfile(UserProfileEntity userProfileEntity) {
        mongoTemplate.save(userProfileEntity, UserProfileEntity.ENTITY_COLLECTION_NAME);
        System.out.println("User profile saved");
    }

    // public void updateUserProfilePictureId(String username, String profilePictureId) {

    //     Query query = new Query(Criteria.where("username").is(username));

    //     Update update = new Update();

    //     if (profilePictureId != null) { 
    //         System.out.println("UPDATING PROFILE PICTURE ID");
    //         update.set("profilePictureId", profilePictureId);
    //         mongoTemplate.updateFirst(query, update, UserProfileEntity.class);
    //     } else {
    //         throw new IllegalArgumentException("profilePictureId cannot be null");
    //     }

    //     // if (!update.getUpdateObject().isEmpty()) {
    //     //     System.out.println("UPDATING PROFILE PICTURE ID");
    //     //     mongoTemplate.updateFirst(query, update, UserEntity.class);
    //     // }
    // }

    public void incrementScoreAndAddLanguageSkill(String username, int points, String languageSkill) {
        Query query = new Query(Criteria.where("username").is(username));
        Update update = new Update().inc("score", points);
        if (languageSkill != null && !languageSkill.isEmpty()) {
            update.addToSet("skillTags", languageSkill);
        }
        mongoTemplate.updateFirst(query, update, UserProfileEntity.class);
    }

    public void addEarnedBadge(String username, String badgeId) {
        Query query = new Query(Criteria.where("username").is(username));
        Update update = new Update().addToSet("earnedBadgeIds", badgeId);
        mongoTemplate.updateFirst(query, update, UserProfileEntity.class);
    }

    public boolean updateUserProfile(String username, Map<String, Object> updatePropertiesMap) {
        Query query = new Query(Criteria.where("username").is(username));
        Update update = new Update();
        updatePropertiesMap.entrySet().stream().forEach(e -> {
            update.set(e.getKey(), e.getValue());
        });
        UpdateResult result = mongoTemplate.updateFirst(query, update, UserProfileEntity.class);
        if (result.getModifiedCount() > 0) return true;
        return false;
    }

    /**
     * Appends the entry, or replaces the existing one for the same company.
     *
     * Append first, guarded on the company not already being listed, so the guard and the write are
     * one atomic operation - two concurrent calls cannot both decide it is absent and both append.
     * Only when that matches nothing is the company already there, and then the existing element is
     * overwritten in place. The previous pull-then-push could fail between its two updates and
     * leave the user's entry deleted outright.
     */
    public void upsertWorkExperience(String username, UserWorkExperience experience) {
        UpdateResult appended = mongoTemplate.updateFirst(
            new Query(Criteria.where("_id").is(username)
                .and("workExperience.companySlug").ne(experience.getCompanySlug())),
            new Update().push("workExperience", experience),
            UserProfileEntity.class);
        if (appended.getMatchedCount() > 0) return;

        mongoTemplate.updateFirst(
            new Query(Criteria.where("_id").is(username)
                .and("workExperience.companySlug").is(experience.getCompanySlug())),
            new Update().set("workExperience.$", experience),
            UserProfileEntity.class);
    }
}
