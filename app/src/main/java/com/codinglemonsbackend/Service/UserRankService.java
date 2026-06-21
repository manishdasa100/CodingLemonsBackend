package com.codinglemonsbackend.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.UserRankDto;
import com.codinglemonsbackend.Entities.UserRank;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Properties.S3Properties;
import com.codinglemonsbackend.Repository.UserRankRepository;
import com.codinglemonsbackend.Utils.URIUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserRankService {

    @Autowired
    private UserRankRepository userRankRepository;
  
    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private S3Properties s3Properties;

    @Value("${assets.domain}")
    private String ASSETS_DOMAIN;

    private static final String ASSET_BASE_PATH = "static/rankBadges";
    
    @Cacheable(cacheNames = RedisService.USER_RANKS)
    private List<UserRankDto> loadAllRanks() {
        return userRankRepository.getAllRanks().stream().map(rank -> new UserRankDto(
            rank.getRankName(),
            rank.getMilestonePoints(),
            URIUtils.createURI(ASSETS_DOMAIN, ASSET_BASE_PATH, rank.getRankBadgeId()).toString()
        )).sorted(Comparator.comparing(UserRankDto::getMilestonePoints))
        .collect(Collectors.toList());
    }

    public UserRankDto getInitialRank() {
        List<UserRankDto> availableUserRanks = loadAllRanks();
        if (availableUserRanks.isEmpty()) throw new RuntimeException("No ranks found!!"); 
        return availableUserRanks.get(0);
    }
    
    public Optional<UserRankDto> getRankByName(String name) {
        return loadAllRanks().stream().filter(rank -> rank.getRankName().equals(name)).findFirst();
    }

    public Optional<UserRankDto> getRankByMilestonePoints(Integer points) {
        return loadAllRanks().stream().filter(rank -> rank.getMilestonePoints().equals(points)).findFirst();
    }

    public UserRank createUserRank(UserRankDto newRankDetails, byte[] badgeImageFile) throws FileUploadFailureException  {

        if (getRankByName(newRankDetails.getRankName()).isPresent()) {
            throw new IllegalArgumentException("Rank with same name already exists");
        }

        if (getRankByMilestonePoints(newRankDetails.getMilestonePoints()).isPresent()) {
            throw new IllegalArgumentException("Rank with same milestone points already exists");
        }

        UserRank newUserRank = UserRank.builder()
                                        .rankName(newRankDetails.getRankName())
                                        .milestonePoints(newRankDetails.getMilestonePoints())
                                        .build();
        
        String rankBadgeId = UUID.randomUUID().toString();
        String s3Key = ASSET_BASE_PATH + "/" + rankBadgeId;
        Boolean s3Uploaded = false;
        
        try {
            s3Service.putObject(
                s3Properties.getBucket(), 
                s3Key, 
                badgeImageFile
            );

            s3Uploaded = true;
            newUserRank.setRankBadgeId(rankBadgeId);
            return userRankRepository.saveUserRank(newUserRank);
        } catch (Exception exception) {

            if (s3Uploaded) {

                // Rollback S3
                try {
                    s3Service.deleteObject(s3Properties.getBucket(), s3Key);
                } catch (Exception deleteException) {
                    log.error("Failed to delete orphaned rank badge with id {} from S3 with message", rankBadgeId, deleteException);
                }

            
                log.error("Failed to save rank entity {} to database with message:", newUserRank.getRankName(), exception);
                throw exception;
            } else {
                log.error("Failed to upload rank badge to S3", exception);
                throw new FileUploadFailureException("Rank badge upload to S3 failed with message: " + exception.getMessage());
            }
        }

    }

    public String getUserRank(Integer points) {
        List<UserRankDto> ranks = loadAllRanks();
        for (UserRankDto rank : ranks) {
            if (rank.getMilestonePoints() <= points) {
                return rank.getRankName();
            }
        }
        return null;
    }

    public void updateUserRank(String rankName, Map<String, Object> updateProperties) {
        userRankRepository.updateUserRank(rankName, updateProperties);
    }

    public Long deleteUserRank(String rankName) {
        return userRankRepository.deleteUserRank(rankName);
    }
}
