package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.mongodb.client.result.DeleteResult;

@Repository
public class ProblemsRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ModelMapper modelMapper;

    private String[] projectionFields = {"title", "difficulty", "acceptedCount", "submissionCount"};

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
    
    public Optional<ProblemEntity> getProblemById(Integer id) {
        Query query = new Query(Criteria.where("id").is(id));
        ProblemEntity problemEntity = mongoTemplate.findOne(query, ProblemEntity.class, "problems");
        return Optional.ofNullable(problemEntity);
    }

    public List<ProblemEntity> getProblemsByIds(Integer[] ids) {

        Query query = new Query(Criteria.where("id").in((Object[])ids));
        query.fields().include(projectionFields);
        List<ProblemEntity> problemEntities = mongoTemplate.find(query, ProblemEntity.class);
        return problemEntities;
    }

    public void addProblem(ProblemEntity problemEntity) {
        mongoTemplate.save(problemEntity);
    }


    public DeleteResult removeProblemById(Integer id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.remove(query, ProblemEntity.class);
    }

    public void removeAllProblems() {
        mongoTemplate.dropCollection("problems");
        mongoTemplate.dropCollection("database_sequences");
    }


    public void updateProblem(Integer problemId, Map<String, Object> updatePropertiesMap) {
        
        Query query = new Query(Criteria.where("id").is(problemId));

        Update update = new Update();

        updatePropertiesMap.entrySet().stream().forEach(e -> {
            update.set(e.getKey(), e.getValue());
        });

        mongoTemplate.updateFirst(query, update, ProblemEntity.class);
    }

    public Optional<ProblemEntity> getLasEntity(){

        Query query = new Query().with(Sort.by(Sort.Order.desc("id"))).limit(1);

        return Optional.ofNullable(mongoTemplate.findOne(query, ProblemEntity.class));
    }
}
