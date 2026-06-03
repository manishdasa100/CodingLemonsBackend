package com.codinglemonsbackend.Config;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        String provider = registrationId.toLowerCase();
        if ("google".equals(provider)) {
            return new GoogleOAuth2UserInfo(attributes);
        }
        if ("github".equals(provider)) {
            return new GitHubOAuth2UserInfo(attributes);
        }
        throw new OAuth2AuthenticationException(
            new OAuth2Error("unsupported_provider"),
            "Sorry! Login with " + registrationId + " is not supported yet."
        );
    }
}
