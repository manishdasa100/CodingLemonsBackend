package com.codinglemonsbackend.Exceptions;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.codinglemonsbackend.Payloads.ExceptionMessage;

import lombok.extern.slf4j.Slf4j;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionMessage> handleAccessDeniedException(AccessDeniedException e) {
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.FORBIDDEN);
    }
    
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionMessage> handleException(Exception e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionMessage> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

   
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionMessage> handleBadCredentialsException(BadCredentialsException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage("Username or password incorrect"), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ExceptionMessage> handleResourceAlreadyExistsException(DuplicateResourceException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.BAD_REQUEST);
    
    }
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ExceptionMessage> resourceNotFound(NoSuchElementException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FailedSubmissionException.class)
    public ResponseEntity<ExceptionMessage> handleSubmissionFailure(FailedSubmissionException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.FAILED_DEPENDENCY);
    }

    @ExceptionHandler(FileUploadFailureException.class)
    public ResponseEntity<ExceptionMessage> handleProfilePictureUploadFailureException(FileUploadFailureException e){
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionMessage> handleHttpMessageNotReadablException(HttpMessageNotReadableException e) {
       return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.BAD_REQUEST); 
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ExceptionMessage> handleUsernameNotFoundException(UsernameNotFoundException e) {
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ExceptionMessage> handleMaxUploadSizeLimitExceedException(MaxUploadSizeExceededException e) {
        return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    // @ExceptionHandler(RedisConnectionFailureException.class)
    // public ResponseEntity<ExceptionMessage> handleRedisConnectionFailureException(RedisConnectionFailureException e) {
    //     return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    // }

    // @ExceptionHandler(DuplicateKeyException.class)
    // public ResponseEntity<ExceptionMessage> handleDuplicateKeyException(DuplicateKeyException e) {
    //     return new ResponseEntity<ExceptionMessage>(new ExceptionMessage(e.getMessage()), HttpStatus.BAD_REQUEST);
    // }
}
    

