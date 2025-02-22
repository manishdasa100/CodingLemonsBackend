package com.codinglemonsbackend.Controller;

import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;
import com.codinglemonsbackend.Exceptions.ProfilePictureUploadFailureException;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Payloads.AddProblemToListRequest;
import com.codinglemonsbackend.Payloads.LikeRequest;
import com.codinglemonsbackend.Payloads.SubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Payloads.SubmitCodeResponsePayload;
import com.codinglemonsbackend.Payloads.UpdateProblemListRequest;
import com.codinglemonsbackend.Service.MainService;

import jakarta.validation.Valid;

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
                                            @RequestParam(name = "difficulties", required = false) String difficultyStr, @RequestParam(name = "topics", required = false) String topicsStr, @RequestParam(name = "companies", required = false) String companiesStr) {
        return ResponseEntity.ok().body(mainService.getProblemSet(difficultyStr, topicsStr, companiesStr, page, size));
    }

    @GetMapping("/problem/{id}")
    public ResponseEntity<ProblemDto> getProblem(@PathVariable Integer id) {
        ProblemDto problemDto = mainService.getProblem(id);
        return ResponseEntity.ok().body(problemDto);
    }

    @PostMapping("/like")
    public void likeProblem(@RequestBody LikeRequest likeRequest) throws DuplicateResourceException {
       mainService.likeProblem(likeRequest); 
    } 

    @PostMapping("/list/create")
    public ResponseEntity<String> addProblemList(@Valid @RequestBody ProblemListDto payload) throws DuplicateResourceException{
        mainService.addProblemList(payload);
        return ResponseEntity.ok().body("List added");
    }

    @PostMapping("list/add")
    public ResponseEntity<String> addProblemToList(@Valid @RequestBody AddProblemToListRequest request) {
        int problemIdsAdded = mainService.addProblemToList(request.getId(), request.getProblemIds());
        if (problemIdsAdded == 0) {
            return ResponseEntity.badRequest().body("No new problem ids found to add to list " + request.getId());
        } 
        return ResponseEntity.ok().body(String.format("Added %d problems to list %s", problemIdsAdded, request.getId()));
    }

    @PutMapping("list/update/{id}")
    public ResponseEntity<Object> updateProblemList(@PathVariable String id, @Valid @RequestBody UpdateProblemListRequest request) {
        Map<String, Object> updatedFields = mainService.updateProblemList(id, request);
        if (updatedFields.size() == 0) {
            return ResponseEntity.ok().body("Nothing to update for list id " + id);
        }
        return ResponseEntity.ok().body(updatedFields);
    }

    @GetMapping("/lists/{username}")
    public ResponseEntity<List<ProblemListDto>> getUserFavorites(@PathVariable String username){
        List<ProblemListDto> userFavorites = mainService.getUserFavorites(username);
        return ResponseEntity.ok().body(userFavorites);
    }

    @GetMapping("/lists/{username}/{name}")
    public ResponseEntity<ProblemListDto> getUserFavorite(@PathVariable String username, @PathVariable String name) {
        ProblemListDto problemDto = mainService.getUserProblemList(username, name);
        return ResponseEntity.ok().body(problemDto);
    }

    @PostMapping("/submission/submit")
    public ResponseEntity<?> submit(@Valid @RequestBody SubmitCodeRequestPayload payload){

        String submissionId = mainService.submitCode(payload);

        SubmitCodeResponsePayload responsePayload = new SubmitCodeResponsePayload(submissionId);

        return ResponseEntity.accepted().body(responsePayload);
    }

    @GetMapping("/submission/get/{submissionId}")
    public ResponseEntity<SubmissionResponsePayload<?>> getSubmission(@PathVariable String submissionId) throws FailedSubmissionException{

        SubmissionResponsePayload<?> payload = mainService.getSubmission(submissionId);

        return ResponseEntity.ok().body(payload);
    }

    @GetMapping("/problem/today")
    public ResponseEntity<ProblemDto> getProblemOfTheDay(){

        ProblemDto problemOfTheDay = mainService.getProblemOfTheDay();

        return ResponseEntity.ok().body(problemOfTheDay);
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable String username) {

        UserProfileDto userProfile = mainService.getUserProfile(username);

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
