package com.codinglemonsbackend.Exceptions;

import org.springframework.http.HttpStatus;

public class NotHintableException extends AIHintException {

    public NotHintableException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
