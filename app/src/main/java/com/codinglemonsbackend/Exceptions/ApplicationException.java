package com.codinglemonsbackend.Exceptions;

import org.springframework.http.HttpStatus;

public abstract class ApplicationException extends Exception{
    
    public ApplicationException(String message) {
        super(message);
    }

    public abstract HttpStatus getStatus();
}
