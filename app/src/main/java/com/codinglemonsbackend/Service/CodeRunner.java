package com.codinglemonsbackend.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.codinglemonsbackend.Dto.CodeOutput;
import com.codinglemonsbackend.Dto.RunCodeMatadata;
import com.codinglemonsbackend.Entities.ProgrammingLanguage;
import com.codinglemonsbackend.Payloads.DockerRunRequestPayloadJsonObject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public abstract class CodeRunner {

    protected String imageName;

    protected ProgrammingLanguage language;

    protected String fileName;

    protected RestTemplate restTemplate;

    // @Value("${something.variable:http://3.19.59.52:8088/run}")
    // protected String dockerRunUrl;
    
    public CodeOutput runCode(RunCodeMatadata metadata){
        String code = metadata.getCode();
        List<String> testCases = metadata.getTestCases();

        String stdin = getTestCasesAsString(testCases).trim();

        System.out.println("Running " + language + " code with " + stdin);

        DockerRunRequestPayloadJsonObject.File file = DockerRunRequestPayloadJsonObject.File.builder()
                                                        .name(fileName)
                                                        .content(code)
                                                        .build();
        
        DockerRunRequestPayloadJsonObject.Payload payload = DockerRunRequestPayloadJsonObject.Payload.builder()
                                                                .language(language.toString().toLowerCase())
                                                                .stdin(stdin)
                                                                .files(new DockerRunRequestPayloadJsonObject.File[] {file})
                                                                .build();

        DockerRunRequestPayloadJsonObject jsonObject = DockerRunRequestPayloadJsonObject.builder()
                                                        .image(imageName)
                                                        .payload(payload)
                                                        .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Access-Token", "my-token");
        headers.set("Content-Type", "application/json");

        HttpEntity<DockerRunRequestPayloadJsonObject> request = new HttpEntity<>(jsonObject, headers);

        String dockerRunUrl = "http://3.19.59.52:8088/run";

        ResponseEntity<CodeOutput> response = restTemplate.exchange(dockerRunUrl, HttpMethod.POST, request, CodeOutput.class);

        System.out.println("Status code received from code runner :" + response.getStatusCode());
        System.out.println("Body from code runner :" + response.getBody());

        return response.getBody();
    }
    
    public static String getTestCasesAsString(List<String> testCases) {
        String result="";
        for (String testCase : testCases) {
            result = result + testCase + "\n";
        }
        return result;
    }
}
