package com.codinglemonsbackend.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Entities.CompanyTag;
import com.codinglemonsbackend.Entities.ProblemExecutionDetails;
import com.codinglemonsbackend.Entities.TopicTag;
import com.codinglemonsbackend.Payloads.CreateProblemRequestPayload;
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
    
    public void addProblem(CreateProblemRequestPayload payload) throws Exception {
        System.out.println("--------PROBLEM PAYLOAD--------------");
        System.out.println(payload);

        Set<String> topicSlugs = payload.getTopics();
        Set<TopicTag> validTopics = topicRepository.getValidTags(topicSlugs);
        if(validTopics.isEmpty()) throw new Exception("No matching topics were found. Please provide valid topics.");

        Set<CompanyTag> validCompanies = null;
        if (payload.getCompanies() != null) {
            Set<String> companySlugs = payload.getCompanies();
            validCompanies = companyRepository.getValidTags(companySlugs);
        }

        ProblemDto problemDto = ProblemDto.builder()
                                    .title(payload.getTitle())
                                    .description(payload.getDescription())
                                    .constraints(payload.getConstraints())
                                    .examples(payload.getExamples())
                                    .difficulty(payload.getDifficulty())
                                    .codeSnippets(payload.getCodeSnippets())
                                    .topics(validTopics)
                                    .companies(validCompanies)
                                    .build();

        ProblemExecutionDetails executionDetails = ProblemExecutionDetails.builder()
                                                    .cpuTimeLimit(payload.getCpuTimeLimit())
                                                    .memoryLimit(payload.getMemoryLimit())
                                                    .stackLimit(payload.getStackLimit())
                                                    .testCasesWithExpectedOutputs(payload.getTestCasesWithExpectedOutputs())
                                                    .driverCodes(payload.getDriverCodes())
                                                    .build();
        
        problemRepositoryService.addProblem(problemDto, executionDetails);
    }

    public long updateProblem(Integer problemId, ProblemUpdateDto updateMetadata) {
        return problemRepositoryService.updateProblem(problemId, updateMetadata);
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
