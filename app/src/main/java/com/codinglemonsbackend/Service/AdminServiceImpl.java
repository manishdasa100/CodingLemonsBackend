package com.codinglemonsbackend.Service;

import java.io.IOException;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.DriverCodeRegistryDto;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.RegistryOperationResult;
import com.codinglemonsbackend.Dto.TestcaseRegistryDto;
import com.codinglemonsbackend.Dto.UserRankDto;
import com.codinglemonsbackend.Entities.Company;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.Topic;
import com.codinglemonsbackend.Entities.UserRank;
import com.codinglemonsbackend.Events.ProblemRegistryUpdatedEvent;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Repository.DriverCodeRepository;
import com.codinglemonsbackend.Repository.TestcaseRepository;
import com.codinglemonsbackend.Repository.TopicRepository;
import com.codinglemonsbackend.Utils.ImageUtils;
import com.codinglemonsbackend.Utils.ImageUtils.ImageDimension;
import com.github.slugify.Slugify;


@Service
public class AdminServiceImpl {

    @Autowired
    private ProblemRepositoryService problemRepositoryService;

    @Autowired
    private TestcaseRepository testcaseRepository;

    @Autowired
    private DriverCodeRepository driverCodeRepository;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRankService userRankService;

    @Autowired
    private Slugify slugify;

    @Autowired
    private ProblemOfTheDayService problemOfTheDayService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ModelMapper moddModelMapper;
    
    public ProblemEntity addProblem(ProblemDto payload) throws Exception {
        if(payload.getTopics().isEmpty()) throw new IllegalArgumentException("No matching topics were found. Please provide valid topics.");

        ProblemEntity savedEntity =  problemRepositoryService.addProblem(payload);

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

    public String publishProblem(Integer problemId) {
        long updatedDocumentCount = problemRepositoryService.updateProblem(problemId, 
        new ProblemUpdateDto(Map.of("status", ProblemStatus.PUBLISHED.name())));
        return updatedDocumentCount > 0 ? "Problem published successfully" : "Problem is already published";
    }

    public void createCompanyTag(CompanyDto companyDto, MultipartFile companyLogoImageFile) throws FileUploadFailureException, IOException{
        Company company = moddModelMapper.map(companyDto, Company.class);
        if (companyLogoImageFile == null) {
            companyService.addCompany(company);
            return;
        }

        if (companyLogoImageFile.isEmpty()) {
            throw new IllegalArgumentException("Provided image file is empty");
        }
        
        byte[] resizedImageBytes = ImageUtils.resizeImage(companyLogoImageFile, ImageDimension.SQUARE_SMALL);
        companyService.addCompany(company, resizedImageBytes);
    }

    public void createTopicTag(Topic topicTag) {
        String topicName = topicTag.getName();
        String slug = slugify.slugify(topicName);
        topicTag.setSlug(slug);
        topicRepository.addTopicTag(topicTag);
    }

    public String createUserRank(UserRankDto newRankDetails, MultipartFile rankIconImageFile) throws IOException, FileUploadFailureException{
        String rankNameCapitalized = newRankDetails.getRankName().toUpperCase();
        newRankDetails.setRankName(rankNameCapitalized);
        byte[] resizedImageBytes = ImageUtils.resizeImage(rankIconImageFile, ImageDimension.SQUARE_SMALL); 
        UserRank savedRank = userRankService.createUserRank(newRankDetails, resizedImageBytes);
        return savedRank.getRankName();
    }
    
    public RegistryOperationResult syncTestcases(Integer problemId, TestcaseRegistryDto dto) {
        RegistryOperationResult result = testcaseRepository.syncItems(problemId, dto);
        applicationEventPublisher.publishEvent(new ProblemRegistryUpdatedEvent(this, problemId));
        return result;
    }

    public RegistryOperationResult deleteTestcaseRegistry(Integer problemId) {
        RegistryOperationResult result = testcaseRepository.deleteByProblemId(problemId);
        applicationEventPublisher.publishEvent(new ProblemRegistryUpdatedEvent(this, problemId));
        return result;
    }

    public RegistryOperationResult syncDriverCodes(Integer problemId, DriverCodeRegistryDto dto) {
        RegistryOperationResult result = driverCodeRepository.syncItems(problemId, dto);
        applicationEventPublisher.publishEvent(new ProblemRegistryUpdatedEvent(this, problemId));
        return result;
    }

    public RegistryOperationResult deleteDriverCodeRegistry(Integer problemId) {
        RegistryOperationResult result = driverCodeRepository.deleteByProblemId(problemId);
        applicationEventPublisher.publishEvent(new ProblemRegistryUpdatedEvent(this, problemId));
        return result;
    }

    public void overrideProblemOfTheDay(Integer problemId) {
        problemOfTheDayService.overrideProblemOfTheDay(problemId);
    }
}
