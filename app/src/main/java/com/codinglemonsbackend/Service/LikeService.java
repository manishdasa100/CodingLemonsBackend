package com.codinglemonsbackend.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Config.CustomCacheConfig;
import com.codinglemonsbackend.Config.RabbitMQConfig;
import com.codinglemonsbackend.Dto.LikeEvent;
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
        
        if (problemAlreadyLiked(username, problemId)) {
            throw new DuplicateResourceException(String.format("User %s has already liked problem %d", username, problemId));
        }

        System.out.println(String.format("User %s liked problem %d", username, problemId));

        LikeEvent likeEvent = new LikeEvent(
            problemId, 
            username,  
            LocalDateTime.now(ZoneId.of("UTC")), 
            true
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.MAINEXCHANGE, RabbitMQConfig.LIKE_EVENTS, likeEvent);
        
        addProblemToRedisSet(username, problemId, likeEvent.getIsLike());
    }
    
    /*
     * When this function is called
     * 1. Check if the user hasn't liked the problem, if yes then throw an exception
     * 2. If not then:
     *   a. Check if the proble id is in cache or bring the total dislike count into cache and increment the counter.
     *   b. Send the dislike event to rabbit queue for aync processing. 
     */
    public void dislikeProblem(String username, Integer problemId) throws DuplicateResourceException {
        
        if (problemAlreadyDisliked(username, problemId)) {
            throw new DuplicateResourceException("You have not liked this problem or already disliked it");
        }

        System.out.println(String.format("User %s has disliked problem %d", username, problemId));

        LikeEvent likeEvent = new LikeEvent( 
            problemId, 
            username, 
            LocalDateTime.now(ZoneId.of("UTC")), 
            false
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.MAINEXCHANGE, RabbitMQConfig.LIKE_EVENTS, likeEvent);

        addProblemToRedisSet(username, problemId, likeEvent.getIsLike());
    }

    private Boolean problemAlreadyLiked(String username, Integer problemId) {

        /*Conditions under which a user cannot like a problem
        1. The problem id should not be in the disliked set for the user
        2. User has already liked the problem(i.e the problem id is in the liked set for the user)
        3. User has liked the problem before
        */
        String redisLikesKey = RedisService.USER_LIKES_CACHE_PREFIX+username;
        String redisDislikesKey = RedisService.USER_DISLIKES_CACHE_PREFIX+username;

        if (redisService.isSetMember(redisDislikesKey, Integer.toString(problemId))) {
            return false;
        }
        return redisService.isSetMember(redisLikesKey, Integer.toString(problemId)) ||
        likeRepository.findByUsernameAndProblemId(username, problemId).isPresent();
    }

    private Boolean problemAlreadyDisliked(String username, Integer problemId) {

        /*Conditions under which a user cannot dislike a problem
        1. The problem id should not be in the liked set for the user
        2. User has already disliked the problem(i.e. the problem id is in the disliked set for the user)
        3. User has not liked the problem before
        */
        String redisLikesKey = RedisService.USER_LIKES_CACHE_PREFIX+username;
        String redisDislikesKey = RedisService.USER_DISLIKES_CACHE_PREFIX+username;

        if (redisService.isSetMember(redisLikesKey, Integer.toString(problemId))) {
            return false;
        }

        return redisService.isSetMember(redisDislikesKey, Integer.toString(problemId)) || 
        !likeRepository.findByUsernameAndProblemId(username, problemId).isPresent();
    }

    private void addProblemToRedisSet(String username, Integer problemId, Boolean isLike) {
        if (isLike) {
            redisService.addToSet(RedisService.USER_LIKES_CACHE_PREFIX+username, Integer.toString(problemId));
            redisService.removeFromSet(RedisService.USER_DISLIKES_CACHE_PREFIX+username, Integer.toString(problemId));
        } else {
            redisService.addToSet(RedisService.USER_DISLIKES_CACHE_PREFIX+username, Integer.toString(problemId));
            redisService.removeFromSet(RedisService.USER_LIKES_CACHE_PREFIX+username, Integer.toString(problemId));
        }
    }
}
