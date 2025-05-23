package com.codinglemonsbackend.Controller;

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

import jakarta.validation.Valid;

@Controller
@RequestMapping(value = "/api/v1/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> registerUser(@Valid @RequestBody UserDto userDto) throws UserAlreadyExistException{
        return ResponseEntity.ok().body(new AuthenticationResponse(authService.registerUser(userDto, false)));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<AuthenticationResponse> registerAdminUser(@Valid @RequestBody UserDto userDto) throws UserAlreadyExistException{
        return ResponseEntity.ok().body(new AuthenticationResponse(authService.registerUser(userDto, true)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> loginUser(@Valid @RequestBody LoginRequestPayload request){
        return ResponseEntity.ok().body(new AuthenticationResponse(authService.loginUser(request)));
    }

    @PostMapping("/resetpassword")
    public ResponseEntity<String> resetUserPassword(@Valid @RequestBody ResetPasswordRequestPayload resetPasswordRequestPayload){
        
        boolean updateStatus = authService.resetUserPassword(resetPasswordRequestPayload.getUsername(), 
                                        resetPasswordRequestPayload.getNewPassword());

        return ResponseEntity.ok().body(updateStatus?"Password updated successfully":"Password not updated");
    }
    
}
