package com.codinglemonsbackend.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.RegistryOperationResult;
import com.codinglemonsbackend.Dto.UserRankDto;
import com.codinglemonsbackend.Entities.Company;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.Topic;
import com.codinglemonsbackend.Entities.UserRank;
import com.codinglemonsbackend.Events.ProblemRegistryUpdatedEvent;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Repository.CompanyRepository;
import com.codinglemonsbackend.Repository.IRegistryService;
import com.codinglemonsbackend.Repository.TopicRepository;
import com.codinglemonsbackend.Utils.ImageUtils;
import com.codinglemonsbackend.Utils.ImageUtils.ImageDimension;
import com.github.slugify.Slugify;


@Service
public class AdminServiceImpl {

    @Autowired
    private ProblemRepositoryService problemRepositoryService;

    @Autowired
    private RegistryServiceDispatcher registryServiceDispatcher;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRankService userRankService;

    @Autowired
    private Slugify slugify;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ModelMapper moddModelMapper;
    
    public ProblemEntity addProblem(ProblemDto payload) throws Exception {
        List<String> topicSlugs = payload.getTopics().stream().map(topic -> topic.getSlug()).collect(Collectors.toList());
        Set<Topic> validTopics = topicRepository.getValidTags(topicSlugs);
        if(validTopics.isEmpty()) throw new IllegalArgumentException("No matching topics were found. Please provide valid topics.");
        
        payload.setTopics(validTopics);

        if (payload.getCompanies() != null) {
            List<String> companySlugs = payload.getCompanies().stream().map(company -> company.getSlug()).collect(Collectors.toList());
            Set<CompanyDto> validCompanies = companyService.getValidTags(companySlugs);
            payload.setCompanies(validCompanies);
        }

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
    
    public RegistryOperationResult addItemsInRegistry(Integer problemId, Object data, String registryType) {
        IRegistryService<?> registryService = registryServiceDispatcher.getService(registryType);
        RegistryOperationResult result = registryService.addItemsInRegistry(problemId, data);
        applicationEventPublisher.publishEvent(new ProblemRegistryUpdatedEvent(this, result.getProblemId()));
        return result;
    }
    
    public RegistryOperationResult updateRegistry(String registryId, Object data, String registryType) {
        IRegistryService<?> registryService = registryServiceDispatcher.getService(registryType);
        RegistryOperationResult result = registryService.updateItemsInRegistry(registryId, data);
        applicationEventPublisher.publishEvent(new ProblemRegistryUpdatedEvent(this, result.getProblemId()));
        return result;
    }

    public RegistryOperationResult removeItemFromRegistry(String registryId, Object data, String registryType) {
        IRegistryService<?> registryService = registryServiceDispatcher.getService(registryType);
        RegistryOperationResult result = registryService.removeItemFromRegistry(registryId, data);
        applicationEventPublisher.publishEvent(new ProblemRegistryUpdatedEvent(this, result.getProblemId()));
        return result;
    }

    public RegistryOperationResult deleteRegistry(String registryType, String registryId) {
        IRegistryService<?> registryService = registryServiceDispatcher.getService(registryType);
        RegistryOperationResult result = registryService.deleteRegistry(registryId);
        applicationEventPublisher.publishEvent(new ProblemRegistryUpdatedEvent(this, result.getProblemId()));
        return result;
    }

    public List<String> getSupportedRegistryTypes() {
        return registryServiceDispatcher.getSupportedRegistryTypes();
    }
}
