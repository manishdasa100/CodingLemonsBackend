package com.codinglemonsbackend.Exceptions;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException{
    
    public ResourceNotFoundException(String message){
        super(message);
    }

    public HttpStatus getStatus() {
        return HttpStatus.NO_CONTENT;
    }
}
