package com.codinglemonsbackend.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.AIProvider;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.Hint;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.TestcaseResult;
import com.codinglemonsbackend.Entities.ProblemExecutionLimits;
import com.codinglemonsbackend.Entities.SubmissionEntity;
import com.codinglemonsbackend.Entities.UserProblemHintEntity;
import com.codinglemonsbackend.Exceptions.AIHintException;
import com.codinglemonsbackend.Exceptions.CooldownActiveException;
import com.codinglemonsbackend.Exceptions.NotHintableException;
import com.codinglemonsbackend.Exceptions.ProviderUnavailableException;
import com.codinglemonsbackend.Exceptions.QuotaExceededException;
import com.codinglemonsbackend.Exceptions.ResourceNotFoundException;
import com.codinglemonsbackend.Payloads.HintResponse;
import com.codinglemonsbackend.Properties.AIHintProperties;
import com.codinglemonsbackend.Repository.SubmissionRepository;
import com.codinglemonsbackend.Repository.UserProblemHintRepository;
import com.codinglemonsbackend.Utils.ZoneUtils;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIHintService {
    
    // Product rules, not config — nobody tunes these per environment.

    private static final int TRANSIENT_HISTORY_CAP = 2;                  

    private final SubmissionRepository submissionRepository;
    private final UserProblemHintRepository userProblemHintRepository;
    private final PromptCatalog promptCatalog;
    private final RedisService redisService;
    private final AIHintProperties aiHintProps;
    private final ProblemRepositoryService problemRepository;
    private final List<AIProvider> aiProviders;  
    private final ZoneUtils zoneUtils;
    private final MeterRegistry meterRegistry;

    private boolean isTransient(ExecutionStatus status) {
        return promptCatalog.maxLevel(status) == 1;
    }

    public HintResponse generateNextHint(String username, Integer problemId, String userZoneId) throws AIHintException { 
        SubmissionEntity latestSubmission = this.checkIfHintableAndGetLatestSubmission(username, problemId);
        ExecutionStatus status = latestSubmission.getStatus();
        int maxLevel = promptCatalog.maxLevel(status);

        List<Hint> stored = storedHints(username, problemId).getOrDefault(status, List.of());
        Hint last = stored.isEmpty() ? null : stored.get(stored.size()-1);

        if (!isTransient(status) && last != null && last.level() >= maxLevel) return toResponse(status, last); 

        String cooldownKey = claimCooldown(username, problemId);
        String quotaKey;
        try {
            quotaKey = claimQuota(username, userZoneId);
        } catch (QuotaExceededException e) {
            redisService.deleteKey(cooldownKey);
            throw e;
        }

        int level = (last == null)? 1 : Math.min(last.level() + 1, maxLevel);

        try {
            ProblemDto problemDto = problemRepository.getProblemById(problemId);
            String prompt = promptCatalog.render(status, level, buildPromptVars(problemDto, latestSubmission));
            Hint hint = callWithFailover(prompt, level, latestSubmission.getSubmissionId(), status);
            userProblemHintRepository.appendHint(
                username, 
                problemId, 
                status,
                hint,
                isTransient(status)? TRANSIENT_HISTORY_CAP: null
            );
            return toResponse(status, hint);
        } catch (ProviderUnavailableException | RuntimeException e) {
            refund(cooldownKey, quotaKey);
            throw e;
        }
    }

    private HintResponse toResponse(ExecutionStatus status, Hint hint) {
        int maxLevel = promptCatalog.maxLevel(status);
        return new HintResponse(status, hint.level(), maxLevel, hint.level() >= maxLevel, hint.content(), hint.createdAt());
    }

    private String claimQuota(String username, String userZoneId) throws QuotaExceededException {
        ZoneId zone = zoneUtils.resolveZone(username, userZoneId);
        LocalDate today = LocalDate.now(zone);
        String key = RedisService.AI_HINT_QUOTA_PREFIX + username + ":" + today;

        Long used = redisService.increment(key, 1);
        redisService.setExpiry(key, today.plusDays(1).atStartOfDay(zone).toInstant());

        if (used > aiHintProps.getDailyUserQuota()) throw new QuotaExceededException(String.format("AI hint quota exhausteds for %s. Retry after %d", today.toString(), redisService.getExpiry(key))); 
        return key;
    }

    private String claimCooldown(String username, Integer problemId) throws CooldownActiveException {
        String cooldownKey = redisService.AI_HINT_COOLDOWN_PREFIX + username + ":" + problemId;
        if (!Boolean.TRUE.equals(redisService.setIfAbsent(cooldownKey, "1", aiHintProps.getCooldownSeconds()))) {
            throw new CooldownActiveException(String.format(
                    "Cooldown is active for this problem. Retry after %d seconds", redisService.getExpiry(cooldownKey)));
        }
        return cooldownKey;
    }

    private Map<ExecutionStatus, List<Hint>> storedHints(String username, Integer problemId) {
        return userProblemHintRepository.find(username, problemId)
        .map(UserProblemHintEntity::getHints)
        .orElse(Map.of());
    }

    // most recent hint across all statuses (max createdAt in the doc); none → 404
    public HintResponse getLastHint(String username, Integer problemId) throws ResourceNotFoundException { 
        return storedHints(username, problemId).entrySet().stream()
        .flatMap(e -> e.getValue().stream().map(h->toResponse(e.getKey(), h)))
        .max(Comparator.comparing(HintResponse::createdAt))
        .orElseThrow(()->new ResourceNotFoundException("No hints yet for problem " + problemId));
    }

    // the stored doc grouped by status — storage IS the retention policy, no read logic
    public Map<ExecutionStatus, List<HintResponse>> getHintHistory(String username, Integer problemId) { 
        Map<ExecutionStatus, List<HintResponse>> history = new EnumMap<>(ExecutionStatus.class);
        storedHints(username, problemId).forEach((status, hints) -> history.put(status, hints.stream().map(h -> toResponse(status, h)).toList()));
        return history;
    }

    private Map<String,Object> buildPromptVars(ProblemDto problem, SubmissionEntity submission) {  
        ProblemExecutionLimits limits = problem.getExecutionLimits();
        TestcaseResult failed = submission.getFailedTestCase();

        Map<String, Object> vars = new HashMap<>();
        vars.put("problemTitle", problem.getTitle());
        vars.put("problemDescription", problem.getDescription());
        vars.put("constraints", String.join("\n", problem.getConstraints()));
        vars.put("language", submission.getLanguage().name());
        vars.put("userCode", submission.getUserCode());
        vars.put("passedTestcases", submission.getTotalCorrectOutput());
        vars.put("totalTestcases", submission.getTotalTestCases());

        switch (submission.getStatus()) {
            case TLE -> {
                vars.put("cpuTimeLimitMs", limits.getCpuTimeLimit());
                vars.put("optimalTimeComplexity", limits.getOptimalTimeComplexity());
            }
            case MLE -> {
                vars.put("memoryLimitMb", limits.getMemoryLimit());
                vars.put("optimalSpaceComplexity", limits.getOptimalSpaceComplexity());
            }
            case WA -> {
                vars.put("testcaseInput", failed.input());
                vars.put("expectedOutput", failed.expectedOutput());
                vars.put("actualOutput", failed.actualOutput());
            }
            case RE -> {
                vars.put("testcaseInput", failed == null? "":failed.input());
                vars.put("errorMessage", errorText(submission));
            }
            case CE -> vars.put("errorMessage", errorText(submission));
            default -> throw new IllegalStateException("Not hintable: " + submission.getStatus());
        }
        return vars;
    }

    private String errorText(SubmissionEntity submission) {
        if (submission.getError() != null) return submission.getError();
        TestcaseResult failed = submission.getFailedTestCase();
        return failed == null ? "":failed.errorMsg();
    }

    private Hint callWithFailover(String prompt, int level, String submissionId, ExecutionStatus status) throws ProviderUnavailableException {                          
        // sequential, per-call timeout
        for (AIProvider provider : aiProviders) {
            try {
                ChatResponse response = provider.chatClient().prompt().user(prompt).call().chatResponse();
                String content = response == null ? null : response.getResult().getOutput().getText();
                if (content == null || content.isBlank()) throw new IllegalStateException("empty response");
                String model = response.getMetadata().getModel() == null ? provider.name() : response.getMetadata().getModel();
                if (level == promptCatalog.maxLevel(status)
                && "length".equalsIgnoreCase(response.getResult().getMetadata().getFinishReason())) {
                    log.warn("Response trucated for model: {} at max level for status {}", model, status);
                    throw new IllegalStateException("Response truncated at max tokens");                    
                }

                Usage usage = response.getMetadata().getUsage();
                recordTokens(model, status, level, "input", usage.getPromptTokens());
                recordTokens(model, status, level, "output", usage.getCompletionTokens());
                log.info("AI hint generated: provider={} model={} status={} level={} in={} out={}",
                    provider.name(), model, status, level, usage.getPromptTokens(), usage.getCompletionTokens()
                );

                return new Hint(level, content, submissionId, provider.name(), Instant.now()); 
            } catch (Exception e) {
                log.warn("AI provider {} failed, falling through to next", provider.name(), e);
            }
        }
        throw new ProviderUnavailableException("All AI provider failed to produce a hint");
    }

    private void recordTokens(String model, ExecutionStatus status, int level, String type, Integer tokens) {
        if (tokens == null) return;
        DistributionSummary.builder("ai.hint.tokens")
                            .tag("model", model)
                            .tag("status", status.name())
                            .tag("level", String.valueOf(level))
                            .tag("type", type)
                            .register(meterRegistry)
                            .record(tokens);
    }

    private void refund(String cooldownKey, String quotaKey) {
        redisService.deleteKey(cooldownKey);
        redisService.increment(quotaKey, -1);
    }

    private SubmissionEntity checkIfHintableAndGetLatestSubmission(String username, Integer problemId) throws NotHintableException {
        SubmissionEntity latestSubmission = submissionRepository.getLatestSubmissionForProblem(username, problemId).orElseThrow(() -> new NotHintableException(String.format("No submission found for problem id: %d. Make a submission first to receive hints", problemId)));
        ExecutionStatus status = latestSubmission.getStatus();
        if (!promptCatalog.supports(status))
        {
            throw new NotHintableException(String.format("Hints are not applicable after a %s submission", status.name()));
        }
        return latestSubmission;
    }
}
