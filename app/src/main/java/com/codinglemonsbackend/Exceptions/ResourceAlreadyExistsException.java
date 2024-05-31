package com.codinglemonsbackend.Exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ResourceAlreadyExistsException extends Exception{
    
    public ResourceAlreadyExistsException(String message){
        super(message);
    }
}
