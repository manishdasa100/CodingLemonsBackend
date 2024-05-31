package com.codinglemonsbackend.Service;

import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Entities.ProgrammingLanguage;

@Service
public class SourceCodeFormatter {
    
    public static String formatCode(String driverCode, String userCode, ProgrammingLanguage language) {

        if (language.equals(ProgrammingLanguage.JAVA) || language.equals(ProgrammingLanguage.JAVASCRIPT) || language.equals(ProgrammingLanguage.CPP)) {
            System.out.println("Returning " + language.name() + " formatted code with driver code + user code");
            return driverCode.concat(userCode);
        } else if (language.equals(ProgrammingLanguage.PYTHON)) {
            System.out.println("Returning " + ProgrammingLanguage.PYTHON + " formatted code");
            return userCode.concat(driverCode);
        }

        return null;
    }
}
