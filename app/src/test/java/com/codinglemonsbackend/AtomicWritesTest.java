package com.codinglemonsbackend;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.BsonValue;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;

import com.codinglemonsbackend.Entities.DatabaseSequence;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.ProblemOfTheDayEntity;
import com.codinglemonsbackend.Entities.UserProfileEntity;
import com.codinglemonsbackend.Entities.UserWorkExperience;
import com.codinglemonsbackend.Events.ProblemEntityEventListener;
import com.codinglemonsbackend.Repository.ProblemOfTheDayRepository;
import com.codinglemonsbackend.Repository.ProblemsRepository;
import com.codinglemonsbackend.Repository.SequenceGeneratorRepository;
import com.codinglemonsbackend.Repository.UserProfileRepository;
import com.codinglemonsbackend.Service.SequenceService;
import com.mongodb.client.result.UpdateResult;

/**
 * Four writes that each used to be a read-modify-write or a write in the wrong order, and one read
 * that asked for far more than it used. None of them can be caught by asserting on the end state,
 * because every one produces the right end state when nothing interleaves and nothing fails - what
 * has to be asserted is the shape of the call that goes to Mongo.
 */
class AtomicWritesTest {

    // ---- #9: a forward pointer to a document that may never exist -----------------------------

    @Test
    void forwardPointerIsNotWrittenUntilTheNewProblemIsSaved() {
        SequenceService sequenceService = mock(SequenceService.class);
        ProblemsRepository problemsRepository = mock(ProblemsRepository.class);
        when(sequenceService.getNextSequence(anyString())).thenReturn(10);

        ProblemEntity last = new ProblemEntity();
        last.setId(9);
        when(problemsRepository.getLasEntity()).thenReturn(Optional.of(last));

        ProblemEntityEventListener listener = new ProblemEntityEventListener();
        ReflectionTestUtils.setField(listener, "sequenceService", sequenceService);
        ReflectionTestUtils.setField(listener, "problemsRepository", problemsRepository);

        ProblemEntity newProblem = new ProblemEntity();
        listener.onBeforeConvert(new BeforeConvertEvent<>(newProblem, "problems"));

        assertEquals(9, newProblem.getPreviousProblemId(), "the back pointer still belongs here");
        // The insert has not happened yet. Touching problem 9 now is what left it pointing at a
        // problem 10 that a failed insert never created.
        verify(problemsRepository, never()).updateProblemProperties(anyInt(), anyMap());

        listener.onAfterSave(new AfterSaveEvent<>(newProblem, new Document(), "problems"));
        verify(problemsRepository).updateProblemProperties(9, Map.of("nextProblemId", 10));
    }

    // ---- #11b: the sequence counter's first use -----------------------------------------------

    @Test
    void sequenceCounterIsCreatedByTheIncrementItself() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(DatabaseSequence.class), anyString()))
                .thenReturn(new DatabaseSequence("problem_sequence", 1));

        SequenceService service = new SequenceService(new SequenceGeneratorRepository(mongoTemplate));

        assertEquals(1, service.getNextSequence("problem_sequence"));

        ArgumentCaptor<FindAndModifyOptions> options =
                ArgumentCaptor.forClass(FindAndModifyOptions.class);
        verify(mongoTemplate).findAndModify(any(Query.class), any(Update.class), options.capture(),
                eq(DatabaseSequence.class), anyString());
        assertTrue(options.getValue().isUpsert(),
                "without upsert the first call returns null and the caller has to create the "
                        + "counter, which two callers can do at once and both get 1");
    }

    // ---- #11a: replacing one work experience entry --------------------------------------------

    @Test
    void newWorkExperienceIsAppendedInOneGuardedUpdate() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        // Matched: the company was not listed, so the guarded push appended it.
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(UserProfileEntity.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        UserProfileRepository repository = repositoryWith(mongoTemplate);
        repository.upsertWorkExperience("ada", experienceAt("acme"));

        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate, times(1))
                .updateFirst(any(Query.class), update.capture(), eq(UserProfileEntity.class));

        Document sent = update.getValue().getUpdateObject();
        assertTrue(sent.containsKey("$push"), "the entry should go in on the first update");
        assertFalse(sent.containsKey("$pull"),
                "a pull that is followed by a separate push can fail in between and lose the entry");
    }

    @Test
    void existingWorkExperienceIsOverwrittenInPlace() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        // Matched nothing: the guard found the company already listed, so nothing was appended.
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(UserProfileEntity.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, (BsonValue) null));

        UserProfileRepository repository = repositoryWith(mongoTemplate);
        repository.upsertWorkExperience("ada", experienceAt("acme"));

        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate, times(2))
                .updateFirst(any(Query.class), update.capture(), eq(UserProfileEntity.class));

        Document replace = update.getAllValues().get(1).getUpdateObject();
        assertTrue(replace.get("$set", Document.class).containsKey("workExperience.$"),
                "the matched element should be set in place rather than removed and re-added");
    }

    // ---- #11c: the problem-of-the-day history ------------------------------------------------

    @Test
    void potdHistoryIsPrependedByMongoRatherThanRewritten() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ProblemOfTheDayRepository repository = new ProblemOfTheDayRepository();
        ReflectionTestUtils.setField(repository, "mongoTemplate", mongoTemplate);

        repository.saveProblemOfTheDay(12, 9, 30);

        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(any(Query.class), update.capture(), eq(ProblemOfTheDayEntity.class));
        Document sent = update.getValue().getUpdateObject();

        assertEquals(12, sent.get("$set", Document.class).get("problemId"));
        assertFalse(sent.get("$set", Document.class).containsKey("history"),
                "setting the whole array replays a copy read earlier and drops a concurrent write");

        // Spring Data holds the $push modifiers in a Modifiers holder and only renders them to BSON
        // when the update is mapped, so they have to be read back through it rather than as a Document.
        Map<String, Object> history = modifiersOf(sent.get("$push", Document.class).get("history"));
        assertArrayEquals(new Object[] { 9 }, (Object[]) history.get("$each"),
                "the outgoing problem goes onto the history");
        assertEquals(0, history.get("$position"), "onto the front of it");
        assertEquals(30, history.get("$slice"), "trimmed by Mongo, against what is actually stored");
    }

    @Test
    void potdHistoryIsUntouchedWhenThereIsNoOutgoingProblem() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ProblemOfTheDayRepository repository = new ProblemOfTheDayRepository();
        ReflectionTestUtils.setField(repository, "mongoTemplate", mongoTemplate);

        repository.saveProblemOfTheDay(12, null, 30);

        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(any(Query.class), update.capture(), eq(ProblemOfTheDayEntity.class));
        assertFalse(update.getValue().getUpdateObject().containsKey("$push"),
                "the very first POTD has no predecessor to record");
    }

    // ---- the same idea for a read: ask for what is used ---------------------------------------

    @Test
    void aRandomProblemComesBackAsAnIdAndNothingElse() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.aggregate(any(Aggregation.class), anyString(), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(), new Document()));

        ProblemsRepository repository = new ProblemsRepository();
        ReflectionTestUtils.setField(repository, "mongoTemplate", mongoTemplate);
        repository.getRandomPublishedProblemId(List.of(3));

        ArgumentCaptor<Aggregation> pipeline = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongoTemplate).aggregate(pipeline.capture(), anyString(), eq(Document.class));

        List<Document> stages = pipeline.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        Document projection = stages.get(stages.size() - 1).get("$project", Document.class);
        assertEquals(Set.of("_id"), projection.keySet(),
                "the whole problem used to come over the wire - description, examples and a code "
                        + "snippet per language - so that one integer could be read off it");
    }

    private static Map<String, Object> modifiersOf(Object pushValue) {
        return ((Update.Modifiers) pushValue).getModifiers().stream()
                .collect(Collectors.toMap(Update.Modifier::getKey, Update.Modifier::getValue));
    }

    private static UserProfileRepository repositoryWith(MongoTemplate mongoTemplate) {
        UserProfileRepository repository = new UserProfileRepository();
        ReflectionTestUtils.setField(repository, "mongoTemplate", mongoTemplate);
        return repository;
    }

    private static UserWorkExperience experienceAt(String companySlug) {
        return new UserWorkExperience("Acme", companySlug, "Engineer", 2020, 2024);
    }
}
