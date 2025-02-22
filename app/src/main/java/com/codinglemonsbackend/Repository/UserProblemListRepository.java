package com.codinglemonsbackend.Repository;

import java.lang.reflect.Field;
import java.lang.NoSuchFieldException;
import java.lang.IllegalAccessException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.VariableOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.ProblemListEntity;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.mongodb.client.result.DeleteResult;

@Repository
public class UserProblemListRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    public Optional<ProblemListDto> getUserProblemListDetails(String creator, String name){

        ProblemListDto userProblemList = null;

        UserEntity signedInUser= (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Criteria criteria = new Criteria().andOperator(
            Criteria.where("creator").is(creator),
            Criteria.where("name").is(name)
        );

        Query query = new Query(criteria);

        ProblemListEntity problemListEntity = mongoTemplate.findOne(query, ProblemListEntity.class);

        if (problemListEntity != null) {

            if (!creator.equals(signedInUser.getUsername())) {
                criteria = criteria.and("isPublic").is(true);
            }
            
            MatchOperation matchOperation = Aggregation.match(criteria);
    
            LookupOperation lookupOperation = LookupOperation.newLookup()
                    .from(ProblemEntity.ENTITY_COLLECTION_NAME)                            
                    .localField("problemIds")                    
                    .foreignField("_id")                          
                    .as("problemsData");                           
    
    
            ProjectionOperation projectFields = Aggregation.project()
                .and( 
                    VariableOperators.Map.itemsOf("problemsData")
                        .as("e")
                        .andApply(ctx -> new Document("_id", "$$e._id")
                                            .append("title", "$$e.title")
                                            .append("difficulty", "$$e.difficulty")))
                .as("problemsData")
                .andInclude("name", "description", "isPublic", "isPinned");
    
    
            Aggregation aggregation = Aggregation.newAggregation(
                    matchOperation,
                    lookupOperation,
                    projectFields
            );
    
            List<ProblemListDto> result = mongoTemplate.aggregate(aggregation, ProblemListEntity.ENTITY_COLLECTION_NAME, ProblemListDto.class).getMappedResults();
    
            if (result.isEmpty()) {
                throw new AccessDeniedException("The list you are trying to view is private!!");
            } 
            
            userProblemList = result.get(0);
        }

        return Optional.ofNullable(userProblemList);
    }

    public Optional<ProblemListEntity> getUserProblemListEntityById(ObjectId id) {
        ProblemListEntity entity =  mongoTemplate.findById(id, ProblemListEntity.class);
        return Optional.ofNullable(entity);
    }

    public List<ProblemListEntity> getAllProblemListsOfUser(String username){

        UserEntity signedInUser= (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Criteria criteria = Criteria.where("creator").is(username);

        if (!username.equals(signedInUser.getUsername())) {
            criteria = new Criteria().andOperator(
                criteria,
                Criteria.where("isPublic").is(true) 
            );
        }

        Query query = new Query(criteria);

        List<ProblemListEntity> problemListEntities = mongoTemplate.find(query, ProblemListEntity.class);

        return problemListEntities;      
    }

    public void saveProblemList(ProblemListEntity problemList) throws DuplicateResourceException{
        try{
            mongoTemplate.save(problemList);
        } catch(DuplicateKeyException e) {
            throw new DuplicateResourceException("Problem list with same name already exists");
        } catch(Exception e) {
            throw e;
        }
    }

    public Map<String, Object> updateProblemList(ObjectId listId, Map<String, Object> fieldsToUpdate, ProblemListEntity originalEntity) {
        Update update = new Update();
        fieldsToUpdate.entrySet().stream().forEach(e -> update.set(e.getKey(), e.getValue()));
        ProblemListEntity updatedEntity = mongoTemplate.findAndModify(
                                        new Query(Criteria.where("_id").is(listId)), 
                                        update, 
                                        FindAndModifyOptions.options().returnNew(true),
                                        ProblemListEntity.class);

        // Optional<ProblemListEntity> updatedEntity = getUserProblemListEntityById(listId);

        Map<String, Object> updatedFields = new HashMap<>();

        for (Map.Entry<String, Object> entry : fieldsToUpdate.entrySet()) {
            String key = entry.getKey();
            try {
                Field field = ProblemListEntity.class.getDeclaredField(key);
                field.setAccessible(true);
                Object originalValue = field.get(originalEntity);
                Object updatedValue = field.get(updatedEntity);
                if (!Objects.equals(originalValue, updatedValue)) {
                    updatedFields.put(key, updatedValue);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // Log the exception or handle it as needed
                e.printStackTrace();
            }
        }

        return updatedFields;
    }

    public int addProblemToProblemList(String listId, Set<Integer> newProblemIds){

        ObjectId objectId;

        try {
            objectId = new ObjectId(listId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid problem list id");
        }

        UserEntity signedInUser= (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        //Query query = new Query(Criteria.where("_id").is(objectId).and("creator").is(signedInUser.getUsername()));
   
        Query query = new Query(Criteria.where("_id").is(objectId));
   
        ProblemListEntity listEntity = mongoTemplate.findOne(query, ProblemListEntity.class);
        
        if (listEntity == null) {
            throw new NoSuchElementException(String.format("The requested list id %s not found!!", listId));
        }
        
        if (!listEntity.getCreator().equals(signedInUser.getUsername())) {
            throw new AccessDeniedException("You are not allowed to update list this list.");
        }

        if (listEntity.getProblemIds() != null) {
            newProblemIds.removeAll(listEntity.getProblemIds());
        }
        
        if (newProblemIds.size() > 0) {
            Update update = new Update().addToSet("problemIds").each(newProblemIds.toArray());
            mongoTemplate.updateFirst(query, update, ProblemListEntity.class);
        }

        return newProblemIds.size();
    }

    public Boolean deleteProblemList(String id){
        Query query = new Query(Criteria.where("_id").is(id));
        DeleteResult result = mongoTemplate.remove(query, ProblemListEntity.class);
        return result.getDeletedCount()>0;
    }
}
