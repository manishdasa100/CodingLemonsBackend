package com.codinglemonsbackend.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Entities.CompanyTag;
import com.codinglemonsbackend.Entities.TopicTag;
import com.codinglemonsbackend.Service.AdminServiceImpl;

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
    public boolean addProblem(@Valid @RequestBody ProblemDto problemDto) throws Exception{

        adminService.addProblem(problemDto);
        
        return true;
    }

    @PutMapping("/problem/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public void updateProblem(@PathVariable Integer id, @RequestBody ProblemUpdateDto updateMetadata){
        adminService.updateProblem(id, updateMetadata);
    } 
    
    @DeleteMapping("/problem/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public void deleteProblemById(@PathVariable Integer id){
        adminService.deleteProblemById(id);
    }

    @DeleteMapping("/removeAllProblems")
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
