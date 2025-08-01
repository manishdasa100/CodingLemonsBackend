package com.codinglemonsbackend.Service;

import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.Role;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Events.UserAccountCreationEvent;
import com.codinglemonsbackend.Exceptions.UserAlreadyExistException;
import com.codinglemonsbackend.Payloads.LoginRequestPayload;
import com.codinglemonsbackend.Utils.JwtUtils;

@Service
public class AuthenticationService {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils; 

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public String registerUser(UserDto userDto, Boolean isAdmin) throws UserAlreadyExistException{
        
        UserEntity user = UserEntity.builder()
                            .username(userDto.getUsername())
                            .firstName(userDto.getFirstName())
                            .lastName(userDto.getLastName())
                            .email(userDto.getEmail())
                            .password(passwordEncoder.encode(userDto.getPassword()))
                            .passwordIssueDate(new Date(System.currentTimeMillis()))
                            .role((isAdmin)?Role.ADMIN:Role.USER)
                            .build();

        userService.saveUser(user);

        String jwtToken = jwtUtils.generateToken(user);

        UserAccountCreationEvent event = new UserAccountCreationEvent(this, user);

        eventPublisher.publishEvent(event);

        return jwtToken;
    }

    public String loginUser(LoginRequestPayload request){

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(), 
                request.getPassword()
            ));

        if (!authentication.isAuthenticated()) throw new BadCredentialsException("");

        UserDetails user = userService.loadUserByUsername(request.getUsername());

        String jwtToken = jwtUtils.generateToken(user);

        return jwtToken;
    } 

    public boolean resetUserPassword(String username, String password) {
        return userService.resetUserPassword(username, passwordEncoder.encode(password));
    }

    // private String getRandomPassKey() {
    //     Random random = ThreadLocalRandom.current();
    //     byte[] r = new byte[64]; //64 bytes
    //     random.nextBytes(r);
    //     return Base64.getEncoder().encodeToString(r);
    // }
}

