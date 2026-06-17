package com.codinglemonsbackend.Controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.BadgeDto;
import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.DriverCodeRegistryDto;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemListDto;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.RegistryOperationResult;
import com.codinglemonsbackend.Dto.TestcaseRegistryDto;
import com.codinglemonsbackend.Dto.UserRankDto;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.Topic;
import com.codinglemonsbackend.Exceptions.DuplicateResourceException;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Service.AdminServiceImpl;
import com.codinglemonsbackend.Utils.ImageUtils;

import jakarta.validation.Valid;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestController
@RequestMapping(value = "/api/v1/admin")
public class AdminController {

    @Autowired
    private AdminServiceImpl adminService;

    @PostMapping("/add/admin")
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public void addAdmin(){
        System.out.println("Adding admin");

        // TODO: implement add admin method
    }

    @PostMapping("/problem/add")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> addProblem(@Valid @RequestBody ProblemDto payload) throws Exception{
        ProblemEntity savedEntity = adminService.addProblem(payload);
        return ResponseEntity.ok().body(String.format("Problem created with id %d", savedEntity.getId()));
    }

    @PutMapping("/problem/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> updateProblem(
        @PathVariable Integer id, 
        @Valid @RequestBody ProblemUpdateDto updateMetadata)
    {
        long updatedDocumentCount = adminService.updateProblem(id, updateMetadata);
        return ResponseEntity.ok().body(String.format("Modified %d documents", updatedDocumentCount));
    } 
    
    @DeleteMapping("/problem/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> deleteProblemById(@PathVariable Integer id){
        adminService.deleteProblemById(id);
        return ResponseEntity.ok().body(String.format("Problem with id {} deleted", id));
    }

    @DeleteMapping("/problem/delete/all")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> clearAllProblems(){
        adminService.clearAllProblems();
        return ResponseEntity.ok().body("All problems deleted");
    }

    @PutMapping("/problem/publish/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> publishProblem(@PathVariable Integer id){
        String result = adminService.publishProblem(id);
        return ResponseEntity.ok().body(result);
    }

    // --- Testcase endpoints ---

    @PatchMapping("/testcases/{problemId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<RegistryOperationResult> syncTestcases(
        @PathVariable Integer problemId,
        @RequestBody TestcaseRegistryDto dto)
    {
        return ResponseEntity.ok(adminService.syncTestcases(problemId, dto));
    }

    @DeleteMapping("/testcases/{problemId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<RegistryOperationResult> deleteTestcaseRegistry(
        @PathVariable Integer problemId)
    {
        return ResponseEntity.ok(adminService.deleteTestcaseRegistry(problemId));
    }

    // --- Driver code endpoints ---

    @PatchMapping("/driverCodes/{problemId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<RegistryOperationResult> syncDriverCodes(
        @PathVariable Integer problemId,
        @RequestBody DriverCodeRegistryDto dto)
    {
        return ResponseEntity.ok(adminService.syncDriverCodes(problemId, dto));
    }

    @DeleteMapping("/driverCodes/{problemId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<RegistryOperationResult> deleteDriverCodeRegistry(
        @PathVariable Integer problemId)
    {
        return ResponseEntity.ok(adminService.deleteDriverCodeRegistry(problemId));
    }


    @PostMapping(value = "/company/create" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> addCompany(
        @Valid @RequestPart CompanyDto company,
        @RequestPart(value = "companyLogo", required = false) MultipartFile companyLogoImageFile
    ) throws FileUploadFailureException, IOException
    {
        // List<String> validImageExtensions = ImageUtils.validImageUploadExtensions;
        // String fileExtension = FilenameUtils.getExtension(companyLogoImageFile.getOriginalFilename());

        // if (fileExtension != null && !validImageExtensions.contains(fileExtension)) {
        //     throw new IllegalArgumentException(String.format("Unsupported file extension: %s. Please upload one of %s", fileExtension, validImageExtensions));
        // }

        if (companyLogoImageFile != null && !isValidImageFile(companyLogoImageFile.getOriginalFilename())) {
            throw new IllegalArgumentException(String.format("Unsupported file extension for file %s. Please upload one of %s", companyLogoImageFile.getOriginalFilename(), "jpg, png, jpeg"));
        }
        adminService.createCompanyTag(company, companyLogoImageFile);
        return ResponseEntity.ok().body("Company tag created");
    }

    @PutMapping("/potd/{problemId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> overrideProblemOfTheDay(@PathVariable Integer problemId) {
        adminService.overrideProblemOfTheDay(problemId);
        return ResponseEntity.ok().body("Problem of the day set to problem " + problemId);
    }

    // --- Badge endpoints ---

    @PostMapping(value = "/badge/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<BadgeDto> createBadge(
        @Valid @RequestPart BadgeDto badge,
        @RequestPart MultipartFile badgeImageFile
    ) throws FileUploadFailureException, IOException
    {
        if (!isValidImageFile(badgeImageFile.getOriginalFilename())) {
            throw new IllegalArgumentException(String.format("Unsupported file extension for file %s. Please upload one of %s", badgeImageFile.getOriginalFilename(), "jpg, png, jpeg"));
        }
        BadgeDto created = adminService.createBadge(badge, badgeImageFile);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/badges")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<Map<String, List<BadgeDto>>> getAllBadges() {
        return ResponseEntity.ok(adminService.getAllBadges());
    }

    @DeleteMapping("/badge/delete/{badgeId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> deleteBadge(@PathVariable String badgeId) {
        adminService.deleteBadge(badgeId);
        return ResponseEntity.ok("Badge " + badgeId + " deleted");
    }

    @PostMapping("/topic/create")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> addTopicTag(@Valid @RequestBody Topic topicTag){
        adminService.createTopicTag(topicTag);
        return ResponseEntity.ok().body("Topic tag created");
    }

    @PostMapping(value = "/userRank/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> createUserRank(
        @Valid @RequestPart UserRankDto rankDetails,
        @RequestPart MultipartFile rankBadgeImageFile) throws FileUploadFailureException, IOException
    {
        if (!isValidImageFile(rankBadgeImageFile.getOriginalFilename())) {
            throw new IllegalArgumentException(String.format("Unsupported file extension for file %s. Please upload one of %s", rankBadgeImageFile.getOriginalFilename(), "jpg, png, jpeg"));
        }
        adminService.createUserRank(rankDetails, rankBadgeImageFile);
        return ResponseEntity.ok().body("User rank created");
    }

    @PostMapping("/globalProblemList/create")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> createGlobalProblemList(@Valid @RequestBody ProblemListDto payload) throws DuplicateResourceException, MethodArgumentNotValidException {
        adminService.createGlobalProblemList(payload);
        String listType = payload.getIsStudyPlan() ? "study plan" : "problem list";
        return ResponseEntity.ok().body(String.format("Global %s created", listType));
    }

    private Boolean isValidImageFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty!!");
        }
        List<String> validImageExtensions = ImageUtils.validImageUploadExtensions;
        String fileExtension = FilenameUtils.getExtension(filename);
        if (fileExtension == null) {
            return false;
        }
        if (fileExtension != null && !validImageExtensions.contains(fileExtension)) {
            return false;
        }
        return true;
    }
}
