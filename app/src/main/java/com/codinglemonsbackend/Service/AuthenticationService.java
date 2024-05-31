package com.codinglemonsbackend.Service;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Entities.Role;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Exceptions.UserAlreadyExistException;
import com.codinglemonsbackend.Payloads.LoginRequestPayload;
import com.codinglemonsbackend.Utils.JwtUtils;

@Service
public class AuthenticationService {

    @Autowired
    private UserService userRepositoryService;

    @Autowired
    private JwtUtils jwtUtils; 

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;
    
    public String registerUser(UserDto userDto, Boolean isAdmin) throws UserAlreadyExistException{
        
        UserEntity user = UserEntity.builder()
                            .username(userDto.getUsername())
                            .firstName(userDto.getFirstName())
                            .lastName(userDto.getLastName())
                            .email(userDto.getEmail())
                            .password(passwordEncoder.encode(userDto.getPassword()))
                            .role((isAdmin)?Role.ADMIN:Role.USER)
                            .points(0)
                            // .problemLists(UserRepositoryService.getDefaultProblemList())
                            .submissions(new ArrayList<>())
                            .build();

        userRepositoryService.saveUser(user);

        String jwtToken = jwtUtils.generateToken(user);

        return jwtToken;
    }
    

    public String loginUser(LoginRequestPayload request){

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(), 
                request.getPassword()
            ));

        if (!authentication.isAuthenticated()) throw new BadCredentialsException("");

        UserDetails user = userRepositoryService.loadUserByUsername(request.getUsername());

        String jwtToken = jwtUtils.generateToken(user);

        return jwtToken;
    } 
}

