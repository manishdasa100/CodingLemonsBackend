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

import com.codinglemonsbackend.Entities.Company;

import jakarta.annotation.PostConstruct;

@Repository
public class CompanyRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    private List<Company> companyTags;

    @PostConstruct
    private void fetchCompanyTags() {
        this.companyTags = mongoTemplate.findAll(Company.class);
    }

    public void addCompanyTag(Company companyTag) {
        mongoTemplate.save(companyTag);
    }

    public List<Company> getAllCompanyTags() {
        return this.companyTags;
    }

    public Set<Company> getValidTags(List<String> companySlugs) {
        
        // Return the matching tags
        Set<Company> matchingTags = this.companyTags.stream()
            .filter(companyTag -> companySlugs.contains(companyTag.getSlug()))
            .collect(Collectors.toSet());
        return matchingTags;
    }

    public void removeCompanyTag(String slug){
        Query query = new Query(Criteria.where("slug").is(slug));
        mongoTemplate.remove(query, Company.class);
        this.companyTags.removeIf(companyTag -> companyTag.getSlug().equals(slug));
    }
}
