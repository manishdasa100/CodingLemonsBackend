package com.codinglemonsbackend.Exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ProfilePictureUploadFailureException extends Exception{

    public ProfilePictureUploadFailureException(String message) {
        super(message);
    }
    
}
