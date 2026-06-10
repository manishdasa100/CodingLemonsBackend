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
import org.springframework.web.bind.annotation.GetMapping;
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
import com.codinglemonsbackend.Dto.SkillTags;
import com.codinglemonsbackend.Dto.UserOccupation;
import com.codinglemonsbackend.Dto.UserProfileDto;
import com.codinglemonsbackend.Entities.Topic;
import com.codinglemonsbackend.Entities.UserWorkExperience;
import com.codinglemonsbackend.Dto.UserStreakDto;
import com.codinglemonsbackend.Dto.UserSubmissionStatusDto;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Payloads.AddProblemToListRequest;
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
    public ResponseEntity<String> addProblemList(@Valid @RequestBody ProblemListDto payload) throws DuplicateResourceException{
        mainService.addProblemList(payload);
        return ResponseEntity.ok().body("List added");
    }

    @PostMapping("list/add")
    public ResponseEntity<String> addProblemToList(@Valid @RequestBody AddProblemToListRequest request) {
        Map<String, Object> result = mainService.addProblemToList(request.getId(), request.getProblemIds());
        Set<Integer> problemIdsAdded = (Set<Integer>) result.get("addedProblemIds");
        String listName = (String) result.get("listName");
        if (problemIdsAdded == null || problemIdsAdded.isEmpty()) {
            return ResponseEntity.ok().body("Problems already present in " + listName);
        } 
        return ResponseEntity.ok().body(String.format("Added %d new problems to list %s", problemIdsAdded.size(), listName));
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

    @GetMapping("/list/{username}")
    public ResponseEntity<ProblemListDto> getUserFavorite(@PathVariable String username, @RequestParam String name) {
        ProblemListDto problemDto = mainService.getUserProblemList(username, name);
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

    // @GetMapping("/submission/get/{submissionId}")
    // public ResponseEntity<SubmissionResponsePayload<?>> getSubmission(@PathVariable String submissionId) throws FailedSubmissionException{
    //     SubmissionResponsePayload<?> payload = mainService.getSubmission(submissionId);
    //     return ResponseEntity.ok().body(payload);
    // }

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

    @PostMapping("/user/work-experience")
    public ResponseEntity<String> addWorkExperience(@Valid @RequestBody UserWorkExperience experience) {
        mainService.addWorkExperience(experience);
        return ResponseEntity.ok().body("Work experience added successfully");
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

    @GetMapping("/skill-tags")
    public ResponseEntity<List<String>> getSkillTags() {
        List<String> labels = Arrays.stream(SkillTags.values())
                .map(SkillTags::getSkillName)
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
