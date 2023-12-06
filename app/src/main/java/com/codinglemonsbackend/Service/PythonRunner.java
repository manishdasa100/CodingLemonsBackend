package com.codinglemonsbackend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.codinglemonsbackend.Entities.ProgrammingLanguage;

@Component
public class PythonRunner extends CodeRunner{

    public PythonRunner(@Autowired RestTemplate restTemplate) {
        super("glot/python:latest", ProgrammingLanguage.PYTHON, "Main.py", restTemplate);
    }
    
}
