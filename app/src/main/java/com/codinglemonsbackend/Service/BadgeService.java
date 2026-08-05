package com.codinglemonsbackend.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.BadgeDto;
import com.codinglemonsbackend.Dto.BadgeRuleType;
import com.codinglemonsbackend.Dto.EarnedBadgeDto;
import com.codinglemonsbackend.Entities.BadgeEntity;
import com.codinglemonsbackend.Exceptions.FileUploadFailureException;
import com.codinglemonsbackend.Properties.S3Properties;
import com.codinglemonsbackend.Repository.BadgeRepository;
import com.codinglemonsbackend.Utils.URIUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BadgeService {

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private S3Properties s3Properties;

    @Value("${assets.domain}")
    private String ASSETS_DOMAIN;

    private static final String ASSET_BASE_PATH = "static/badges";

    public BadgeDto createBadge(BadgeDto badgeDto, byte[] imageBytes) throws FileUploadFailureException {
        if (badgeRepository.findByName(badgeDto.getName()).isPresent()) {
            throw new IllegalArgumentException("Badge with name '" + badgeDto.getName() + "' already exists");
        }

        BadgeEntity badge = BadgeEntity.builder()
                .name(badgeDto.getName())
                .description(badgeDto.getDescription())
                .rule(badgeDto.getRule())
                .build();

        String imageId = UUID.randomUUID().toString();
        //String s3Key = ASSET_BASE_PATH + "/" + imageId;
        String s3Key = "%s/%s/%s".formatted(ASSET_BASE_PATH, badgeDto.getRule().getType().getDisplayName(), imageId);
        boolean s3Uploaded = false;

        try {
            s3Service.putObject(s3Properties.getBucket(), s3Key, imageBytes);
            s3Uploaded = true;
            badge.setImageId(imageId);
            BadgeEntity saved = badgeRepository.save(badge);
            log.info("Badge '{}' created with id {}", saved.getName(), saved.getId());
            return toDto(saved);
        } catch (Exception e) {
            if (s3Uploaded) {
                try {
                    s3Service.deleteObject(s3Properties.getBucket(), s3Key);
                } catch (Exception deleteEx) {
                    log.error("Failed to delete orphaned badge image {} from S3", imageId, deleteEx);
                }
                log.error("Failed to save badge '{}' to database", badge.getName(), e);
                throw e;
            } else {
                log.error("Failed to upload badge image to S3 for badge '{}'", badge.getName(), e);
                throw new FileUploadFailureException("Badge image upload to S3 failed: " + e.getMessage());
            }
        }
    }

    public Map<String, List<BadgeDto>> getAllBadges() {
        return badgeRepository.findAll().stream()
                .collect(Collectors.groupingBy(b -> b.getRule().getType().getDisplayName(),
                        Collectors.mapping(
                            this::toDto, 
                            Collectors.toList()
                        )
                    ));
    }

    public void deleteBadge(String badgeId) {
        BadgeEntity badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new NoSuchElementException("Badge not found with id: " + badgeId));

        String s3Key = "%s/%s/%s".formatted(ASSET_BASE_PATH, badge.getRule().getType().getDisplayName(), badge.getImageId());
        long deleted = badgeRepository.deleteById(badgeId);

        if (deleted > 0) {
            try {
                s3Service.deleteObject(s3Properties.getBucket(), s3Key);
            } catch (Exception e) {
                log.error("Badge {} deleted from DB but failed to remove image {} from S3", badgeId, badge.getImageId(), e);
            }
            log.info("Badge '{}' deleted", badge.getName());
        }
    }

    public BadgeDto getHighestEarnedStreakBadge(List<String> earnedBadgeIds) {
        if (earnedBadgeIds == null || earnedBadgeIds.isEmpty()) return null;
        return badgeRepository.findAllByIds(earnedBadgeIds).stream()
                .filter(b -> b.getRule().getType() == BadgeRuleType.STREAK_DAYS)
                .max(Comparator.comparingInt(b -> b.getRule().getThreshold()))
                .map(this::toDto)
                .orElse(null);
    }

    public Map<String, List<EarnedBadgeDto>> getEarnedBadges(List<String> badgeIds) {
        if (badgeIds == null || badgeIds.isEmpty()) return Map.of();
        return badgeRepository.findAllByIds(badgeIds).stream()
                .collect(Collectors.groupingBy(b -> b.getRule().getType().getDisplayName(),
                        Collectors.mapping(
                            b -> new EarnedBadgeDto(b.getName(), b.getDescription(), buildImageUrl(b.getRule().getType().getDisplayName(), b.getImageId())), 
                            Collectors.toList()
                        )
                    ));
    }

    public Integer getNextBadgeThreshold(String badgeId) {
        if (badgeId == null || badgeId.isEmpty()) return null;
        BadgeEntity badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new NoSuchElementException("Badge not found with id: " + badgeId));

        return badgeRepository.findByRuleType(badge.getRule().getType()).stream()
                .filter(b -> b.getRule().getThreshold() > badge.getRule().getThreshold())
                .min(Comparator.comparingInt(b -> b.getRule().getThreshold()))
                .map(b -> b.getRule().getThreshold())
                .orElse(null);
    }

    private BadgeDto toDto(BadgeEntity badge) {
        return new BadgeDto(
                badge.getId(),
                badge.getName(),
                badge.getDescription(),
                badge.getRule(),
                buildImageUrl(badge.getRule().getType().getDisplayName(), badge.getImageId())
        );
    }

    private String buildImageUrl(String badgeRuleType, String imageId) {
        return URIUtils.createURI(ASSETS_DOMAIN, ASSET_BASE_PATH, badgeRuleType, imageId).toString();
    }
}
