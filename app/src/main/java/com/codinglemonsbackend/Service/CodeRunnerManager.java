package com.codinglemonsbackend.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.CodeOutput;
import com.codinglemonsbackend.Dto.RunCodeMatadata;
import com.codinglemonsbackend.Entities.ProgrammingLanguage;

@Service
public class CodeRunnerManager {

    private Map<ProgrammingLanguage,CodeRunner> codeRunnersMap;

    public CodeRunnerManager(@Autowired List<CodeRunner> codeRunners) {
        codeRunnersMap = new HashMap<ProgrammingLanguage,CodeRunner>();
        codeRunners.stream().forEach(runner -> codeRunnersMap.put(runner.language, runner));
        System.out.println("All available code runners");
        for (ProgrammingLanguage language : codeRunnersMap.keySet()) {
            System.out.println(language);
        }
    }

    public CodeOutput runCode(RunCodeMatadata metadata) {
        ProgrammingLanguage programingLanguage = metadata.getLanguage();
        CodeRunner codeRunner = getCodeRunner(programingLanguage);
        return codeRunner.runCode(metadata);
    }

    private CodeRunner getCodeRunner(ProgrammingLanguage language){
        return codeRunnersMap.get(language);
    }
    
}
