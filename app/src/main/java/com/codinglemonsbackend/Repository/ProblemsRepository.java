package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.bson.Document;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.VariableOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.codinglemonsbackend.Dto.Difficulty;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemSet;
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

    @Autowired
    private ModelMapper modelMapper;

    private String[] projectionFields = {"title", "difficulty", "acceptedCount", "submissionCount", "likes"};

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
    public ProblemSet findAll(Integer page, Integer size) {
       
       System.out.println("CACHE MISS");
       return getProblems(null, null, null, page, size);
    } 
    

    public ProblemSet getProblems(Difficulty[] difficulties, String[] topicSlugs, String[] companySlugs, int page, int size) {

        List<ProblemEntity> filteredProblemSet;

        Query query = new Query(); 

        if (ArrayUtils.isNotEmpty(difficulties)) {
            query.addCriteria(Criteria.where("difficulty").in((Object[])difficulties));
        }
        if (ArrayUtils.isNotEmpty(topicSlugs)) {
            query.addCriteria(Criteria.where("topics.slug").in((Object[])topicSlugs));
        }
        if (ArrayUtils.isNotEmpty(companySlugs)) {
            query.addCriteria(Criteria.where("companies.slug").in((Object[])companySlugs));
        }

        query.skip(page*size).limit(size);
      
        query.fields().include(projectionFields);

        filteredProblemSet = mongoTemplate.find(query, ProblemEntity.class);

        List<ProblemDto> problemDtos = filteredProblemSet.stream().map(prob->modelMapper.map(prob, ProblemDto.class))
                                                                    .collect(Collectors.toList());

        return new ProblemSet(
            mongoTemplate.count(query.skip(0).limit(0), ProblemEntity.class),
            problemDtos
        );

    }
    
    public Optional<ProblemDto> getProblemById(Integer id) {
        Criteria criteria = Criteria.where("_id").is(id);

        MatchOperation matchOperation = Aggregation.match(criteria);

        LookupOperation lookupOperation1 = LookupOperation.newLookup()
                .from(Company.ENTITY_COLLECTION_NAME)                            
                .localField("companySlugs")                    
                .foreignField("slug")                          
                .as("companies"); 
    
        LookupOperation lookupOperation2 = LookupOperation.newLookup()
                .from(Topic.ENTITY_COLLECTION_NAME)                            
                .localField("topicSlugs")                    
                .foreignField("slug")                          
                .as("topics"); 

        Aggregation aggregation = Aggregation.newAggregation(
            matchOperation,
            lookupOperation1,
            lookupOperation2
        );

        return Optional.ofNullable(mongoTemplate.aggregate(aggregation, ProblemEntity.ENTITY_COLLECTION_NAME, ProblemDto.class).getUniqueMappedResult());    
    }

    public List<ProblemEntity> getProblemsByIds(List<Integer> ids) {
        Query query = new Query(Criteria.where("id").in((Object[])ids.toArray()));
        query.fields().include(projectionFields);
        List<ProblemEntity> problemEntities = mongoTemplate.find(query, ProblemEntity.class);
        return problemEntities;
    }

    public Boolean problemExists(Integer problemId) {
        Query query = new Query(Criteria.where("id").is(problemId));
        return mongoTemplate.exists(query, ProblemEntity.class);
    }

    public long updateProblemProperties(Integer id, Map<String, Object> updatePropertiesMap) {
        Query query = new Query(Criteria.where("id").is(id));
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
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.remove(query, ProblemEntity.class);
    }

    @Transactional
    public void removeAllProblems() {
        mongoTemplate.dropCollection(ProblemEntity.ENTITY_COLLECTION_NAME);
        
        Query query = new Query(Criteria.where("id").is(ProblemEntity.SEQUENCE_NAME));
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
