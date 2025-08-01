package com.codinglemonsbackend.Utils;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.web.util.UriComponentsBuilder;

public class URIUtils {
    
    private static final String protocol = "https";

    public static URL createURI(String domain, String... pathSegments) {
        URL url = null;
        try {
            url = UriComponentsBuilder.newInstance()
                .scheme(protocol)
                .host(domain)
                .pathSegment(pathSegments)
                .build()
                .toUri()
                .toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }
}
