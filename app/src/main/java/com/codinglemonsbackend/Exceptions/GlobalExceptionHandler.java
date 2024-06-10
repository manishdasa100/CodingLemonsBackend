package com.codinglemonsbackend.Exceptions;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.codinglemonsbackend.Payloads.ExceptionMessage;


@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<ExceptionMessage> handleUserAlreadyExistException(UserAlreadyExistException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        Map<String,String> errorsMap = new HashMap<String,String>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError)error).getField();
            String message = error.getDefaultMessage();
            errorsMap.put(field, message);
        });
        return new ResponseEntity<Map<String,String>>(errorsMap, HttpStatus.BAD_REQUEST);
    }

    // @ExceptionHandler(Exception.class)
    // public ResponseEntity<ExceptionMessage> handleException(Exception e){
    //     return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    // }

   
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionMessage> handleBadCredentialsException(BadCredentialsException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage("Username or password incorrect"), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ExceptionMessage> handleResourceAlreadyExistsException(ResourceAlreadyExistsException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.BAD_REQUEST);
    
    }
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ExceptionMessage> resourceNotFound(NoSuchElementException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FailedSubmissionException.class)
    public ResponseEntity<ExceptionMessage> handleSubmissionFailure(FailedSubmissionException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.FAILED_DEPENDENCY);
    }

    @ExceptionHandler(ProfilePictureUploadFailureException.class)
    public ResponseEntity<ExceptionMessage> handleProfilePictureUploadFailureException(ProfilePictureUploadFailureException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionMessage> handleHttpMessageNotReadablException(HttpMessageNotReadableException e) {
       return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.BAD_REQUEST); 
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionMessage> handleUsernameNotFoundException(UsernameNotFoundException e) {
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

}
    

