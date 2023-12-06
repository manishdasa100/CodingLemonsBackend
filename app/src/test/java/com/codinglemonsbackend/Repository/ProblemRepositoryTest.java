package com.codinglemonsbackend.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Payloads.ProblemSetResponse;

// @DataMongoTest
@SpringBootTest
// @EnableAutoConfiguration
@TestInstance(Lifecycle.PER_CLASS)
public class ProblemRepositoryTest {

    /* Start the local mongoDB server before testing */  

    @Autowired
    private MongoTemplate mongoTemplate;

    private ProblemsRepository underTest;

    private List<ProblemEntity> dummyProblemEntityList;

    @BeforeAll
    void setUp() {
        dummyProblemEntityList = List.of(ProblemEntity.builder().title("Title1").description("Descr1").testCases(new ArrayList<>()).testCaseOutputs(new ArrayList<>()).difficulty(Difficulty.EASY).driverCodes(new HashMap<>()).optimalSolutions(new HashMap<>()).topics(List.of("Binary Search", "Arrays")).acceptance(80).build(), 
                                    ProblemEntity.builder().title("Title2").description("Descr2").testCases(new ArrayList<>()).testCaseOutputs(new ArrayList<>()).difficulty(Difficulty.MEDIUM).driverCodes(new HashMap<>()).optimalSolutions(new HashMap<>()).topics(List.of("Sorting", "Linked List")).acceptance(50).build(), 
                                    ProblemEntity.builder().title("Title3").description("Descr3").testCases(new ArrayList<>()).testCaseOutputs(new ArrayList<>()).difficulty(Difficulty.HARD).driverCodes(new HashMap<>()).optimalSolutions(new HashMap<>()).topics(List.of("Stack", "Dynamic programming")).acceptance(40).build(),
                                    ProblemEntity.builder().title("Title4").description("Descr4").testCases(new ArrayList<>()).testCaseOutputs(new ArrayList<>()).difficulty(Difficulty.EASY).driverCodes(new HashMap<>()).optimalSolutions(new HashMap<>()).topics(List.of("BFS")).acceptance(80).build(),
                                    ProblemEntity.builder().title("Title5").description("Descr5").testCases(new ArrayList<>()).testCaseOutputs(new ArrayList<>()).difficulty(Difficulty.MEDIUM).driverCodes(new HashMap<>()).optimalSolutions(new HashMap<>()).topics(List.of("Arrays", "Two pointer")).acceptance(60).build()
                                );
        underTest = new ProblemsRepository(mongoTemplate);

    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection("problems");
        mongoTemplate.dropCollection("database_sequences");
    }

    @Test
    public void testMongoTemplate() {
        assertNotNull(mongoTemplate);
        assertThat(mongoTemplate.getDb().getName()).isEqualTo("test");
    }

    // Test getProblemById() method for success
    @Test
    public void testGetProblemById_success() {

        mongoTemplate.save(dummyProblemEntityList.get(0), "problems");

        ProblemEntity problemEntity = underTest.getProblemById(1).get();

        assertThat(problemEntity).extracting("title").isEqualTo("Title1");
    }

    //Test for getProblemById() method for failure
    @Test
    public void testGetProblemById_failure() {
        
        Optional<ProblemEntity> problemEntity = underTest.getProblemById(1);

        assertThat(problemEntity.isPresent()).isFalse();
    }

    @Test
    public void testFindAllProblems(){
        
        dummyProblemEntityList.stream().forEach(problemEntity -> mongoTemplate.save(problemEntity, "problems"));

        int page = 1, size = 2;

        ProblemSetResponse response = underTest.findAll(page, size);

        assertNotNull(response);

        assertThat(response).extracting("total").isEqualTo((long)dummyProblemEntityList.size());

        assertThat(response).extracting("problems").asList().hasSize(size);
    }

    @Test
    public void testGetFilteredProblems_filter_by_difficulty_only() {

        int page = 0, size = 1;

        for (ProblemEntity dummyProblemEntity : dummyProblemEntityList) {
            mongoTemplate.save(dummyProblemEntity); 
        }

        ProblemSetResponse response = underTest.getFilteredProblems(List.of(Difficulty.EASY, Difficulty.MEDIUM), null, page, size);

        assertThat(response.getProblems()).asList().hasSize(1);

        assertThat(response.getTotal()).isEqualTo(4);
    }

    @Test
    public void testGetFilteredProblems_filter_by_topics_only() {

        int page = 0, size = 2;

        for (ProblemEntity dummyProblemEntity : dummyProblemEntityList) {
            mongoTemplate.save(dummyProblemEntity); 
        }

        ProblemSetResponse response = underTest.getFilteredProblems(null, new String[]{"Arrays"}, page, size);

        assertThat(response.getProblems()).asList().hasSize(2);

        assertThat(response.getProblems().get(0).getDifficulty()).isEqualTo(Difficulty.EASY);

        assertThat(response.getProblems().get(1).getDifficulty()).isEqualTo(Difficulty.MEDIUM);
    }


    @Test
    public void testGetFilteredProblems_filter_by_both_topics_and_difficulty() {

        int page = 0, size = 2;

        for (ProblemEntity dummyProblemEntity : dummyProblemEntityList) {
            mongoTemplate.save(dummyProblemEntity); 
        }

        ProblemSetResponse response = underTest.getFilteredProblems(List.of(Difficulty.EASY, Difficulty.MEDIUM), new String[]{"Arrays"}, page, size);

        assertThat(response.getProblems()).asList().hasSize(2);

        assertThat(response.getProblems().get(0).getTitle()).isEqualTo("Title1");

        assertThat(response.getTotal()).isEqualTo(2);

    }



    @Test
    public void testGetFilteredProblems_invalid_arguments() {

        int page = 0, size = 2;

        for (ProblemEntity dummyProblemEntity : dummyProblemEntityList) {
            mongoTemplate.save(dummyProblemEntity); 
        }

        assertThatThrownBy(() -> underTest.getFilteredProblems(null, null, page, size))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Atleast one of the filter criteria must be provided. Found both difficulty and topics array to be null or empty");

    }


    @Test
    public void testAddProblem(){

        ProblemEntity firstEntityToSave = dummyProblemEntityList.get(0);

        ProblemEntity secondEntityToSave = dummyProblemEntityList.get(1);

        underTest.addProblem(firstEntityToSave);

        underTest.addProblem(secondEntityToSave);

        List<ProblemEntity> allProblems = mongoTemplate.findAll(ProblemEntity.class, "problems");

        assertThat(allProblems).hasSize(2);

        assertThat(allProblems.get(0)).hasFieldOrPropertyWithValue("problemId", 1);
        assertThat(allProblems.get(0)).hasFieldOrPropertyWithValue("title", firstEntityToSave.getTitle());

        assertThat(allProblems.get(1)).hasFieldOrPropertyWithValue("problemId", 2);
        assertThat(allProblems.get(1)).hasFieldOrPropertyWithValue("title", secondEntityToSave.getTitle());

    }

    @Test
    public void testRemoveAllProblems() {
        
        mongoTemplate.createCollection("problems");
        mongoTemplate.createCollection("database_sequences");

        assertThat(mongoTemplate.getCollectionNames()).contains("problems", "database_sequences");

        underTest.removeAllProblems();

        assertThat(mongoTemplate.getCollectionNames()).isEmpty();
    }

}
