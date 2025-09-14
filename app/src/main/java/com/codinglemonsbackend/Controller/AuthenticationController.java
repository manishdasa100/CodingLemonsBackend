package com.codinglemonsbackend.Controller;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Exceptions.UserAlreadyExistException;
import com.codinglemonsbackend.Payloads.AuthenticationResponse;
import com.codinglemonsbackend.Payloads.LoginRequestPayload;
import com.codinglemonsbackend.Payloads.ResetPasswordRequestPayload;
import com.codinglemonsbackend.Service.AuthenticationService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;

@Controller
@RequestMapping(value = "/api/v1/auth")
public class AuthenticationController {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private AuthenticationService authService;

    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> registerUser(@Valid @RequestBody UserDto userDto) throws UserAlreadyExistException{
        AuthenticationResponse response = new AuthenticationResponse(authService.registerUser(userDto, false));
        Counter registrationCounter = counters.computeIfAbsent("user.registration", key -> 
            Counter.builder("user.registration.total")
                .description("Total number of user registrations")
                .tag("type", "user-registration")
                .register(meterRegistry)
        );
        registrationCounter.increment();
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/admin/register")
    public ResponseEntity<AuthenticationResponse> registerAdminUser(@Valid @RequestBody UserDto userDto) throws UserAlreadyExistException{
        return ResponseEntity.ok().body(new AuthenticationResponse(authService.registerUser(userDto, true)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> loginUser(@Valid @RequestBody LoginRequestPayload request){
        AuthenticationResponse response = new AuthenticationResponse(authService.loginUser(request));
        Counter loginCounter = counters.computeIfAbsent("user.login", key -> 
            Counter.builder("user.login.total")
                .description("Total number of user logins")
                .tag("type", "user-login")
                .register(meterRegistry)
        );
        loginCounter.increment();
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/resetpassword")
    public ResponseEntity<String> resetUserPassword(@Valid @RequestBody ResetPasswordRequestPayload resetPasswordRequestPayload){
        boolean updateStatus = authService.resetUserPassword(resetPasswordRequestPayload.getUsername(), 
                                        resetPasswordRequestPayload.getNewPassword());
        Counter resetPasswordCounter = counters.computeIfAbsent("user.reset-password", key -> 
            Counter.builder("user.reset-password.total")
                .description("Total number of user password resets")
                .tag("type", "password-reset")
                .register(meterRegistry)
        );
        resetPasswordCounter.increment();

        return ResponseEntity.ok().body(updateStatus?"Password updated successfully":"Password not updated");
    }
    
}
