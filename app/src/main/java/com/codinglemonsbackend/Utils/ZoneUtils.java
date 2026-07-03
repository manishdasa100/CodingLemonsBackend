package com.codinglemonsbackend.Utils;

import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ZoneUtils {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    private final UserRepository userRepository;

    /**
     * Resolves the effective zone for a user, in precedence order:
     *   1. a valid per-request zone (absolute truth) — persisted back if it differs from the stored one
     *   2. the user's stored zone (fallback)
     *   3. {@link #DEFAULT_ZONE}
     *
     * Each concern is isolated: a missing/invalid request zone or a failed
     * write-back never discards an otherwise-valid resolution.
     */
    public ZoneId resolveZone(String username, String requestZoneId) {
        ZoneId requestZone = tryParse(requestZoneId);
        UserEntity user = userRepository.findUserByUsername(username).orElse(null);

        if (requestZone != null) {
            if (user != null && !requestZone.getId().equals(user.getZoneId())) {
                try {
                    user.setZoneId(requestZone.getId());
                    userRepository.saveUser(user);
                } catch (Exception e) {
                    log.warn("Failed to persist resolved zone '{}' for user {}", requestZone.getId(), username, e);
                }
            }
            return requestZone;
        }

        ZoneId stored = tryParse(user == null ? null : user.getZoneId());
        return (stored != null) ? stored : DEFAULT_ZONE;
    }

    private ZoneId tryParse(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) return null;
        try {
            return ZoneId.of(zoneId);
        } catch (Exception e) {
            return null;
        }
    }
}
