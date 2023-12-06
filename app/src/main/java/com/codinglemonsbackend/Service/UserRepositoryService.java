package com.codinglemonsbackend.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Exceptions.UserAlreadyExistException;
import com.codinglemonsbackend.Repository.UserRepository;

@Service
public class UserRepositoryService implements UserDetailsService{
    
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

    public void updateUser(UserEntity user) {
        userRepository.saveUser(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserEntity> user = userRepository.getUser(username);
        if (user.isEmpty()) throw new UsernameNotFoundException("User not found");
        return user.get();
    }

}
