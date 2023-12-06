package com.codinglemonsbackend.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.CodeOutput;
import com.codinglemonsbackend.Dto.RunCodeMatadata;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Entities.ProgrammingLanguage;
import com.codinglemonsbackend.Entities.StatusMessage;
import com.codinglemonsbackend.Entities.Submission;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Payloads.CodeSubmissionResponse;
import com.codinglemonsbackend.Payloads.RunCodeRequest;

@Service
public class SubmissionService {

    private static final int numberOfTestcasesToUseForRunCodeRequest = 2;

    @Autowired
    private CodeRunnerManager codeRunnerManager;

    @Autowired
    private ProblemRepositoryService problemRepositoryService;

    @Autowired
    private UserRepositoryService userRepositoryService;

    // @Autowired
    // private Authentication authentication;

    private CodeOutput runCode(ProgrammingLanguage programingLanguage, String userCode, List<String> testCases){

        RunCodeMatadata metadata = new RunCodeMatadata(
            userCode, 
            programingLanguage, 
            testCases
        );

        return codeRunnerManager.runCode(metadata);
    }

    // public CodeSubmissionResponse handleSubmission(SubmitCodeRequest request, UserDetails userDetails){
    //     Integer problemId = request.getProblemId();
    //     String programingLanguage = request.getLanguage();
    //     String userCode = request.getUserCode();

    //     ProblemEntity problemEntity = problemRepositoryService.getProblem(problemId);

    //     CodeOutput codeOutput = runCode(
    //         programingLanguage, 
    //         problemEntity.getDriverCode()+userCode, 
    //         problemEntity.getTestCases()
    //     );

        
    //     //Checking for code output for any error
    //     if (!codeOutput.getError().isEmpty()) {
    //         CodeSubmissionResponse codeSubmissionResponse = CodeSubmissionResponse.builder()
    //             .runSuccess(false)
    //             .error(codeOutput.getStderr())
    //             .statusMsg(StatusMessage.ERR)
    //             .build();   
    //         saveSubmission(codeSubmissionResponse, userDetails);   
    //         return codeSubmissionResponse; 
    //     }

    //     //If run is successful, getting the test cases with incorrect output
    //     List<String> testcasesWithIncorrectOutputs = verifyCodeOutputForCorrectness(
    //                                 getCodeOutputAsArray(codeOutput.getStdout()), 
    //                                 problemEntity.getTestCaseOutputs(),
    //                                 problemEntity.getTestCases()
    //                         );

    //     // Setting the status message based on the number of correct answers
    //     StatusMessage statusMsg = testcasesWithIncorrectOutputs.size()>0?StatusMessage.TESTCASEFAILED:StatusMessage.ACC;

    //     CodeSubmissionResponse codeSubmissionResponse = CodeSubmissionResponse.builder()
    //             .runSuccess(true)
    //             .totalTestCases(problemEntity.getTestCases().size())
    //             .totalCorrect(problemEntity.getTestCases().size() - testcasesWithIncorrectOutputs.size())
    //             .failedTestCase(testcasesWithIncorrectOutputs.size()>0?testcasesWithIncorrectOutputs.get(0):null)
    //             .statusMsg(statusMsg)
    //             .build();
        
    //     saveSubmission(codeSubmissionResponse, userDetails);

    //     return codeSubmissionResponse;
    // }



    public CodeSubmissionResponse handleSubmission(RunCodeRequest request, Boolean submission){
        
        Integer problemId = request.getProblemId();
        ProgrammingLanguage programingLanguage = request.getLanguage();
        String userCode = request.getUserCode();

        ProblemEntity problemEntity = problemRepositoryService.getProblem(problemId);

        List<String> testCasesToUse = (submission) ? problemEntity.getTestCases() : problemEntity.getTestCases().subList(0, numberOfTestcasesToUseForRunCodeRequest);

        String driverCode = problemEntity.getDriverCodes().get(programingLanguage);

        CodeOutput codeOutput = runCode(
            programingLanguage, 
            driverCode+userCode, 
            testCasesToUse
        );

        //Checking for code output for any error
        if (!codeOutput.getError().isEmpty()) {
            CodeSubmissionResponse response = CodeSubmissionResponse.builder()
                .runSuccess(false)
                .error(codeOutput.getStderr())
                .statusMsg(StatusMessage.ERR)
                .build();  
            saveSubmission(request, response);
            return response;      
        }

        //If run is successful, getting the test cases with incorrect output
        List<String> testcasesWithIncorrectOutputs = verifyCodeOutputForCorrectness(
                                    getCodeOutputAsArray(codeOutput.getStdout()), 
                                    problemEntity.getTestCaseOutputs().subList(0, testCasesToUse.size()),
                                    testCasesToUse
                            );

        // Setting the status message based on the number of correct answers
        StatusMessage statusMsg = testcasesWithIncorrectOutputs.size()>0?StatusMessage.TESTCASEFAILED:StatusMessage.ACC;

        if (submission==true){
            CodeSubmissionResponse response = CodeSubmissionResponse.builder()
                .runSuccess(true)
                .totalTestCases(problemEntity.getTestCases().size())
                .totalCorrect(problemEntity.getTestCases().size() - testcasesWithIncorrectOutputs.size())
                .failedTestCase(testcasesWithIncorrectOutputs.size()>0?testcasesWithIncorrectOutputs.get(0):null)
                .statusMsg(statusMsg)
                .build();

            saveSubmission(request,response);

            return response;
        }

        return CodeSubmissionResponse.builder()
                .runSuccess(true)
                .codeAnswer(getCodeOutputAsArray(codeOutput.getStdout()))
                .expectedAnswer(problemEntity.getTestCaseOutputs().subList(0, numberOfTestcasesToUseForRunCodeRequest))
                .totalTestCases(testCasesToUse.size())
                .totalCorrect(testCasesToUse.size() - testcasesWithIncorrectOutputs.size())
                .statusMsg(statusMsg)
                .build();
    }

    private List<String> getCodeOutputAsArray(String codeAnswerString){
        String codeAnswerStringTrim = codeAnswerString.trim();
        String[] codeAnswerArr = codeAnswerStringTrim.split("\n");
        return Arrays.asList(codeAnswerArr);
    }

    //Function to check if the code answer and the correct answer matches. Returns the test cases with incorrect output
    private List<String> verifyCodeOutputForCorrectness(List<String> codeOutput, List<String> expectedAnswers, List<String> testCases){
        List<String> testcasesWithincorrectOutputs = new ArrayList<String>();
        for (int i = 0; i < expectedAnswers.size(); i++){
            String codeAnswer = codeOutput.get(i);
            String expectedAnswer = expectedAnswers.get(i);
            String testCase = testCases.get(i);
            if (!codeAnswer.equals(expectedAnswer)) testcasesWithincorrectOutputs.add(testCase);
        }
        return testcasesWithincorrectOutputs;
    }

    private void saveSubmission(RunCodeRequest request, CodeSubmissionResponse codeSubmissionResponse) {
        UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Submission submission = Submission.builder()
                                    .submissionId(UUID.randomUUID().toString())
                                    .problemId(request.getProblemId())
                                    .language(request.getLanguage().toString().toLowerCase())
                                    .userCode(request.getUserCode())
                                    .runtime(0)
                                    .dateOfSubmission(LocalDateTime.now(ZoneId.of("GMT")).toString())
                                    .runSucccess(codeSubmissionResponse.getRunSuccess())
                                    .error(codeSubmissionResponse.getError())
                                    .totalTestCases(codeSubmissionResponse.getTotalTestCases())
                                    .totalCorrectOutput(codeSubmissionResponse.getTotalCorrect())
                                    .failedTestCase(codeSubmissionResponse.getFailedTestCase())
                                    .statusMessage(codeSubmissionResponse.getStatusMsg())
                                    .build();
        
        userEntity.getSubmissions().add(submission);
        userRepositoryService.updateUser(userEntity);
        System.out.println(TimeZone.getDefault().getID());
        System.out.println("Saving submission for"+userEntity.getUsername()+userEntity.getSubmissions());
    }

}