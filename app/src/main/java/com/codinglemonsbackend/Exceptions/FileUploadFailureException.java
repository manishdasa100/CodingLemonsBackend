package com.codinglemonsbackend.Exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class FileUploadFailureException extends Exception{

    public FileUploadFailureException(String message) {
        super(message);
    }
    
}
