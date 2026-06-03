package com.codinglemonsbackend.Config;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User delegate;
    private final String username;

    public CustomOAuth2User(OAuth2User delegate, String username) {
        this.delegate = delegate;
        this.username = username;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    /** Returns the internal application username, not the OAuth provider's name. */
    @Override
    public String getName() {
        return username;
    }
}
