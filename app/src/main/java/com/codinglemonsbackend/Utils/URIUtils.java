package com.codinglemonsbackend.Utils;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.web.util.UriComponentsBuilder;

public class URIUtils {
    
    private static final String protocol = "https";

    public static URL createURI(String domain, String... pathSegments) {
        try {
            return UriComponentsBuilder.newInstance()
                .scheme(protocol)
                .host(domain)
                .pathSegment(pathSegments)
                .build()
                .toUri()
                .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Cannot build an https URL for host: " + domain, e);
        }
    }
}
