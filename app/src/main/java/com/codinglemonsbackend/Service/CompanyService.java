package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.CompanyDto;
import com.codinglemonsbackend.Entities.Company;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Properties.S3Properties;
import com.codinglemonsbackend.Repository.CompanyRepository;
import com.codinglemonsbackend.Utils.URIUtils;
import com.github.slugify.Slugify;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private S3Service s3Service;

    @Autowired
    private S3Properties s3Properties;

    @Autowired
    private Slugify slugify;

    @Value("${assets.domain}")
    private String ASSETS_DOMAIN;

    private List<CompanyDto> allCompanies = null;

    public static final String ASSET_BASE_PATH = "static/company/logos";

    private void loadAllCompanies() {
        if (this.allCompanies == null) {
            this.allCompanies = companyRepository.getAllCompanies().stream().map(company -> {
                CompanyDto companyDto = new CompanyDto(company.getName(), company.getSlug(), company.getWebsiteLink());
                if (company.getCompanyLogoId() != null) {
                    String companyLogoUri = URIUtils.createURI(ASSETS_DOMAIN, ASSET_BASE_PATH, company.getCompanyLogoId()).toString();
                    companyDto.setCompanyLogoUri(companyLogoUri);
                } 
                return companyDto;
            }).collect(Collectors.toList());
        }
    }

    public CompanyDto getCompanyDetails(String name) {
        if (this.allCompanies == null) loadAllCompanies(); 
        try{
            CompanyDto company = allCompanies.stream().filter(e -> e.getName().equals(name)).findFirst().get();
            return company;
        } catch(NoSuchElementException e) {
            throw new NoSuchElementException("No company found with name {}".formatted(name));
        }
    }

    public Set<CompanyDto> getValidTags(List<String> companySlugs) {
        if (this.allCompanies == null) loadAllCompanies();
        // Return the matching tags
        Set<CompanyDto> matchingCompanies = this.allCompanies.stream()
            .filter(company -> companySlugs.contains(company.getSlug()))
            .collect(Collectors.toSet());
        return matchingCompanies;
    }

    public void addCompany(Company company) {
        if (this.allCompanies == null) loadAllCompanies();
        String companyName = company.getName();
        Set<String> companyNamesList = allCompanies.stream().map(e -> e.getName()).collect(Collectors.toSet());

        if (companyNamesList.contains(companyName)) {
            throw new IllegalArgumentException("Company with same name already exist");
        }

        String slug = slugify.slugify(companyName);
        company.setSlug(slug);

        companyRepository.saveCompany(company);
    }

    public void addCompany(Company company, byte[] logoBytes) throws FileUploadFailureException {

        String assetId = UUID.randomUUID().toString();
        String s3Key = ASSET_BASE_PATH + "/" + assetId;

        Boolean s3Uploaded = false;

        try {
            s3Service.putObject(
                s3Properties.getBucket(), 
                s3Key, 
                logoBytes
            );
            s3Uploaded = true;
            company.setCompanyLogoId(assetId);
            addCompany(company);
        } catch (Exception exception) {

            if (s3Uploaded) {

                // Rollback S3
                try {
                    s3Service.deleteObject(s3Properties.getBucket(), s3Key);
                } catch (Exception deleteException) {
                    log.error("Failed to delete orphaned company logo with id {} from S3 with message", assetId, deleteException);
                }

                log.error("Failed to company {} to database with message:", company.getName(), exception);
                throw exception;
            } else {
                log.error("Failed to upload rank badge to S3", exception);
                throw new FileUploadFailureException("Upload of company logo to S3 failed with message: " + exception.getMessage());
            }
        }
    }
}
