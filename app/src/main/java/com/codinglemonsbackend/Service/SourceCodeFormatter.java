package com.codinglemonsbackend.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ProgrammingLanguage;

@Service
public class SourceCodeFormatter {
    
    public static String formatCode(String encodedDriverCode, String encodedUserCode, ProgrammingLanguage language) {

        byte[] driverCodeBytes, userCodeBytes;

        try {
            driverCodeBytes = Base64.getDecoder().decode(encodedDriverCode);
        } catch(Exception e) {
            throw new IllegalArgumentException("Base64 decoding error for driver code with cause: " + e.getMessage());
        }

        try{
            userCodeBytes = Base64.getDecoder().decode(encodedUserCode);
        } catch(Exception e) 
        {
            throw new IllegalArgumentException("Base64 decoding error for user code with cause: " + e.getMessage());
        }

        String driverCode = new String(driverCodeBytes, StandardCharsets.UTF_8);
        String userCode = new String(userCodeBytes, StandardCharsets.UTF_8);

        String fullCode = driverCode.replace("INSERT_USER_CODE", userCode);

        return fullCode;
    }
}
