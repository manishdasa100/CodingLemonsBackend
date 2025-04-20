package com.codinglemonsbackend.Exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DuplicateResourceException extends Exception{
    
    public DuplicateResourceException(String message){
        super(message);
    }
}
