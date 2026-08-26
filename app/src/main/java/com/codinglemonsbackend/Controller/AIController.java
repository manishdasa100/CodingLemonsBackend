package com.codinglemonsbackend.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Exceptions.AIHintException;
import com.codinglemonsbackend.Exceptions.ResourceNotFoundException;
import com.codinglemonsbackend.Payloads.HintResponse;
import com.codinglemonsbackend.Service.AIHintService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai/hint")
@RequiredArgsConstructor
public class AIController {

    private final AIHintService aiHintService;
    
    private UserEntity getSignedInUser() {
        return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping("/generate/{problemId}")
    public ResponseEntity<HintResponse> generate(
        @PathVariable Integer problemId,
        @RequestHeader(value = "X-Timezone", required = false) String zoneId
    ) throws AIHintException   {
        String username = getSignedInUser().getUsername();
        HintResponse response = aiHintService.generateNextHint(username, problemId, zoneId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest/{problemId}")
    public ResponseEntity<HintResponse> latest(@PathVariable Integer problemId) throws ResourceNotFoundException {
        String username = getSignedInUser().getUsername();
        HintResponse lastHint = aiHintService.getLastHint(username, problemId);
        return ResponseEntity.ok(lastHint);
    }

    @GetMapping("/history/{problemId}")
    public ResponseEntity<Map<ExecutionStatus, List<HintResponse>>> history(@PathVariable Integer problemId) {
        String username = getSignedInUser().getUsername();
        Map<ExecutionStatus, List<HintResponse>> hintHistory = aiHintService.getHintHistory(username, problemId);
        return ResponseEntity.ok(hintHistory);
    }
}
