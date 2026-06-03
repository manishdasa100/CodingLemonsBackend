package com.codinglemonsbackend.Service;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Config.CustomOAuth2User;
import com.codinglemonsbackend.Config.OAuth2UserInfo;
import com.codinglemonsbackend.Config.OAuth2UserInfoFactory;
import com.codinglemonsbackend.Dto.AuthProvider;
import com.codinglemonsbackend.Dto.Role;
import com.codinglemonsbackend.Dto.UserDto;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Events.UserAccountCreationEvent;
import com.codinglemonsbackend.Repository.UserProfileRepository;
import com.codinglemonsbackend.Repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        UserEntity user = resolveUser(userInfo, registrationId);
        return new CustomOAuth2User(oAuth2User, user.getUsername());
    }

    private UserEntity resolveUser(OAuth2UserInfo userInfo, String registrationId) {
        
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        UserEntity userByProviderIdAndType = userRepository.getUserbyAuthProviderIdAndProviderType(userInfo.getId(), provider).orElse(null);

        if (userByProviderIdAndType != null) {
            if (userInfo.getEmail() != null && !userInfo.getEmail().isBlank() && !userInfo.getEmail().equals(userByProviderIdAndType.getEmail())) {
                userByProviderIdAndType.setEmail(userInfo.getEmail());
                userRepository.saveUser(userByProviderIdAndType); 
                userProfileRepository.updateUserProfile(
                    userByProviderIdAndType.getUsername(), 
                    Map.of("email", userInfo.getEmail())
                );   
            }
            return userByProviderIdAndType;
        }

        UserEntity byEntityEmail = (userInfo.getEmail() != null && !userInfo.getEmail().isBlank())
        ? userRepository.findUserByEmail(userInfo.getEmail()).orElse(null)
        : null;

        if (byEntityEmail == null) {
            //User does not exist, create new account
            return createOAuthUser(userInfo, provider);
        } else {
            AuthProvider existingUserProvider = byEntityEmail.getAuthProvider();
            throw new OAuth2AuthenticationException(
                new OAuth2Error("email_conflict"),
                "An account with email " + userInfo.getEmail() + " already exists. Please log in using " + (existingUserProvider.equals(AuthProvider.LOCAL) ? "your username and password" : existingUserProvider.name().toLowerCase() + " authentication.")
            );
        }
    }

    private UserEntity createOAuthUser(OAuth2UserInfo userInfo, AuthProvider provider) {
        String username = generateUsernameFromUserInfo(userInfo);

        UserEntity newUser = UserEntity.builder()
                .username(username)
                .email(userInfo.getEmail())
                .authProvider(provider)
                .authProviderId(userInfo.getId())
                .passwordIssueDate(new Date((System.currentTimeMillis() / 1000) * 1000))
                .role(Role.USER)
                .build();

        userRepository.saveUser(newUser);

        UserDto userDto = UserDto.builder()
                .username(username)
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName() != null ? userInfo.getLastName() : "")
                .email(userInfo.getEmail())
                .build();

        eventPublisher.publishEvent(new UserAccountCreationEvent(this, userDto));

        log.info("Created new OAuth user: username={}, provider={}", username, provider);
        return newUser;
    }

    private String generateUsernameFromUserInfo(OAuth2UserInfo userInfo) {
        String base = userInfo.getUsernameBase()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
        if (base.isBlank()) base = "user";
        return base + "_" + userInfo.getId();
    }
}
