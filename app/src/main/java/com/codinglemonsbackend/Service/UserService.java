package com.codinglemonsbackend.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Events.UserProfileUpdateEvent;
import com.codinglemonsbackend.Exceptions.UserAlreadyExistException;
import com.codinglemonsbackend.Repository.UserRepository;
import com.mongodb.client.result.UpdateResult;

@Service
public class UserService implements UserDetailsService{
    
    @Autowired
    private UserRepository userRepository;

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
        Optional<UserEntity> user = userRepository.getUser(username);
        if (user.isEmpty()) throw new UsernameNotFoundException("Username not found");
        return user.get();
    }

    public boolean resetUserPassword(String username, String newPassword) throws UsernameNotFoundException {
        UpdateResult updateResult = userRepository.resetUserPassword(username, newPassword);
        if (updateResult.getMatchedCount() == 0) throw new UsernameNotFoundException("Username not found");
        else if (updateResult.getModifiedCount() > 0) return true;
        return false;
    }

    // public boolean updateUserDetails(UserEntity user, UserUpdateRequestPayload updateRequest){
    //     return userRepository.updateUserDetails(user, updateRequest);
    // }
    @Async("applicationAsyncExecutor")
    @EventListener
    public void updateUserDetails(UserProfileUpdateEvent updateEvent) {

        String username = updateEvent.getUsername();

        UserProfileDto newUserDetails = updateEvent.getNewUserProfileDetails();

        Map<String, Object> updatePropertiesMap = new HashMap<>();
        
        if (newUserDetails.getFirstName()!= null && !newUserDetails.getFirstName().trim().isEmpty()) {
            updatePropertiesMap.put("firstName", newUserDetails.getFirstName());
        }
        if (newUserDetails.getLastName() != null && !newUserDetails.getLastName().trim().isEmpty()) {
            updatePropertiesMap.put("lastName", newUserDetails.getLastName());
        }
        if (newUserDetails.getEmail() !=  null && !newUserDetails.getEmail().trim().isEmpty()) {
            updatePropertiesMap.put("email", newUserDetails.getEmail());
        }

        if (updatePropertiesMap.isEmpty()) return; 

        userRepository.updateUserDetails(username, updatePropertiesMap);
    }

}
