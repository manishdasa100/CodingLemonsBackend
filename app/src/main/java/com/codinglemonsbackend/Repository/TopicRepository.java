package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.Topic;

import jakarta.annotation.PostConstruct;

@Repository
public class TopicRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    private List<Topic> topicTags;

    @PostConstruct
    public void fetchTopicTags() {
        this.topicTags = mongoTemplate.findAll(Topic.class);
    }

    public void addTopicTag(Topic topicTag) {
        mongoTemplate.save(topicTag);
    }

    public List<Topic> getAllTopicTags() {
        return this.topicTags;
    }

    public void removeTopicTag(String slug){
        Query query = new Query(Criteria.where("slug").is(slug));
        mongoTemplate.remove(query, Topic.class);
        this.topicTags.removeIf(topicTag -> topicTag.getSlug().equals(slug));
    }

    public Set<Topic> getValidTags(List<String> topicSlugs) {
        
        Set<Topic> matchingTags = this.topicTags.stream()
            .filter(topicTag -> topicSlugs.contains(topicTag.getSlug()))
            .collect(Collectors.toSet());

        return matchingTags;
    }

}
