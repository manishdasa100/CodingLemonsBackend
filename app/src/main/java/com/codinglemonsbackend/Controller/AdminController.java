package com.codinglemonsbackend.Controller;

import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Entities.CompanyTag;
import com.codinglemonsbackend.Entities.TopicTag;
import com.codinglemonsbackend.Payloads.CreateProblemRequestPayload;
import com.codinglemonsbackend.Service.AdminServiceImpl;

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
    public boolean addProblem(@Valid @RequestBody CreateProblemRequestPayload payload) throws Exception{

        //ProblemDto problemDto = modelMapper.map(payload, ProblemDto.class);

        adminService.addProblem(payload);
        
        return true;
    }

    @PutMapping("/problem/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> updateProblem(@PathVariable Integer id, @RequestBody ProblemUpdateDto updateMetadata){
        long updatedDocumentCount = adminService.updateProblem(id, updateMetadata);
        return ResponseEntity.ok().body(String.format("Modified %d documents", updatedDocumentCount));
    } 
    
    @DeleteMapping("/problem/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public void deleteProblemById(@PathVariable Integer id){
        adminService.deleteProblemById(id);
    }

    @DeleteMapping("/problem/delete/all")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public void clearAllProblems(){
        adminService.clearAllProblems();
    }

    @PostMapping("/companyTag/create")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public void addCompanyTag(@Valid @RequestBody CompanyTag companyTag){
        adminService.createCompanyTag(companyTag);
    }

    @PostMapping("topicTag/create")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public void addTopicTag(@Valid @RequestBody TopicTag topicTag){
        adminService.createTopicTag(topicTag);
    }
}
