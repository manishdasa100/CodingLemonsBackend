package com.codinglemonsbackend.Service;

import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import com.codinglemonsbackend.Dto.RunCodeMatadata;
import com.codinglemonsbackend.Entities.ProgrammingLanguage;
import com.codinglemonsbackend.Payloads.DockerRunRequestPayloadJsonObject;

@ExtendWith(MockitoExtension.class)
public class CodeRunnerTest {
    
    @Mock
    private RestTemplate mockRestTemplate;

    @InjectMocks
    private PythonRunner underTestCodeRunner;

    public void testRunCode(){

        // given
        String testCode = "import java.util.Scanner; class Main{static Scanner sc = new Scanner(System.in); public static void main(String[] args){String str = sc.nextLine(); System.out.println(str); String str2 = sc.nextLine(); System.out.println(\"Str2=\" + str2);}}";

        RunCodeMatadata metadata = new RunCodeMatadata(testCode, ProgrammingLanguage.JAVA, List.of("test1","test2"));

        // DockerRunRequestPayloadJsonObject.File file = DockerRunRequestPayloadJsonObject.File.builder()
        //                                                 .name(underTestCodeRunner.fileName)
        //                                                 .content(metadata.getCode())
        //                                                 .build();
        
        // DockerRunRequestPayloadJsonObject.Payload payload = DockerRunRequestPayloadJsonObject.Payload.builder()
        //                                                         .language(underTestCodeRunner.getLanguage().toString().toLowerCase())
        //                                                         .stdin(CodeRunner.getTestCasesAsString(metadata.getTestCases()))
        //                                                         .files(new DockerRunRequestPayloadJsonObject.File[] {file})
        //                                                         .build();

        // DockerRunRequestPayloadJsonObject jsonObject = DockerRunRequestPayloadJsonObject.builder()
        //                                                 .image(underTestCodeRunner.getImageName())
        //                                                 .payload(payload)
        //                                                 .build();

        //ArgumentCaptor<HttpEntity<DockerRunRequestPayloadJsonObject>> agrCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        //when
        underTestCodeRunner.runCode(metadata);

        //then

    }
}
