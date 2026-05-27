package com.codinglemonsbackend.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDto.Difficulty;
import com.codinglemonsbackend.Dto.ProblemsPage;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Entities.Company;
import com.codinglemonsbackend.Entities.DatabaseSequence;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.Topic;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

@Repository
public class ProblemsRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    //private String[] projectionFields = {"title", "difficulty", "acceptedCount", "submissionCount", "topics", "companies"};

    // public Page<ProblemEntity> findAll(Integer page, Integer size) {
    //     Pageable pageable = PageRequest.of(page, size);
    //     Query query = new Query();
    //     query.with(pageable);
    
    //     return PageableExecutionUtils.getPage(
    //         mongoTemplate.find(query, ProblemEntity.class, "problems"), 
    //         pageable, 
    //         ()->mongoTemplate.count(query.skip(0).limit(0), ProblemEntity.class)
    //     );
    // } 
    
    // public ProblemSet findAll(Integer page, Integer size, Boolean isAdmin) {
    //    return getProblems(null, null, null, page, size, isAdmin);
    // } 
    

    public ProblemsPage getProblems(Difficulty[] difficulties, String[] topicSlugs, String[] companySlugs, int page, int size, Boolean isAdmin) {

        //Query query = new Query();

        List<Criteria> criteriaList = new ArrayList<>();

        if (ArrayUtils.isNotEmpty(difficulties)) {
            //query.addCriteria(Criteria.where("difficulty").in((Object[])difficulties));
            criteriaList.add(Criteria.where("difficulty").in((Object[])difficulties));
        }
        if (ArrayUtils.isNotEmpty(topicSlugs)) {
            //query.addCriteria(Criteria.where("topics").in((Object[])topicSlugs));
            criteriaList.add(Criteria.where("topics").in((Object[])topicSlugs));
        }
        if (ArrayUtils.isNotEmpty(companySlugs)) {
            //query.addCriteria(Criteria.where("companies").in((Object[])companySlugs));
            criteriaList.add(Criteria.where("companies").in((Object[])companySlugs));
        }

        ProjectionOperation projectionOperation = Aggregation.project("title", "difficulty", "acceptedCount", "submissionCount", "topics", "companies");

        if (isAdmin) {
            projectionOperation = projectionOperation.and("status").as("status");
        } else {
           // query.addCriteria(Criteria.where("status").is(ProblemStatus.PUBLISHED));
            criteriaList.add(Criteria.where("status").is(ProblemStatus.PUBLISHED));
        }

        Criteria combinedCriteria = criteriaList.isEmpty() ? new Criteria() : new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));

        MatchOperation matchOperation = Aggregation.match(combinedCriteria);

        LookupOperation lookupCompanies = LookupOperation.newLookup()
                .from(Company.ENTITY_COLLECTION_NAME)
                .localField("companies")
                .foreignField("slug")
                .as("companies");

        LookupOperation lookupTopics = LookupOperation.newLookup()
                .from(Topic.ENTITY_COLLECTION_NAME)
                .localField("topics")
                .foreignField("slug")
                .as("topics");

        AggregationOperation extractNames = ctx -> new Document("$set", new Document()
                .append("topics", new Document("$map", new Document()
                        .append("input", "$topics")
                        .append("as", "t")
                        .append("in", "$$t.name")))
                .append("companies", new Document("$map", new Document()
                        .append("input", "$companies")
                        .append("as", "c")
                        .append("in", "$$c.name"))));

                        
        long total = mongoTemplate.count(new Query(combinedCriteria), ProblemEntity.class);
        
        // query.skip(page * size).limit(size);
        // query.fields().include(fields);
        
        Aggregation aggregation = Aggregation.newAggregation(
            matchOperation,
            lookupCompanies,
            lookupTopics,
            extractNames,
            projectionOperation,
            Aggregation.skip((long) page * size),
            Aggregation.limit(size)
        );
        //List<ProblemEntity> entities = mongoTemplate.find(query, ProblemEntity.class);

        List<ProblemDto> entities = mongoTemplate.aggregate(aggregation, ProblemEntity.ENTITY_COLLECTION_NAME, ProblemDto.class).getMappedResults();
        
        return new ProblemsPage(total, entities);

    }
    
    public Optional<ProblemDto> getProblemById(Integer id) {
        Criteria criteria = Criteria.where("_id").is(id);

        MatchOperation matchOperation = Aggregation.match(criteria);

        LookupOperation lookupOperation1 = LookupOperation.newLookup()
                .from(Company.ENTITY_COLLECTION_NAME)
                .localField("companies")
                .foreignField("slug")
                .as("companies");

        LookupOperation lookupOperation2 = LookupOperation.newLookup()
                .from(Topic.ENTITY_COLLECTION_NAME)
                .localField("topics")
                .foreignField("slug")
                .as("topics");

        AggregationOperation extractNames = ctx -> new Document("$set", new Document()
                .append("topics", new Document("$map", new Document()
                        .append("input", "$topics")
                        .append("as", "t")
                        .append("in", "$$t.name")))
                .append("companies", new Document("$map", new Document()
                        .append("input", "$companies")
                        .append("as", "c")
                        .append("in", "$$c.name"))));

        Aggregation aggregation = Aggregation.newAggregation(
            matchOperation,
            lookupOperation1,
            lookupOperation2,
            extractNames
        );

        return Optional.ofNullable(mongoTemplate.aggregate(aggregation, ProblemEntity.ENTITY_COLLECTION_NAME, ProblemDto.class).getUniqueMappedResult());    
    }

    public List<ProblemDto> getProblemsByIds(List<Integer> ids, Boolean isAdmin) {
        
        Criteria criteria = Criteria.where("_id").in((Object[])ids.toArray());

        ProjectionOperation projectionOperation = Aggregation.project("title", "difficulty", "acceptedCount", "submissionCount", "topics", "companies");

        if (isAdmin) {
            projectionOperation = projectionOperation.and("status").as("status");
        } else {
            criteria.andOperator(Criteria.where("status").is(ProblemStatus.PUBLISHED));
        }
        
        MatchOperation matchOperation = Aggregation.match(criteria);

        LookupOperation lookupCompanies = LookupOperation.newLookup()
                .from(Company.ENTITY_COLLECTION_NAME)
                .localField("companies")
                .foreignField("slug")
                .as("companies");

        LookupOperation lookupTopics = LookupOperation.newLookup()
                .from(Topic.ENTITY_COLLECTION_NAME)
                .localField("topics")
                .foreignField("slug")
                .as("topics");

        AggregationOperation extractNames = ctx -> new Document("$set", new Document()
                .append("topics", new Document("$map", new Document()
                        .append("input", "$topics")
                        .append("as", "t")
                        .append("in", "$$t.name")))
                .append("companies", new Document("$map", new Document()
                        .append("input", "$companies")
                        .append("as", "c")
                        .append("in", "$$c.name"))));

        Aggregation aggregation = Aggregation.newAggregation(
            matchOperation,
            lookupCompanies,
            lookupTopics,
            extractNames,
            projectionOperation
        );
        //List<ProblemEntity> entities = mongoTemplate.find(query, ProblemEntity.class);

        List<ProblemDto> entities = mongoTemplate.aggregate(aggregation, ProblemEntity.ENTITY_COLLECTION_NAME, ProblemDto.class).getMappedResults();

        return entities;
    }

    public void incrementProblemStats(Integer problemId, boolean accepted) {
        Query query = new Query(Criteria.where("_id").is(problemId));
        Update update = new Update().inc("submissionCount", 1);
        if (accepted) update.inc("acceptedCount", 1);
        mongoTemplate.updateFirst(query, update, ProblemEntity.class);
    }

    public Boolean problemExists(Integer problemId) {
        Query query = new Query(Criteria.where("_id").is(problemId));
        return mongoTemplate.exists(query, ProblemEntity.class);
    }

    public long updateProblemProperties(Integer id, Map<String, Object> updatePropertiesMap) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        updatePropertiesMap.entrySet().stream().forEach(e -> update.set(e.getKey(), e.getValue()));
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, ProblemEntity.class);
        return updateResult.getModifiedCount();
    }

    public ProblemEntity addProblem(ProblemEntity problemEntity) {
        ProblemEntity savedEntity = mongoTemplate.save(problemEntity);
        return savedEntity;
    }

    public Map<String, Integer> getPublishedCountByDifficulty() {
        MatchOperation matchPublished = Aggregation.match(Criteria.where("status").is(ProblemStatus.PUBLISHED));
        var groupByDifficulty = Aggregation.group("difficulty").count().as("count");
        var aggregation = Aggregation.newAggregation(matchPublished, groupByDifficulty);
        Map<String, Integer> counts = new HashMap<>();
        for (Difficulty d : Difficulty.values()) counts.put(d.name(), 0);
        mongoTemplate.aggregate(aggregation, ProblemEntity.class, Document.class)
                .getMappedResults()
                .forEach(doc -> counts.put(doc.getString("_id"), doc.getInteger("count")));
        return counts;
    }

    public DeleteResult removeProblemById(Integer id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, ProblemEntity.class);
    }

    @Transactional
    public void removeAllProblems() {
        mongoTemplate.dropCollection(ProblemEntity.ENTITY_COLLECTION_NAME);
        
        Query query = new Query(Criteria.where("_id").is(ProblemEntity.SEQUENCE_NAME));
        mongoTemplate.remove(query, DatabaseSequence.class);
    }


    // public long updateProblem(Integer problemId, Map<String, Object> updatePropertiesMap) {
        
    //     Query query = new Query(Criteria.where("id").is(problemId));

    //     Update update = new Update();

    //     updatePropertiesMap.entrySet().stream().forEach(e -> {
    //         update.set(e.getKey(), e.getValue());
    //     });

    //     UpdateResult updateResult = mongoTemplate.updateFirst(query, update, ProblemEntity.class);

    //     return updateResult.getModifiedCount();
    // }

    public Optional<ProblemDto> getProblemSummaryById(Integer id) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("_id").is(id)),
            LookupOperation.newLookup()
                .from(Topic.ENTITY_COLLECTION_NAME)
                .localField("topics")
                .foreignField("slug")
                .as("topics"),
            ctx -> new Document("$set", new Document("topics", new Document("$map", new Document()
                .append("input", "$topics")
                .append("as", "t")
                .append("in", "$$t.name")))),
            Aggregation.project("title", "difficulty", "topics")
        );
        return Optional.ofNullable(
            mongoTemplate.aggregate(aggregation, ProblemEntity.ENTITY_COLLECTION_NAME, ProblemDto.class)
                         .getUniqueMappedResult()
        );
    }

    public Optional<Integer> getRandomPublishedProblemId(List<Integer> excludeIds) {
        Criteria criteria = Criteria.where("status").is(ProblemStatus.PUBLISHED);
        if (excludeIds != null && !excludeIds.isEmpty()) {
            criteria = criteria.and("_id").nin(excludeIds);
        }

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(criteria),
            Aggregation.sample(1)
        );

        ProblemEntity result = mongoTemplate
            .aggregate(aggregation, ProblemEntity.ENTITY_COLLECTION_NAME, ProblemEntity.class)
            .getUniqueMappedResult();

        return Optional.ofNullable(result).map(ProblemEntity::getId);
    }

    public boolean isPublishedProblem(Integer problemId) {
        Query query = new Query(Criteria.where("_id").is(problemId).and("status").is(ProblemStatus.PUBLISHED));
        return mongoTemplate.exists(query, ProblemEntity.class);
    }

    public Optional<ProblemEntity> getLasEntity(){

        Query query = new Query().with(Sort.by(Sort.Order.desc("id"))).limit(1);

        return Optional.ofNullable(mongoTemplate.findOne(query, ProblemEntity.class));
    }
}
