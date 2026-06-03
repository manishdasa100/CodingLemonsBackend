package com.codinglemonsbackend.Config;

import java.util.Map;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo {;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getFirstName() {
        String givenName = (String) attributes.get("given_name");
        return givenName != null ? givenName : getId();
    }

    @Override
    public String getLastName() {
        String familyName = (String) attributes.get("family_name");
        return familyName != null ? familyName : "";
    }
}
