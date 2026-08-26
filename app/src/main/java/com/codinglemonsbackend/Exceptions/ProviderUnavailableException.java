package com.codinglemonsbackend.Exceptions;

import org.springframework.http.HttpStatus;

public class ProviderUnavailableException extends AIHintException{
    
    public ProviderUnavailableException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
