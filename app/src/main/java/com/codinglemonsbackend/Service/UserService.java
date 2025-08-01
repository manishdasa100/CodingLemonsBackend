package com.codinglemonsbackend.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Entities.UserEntity;
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
    public boolean updateUserDetails(UserDto newUserDetails, UserDto currentUserDetails){

        Map<String, Object> updatePropertiesMap = new HashMap<>();
        
        if (newUserDetails.getFirstName()!= null && !newUserDetails.getFirstName().equals(currentUserDetails.getFirstName())) {
            updatePropertiesMap.put("firstName", newUserDetails.getFirstName());
        }
        if (newUserDetails.getLastName() != null && !newUserDetails.getLastName().equals(currentUserDetails.getLastName())) {
            updatePropertiesMap.put("lastName", newUserDetails.getLastName());
        }
        if (newUserDetails.getEmail() !=  null && !newUserDetails.getEmail().equals(currentUserDetails.getEmail())) {
            updatePropertiesMap.put("email", newUserDetails.getEmail());
        }

        if (updatePropertiesMap.isEmpty()) return false; 

        return userRepository.updateUserDetails(newUserDetails.getUsername(), updatePropertiesMap);
    }

}
