package com.codinglemonsbackend.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Exceptions.ProfilePictureUploadFailureException;
import com.codinglemonsbackend.Exceptions.UserAlreadyExistException;
import com.codinglemonsbackend.Properties.S3Buckets;
import com.codinglemonsbackend.Repository.UserRepository;
import com.mongodb.client.result.UpdateResult;

@Service
public class UserService implements UserDetailsService{
    
    @Autowired
    private UserRepository userRepository;

    // @Autowired
    // private S3Service s3Service;

    // @Autowired
    // private S3Buckets s3Buckets;

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

    /*public void uploadUserProfilePicture(UserEntity user, MultipartFile file) throws ProfilePictureUploadFailureException{

        String profilePictureId = UUID.randomUUID().toString();

        try {
            s3Service.putObject(
                s3Buckets.getCustomer(), 
                "profile-picture/%s/%s".formatted(user.getUsername(), profilePictureId), 
                file.getBytes()
            );
        } catch (Exception e) {
            throw new ProfilePictureUploadFailureException("Profile picture upload failed");
        }

        user.setProfilePictureId(profilePictureId);

        userRepository.updateUserProfilePictureId(user.getUsername(), profilePictureId);
    }

    public byte[] getUserProfilePicture(UserEntity user) {

        String profilePictureId = user.getProfilePictureId();

        byte[] userProfilePicture = s3Service.getObject(
            s3Buckets.getCustomer(), 
            "profile-picture/%s/%s".formatted(user.getUsername(), profilePictureId)
        );

        return userProfilePicture;
    }
    */
}
