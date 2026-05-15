package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.BadgeRuleType;
import com.codinglemonsbackend.Entities.BadgeEntity;

@Repository
public class BadgeRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public BadgeEntity save(BadgeEntity badge) {
        return mongoTemplate.save(badge);
    }

    public Optional<BadgeEntity> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, BadgeEntity.class));
    }

    public Optional<BadgeEntity> findByName(String name) {
        Query query = new Query(Criteria.where("name").is(name));
        return Optional.ofNullable(mongoTemplate.findOne(query, BadgeEntity.class));
    }

    public List<BadgeEntity> findAll() {
        return mongoTemplate.findAll(BadgeEntity.class);
    }

    public List<BadgeEntity> findByRuleType(BadgeRuleType type) {
        Query query = new Query(Criteria.where("rule.type").is(type));
        return mongoTemplate.find(query, BadgeEntity.class);
    }

    public List<BadgeEntity> findAllByIds(List<String> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        return mongoTemplate.find(query, BadgeEntity.class);
    }

    public long deleteById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, BadgeEntity.class).getDeletedCount();
    }
}
