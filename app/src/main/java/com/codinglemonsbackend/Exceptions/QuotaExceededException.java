package com.codinglemonsbackend.Exceptions;

import org.springframework.http.HttpStatus;

public class QuotaExceededException extends AIHintException{
    
    public QuotaExceededException(String message) {
        super(message);
    }

    public HttpStatus getStatus(){
        return HttpStatus.TOO_MANY_REQUESTS;
    }
}
