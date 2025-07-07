package com.codinglemonsbackend.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.ProblemUpdateDto;
import com.codinglemonsbackend.Dto.RegistryOperationResult;
import com.codinglemonsbackend.Dto.UserRankDto;
import com.codinglemonsbackend.Entities.CompanyTag;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.TopicTag;
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
    private CompanyRepository companyRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRankService userRankService;

    @Autowired
    private Slugify slugify;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    public ProblemEntity addProblem(ProblemDto payload) throws Exception {
        System.out.println("--------PROBLEM PAYLOAD--------------");
        System.out.println(payload);

        Set<String> topicSlugs = payload.getTopics().stream().map(topic -> topic.getSlug()).collect(Collectors.toSet());
        List<TopicTag> validTopics = topicRepository.getValidTags(topicSlugs);
        if(validTopics.isEmpty()) throw new IllegalArgumentException("No matching topics were found. Please provide valid topics.");
        
        payload.setTopics(new HashSet<>(validTopics));

        if (payload.getCompanies() != null) {
            Set<String> companySlugs = payload.getCompanies().stream().map(company -> company.getSlug()).collect(Collectors.toSet());
            List<CompanyTag>validCompanies = companyRepository.getValidTags(companySlugs);
            payload.setCompanies(new HashSet<>(validCompanies));
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

    public void publishProblem(Integer problemId) {
        // TODO: implement publish problem
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
