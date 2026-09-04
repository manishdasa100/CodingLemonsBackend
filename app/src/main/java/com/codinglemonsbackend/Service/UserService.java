package com.codinglemonsbackend.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Exceptions.UserAlreadyExistException;
import com.codinglemonsbackend.Repository.UserRepository;
import com.mongodb.client.result.UpdateResult;

@Service
public class UserService implements UserDetailsService{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserStreakService userStreakService;

    @Autowired
    private UserSubmissionStatusService userSubmissionStatusService;

    /**
     * Creates a user and everything an account needs to be usable, as one unit.
     * Both signup paths (local registration and OAuth) go through here - a user
     * without a profile or streak document is a broken account: getUserProfile
     * and getUserStreak both throw for it.
     *
     * This is the transaction boundary; the four steps below declare
     * Propagation.MANDATORY so they can never be called outside one.
     */
    @Transactional(rollbackFor = UserAlreadyExistException.class)
    public void provisionUser(UserEntity user, UserDto profile) throws UserAlreadyExistException {
        saveUser(user);
        userProfileService.createUserProfile(profile);
        userStreakService.createUserStreak(user.getUsername());
        userSubmissionStatusService.createForUser(user.getUsername());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveUser(UserEntity user) throws UserAlreadyExistException{
        try{
            loadUserByUsername(user.getUsername());
            throw new UserAlreadyExistException("Username already exists");
        } catch(UsernameNotFoundException e) {
            userRepository.saveUser(user);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserEntity> user = userRepository.findUserByUsername(username);
        if (user.isEmpty()) throw new UsernameNotFoundException("Username not found");
        return user.get();
    }

    public boolean resetUserPassword(String username, String newPassword) throws UsernameNotFoundException {
        UpdateResult updateResult = userRepository.resetUserPassword(username, newPassword);
        if (updateResult.getMatchedCount() == 0) throw new UsernameNotFoundException("Username not found");
        else if (updateResult.getModifiedCount() > 0) return true;
        return false;
    }

    public void updateUserEmail(String username, String newEmail) {
        if (newEmail != null && !newEmail.trim().isEmpty()) userRepository.updateUserEmail(username, newEmail);
    }

}
