package com.codinglemonsbackend.Exceptions;

public class ResourceNotFoundException extends Exception{

    public ResourceNotFoundException(){}
    
    public ResourceNotFoundException(String message){
        super(message);
    }
}
