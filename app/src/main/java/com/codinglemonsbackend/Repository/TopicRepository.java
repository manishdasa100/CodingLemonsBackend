package com.codinglemonsbackend.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.TopicTag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Repository
public class TopicRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    public void addTopicTag(TopicTag topicTag) {
        mongoTemplate.save(topicTag);
    }

    public List<TopicTag> getAllTopicTags() {
        return mongoTemplate.findAll(TopicTag.class);
    }

    public void removeTopicTag(String slug){
        Query query = new Query(Criteria.where("slug").is(slug));
        mongoTemplate.remove(query, TopicTag.class);
    }

    public Set<TopicTag> getValidTags(Set<String> topicSlugs) {
        
        // Return the matching tags

        Query query = new Query(Criteria.where("slug").in(topicSlugs));

        List<TopicTag> matchingTags = mongoTemplate.find(query, TopicTag.class);

        return new HashSet<TopicTag>(matchingTags);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class CountResult {
        private int count;
    }
}
