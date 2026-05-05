package com.codinglemonsbackend.Repository;

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

    private String[] projectionFields = {"title", "difficulty", "acceptedCount", "submissionCount", "topics", "likes"};

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

        Query query = new Query();

        if (ArrayUtils.isNotEmpty(difficulties)) {
            query.addCriteria(Criteria.where("difficulty").in((Object[])difficulties));
        }
        if (ArrayUtils.isNotEmpty(topicSlugs)) {
            query.addCriteria(Criteria.where("topics").in((Object[])topicSlugs));
        }
        if (ArrayUtils.isNotEmpty(companySlugs)) {
            query.addCriteria(Criteria.where("companies").in((Object[])companySlugs));
        }

        String[] fields = isAdmin
                ? ArrayUtils.add(projectionFields, "status")
                : projectionFields;

        if (!isAdmin) {
            query.addCriteria(Criteria.where("status").is(ProblemStatus.PUBLISHED));
        }

        long total = mongoTemplate.count(query, ProblemEntity.class);

        query.skip(page * size).limit(size);
        query.fields().include(fields);

        List<ProblemEntity> entities = mongoTemplate.find(query, ProblemEntity.class);

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

    public List<ProblemEntity> getProblemsByIds(List<Integer> ids, Boolean isAdmin) {
        Query query = new Query(Criteria.where("_id").in((Object[])ids.toArray()));
        String[] fields = isAdmin
                ? ArrayUtils.add(projectionFields, "status")
                : projectionFields;

        if (!isAdmin) {
            query.addCriteria(Criteria.where("status").is(ProblemStatus.PUBLISHED));
        }
        query.fields().include(fields);
        List<ProblemEntity> problemEntities = mongoTemplate.find(query, ProblemEntity.class);
        return problemEntities;
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

    public Optional<ProblemEntity> getLasEntity(){

        Query query = new Query().with(Sort.by(Sort.Order.desc("id"))).limit(1);

        return Optional.ofNullable(mongoTemplate.findOne(query, ProblemEntity.class));
    }
}
