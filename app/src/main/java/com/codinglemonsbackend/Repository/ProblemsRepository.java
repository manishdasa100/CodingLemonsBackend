package com.codinglemonsbackend.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Payloads.ProblemSetResponse;

@Repository
public class ProblemsRepository {

    private MongoTemplate mongoTemplate;

    public ProblemsRepository(@Autowired MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

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
    public ProblemSetResponse findAll(Integer page, Integer size) {
        Query query = new Query().skip(page*size).limit(size);

        List<ProblemEntity> problemSet = mongoTemplate.find(query, ProblemEntity.class, "problems");

        List<ProblemDto> problemDtos = problemSet.stream().map(prob->ProblemDto.builder().title(prob.getTitle()).description(prob.getDescription()).topics(prob.getTopics()).difficulty(prob.getDifficulty()).build()).collect(Collectors.toList());
         
        return new ProblemSetResponse(
            mongoTemplate.count(query.skip(0).limit(0), ProblemEntity.class),
            problemDtos
        );
    } 
    

    public ProblemSetResponse getFilteredProblems(List<Difficulty> difficulties, String[] topics, int page, int size) {

        if(difficulties == null && topics == null) throw new IllegalArgumentException("Atleast one of the filter criteria must be provided. Found both difficulty and topics array to be null or empty");

        List<ProblemEntity> filteredProblemSet;

        Query query; 

        if (topics == null) {  // filter problems based on difficulty only
            System.out.println("ello"); 
            query = new Query(Criteria.where("difficulty").in(difficulties)).skip(page*size).limit(size);
        } else if (difficulties == null) {  // filter problems based on topics array only
            System.out.println("mello"); 
            query = new Query(Criteria.where("topics").all((Object[])topics)).skip(page*size).limit(size);
        } else {  // filter problems based on both difficulty and topics array
            System.out.println("Hello"); 
            query = new Query(
                new Criteria().andOperator(
                    Criteria.where("difficulty").in(difficulties), 
                    Criteria.where("topics").all(List.of(topics))
                )
            ).skip(page*size).limit(size);
        }
      
        filteredProblemSet = mongoTemplate.find(query, ProblemEntity.class);

        List<ProblemDto> problemDtos = filteredProblemSet.stream().map(prob->ProblemDto.builder()
                                                                                        .title(prob.getTitle())
                                                                                        .description(prob.getDescription())
                                                                                        .constraints(prob.getConstraints())
                                                                                        .examples(prob.getExamples())
                                                                                        .topics(prob.getTopics())
                                                                                        .difficulty(prob.getDifficulty())
                                                                                        .build())
                                                                    .collect(Collectors.toList());

        return new ProblemSetResponse(
            mongoTemplate.count(query.skip(0).limit(0), ProblemEntity.class),
            problemDtos
        );

    }

    
    public void addProblem(ProblemEntity problemEntity) {
        mongoTemplate.save(problemEntity);
    }


    public Optional<ProblemEntity> getProblemById(Integer id) {
        Query query = new Query(Criteria.where("problemId").is(id));
        ProblemEntity problemEntity = mongoTemplate.findOne(query, ProblemEntity.class, "problems");
        return Optional.ofNullable(problemEntity);
    }


    public void removeAllProblems() {
        mongoTemplate.dropCollection("problems");
        mongoTemplate.dropCollection("database_sequences");
    }

    
}
