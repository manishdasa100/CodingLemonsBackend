package com.codinglemonsbackend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Entities.CompanyTag;
import com.codinglemonsbackend.Entities.TopicTag;
import com.codinglemonsbackend.Repository.CompanyRepository;
import com.codinglemonsbackend.Repository.TopicRepository;
import com.github.slugify.Slugify;

@Service
public class AdminServiceImpl {

    @Autowired
    private ProblemRepositoryService problemRepositoryService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private Slugify slugify;
    
    public void addProblem(ProblemDto problemDto) throws Exception {
        System.out.println("--------PROBLEM DTO--------------");
        System.out.println(problemDto);
        problemRepositoryService.addProblem(problemDto);
    }

    public void updateProblem(Integer problemId, ProblemUpdateDto updateMetadata) {
        problemRepositoryService.updateProblem(problemId, updateMetadata);
    }

    public void deleteProblemById(Integer problemId) {
        problemRepositoryService.deleteProblemById(problemId);
    }

    public void clearAllProblems() {
        problemRepositoryService.removeAllProblems();
    }

    public void createCompanyTag(CompanyTag companyTag){
        String companyName = companyTag.getName();
        String slug = slugify.slugify(companyName);
        companyTag.setSlug(slug);
        companyRepository.addCompanyTag(companyTag);
    }

    public void createTopicTag(TopicTag topicTag) {
        String topicName = topicTag.getName();
        String slug = slugify.slugify(topicName);
        topicTag.setSlug(slug);
        topicRepository.addTopicTag(topicTag);
    }
}
