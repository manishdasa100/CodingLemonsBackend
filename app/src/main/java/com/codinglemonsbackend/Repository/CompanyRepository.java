package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.Company;

@Repository
public class CompanyRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void saveCompany(Company company) {
        mongoTemplate.save(company);
    }

    public List<Company> getAllCompanies() {
        return mongoTemplate.findAll(Company.class);
    }

    public void removeCompany(String slug){
        Query query = new Query(Criteria.where("slug").is(slug));
        mongoTemplate.remove(query, Company.class);
    }

    public List<Company> findBySlugIn(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) return List.of();
        Query query = new Query(Criteria.where("slug").in(slugs));
        return mongoTemplate.find(query, Company.class);
    }

    public Optional<Company> findBySlug(String slug) {
        Query query = new Query(Criteria.where("slug").is(slug));
        return Optional.ofNullable(mongoTemplate.findOne(query, Company.class));
    }
}
