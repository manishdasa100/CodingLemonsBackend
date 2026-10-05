package com.codinglemonsbackend.Exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class GlobalExceptionHandlerTest {
    
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test 
    void springClientErrorsKeepTheirStatus() {
        assertEquals(405, handler.handleException(new HttpRequestMethodNotSupportedException("PATCH"))
            .getStatusCode().value());
    }

    @Test
    void realFailuresAreStillServerErrors() {
        assertEquals(500, handler.handleException(new RuntimeException("boom"))
            .getStatusCode().value());
    }
}
