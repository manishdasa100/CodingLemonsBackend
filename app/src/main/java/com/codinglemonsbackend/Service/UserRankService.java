package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.UserRankDto;
import com.codinglemonsbackend.Entities.UserRank;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Properties.S3Buckets;
import com.codinglemonsbackend.Repository.UserRankRepository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserRankService {

    @Autowired
    private UserRankRepository userRankRepository;

    
    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private S3Buckets s3Buckets;
    
    public List<UserRank> ranks;

    @PostConstruct
    private void loadAllRanks() {
        ranks = userRankRepository.getAllRanks();

        // Sorting ranks in descending order based on milestone points
        ranks.sort((rank1, rank2) -> rank2.getMilestonePoints() - rank1.getMilestonePoints());
    }

    public UserRank getInitialRank() {
        // Getting the rank with the least milestone points
        return ranks.get(ranks.size()-1);
    }
    
    public Boolean checkIfRankExistByName(String name) {
        return ranks.stream().anyMatch(rank -> rank.getRankName().equals(name));
    }

    public Boolean checkIfRankExistByMilestonePoints(Integer points) {
        return ranks.stream().anyMatch(rank -> rank.getMilestonePoints().equals(points));
    }

    public UserRank createUserRank(UserRankDto newRankDetails, byte[] badgeImageBytes) throws FileUploadFailureException  {

        if (checkIfRankExistByName(newRankDetails.getRankName())) {
            throw new IllegalArgumentException("Rank with same name already exists");
        }

        if (checkIfRankExistByMilestonePoints(newRankDetails.getMilestonePoints())) {
            throw new IllegalArgumentException("Rank with same milestone points already exists");
        }

        UserRank newUserRank = UserRank.builder()
                                        .rankName(newRankDetails.getRankName())
                                        .milestonePoints(newRankDetails.getMilestonePoints())
                                        .build();
        
        String rankBadgeId = UUID.randomUUID().toString();
        String s3Key = "static/rankBadges/%s".formatted(rankBadgeId);
        Boolean s3Uploaded = false;
        
        try {
            s3Service.putObject(
                s3Buckets.getImages(), 
                s3Key, 
                badgeImageBytes
            );

            s3Uploaded = true;
            newUserRank.setRankBadgeId(rankBadgeId);
            return userRankRepository.saveUserRank(newUserRank);
        } catch (Exception exception) {

            if (s3Uploaded) {

                // Rollback S3
                try {
                    s3Service.deleteObject(s3Buckets.getImages(), s3Key);
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

    public UserRank getUserRank(Integer points) {
        for (UserRank rank : ranks) {
            if (rank.getMilestonePoints() <= points) {
                return rank;
            }
        }

        return null;
    }

    public void updateUserRank() {
        //TODO: Implement updateUserRank
    }

    public Long deleteUserRank(String rankId) {
        return userRankRepository.deleteUserRank(null);
    }
}
