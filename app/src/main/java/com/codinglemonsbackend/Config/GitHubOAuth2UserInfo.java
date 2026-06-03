package com.codinglemonsbackend.Config;

import java.util.Map;

public class GitHubOAuth2UserInfo extends OAuth2UserInfo {

    public GitHubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        Object id = attributes.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getFirstName() {
        String name = (String) attributes.get("name");
        if (name == null || name.isBlank()) return (String) attributes.get("login");
        int spaceIndex = name.indexOf(' ');
        return spaceIndex > 0 ? name.substring(0, spaceIndex) : name;
    }

    @Override
    public String getLastName() {
        String name = (String) attributes.get("name");
        if (name == null || name.isBlank()) return "";
        int spaceIndex = name.indexOf(' ');
        return spaceIndex > 0 ? name.substring(spaceIndex + 1) : "";
    }

    @Override
    public String getUsernameBase() {
        return (String) attributes.get("login");
    }
}
