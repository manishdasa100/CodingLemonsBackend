package com.codinglemonsbackend.Controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.UserRankDto;
import com.codinglemonsbackend.Entities.CompanyTag;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.TopicTag;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Payloads.CreateProblemRequestPayload;
import com.codinglemonsbackend.Service.AdminServiceImpl;
import com.codinglemonsbackend.Utils.ImageUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api/v1/admin")
public class AdminController {

    @Autowired
    private ModelMapper modelMapper;

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
    public ResponseEntity<String> addProblem(@Valid @RequestBody CreateProblemRequestPayload payload) throws Exception{
        ProblemEntity savedEntity = adminService.addProblem(payload);
        return ResponseEntity.ok().body(String.format("Problem created with id {}", savedEntity.getId()));
    }

    @PutMapping("/problem/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> updateProblem(@PathVariable Integer id, @RequestBody ProblemUpdateDto updateMetadata){
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

    @PostMapping("/companyTag/create")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> addCompanyTag(@Valid @RequestBody CompanyTag companyTag){
        adminService.createCompanyTag(companyTag);
        return ResponseEntity.ok().body("Company tag created");
    }

    @PostMapping("/topicTag/create")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> addTopicTag(@Valid @RequestBody TopicTag topicTag){
        adminService.createTopicTag(topicTag);
        return ResponseEntity.ok().body("Topic tag created");
    }

    @PostMapping(value = "/userRank/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> createUserRank(@Valid @RequestPart UserRankDto rankDetails,
                                                 @RequestPart MultipartFile rankBadgeImageFile) throws FileUploadFailureException, IOException{
        // Validate the file extension
        List<String> validImageExtensions = ImageUtils.validImageExtensions;
        String fileExtension = FilenameUtils.getExtension(rankBadgeImageFile.getOriginalFilename());

        if (fileExtension != null && !validImageExtensions.contains(fileExtension)) {
            throw new IllegalArgumentException(String.format("Unsupported file extension: %s. Please upload one of %s", fileExtension, validImageExtensions));
        }
        
        adminService.createUserRank(rankDetails, rankBadgeImageFile);
        
        return ResponseEntity.ok().body("User rank created");
    }
}
