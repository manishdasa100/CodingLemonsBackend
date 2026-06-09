package com.codinglemonsbackend.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
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

    public List<CompanyDto> getAllCompanies() {
        if (this.allCompanies == null) loadAllCompanies();
        return this.allCompanies;
    }

    public CompanyDto getCompanyDetailsBySlug(String slug) {
        if (this.allCompanies == null) loadAllCompanies(); 
        try{
            CompanyDto company = allCompanies.stream().filter(e -> e.getSlug().equals(slug)).findFirst().get();
            return company;
        } catch(NoSuchElementException e) {
            throw new NoSuchElementException("No company found with name {}".formatted(slug));
        }
    }

    public Boolean isValidCompany(String companySlug) {
        if (this.allCompanies == null) loadAllCompanies();
        return this.allCompanies.stream().anyMatch(company -> company.getSlug().equals(companySlug));
    }

    public Map<String, CompanyDto> getCompaniesBySlugMap(List<String> slugs) {
        if (this.allCompanies == null) loadAllCompanies();
        Set<String> slugSet = new HashSet<>(slugs);
        return this.allCompanies.stream()
                .filter(c -> slugSet.contains(c.getSlug()))
                .collect(Collectors.toMap(CompanyDto::getSlug, c -> c));
    }

    public Set<CompanyDto> getValidTags(Set<String> companySlugs) {
        if (this.allCompanies == null) loadAllCompanies();
        // Return the matching tags
        Set<CompanyDto> matchingCompanies = this.allCompanies.stream()
            .filter(company -> companySlugs.contains(company.getSlug()))
            .collect(Collectors.toSet());
        return matchingCompanies;
    }

    private void checkDuplicate(String companyName) {
        if (this.allCompanies == null) loadAllCompanies();
        boolean exists = allCompanies.stream().anyMatch(c -> c.getName().equals(companyName));
        if (exists) {
            throw new IllegalArgumentException("Company with same name already exists");
        }
    }

    public void addCompany(Company company) {
        checkDuplicate(company.getName());
        company.setSlug(slugify.slugify(company.getName()));
        companyRepository.saveCompany(company);
    }

    public void addCompany(Company company, byte[] logoBytes) throws FileUploadFailureException {

        checkDuplicate(company.getName());

        String assetId = slugify.slugify(company.getName());
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
                    log.error("Failed to delete orphaned company logo with id {} from S3", assetId, deleteException);
                }

                log.error("Failed to save company {} to database", company.getName(), exception);
                throw exception;
            } else {
                log.error("Failed to upload company logo to S3", exception);
                throw new FileUploadFailureException("Upload of company logo to S3 failed with message: " + exception.getMessage());
            }
        }
    }
}
