package com.codinglemonsbackend.Controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.CurrentUserDto;
import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.EarnedBadgeDto;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Dto.ProblemOfTheDayDto;
import com.codinglemonsbackend.Dto.ProblemSet;
import com.codinglemonsbackend.Dto.ProblemsPage;
import com.codinglemonsbackend.Dto.UserOccupation;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.Topic;
import com.codinglemonsbackend.Entities.UserWorkExperience;
import com.codinglemonsbackend.Dto.UserStreakDto;
import com.codinglemonsbackend.Dto.UserSubmissionStatusDto;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Payloads.ProblemListOperationRequest;
import com.codinglemonsbackend.Payloads.LikeRequest;
import com.codinglemonsbackend.Payloads.LikesData;
import com.codinglemonsbackend.Payloads.SubmissionResponsePayload;
import com.codinglemonsbackend.Payloads.SubmitCodeRequestPayload;
import com.codinglemonsbackend.Payloads.SubmitCodeResponsePayload;
import com.codinglemonsbackend.Payloads.UpdateProblemListRequest;
import com.codinglemonsbackend.Service.MainServiceImpl;
import com.codinglemonsbackend.Utils.ImageUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api/v1")
public class MainController {

    @Autowired
    private MainServiceImpl mainService;

    @GetMapping("/hello")
    public String hello(){
        return "Hello";
    }
    
    @GetMapping("/problemset/all")
    public ResponseEntity<ProblemsPage> getProblemSet(@RequestParam Integer page, @RequestParam Integer size, 
                                            @RequestParam(name = "difficulties", required = false) String difficultyStr, @RequestParam(name = "topics", required = false) String topicsStr, @RequestParam(name = "companies", required = false) String companiesStr) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ADMIN") || grantedAuthority.getAuthority().equals("SUPERADMIN"));
        return ResponseEntity.ok().body(mainService.getProblemSet(difficultyStr, topicsStr, companiesStr, page, size, isAdmin));
    }

    @GetMapping("/problem/{id}")
    public ResponseEntity<ProblemDto> getProblem(@PathVariable Integer id) {
        ProblemDto problemDto = mainService.getProblem(id);
        return ResponseEntity.ok().body(problemDto);
    }

    @GetMapping("/problem/{id}/metadata")
    public ResponseEntity<LikesData> getProblemLikesData(@PathVariable Integer id) {
        LikesData likeData = mainService.getProblemLikesData(id);
        return ResponseEntity.ok().body(likeData);
    }

    @PostMapping("/like")
    public void likeProblem(@RequestBody LikeRequest likeRequest) throws DuplicateResourceException {
       mainService.likeProblem(likeRequest); 
    } 

    @PostMapping("/list/create")
    public ResponseEntity<String> createProblemList(@Valid @RequestBody ProblemListDto payload) throws DuplicateResourceException{
        mainService.createProblemList(payload);
        String listType = payload.getIsStudyPlan() ? "study plan" : "problem list";
        return ResponseEntity.ok().body(String.format("%s created", listType));
    }

    @PutMapping("list/add")
    public ResponseEntity<String> addProblemToList(@Valid @RequestBody ProblemListOperationRequest request) {
        mainService.addProblemToList(request.getId(), request.getProblemIds());
        return ResponseEntity.ok().body("Problems added successfully");
    }

    @DeleteMapping("list/remove")
    public ResponseEntity<String> removeProblemFromList(@Valid @RequestBody ProblemListOperationRequest request) {
        mainService.removeProblemFromList(request.getId(), request.getProblemIds());
        return ResponseEntity.ok().body("Problems removed successfully");
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
        if (username.equals("global")) {
            return ResponseEntity.ok().body(mainService.getAllGlobalProblemLists());
        } 
        return ResponseEntity.ok().body(mainService.getUserFavorites(username));
    }

    @GetMapping("/list/{username}")
    public ResponseEntity<ProblemListDto> getUserFavorite(@PathVariable String username, @RequestParam String name) {
        ProblemListDto problemDto = mainService.getAProblemList(username, name);
        return ResponseEntity.ok().body(problemDto);
    }

    @PostMapping("/submission/submit")
    public ResponseEntity<?> submit(@Valid @RequestBody SubmitCodeRequestPayload payload){
        String submissionId = mainService.submitCode(payload);
        SubmitCodeResponsePayload responsePayload = new SubmitCodeResponsePayload(submissionId);
        return ResponseEntity.accepted().body(responsePayload);
    }

    @GetMapping("/submission/check/{submissionId}")
    public ResponseEntity<SubmissionResponsePayload> check(@PathVariable String submissionId) {
        SubmissionResponsePayload reponse = mainService.check(submissionId);
        return ResponseEntity.accepted().body(reponse);
    }

    @GetMapping("/problem/today")
    public ResponseEntity<ProblemOfTheDayDto> getProblemOfTheDay(){
        ProblemOfTheDayDto problemOfTheDay = mainService.getProblemOfTheDay();
        return ResponseEntity.ok().body(problemOfTheDay);
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserDto> getMe() {
        return ResponseEntity.ok().body(mainService.getCurrentUserInfo());
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable String username) {
        UserProfileDto userProfile = mainService.getUserProfile(username);
        return ResponseEntity.ok().body(userProfile);
    }

    @PutMapping("/user/update")
    public ResponseEntity<String> updateUserProfile(@Valid @RequestBody UserProfileDto newUserProfile) {
        Boolean updated = mainService.updateUserProfile(newUserProfile);
        return ResponseEntity.ok().body("Update status: " + updated);
    }

    @PostMapping(value = "/user/uploadProfilePic", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadUserProfilePicture(@RequestBody MultipartFile profilePictureImageFile) throws FileUploadFailureException, IOException {
        List<String> validImageExtensions = ImageUtils.validImageUploadExtensions;
        String fileExtension = FilenameUtils.getExtension(profilePictureImageFile.getOriginalFilename());

        if (fileExtension != null && !validImageExtensions.contains(fileExtension)) {
            throw new IllegalArgumentException(String.format("Unsupported file extension: %s. Please upload one of %s", fileExtension, validImageExtensions));
        }
        mainService.uploadUserProfilePicture(profilePictureImageFile);
        return ResponseEntity.ok().body("Profile picture uploaded successfully");
    }

    @GetMapping("/occupations")
    public ResponseEntity<List<String>> getOccupations() {
        List<String> labels = Arrays.stream(UserOccupation.values())
                .map(UserOccupation::getLabel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(labels);
    }

    @GetMapping("/allTopics")
    public ResponseEntity<List<Topic>> getTopics() {
        return ResponseEntity.ok().body(mainService.getTopics());
    }

    @GetMapping("/allCompanies")
    public ResponseEntity<List<CompanyDto>> getCompanies() {
        return ResponseEntity.ok().body(mainService.getCompanies());
    }

    @GetMapping("/company/{companySlug}")
    public ResponseEntity<CompanyDto> getCompanyDetails(@PathVariable String companySlug) {
        if (companySlug == null || companySlug.isBlank()) {
            throw new IllegalArgumentException("Company slug cannot be blank");
        }
        CompanyDto companyDetails = mainService.getCompanyDetails(companySlug);
        return ResponseEntity.ok().body(companyDetails);
    }

    @GetMapping("/problem/counts")
    public ResponseEntity<Map<String, Integer>> getProblemCountByDifficulty() {
        return ResponseEntity.ok().body(mainService.getProblemCountByDifficulty());
    }

    @GetMapping("/user/submission-status")
    public ResponseEntity<UserSubmissionStatusDto> getSubmissionStatus() {
        return ResponseEntity.ok().body(mainService.getSubmissionStatusDto());
    }

    @GetMapping("/user/streak")
    public ResponseEntity<UserStreakDto> getUserStreak() {
        return ResponseEntity.ok().body(mainService.getUserStreak());
    }

    @GetMapping("/user/{username}/badges")
    public ResponseEntity<Map<String, List<EarnedBadgeDto>>> getUserBadges(@PathVariable String username) {
        return ResponseEntity.ok(mainService.getUserBadges(username));
    }

}
