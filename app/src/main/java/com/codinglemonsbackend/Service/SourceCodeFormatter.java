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

        //byte[] concatenatedCodeBytes = new byte[driverCodeBytes.length + userCodeBytes.length];
        String driverCode = new String(driverCodeBytes, StandardCharsets.UTF_8);
        String userCode = new String(userCodeBytes, StandardCharsets.UTF_8);

        String fullCode = driverCode.replace("INSERT_USER_CODE", userCode);

        // if (language.equals(ProgrammingLanguage.JAVA) || language.equals(ProgrammingLanguage.JAVASCRIPT) || language.equals(ProgrammingLanguage.CPP)) {
        //     // System.out.println("Returning " + language.name() + " formatted code with driver code + user code");
        //     // String driverCode = new String(driverCodeBytes, StandardCharsets.UTF_8);
        //     // String userCode = new String(userCodeBytes, StandardCharsets.UTF_8);
        //     // String fullCode = driverCode.concat(userCode);
        //     // System.out.println("Code:");
        //     // System.out.println(fullCode);
        //     // return fullCode;
        // } else if (language.equals(ProgrammingLanguage.PYTHON)) {
        //     System.out.println("Returning " + ProgrammingLanguage.PYTHON + " formatted code");
        //     //return userCode.concat(driverCode);
        // }

        return fullCode;
    }
}
