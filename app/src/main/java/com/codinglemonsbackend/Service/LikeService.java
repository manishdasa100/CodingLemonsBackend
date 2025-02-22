package com.codinglemonsbackend.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Config.RabbitMQConfig;
import com.codinglemonsbackend.Dto.LikeEvent;
import com.codinglemonsbackend.Entities.UserLike;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Payloads.LikesData;
import com.codinglemonsbackend.Repository.LikeRepository;

@Service
public class LikeService {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /*
     * This function should return the total likes and whether the current user has liked the problem.
        * 1. Check if the problem id is in cache, if not then bring the total like count into cache.
        * 2. Get the user like status for the problem.
        * 3. Return the total likes and user like status. 
     */
    public LikesData getLikesData(Integer problemId) {
        return null;
    }

    /*
     * When this function is called
     * 1. Check if the user has already liked the problem, if true then throw an exception
     * 2. If not then:
     *   a. Check if the problem id is in cache or bring the total like count into cache and increment the counter.
     *   b. Send the like event to rabbit queue for aync processing. 
     */
    public void likeProblem(String username, Integer problemId) throws DuplicateResourceException {
        
        Boolean problemLiked = redisService.isSetMember(username, Integer.toString(problemId)) ||
            likeRepository.findByUsernameAndProblemId(username, problemId).isPresent();

        if (problemLiked) {
            throw new DuplicateResourceException("You have already liked this problem");
        }

        System.out.println(String.format("User %s liked problem %d", username, problemId));

        LikeEvent likeEvent = new LikeEvent(
            problemId, 
            username, 
            LocalDateTime.now(ZoneId.of("UTC")), 
            true
        );

        redisService.addToSet(username, Integer.toString(problemId));
        
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.LIKE_EVENTS_QUEUE, likeEvent);
    }
    
    /*
     * When this function is called
     * 1. Check if the user hasn't liked the problem, if yes then throw an exception
     * 2. If not then:
     *   a. Check if the proble id is in cache or bring the total dislike count into cache and increment the counter.
     *   b. Send the dislike event to rabbit queue for aync processing. 
     */
    public void dislikeProblem(String username, Integer problemId) throws DuplicateResourceException {
        
        Boolean problemLiked = redisService.isSetMember(username, Integer.toString(problemId)) ||
            likeRepository.findByUsernameAndProblemId(username, problemId).isPresent();

        if (!problemLiked) {
            throw new DuplicateResourceException("You have not liked this problem");
        }

        System.out.println(String.format("User %s has disliked problem %d", username, problemId));

        LikeEvent likeEvent = new LikeEvent( 
            problemId, 
            username, 
            LocalDateTime.now(ZoneId.of("UTC")), 
            false
        );

        redisService.removeFromSet(username, Integer.toString(problemId));
        
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.LIKE_EVENTS_QUEUE, likeEvent);
    }

    
}
