package com.codinglemonsbackend.Controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Entities.*;
import com.codinglemonsbackend.Payloads.CodeSubmissionResponse;
import com.codinglemonsbackend.Payloads.ProblemSetResponse;
import com.codinglemonsbackend.Payloads.RunCodeRequest;
import com.codinglemonsbackend.Service.ProblemRepositoryService;
import com.codinglemonsbackend.Service.SubmissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api/v1")
public class MainController {

    private static final Integer MAXSIZE = 100;

    @Autowired
    private ProblemRepositoryService problemRepositoryService;

    @Autowired
    private SubmissionService submissionService;

    @Value("${mongodb.username:Manish}")
    private String uname;


    @Value("${mongodb.password:Manish}")
    private String pass;

    @GetMapping("/hello")
    public String hello(){
        System.out.println("uname = " + uname);
        System.out.println("uname = " + pass);
        return "Hello";
    }
    
    @GetMapping("/problemset/all")
    public ProblemSetResponse getProblemSet(@RequestParam Integer page, @RequestParam Integer size, 
                                            @RequestParam(name = "difficulties", required = false) String difficultyStr, @RequestParam(name = "topics", required = false) String topicsStr) {
        if (size > MAXSIZE) size = MAXSIZE;

        System.out.println(difficultyStr + " " + topicsStr);

        // if ((difficultyStr == null || difficultyStr.length() == 0) && (topicsStr == null || topicsStr.length() == 0)) {
            
        //     // no filtering needed based on topics and/or difficulty
        //     return problemRepositoryService.getAllProblems(page, size);
        // }
        
        return problemRepositoryService.getProblems(difficultyStr, topicsStr, page, size);
    }

    @GetMapping("/problem/{id}")
    public ResponseEntity<?> getProblem(@PathVariable Integer id) {
        ProblemEntity problemEntity = problemRepositoryService.getProblem(id);

        return ResponseEntity.ok().body(problemEntity);
    }

    @PostMapping("addProblem")
    @PreAuthorize("hasAuthority('ADMIN')")
    public boolean addProblem(@Valid @RequestBody ProblemDto problemDto){
        System.out.println("--------PROBLEM DTO--------------");
        System.out.println(problemDto);
        problemRepositoryService.addProblem(problemDto);
        return true;
    }

    @DeleteMapping("removeAllProblems")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void removeAllProblems(){
        problemRepositoryService.removeAllProblems();
    }

    @PostMapping("runcode")
    public CodeSubmissionResponse runCode(@RequestBody RunCodeRequest request) {
        return submissionService.handleSubmission(request, false);
    }

    @PostMapping("submitcode")
    public CodeSubmissionResponse submitCode(@RequestBody RunCodeRequest request) {
        return submissionService.handleSubmission(request, true);
    }
}
