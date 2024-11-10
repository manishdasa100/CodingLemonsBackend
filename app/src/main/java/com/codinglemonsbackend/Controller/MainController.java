package com.codinglemonsbackend.Controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemDtoWithStatus;
import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.ProblemListEntity;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;
import com.codinglemonsbackend.Exceptions.ProfilePictureUploadFailureException;
import com.codinglemonsbackend.Exceptions.ResourceAlreadyExistsException;
import com.codinglemonsbackend.Payloads.CodeRunResponsePayload;
import com.codinglemonsbackend.Payloads.SubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.ProblemSetResponsePayload;
import com.codinglemonsbackend.Payloads.ProblemUpdateRequestPayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Payloads.SubmitCodeResponsePayload;
import com.codinglemonsbackend.Payloads.UserProblemListPayload;
import com.codinglemonsbackend.Payloads.UserUpdateRequestPayload;
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
    public ResponseEntity<ProblemSet> getProblemSet(@RequestParam Integer page, @RequestParam Integer size, 
                                            @RequestParam(name = "difficulties", required = false) String difficultyStr, @RequestParam(name = "topics", required = false) String topicsStr) {
        
        return ResponseEntity.ok().body(mainService.getProblemSet(difficultyStr, topicsStr, page, size));
    }

    @GetMapping("/problem/{id}")
    public ResponseEntity<ProblemDto> getProblem(@PathVariable Integer id) {
        
        ProblemDto problemDto = mainService.getProblem(id);

        return ResponseEntity.ok().body(problemDto);
    }

    @PostMapping("/addProblem")
    @PreAuthorize("hasAuthority('ADMIN')")
    public boolean addProblem(@Valid @RequestBody ProblemDto problemDto){

        mainService.addProblem(problemDto);
        
        return true;
    }

    @PutMapping("/problem/update/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void updateProblem(@PathVariable Integer id, @RequestBody ProblemUpdateDto updateMetadata){
        mainService.updateProblem(id, updateMetadata);
    } 

    @DeleteMapping("/problem/delete/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteProblemById(@PathVariable Integer id){
        mainService.deleteProblemById(id);
    }

    @DeleteMapping("/removeAllProblems")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void clearAllProblems(){
        mainService.clearAllProblems();
    }

    @PostMapping("/addList")
    public ResponseEntity<String> addProblemList(@Valid @RequestBody UserProblemListPayload payload) throws ResourceAlreadyExistsException{

        ProblemListEntity problemList = modelMapper.map(payload, ProblemListEntity.class);

        mainService.addProblemList(problemList);

        return ResponseEntity.ok().body("List added");
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<ProblemListDto>> getUserFavorites(){

        List<ProblemListDto> userFavorites = mainService.getUserFavorites();

        return ResponseEntity.ok().body(userFavorites);
    }

    // @PostMapping("/runCode")
    // public ResponseEntity<SubmitCodeResponsePayload> runCode(@Valid @RequestBody SubmitCodeRequestPayload payload){

    //     String submissionId = mainService.submitCode(payload, true);

    //     SubmitCodeResponsePayload responsePayload = new SubmitCodeResponsePayload(submissionId);

    //     return ResponseEntity.accepted().body(responsePayload);
    // }

    @PostMapping("/submission/submit")
    public ResponseEntity<?> submit(@Valid @RequestBody SubmitCodeRequestPayload payload){

        String submissionId = mainService.submitCode(payload);

        SubmitCodeResponsePayload responsePayload = new SubmitCodeResponsePayload(submissionId);

        return ResponseEntity.accepted().body(responsePayload);
    }

    @GetMapping("/runCode/get/{interpretId}")
    public ResponseEntity<CodeRunResponsePayload> getRunCodeResult(@PathVariable String interpretId){

        return null;
    }

    @GetMapping("/submission/get/{submissionId}")
    public ResponseEntity<SubmissionResponsePayload<?>> getSubmission(@PathVariable String submissionId) throws FailedSubmissionException{

        SubmissionResponsePayload<?> payload = mainService.getSubmission(submissionId);

        return ResponseEntity.ok().body(payload);
    }

    @GetMapping("problem/today")
    public ResponseEntity<ProblemDto> getProblemOfTheDay(){

        ProblemDto problemOfTheDay = mainService.getProblemOfTheDay();

        return ResponseEntity.ok().body(problemOfTheDay);
    }

    @GetMapping("user/profile")
    public ResponseEntity<UserProfileDto> getUserProfile() {

        UserProfileDto userProfile = mainService.getUserProfile();

        return ResponseEntity.ok().body(userProfile);
    }

    @PutMapping("/user/update")
    public ResponseEntity<String> updateUserProfile(@Valid @RequestBody UserProfileDto newUserProfile) {
        
        boolean updated = mainService.updateUserProfile(newUserProfile);

        if (updated) {
            return ResponseEntity.ok().body("Updated");
        }

        return ResponseEntity.ok().body("No updates done");
    }

    @PostMapping(value = "/user/uploadProfilePic", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadUserProfilePicture(@RequestBody MultipartFile file) throws ProfilePictureUploadFailureException {

        mainService.uploadUserProfilePicture(file);
    }

    @GetMapping("/user/profileImage")
    public byte[] getUserProfilePicture(){
        return mainService.getUserProfilePicture();
    }
}
