package com.codinglemonsbackend.Service;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;

public class CodeRunnerManagerTest {
    
    private List<CodeRunner> codeRunners;

    // TODO: Test runCode() method

    @BeforeAll
    public void setUp(){
        codeRunners.add(new PythonRunner(null));
        codeRunners.add(new JavaRunner(null));
    }

}
