package com.codinglemonsbackend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.codinglemonsbackend.Entities.ProgrammingLanguage;

@Component
public class JavaRunner extends CodeRunner{

    public JavaRunner(@Autowired RestTemplate restTemplate) {
        super("glot/java:latest", ProgrammingLanguage.JAVA, "Main.java", restTemplate);
    }

    // @Override
    // public CodeOutput runCode(RunCodeMatadata metadata) {
    //     String code = metadata.getCode();
    //     List<String> testCases = metadata.getTestCases();

    //     String stdin = getTestCasesAsString(testCases);

    //     System.out.println("Running " + language + "code with... " + stdin);

    //     DockerRunRequestPayloadJsonObject.File file = DockerRunRequestPayloadJsonObject.File.builder()
    //                                                     .name(fileName)
    //                                                     .content(code)
    //                                                     .build();
        
    //     DockerRunRequestPayloadJsonObject.Payload payload = DockerRunRequestPayloadJsonObject.Payload.builder()
    //                                                             .language(language)
    //                                                             .stdin(stdin)
    //                                                             .files(new DockerRunRequestPayloadJsonObject.File[] {file})
    //                                                             .build();

    //     DockerRunRequestPayloadJsonObject jsonObject = DockerRunRequestPayloadJsonObject.builder()
    //                                                     .image(imageName)
    //                                                     .payload(payload)
    //                                                     .build();

    //     HttpHeaders headers = new HttpHeaders();
    //     headers.set("X-Access-Token", "my-token");
    //     headers.set("Content-Type", "application/json");

    //     HttpEntity<DockerRunRequestPayloadJsonObject> request = new HttpEntity<>(jsonObject, headers);

    //     ResponseEntity<CodeOutput> response = restTemplate.exchange("http://3.19.59.52:8088/run", HttpMethod.POST, request, CodeOutput.class);

    //     System.out.println("Status code received from code runner :" + response.getStatusCode());
    //     System.out.println("Body from code runner :" + response.getBody());

    //     return response.getBody();
    // }
    
}
