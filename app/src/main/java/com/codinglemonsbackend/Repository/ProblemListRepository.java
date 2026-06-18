package com.codinglemonsbackend.Repository;

import java.lang.reflect.Field;
import java.lang.NoSuchFieldException;
import java.lang.IllegalAccessException;
import java.util.ArrayList;
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
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
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
import com.mongodb.client.result.UpdateResult;

@Repository
public class ProblemListRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void saveProblemList(ProblemListEntity problemList) throws DuplicateResourceException{
        try{
            mongoTemplate.save(problemList);
        } catch(DuplicateKeyException e) {
            throw new DuplicateResourceException("Problem list with same name already exists");
        } catch(Exception e) {
            throw e;
        }
    }

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
                .and(ArrayOperators.Size.lengthOfArray("problemsData")).as("totalProblems")
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

    public Optional<ProblemListEntity> getUserProblemListEntityById(String id) {
        ProblemListEntity entity =  mongoTemplate.findById(id, ProblemListEntity.class);
        return Optional.ofNullable(entity);
    }

    public List<ProblemListEntity> getAllProblemListsOfUser(String username){

        UserEntity signedInUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

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

    public void updateProblemList(String listId, Map<String, Object> fieldsToUpdate) {
        ProblemListEntity originalEntity = this.getUserProblemListEntityById(listId).orElseThrow(
            () -> new NoSuchElementException(String.format("The requested list id %s not found!!", listId))
        );

        if (!isUserAuthorizedToModifyList(originalEntity)) {
            throw new AccessDeniedException("You are not authorized to update this list");
        }

        Update update = new Update();
        fieldsToUpdate.entrySet().stream().forEach(
            e -> update.set(e.getKey(), e.getValue())
        );
        UpdateResult updateResult = mongoTemplate.updateFirst(
                                        new Query(Criteria.where("_id").is(listId)), 
                                        update,
                                        ProblemListEntity.class);

        
    }

    public void addProblemToProblemList(String listId, Set<Integer> newProblemIds) {   
        ProblemListEntity listEntity = mongoTemplate.findById(listId, ProblemListEntity.class);
        
        if (listEntity == null) {
            throw new NoSuchElementException(String.format("The requested list id %s not found!!", listId));
        }

        if (!isUserAuthorizedToModifyList(listEntity)) {
            throw new AccessDeniedException("You are not authorized to modify this list.");
        }

        if (listEntity.getProblemIds() != null) {
            newProblemIds.removeAll(listEntity.getProblemIds());
        }
        
        if (!newProblemIds.isEmpty()) {
            Update update = new Update().addToSet("problemIds").each(newProblemIds.toArray());
            mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(listId)), 
                update, 
                ProblemListEntity.class);
        }

    }

    public void removeProblemFromProblemList(String listId, Set<Integer> problemIdsToRemove) {
        Query query = new Query(Criteria.where("_id").is(listId));
        ProblemListEntity listEntity = mongoTemplate.findById(listId, ProblemListEntity.class);

        if (listEntity == null) {
            throw new NoSuchElementException(String.format("The requested list id %s not found!!", listId));
        }

        if (!isUserAuthorizedToModifyList(listEntity)) {
            throw new AccessDeniedException("You are not authorized to modify this list.");
        } 

        Update update = new Update().pullAll("problemIds", problemIdsToRemove.toArray());
        mongoTemplate.updateFirst(query, update, ProblemListEntity.class);
    }

    public Boolean deleteProblemList(String id){
        Query query = new Query(Criteria.where("_id").is(id));
        DeleteResult result = mongoTemplate.remove(query, ProblemListEntity.class);
        return result.getDeletedCount()>0;
    }

    private boolean isUserAuthorizedToModifyList(ProblemListEntity listEntity) {
        UserEntity signedInUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = signedInUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("SUPERADMIN"));
        boolean isPublicList = "global".equals(listEntity.getCreator());

        if (isPublicList) {
            return isAdmin;
        } else {
            return listEntity.getCreator().equals(signedInUser.getUsername());
        }
    }
}
