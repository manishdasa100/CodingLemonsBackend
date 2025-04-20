package com.codinglemonsbackend.Exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class FailedSubmissionException extends Exception {
    
    public FailedSubmissionException(String message) {
        super(message);
    }
}
