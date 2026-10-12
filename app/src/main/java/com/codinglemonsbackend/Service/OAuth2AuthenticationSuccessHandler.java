package com.codinglemonsbackend.Service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.codinglemonsbackend.Dto.CustomOAuth2User;
import com.codinglemonsbackend.Utils.JwtUtils;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final String frontendRedirectUrl;

    private final JwtUtils jwtUtils;

    private final UserService userService;

    private final MeterRegistry meterRegistry;

    private OAuth2AuthenticationSuccessHandler(
            @Value("${oauth2.frontend.redirect.url}") String frontendRedirectUrl,
            JwtUtils jwtUtils,
            UserService userService,
            MeterRegistry meterRegistry) {
        this.frontendRedirectUrl = frontendRedirectUrl;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.meterRegistry = meterRegistry; 
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        String username = principal.getName();

        UserDetails userDetails = userService.loadUserByUsername(username);
        String jwt = jwtUtils.generateToken(userDetails);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("token", jwt)
                .build().toUriString();

        String provider = authentication instanceof OAuth2AuthenticationToken token
                ? token.getAuthorizedClientRegistrationId().toUpperCase()
                : "OAUTH2";
        meterRegistry.counter(
            "user.logins", 
            "provider", provider).increment();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
