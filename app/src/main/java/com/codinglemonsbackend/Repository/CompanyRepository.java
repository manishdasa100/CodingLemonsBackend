package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public void saveCompany(Company companyTag) {
        mongoTemplate.save(companyTag);
    }

    public List<Company> getAllCompanies() {
        return mongoTemplate.findAll(Company.class);
    }

    public void removeCompany(String slug){
        Query query = new Query(Criteria.where("slug").is(slug));
        mongoTemplate.remove(query, Company.class);
    }
}
