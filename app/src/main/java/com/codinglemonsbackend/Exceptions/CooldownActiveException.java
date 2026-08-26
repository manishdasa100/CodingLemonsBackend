package com.codinglemonsbackend.Exceptions;

import org.springframework.http.HttpStatus;

public class CooldownActiveException extends AIHintException{

    public CooldownActiveException(String message){
        super(message);
    }

    public HttpStatus getStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }
}
