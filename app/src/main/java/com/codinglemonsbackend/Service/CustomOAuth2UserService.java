package com.codinglemonsbackend.Service;

import java.util.Date;

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
import com.codinglemonsbackend.Exceptions.UserAlreadyExistException;
import com.codinglemonsbackend.Repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    private final UserService userService;

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
                userService.updateUserEmail(userByProviderIdAndType.getUsername(), userInfo.getEmail()); 
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

        UserDto userDto = UserDto.builder()
                .username(username)
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName() != null ? userInfo.getLastName() : "")
                .build();

        try {
            userService.provisionUser(newUser, userDto);
        } catch (UserAlreadyExistException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("username_conflict"), e.getMessage(), e);
        }

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
