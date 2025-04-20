package com.codinglemonsbackend.Payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DockerRunRequestPayloadJsonObject {
    private String image;
    private Payload payload;
    
    @Data
    @Builder
    public static class Payload{
        private String language;
        private String stdin;
        private File[] files;
    }

    @Data
    @Builder
    public static class File{
        private String name;
        private String content;
    }
}
