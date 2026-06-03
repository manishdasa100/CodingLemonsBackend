package com.codinglemonsbackend.Service;

import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.AuthProvider;
import com.codinglemonsbackend.Dto.Role;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Events.UserAccountCreationEvent;
import com.codinglemonsbackend.Exceptions.UserAlreadyExistException;
import com.codinglemonsbackend.Payloads.LoginRequestPayload;
import com.codinglemonsbackend.Utils.JwtUtils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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
    private UserProfileService userProfileService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // @Autowired
    // private MeterRegistry meterRegistry;

    // @Autowired
    // private Counter userRegistrationCounter;

    // @Autowired
    // private Counter userLoginCounter;
    
    public String registerUser(UserDto userDto, Boolean isAdmin) throws UserAlreadyExistException{
        
        UserEntity user = UserEntity.builder()
                            .username(userDto.getUsername())
                            .password(passwordEncoder.encode(userDto.getPassword()))
                            .passwordIssueDate(new Date((System.currentTimeMillis() / 1000) * 1000))
                            .email(userDto.getEmail())
                            .role((isAdmin)?Role.ADMIN:Role.USER)
                            .build();

        userService.saveUser(user);

        // Track user registration metrics
        // userRegistrationCounter.increment();
        
        String jwtToken = jwtUtils.generateToken(user);

        UserAccountCreationEvent event = new UserAccountCreationEvent(this, userDto);

        eventPublisher.publishEvent(event);

        log.info("User registered successfully: username={}, isAdmin={}", userDto.getUsername(), isAdmin);

        return jwtToken;
    }

    public String loginUser(LoginRequestPayload request){

        UserEntity user = (UserEntity) userService.loadUserByUsername(request.getUsername());

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadCredentialsException(
                "This account is linked with " + user.getAuthProvider().name().toLowerCase() + ". Please sign in using that provider."
            );
        }

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            ));

        if (!authentication.isAuthenticated()) throw new BadCredentialsException("Username or password is incorrect");

        // Track user login metrics
        // userLoginCounter.increment();

        String jwtToken = jwtUtils.generateToken(user);

        log.info("User logged in successfully: username={}", request.getUsername());

        return jwtToken;
    } 

    public boolean resetUserPassword(String username, String password) {
        UserEntity user = (UserEntity) userService.loadUserByUsername(username);

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadCredentialsException(
                "Password reset not allowed for this account. This account is linked with " + user.getAuthProvider().name().toLowerCase() + "."
            );
        }
        return userService.resetUserPassword(username, passwordEncoder.encode(password));
    }

    // private String getRandomPassKey() {
    //     Random random = ThreadLocalRandom.current();
    //     byte[] r = new byte[64]; //64 bytes
    //     random.nextBytes(r);
    //     return Base64.getEncoder().encodeToString(r);
    // }
}

