package com.codinglemonsbackend.Controller;

import org.springframework.web.bind.annotation.RestController;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.codinglemonsbackend.Dto.ProblemDtoWithStatus;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Entities.UserProblemList;
import com.codinglemonsbackend.Exceptions.ResourceAlreadyExistsException;
import com.codinglemonsbackend.Payloads.CodeSubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.ProblemSetResponsePayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Payloads.SubmitCodeResponsePayload;
import com.codinglemonsbackend.Payloads.UserProblemListPayload;
import com.codinglemonsbackend.Service.Judge0SubmissionServiceImpl.Judge0SubmissionToken;
import com.codinglemonsbackend.Service.MainService;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1")
public class MainController {

    @Autowired
    private MainService mainService;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/hello")
    public String hello(){
        return "Hello";
    }
    
    @GetMapping("/problemset/all")
    public ResponseEntity<ProblemSetResponsePayload> getProblemSet(@RequestParam Integer page, @RequestParam Integer size, 
                                            @RequestParam(name = "difficulties", required = false) String difficultyStr, @RequestParam(name = "topics", required = false) String topicsStr) {
        
        return ResponseEntity.ok().body(mainService.getProblemSet(difficultyStr, topicsStr, page, size));
    }

    @GetMapping("/problem/{id}")
    public ResponseEntity<ProblemDtoWithStatus> getProblem(@PathVariable Integer id) {
        
        ProblemDtoWithStatus problemDto = mainService.getProblem(id);

        return ResponseEntity.ok().body(problemDto);
    }

    @PostMapping("addProblem")
    @PreAuthorize("hasAuthority('ADMIN')")
    public boolean addProblem(@Valid @RequestBody ProblemDto problemDto){

        mainService.addProblem(problemDto);
        
        return true;
    }

    @DeleteMapping("removeAllProblems")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void clearAllProblems(){
        mainService.clearAllProblems();
    }

    @PostMapping("addList")
    public ResponseEntity<String> addProblemList(@Valid @RequestBody UserProblemListPayload payload) throws ResourceAlreadyExistsException{

        UserProblemList problemList = modelMapper.map(payload, UserProblemList.class);

        mainService.addProblemList(problemList);

        return ResponseEntity.ok().body("List added");
    }

    @GetMapping("favorites")
    public ResponseEntity<List<UserProblemList>> getUserFavorites(){

        List<UserProblemList> userFavorites = mainService.getUserFavorites();

        return ResponseEntity.ok().body(userFavorites);
    }

    @PostMapping("submission/submit")
    public ResponseEntity<SubmitCodeResponsePayload> submit(@RequestBody SubmitCodeRequestPayload payload){

        String submissionId = mainService.submitCode(payload);

        SubmitCodeResponsePayload responsePayload = new SubmitCodeResponsePayload(submissionId);

        return ResponseEntity.accepted().body(responsePayload);
    }

    @GetMapping("submission/get/{submissionId}")
    public ResponseEntity<CodeSubmissionResponsePayload> getSubmission(@PathVariable String submissionId){

        CodeSubmissionResponsePayload payload = mainService.getSubmission(submissionId);

        return ResponseEntity.ok().body(payload);
    }
}
