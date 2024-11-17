package com.codinglemonsbackend.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.CompanyTag;

@Repository
public class CompanyRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void addCompanyTag(CompanyTag companyTag) {
        mongoTemplate.save(companyTag);
    }

    public List<CompanyTag> getAllCompanyTags() {
        return mongoTemplate.findAll(CompanyTag.class);
    }

    public Set<CompanyTag> getValidTags(Set<String> companySlugs) {
        
        // Return the matching tags

        Query query = new Query(Criteria.where("slug").in(companySlugs));
        List<CompanyTag> matchingTags = mongoTemplate.find(query, CompanyTag.class);
        return new HashSet<CompanyTag>(matchingTags);
    }

    public void removeCompanyTag(String slug){
        Query query = new Query(Criteria.where("slug").is(slug));
        mongoTemplate.remove(query, CompanyTag.class);
    }
}
