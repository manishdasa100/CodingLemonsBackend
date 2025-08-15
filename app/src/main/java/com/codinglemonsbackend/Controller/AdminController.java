package com.codinglemonsbackend.Controller;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.RegistryOperationResult;
import com.codinglemonsbackend.Dto.UserRankDto;
import com.codinglemonsbackend.Entities.Company;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.Topic;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Service.AdminServiceImpl;
import com.codinglemonsbackend.Utils.ImageUtils;

import jakarta.validation.Valid;

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

    @GetMapping("/registry/supported")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<String>> getSupportedRegistryTypes() {
        List<String> supportedRegistryTypes = adminService.getSupportedRegistryTypes();
        return ResponseEntity.ok().body(supportedRegistryTypes);
    }

    @PostMapping("/registry/{registryType}/add/{problemId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<RegistryOperationResult> addItemsInRegistry(
        @PathVariable String registryType,
        @PathVariable Integer problemId, 
        @RequestBody Object registryData) throws Exception
    {
        RegistryOperationResult response = adminService.addItemsInRegistry(problemId, registryData, registryType);
        
        return ResponseEntity.ok().body(response);
    }

    @PutMapping("/registry/{registryType}/update/{registryId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<RegistryOperationResult> updateItemsInRegistry(
        @PathVariable String registryType,
        @PathVariable String registryId, 
        @RequestBody Object registryData) 
    {
        RegistryOperationResult result = adminService.updateRegistry(registryId, registryData, registryType);
        return ResponseEntity.ok().body(result);
    }

    @DeleteMapping("/registry/{registryType}/removeItem/{registryId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<RegistryOperationResult> removeItemFromRegistry(
        @PathVariable String registryType,
        @PathVariable String registryId, 
        @RequestBody Object registryData) 
    {
        RegistryOperationResult result = adminService.removeItemFromRegistry(registryId, registryData, registryType);
        return ResponseEntity.ok().body(result);
    }

    @DeleteMapping("/registry/{registryType}/delete/{registryId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<RegistryOperationResult> deleteRegistry(
        @PathVariable String registryType,
        @PathVariable String registryId) 
    {
        RegistryOperationResult result = adminService.deleteRegistry(registryType, registryId);
        return ResponseEntity.ok().body(result);    
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

    @PostMapping("/topicTag/create")
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
        // Validate the file extension
        // List<String> validImageExtensions = ImageUtils.validImageUploadExtensions;
        // String fileExtension = FilenameUtils.getExtension(rankBadgeImageFile.getOriginalFilename());

        // if (fileExtension != null && !validImageExtensions.contains(fileExtension)) {
        //     throw new IllegalArgumentException(String.format("Unsupported file extension: %s. Please upload one of %s", fileExtension, validImageExtensions));
        // }

        if (!isValidImageFile(rankBadgeImageFile.getOriginalFilename())) {
            throw new IllegalArgumentException(String.format("Unsupported file extension for file %s. Please upload one of %s", rankBadgeImageFile.getOriginalFilename(), "jpg, png, jpeg"));
        }
        adminService.createUserRank(rankDetails, rankBadgeImageFile);
        return ResponseEntity.ok().body("User rank created");
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
