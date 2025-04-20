package com.codinglemonsbackend.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.UserRankDto;
import com.codinglemonsbackend.Entities.CompanyTag;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.ProblemExecutionDetails;
import com.codinglemonsbackend.Entities.TopicTag;
import com.codinglemonsbackend.Entities.UserRank;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Payloads.CreateProblemRequestPayload;
import com.codinglemonsbackend.Repository.CompanyRepository;
import com.codinglemonsbackend.Repository.TopicRepository;
import com.codinglemonsbackend.Utils.ImageUtils;
import com.codinglemonsbackend.Utils.ImageUtils.ImageDimension;
import com.github.slugify.Slugify;

import jakarta.validation.Valid;

@Service
public class AdminServiceImpl {

    @Autowired
    private ProblemRepositoryService problemRepositoryService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRankService userRankService;

    @Autowired
    private Slugify slugify;
    
    public ProblemEntity addProblem(CreateProblemRequestPayload payload) throws Exception {
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
        
        ProblemEntity savedEntity =  problemRepositoryService.addProblem(problemDto, executionDetails);

        return savedEntity;
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

    public String createUserRank(UserRankDto newRankDetails, MultipartFile rankIconImageFile) throws IOException, FileUploadFailureException{
        
        String rankNameCapitalized = newRankDetails.getRankName().toUpperCase();
        newRankDetails.setRankName(rankNameCapitalized);

        byte[] resizedImageBytes = ImageUtils.resizeImage(rankIconImageFile, ImageDimension.SQUARE); 
        
        UserRank savedRank = userRankService.createUserRank(newRankDetails, resizedImageBytes);
        
        return savedRank.getRankName();
    }
}
