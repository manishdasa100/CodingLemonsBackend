package com.codinglemonsbackend.Exceptions;

public class RegistryConversionException extends RuntimeException {
    
    public RegistryConversionException(String message) {
        super(message);
    }

    public RegistryConversionException(String message, Throwable cause) {
        super(message, cause);
    }
} 