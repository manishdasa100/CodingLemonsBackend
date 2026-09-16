package com.codinglemonsbackend.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionResultEnvelope;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Dto.SupportedLanguage;
import com.codinglemonsbackend.Dto.TestcaseResult;
import com.codinglemonsbackend.Dto.TestcaseStatus;
import com.codinglemonsbackend.Entities.ProblemExecutionLimits;
import com.codinglemonsbackend.Entities.TestcaseRegistry.TestcasePair;
import com.codinglemonsbackend.Payloads.SubmissionType;
import com.codinglemonsbackend.Repository.DriverCodeRepository;
import com.codinglemonsbackend.Repository.TestcaseRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Fallback executor. Judge0 is an HTTP service with no push channel, so a poller collects
 * finished submissions and writes them onto the same execution-results stream the NSJAIL worker
 * uses. That keeps completion server-driven - a user who closes the tab still gets scored - and
 * leaves exactly one post-processing path for both executors.
 */
@Service
@Slf4j
public class Judge0ExecutionServiceImpl implements ExecutionService {

    /** Job ids waiting on Judge0. Empty in steady state, since Judge0 only runs during failover. */
    private static final String PENDING_JOBS_KEY = "executor:judge0:pending";

    private static final String RESULT_FIELDS =
            "token,status,stdout,stderr,compile_output,time,memory,stdin,expected_output";

    /**
     * Judge0 explains itself in the response body - {@code {"memory_limit":["must be ..."]}} and
     * the like. The default handler throws on status alone and drops that body, which leaves an
     * unactionable stack trace for what is nearly always a fixable request problem.
     */
    private static final RestClient.ResponseSpec.ErrorHandler JUDGE0_ERROR = (request, response) -> {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        throw new RuntimeException("Judge0 answered " + response.getStatusCode()
                + " for " + request.getMethod() + " " + request.getURI().getPath() + ": " + body);
    };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final SubmissionJobStore jobStore;
    private final DriverCodeRepository driverCodeRepository;
    private final TestcaseRepository testcaseRepository;
    private final Integer runCodeTestCaseCount;
    private final String executionResultsStream;
    private final boolean configured;
    private final int batchSize;
    private final long stuckTimeoutSeconds;

    public Judge0ExecutionServiceImpl(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            RedisService redisService,
            SubmissionJobStore jobStore,
            DriverCodeRepository driverCodeRepository,
            TestcaseRepository testcaseRepository,
            @Value("${testcase.runcode.count}") Integer runCodeTestCaseCount,
            @Value("${queue.execution-results.stream}") String executionResultsStream,
            @Value("${executor.judge0.base-url:}") String baseUrl,
            @Value("${executor.judge0.api-key:}") String apiKey,
            @Value("${executor.judge0.api-host:}") String apiHost,
            @Value("${executor.judge0.batch-size:20}") int batchSize,
            @Value("${executor.stuck-timeout-seconds:300}") long stuckTimeoutSeconds) {
        this.objectMapper = objectMapper;
        this.redisService = redisService;
        this.jobStore = jobStore;
        this.driverCodeRepository = driverCodeRepository;
        this.testcaseRepository = testcaseRepository;
        this.runCodeTestCaseCount = runCodeTestCaseCount;
        this.executionResultsStream = executionResultsStream;
        this.batchSize = batchSize;
        this.stuckTimeoutSeconds = stuckTimeoutSeconds;

        // RapidAPI-hosted Judge0 authenticates with these; a self-hosted instance needs neither.
        // Half-configured RapidAPI credentials would 401 on every call, so treat that as unconfigured
        // rather than failing over into it.
        boolean rapidApi = baseUrl.contains("rapidapi.com");
        this.configured = !baseUrl.isBlank() && (!rapidApi || (!apiKey.isBlank() && !apiHost.isBlank()));
        if (!baseUrl.isBlank() && !configured) {
            log.warn("Judge0 base url is set to a RapidAPI host but api-key/api-host are missing"
                    + " - Judge0 will stay out of rotation");
        }

        RestClient.Builder builder = restClientBuilder.baseUrl(baseUrl.isBlank() ? "http://judge0.invalid" : baseUrl);
        if (!apiKey.isBlank()) builder.defaultHeader("X-RapidAPI-Key", apiKey);
        if (!apiHost.isBlank()) builder.defaultHeader("X-RapidAPI-Host", apiHost);
        this.restClient = builder.build();
    }

    @Override
    public ExecutorWorkerType getWorkerType() {
        return ExecutorWorkerType.JUDGE0_WORKER;
    }

    /** Unconfigured Judge0 must never be selected - failing over to it would fail every request. */
    @Override
    public boolean isAvailable() {
        return configured;
    }

    /** Calibration needs the slow/hog probes and a calibration report, which only the NSJAIL worker produces. */
    @Override
    public boolean supports(SubmissionType submissionType) {
        return submissionType != SubmissionType.CALIBRATE;
    }

    // -------------------------------------------------------------------------
    // Dispatch
    // -------------------------------------------------------------------------

    private record Judge0Submission(
        @JsonProperty("source_code") String sourceCode,
        @JsonProperty("language_id") int languageId,
        @JsonProperty("additional_files") String additionalFiles,
        String stdin,
        @JsonProperty("expected_output") String expectedOutput,
        @JsonProperty("cpu_time_limit") Float cpuTimeLimit,
        @JsonProperty("memory_limit") Integer memoryLimit,
        @JsonProperty("stack_limit") Integer stackLimit,
        @JsonProperty("enable_network") boolean enableNetwork
    ) {}

    private record Judge0BatchRequest(List<Judge0Submission> submissions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Judge0Token(String token) {}

    @Override
    public void dispatch(SubmissionMetadata submissionMetadata) {
        String jobId = submissionMetadata.getSubmissionJobId();
        List<Judge0Submission> submissions = buildSubmissions(submissionMetadata);

        List<String> tokens = new ArrayList<>();
        for (int from = 0; from < submissions.size(); from += batchSize) {
            List<Judge0Submission> chunk = submissions.subList(from, Math.min(from + batchSize, submissions.size()));
            for (Judge0Token issued : postBatch(chunk)) {
                // A partially accepted batch still answers 201, with the rejected element's
                // validation errors sitting in the array where its token should be. The size
                // check below cannot see that - the element count is unchanged.
                if (issued.token() == null || issued.token().isBlank()) {
                    throw new RuntimeException("Judge0 partial acceptance rejected at least one submission in the batch");
                }
                tokens.add(issued.token());
            }
        }

        if (tokens.size() != submissions.size()) {
            throw new RuntimeException("Judge0 returned " + tokens.size() + " tokens for "
                    + submissions.size() + " submissions");
        }

        jobStore.setExecutorRef(jobId, String.join(",", tokens));
        redisService.addToSet(PENDING_JOBS_KEY, jobId);
        log.info("Dispatched submission {} to Judge0 with {} testcase submission(s)", jobId, tokens.size());
    }

    /**
     * One retry, because a dispatch failure here is user-visible and Judge0 is already the
     * fallback - there is nowhere left to fall back to if the NSJAIL worker is the reason we
     * are here at all.
     */
    private List<Judge0Token> postBatch(List<Judge0Submission> chunk) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                List<Judge0Token> tokens = restClient.post()
                        .uri(uri -> uri.path("/submissions/batch").queryParam("base64_encoded", "true").build())
                        .body(new Judge0BatchRequest(chunk))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, JUDGE0_ERROR)
                        .body(new ParameterizedTypeReference<List<Judge0Token>>() {});
                if (tokens == null) throw new RuntimeException("Judge0 returned an empty batch submission response");
                return tokens;
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn("Judge0 batch submission attempt {} of 2 failed: {}", attempt, e.getMessage());
            }
        }
        throw new RuntimeException("Judge0 rejected the batch submission - " + lastFailure.getMessage(), lastFailure);
    }

    /**
     * Where the user's solution has to land for each language's driver to find it. The names
     * mirror what the NSJAIL worker writes, so one set of drivers serves both executors.
     */
    private static final Map<SupportedLanguage, String> SOLUTION_FILE = Map.of(
            SupportedLanguage.CPP,    "solution.cpp",
            SupportedLanguage.PYTHON, "solution.py",
            SupportedLanguage.JAVA,   "Solution.java");

    /**
     * Judge0's accepted ranges, from GET /config_info and its submission validations. It answers
     * 422 rather than clamping, so a single out-of-range value fails the batch for that problem.
     * Our own limits are validated looser than these at both ends - stack by 8x at the top,
     * memory below the floor at the bottom - so normalize on the way out.
     */
    private static final float MAX_CPU_TIME_LIMIT_SECONDS = 20f;
    private static final int MIN_MEMORY_LIMIT_KB = 2_048;
    private static final int MAX_MEMORY_LIMIT_KB = 2_048_000;
    private static final int MAX_STACK_LIMIT_KB = 128_000;

    /**
     * Packs the user's solution into the zip Judge0 unpacks next to the driver.
     *
     * Judge0 writes {@code source_code} to its own fixed filename (main.cpp / Main.java /
     * script.py) <em>before</em> extracting, and extracts with {@code unzip -n}, so the archive
     * can never overwrite the driver. That is why the driver goes in source_code and the user's
     * code in the archive, and not the other way round.
     */
    static String solutionArchive(SupportedLanguage language, String encodedUserCode) {
        String entryName = SOLUTION_FILE.get(language);
        if (entryName == null) {
            throw new IllegalStateException("No Judge0 solution filename configured for " + language);
        }
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(archive)) {
            // Entry name only - a wrapping directory would leave the driver's include unresolved.
            zip.putNextEntry(new ZipEntry(entryName));
            // Whitespace stripped first: the strict decoder rejects the line wrapping that
            // openssl and Python's encodebytes emit, which the NSJAIL path accepts happily.
            zip.write(Base64.getDecoder().decode(encodedUserCode.replaceAll("\\s", "")));
            zip.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build the Judge0 solution archive", e);
        }
        // getEncoder, never getMimeEncoder: the line breaks it inserts corrupt the archive.
        return Base64.getEncoder().encodeToString(archive.toByteArray());
    }

    private List<Judge0Submission> buildSubmissions(SubmissionMetadata metadata) {
        Integer problemId = metadata.getProblemId();
        SupportedLanguage language = metadata.getLanguage();
        SubmissionType submissionType = metadata.getSubmissionType();
        ProblemExecutionLimits limits = metadata.getExecutionLimits();

        String driverCode = driverCodeRepository.getByProblemId(problemId)
                .orElseThrow(() -> new IllegalStateException("Driver code registry not found for problem ID: " + problemId))
                .getDriverCodes()
                .get(language);
        if (driverCode == null) {
            throw new IllegalStateException("No " + language + " driver code for problem " + problemId);
        }

        List<TestcasePair> testcases = getTargetTestcases(submissionType, problemId);

        boolean alreadyEncoded = Boolean.TRUE.equals(metadata.getB64Encoded());
        String encodedUserCode = alreadyEncoded
                ? metadata.getUserCode()
                : Base64.getEncoder().encodeToString(metadata.getUserCode().getBytes(StandardCharsets.UTF_8));

        // Identical for every testcase, so it is built once rather than per submission.
        String additionalFiles = solutionArchive(language, encodedUserCode);

        // Judge0 wants seconds and kilobytes; our limits are stored as milliseconds and megabytes.
        Float cpuTimeLimit = limits == null || limits.getCpuTimeLimit() == null ? null
                : Math.min(limits.getCpuTimeLimit() / 1000f, MAX_CPU_TIME_LIMIT_SECONDS);
        Integer memoryLimit = limits == null || limits.getMemoryLimit() == null ? null
                : Math.max(Math.min(limits.getMemoryLimit() * 1024, MAX_MEMORY_LIMIT_KB), MIN_MEMORY_LIMIT_KB);
        Integer requestedStackKb = limits == null || limits.getStackLimit() == null ? null
                : limits.getStackLimit() * 1024;
        Integer stackLimit = requestedStackKb == null ? null
                : Math.min(requestedStackKb, MAX_STACK_LIMIT_KB);

        // Stack is the one limit our validation lets a problem set above what Judge0 will take,
        // and shrinking it can turn a deep-recursion solution into a runtime error.
        if (requestedStackKb != null && requestedStackKb > MAX_STACK_LIMIT_KB) {
            log.warn("Problem {} asks for a {}KB stack but Judge0 caps at {}KB - a deep-recursion"
                    + " solution may fail on the failover path",
                    problemId, requestedStackKb, MAX_STACK_LIMIT_KB);
        }

        List<Judge0Submission> submissions = new ArrayList<>(testcases.size());
        for (TestcasePair testcase : testcases) {
            submissions.add(new Judge0Submission(
                    driverCode,                 // already base64 in Mongo, and we send base64_encoded=true
                    language.getLanguagId(),
                    additionalFiles,
                    encode(testcase.getInput()),
                    encode(testcase.getExpectedOutput()),
                    cpuTimeLimit,
                    memoryLimit,
                    stackLimit,
                    false));
        }
        return submissions;
    }

    private List<TestcasePair> getTargetTestcases(SubmissionType submissionType, Integer problemId) {
        List<TestcasePair> judgeTestcases = testcaseRepository.getByProblemId(problemId)
                .orElseThrow(() -> new IllegalArgumentException("Test case registry not found for problem ID: " + problemId))
                .getJudgeTestcases();
        if (judgeTestcases == null || judgeTestcases.isEmpty()) {
            throw new IllegalStateException("No judge testcases configured for problem " + problemId);
        }
        return submissionType == SubmissionType.RUN_CODE
                ? judgeTestcases.stream().limit(runCodeTestCaseCount).toList()
                : judgeTestcases;
    }

    private static String encode(String value) {
        return value == null ? null : Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        if (value == null) return null;
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;   // self-hosted instances can be configured to answer in plain text
        }
    }

    // -------------------------------------------------------------------------
    // Result collection
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Judge0Status(Integer id, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Judge0Result(
        String token,
        Judge0Status status,
        String stdout,
        String stderr,
        @JsonProperty("compile_output") String compileOutput,
        String time,
        Integer memory,
        String stdin,
        @JsonProperty("expected_output") String expectedOutput
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Judge0BatchResponse(List<Judge0Result> submissions) {}

    /**
     * Polls Judge0 for every in-flight job and republishes finished ones onto the results stream.
     * A no-op costing one SCARD whenever nothing is pending, which is the normal state.
     */
    @Scheduled(fixedDelayString = "${executor.judge0.poll-interval-ms:2000}")
    public void collectFinishedSubmissions() {
        if (!isAvailable()) return;

        Set<String> pendingJobIds = redisService.getSetMembers(PENDING_JOBS_KEY);
        if (pendingJobIds.isEmpty()) return;

        for (String jobId : pendingJobIds) {
            try {
                collectJob(jobId);
            } catch (Exception e) {
                // Leave the job pending: the next sweep retries it, and the stuck-job timeout
                // on the poll endpoint is the backstop if it never recovers.
                log.warn("Failed to collect Judge0 results for job {}: {}", jobId, e.getMessage());
            }
        }
    }

    private void collectJob(String jobId) {
        String tokens = jobStore.getExecutorRef(jobId);
        if (tokens == null) {
            log.warn("Judge0 job {} has no tokens (job expired?) - dropping it from the pending set", jobId);
            redisService.removeFromSet(PENDING_JOBS_KEY, jobId);
            return;
        }

        // Give up once the poll endpoint would already be calling this stuck. Without it a job
        // that can never finish is re-polled every 2s against a rate-limited API until its hash
        // expires, and the user watches a spinner the whole time.
        if (jobStore.ageSeconds(jobId) > stuckTimeoutSeconds) {
            log.error("Judge0 job {} has not finished after {}s - abandoning it", jobId, stuckTimeoutSeconds);
            jobStore.markFailed(jobId, "The code executor did not return a result in time.");
            redisService.removeFromSet(PENDING_JOBS_KEY, jobId);
            return;
        }

        List<String> tokenList = List.of(tokens.split(","));
        List<Judge0Result> results = new ArrayList<>(tokenList.size());
        for (int from = 0; from < tokenList.size(); from += batchSize) {
            List<String> chunk = tokenList.subList(from, Math.min(from + batchSize, tokenList.size()));
            results.addAll(getBatch(chunk));
        }

        // Judge0 status 1 = In Queue, 2 = Processing. Anything above that is a finished verdict.
        boolean allFinished = results.stream()
                .allMatch(r -> r.status() != null && r.status().id() != null && r.status().id() > 2);
        if (!allFinished) return;

        publishResult(jobId, results);
        redisService.removeFromSet(PENDING_JOBS_KEY, jobId);
    }

    private List<Judge0Result> getBatch(List<String> tokens) {
        Judge0BatchResponse response = restClient.get()
                .uri(uri -> uri.path("/submissions/batch")
                        .queryParam("tokens", String.join(",", tokens))
                        .queryParam("base64_encoded", "true")
                        .queryParam("fields", RESULT_FIELDS)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, JUDGE0_ERROR)
                .body(Judge0BatchResponse.class);
        if (response == null || response.submissions() == null) {
            throw new RuntimeException("Judge0 returned an empty batch result response");
        }
        // An unrecognised or purged token comes back as a literal null element rather than being
        // omitted, which would otherwise NPE the status scan below with nothing pointing here.
        if (response.submissions().stream().anyMatch(Objects::isNull)) {
            throw new RuntimeException("Judge0 returned no result for one or more tokens");
        }
        return response.submissions();
    }

    private void publishResult(String jobId, List<Judge0Result> results) {
        SubmissionMetadata metadata = jobStore.getMetadata(jobId);
        if (metadata == null) {
            log.warn("Judge0 job {} completed but its metadata has expired - discarding the result", jobId);
            return;
        }

        List<Judge0TestcaseOutcome> outcomes = new ArrayList<>(results.size());
        for (int index = 0; index < results.size(); index++) {
            Judge0Result r = results.get(index);
            outcomes.add(new Judge0TestcaseOutcome(
                    index,
                    r.status().id(),
                    r.status().description(),
                    decode(r.stdout()),
                    decode(r.stderr()),
                    decode(r.compileOutput()),
                    r.time(),
                    r.memory(),
                    decode(r.stdin()),
                    decode(r.expectedOutput())));
        }

        Judge0ExecutionReport report = new Judge0ExecutionReport(
                jobId,
                metadata.getLanguage().name(),
                metadata.getSubmissionType().name(),
                outcomes);

        try {
            ExecutionResultEnvelope envelope = new ExecutionResultEnvelope(
                    jobId,
                    PendingOrdersStatus.COMPLETED.name(),
                    getWorkerType().name(),
                    objectMapper.writeValueAsString(report));
            redisService.addToStream(executionResultsStream,
                    Map.of("body", objectMapper.writeValueAsString(envelope)));
            log.info("Published Judge0 result for job {} onto stream {}", jobId, executionResultsStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish Judge0 result for job " + jobId, e);
        }
    }

    // -------------------------------------------------------------------------
    // Raw report shape - written by the poller above, read by parseReport below.
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Judge0TestcaseOutcome(
        int index,
        int statusId,
        String statusDescription,
        String stdout,
        String stderr,
        String compileOutput,
        String time,                // seconds, e.g. "0.123"
        Integer memory,             // kilobytes
        String input,
        String expectedOutput
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Judge0ExecutionReport(
        String jobId,
        String language,
        String task,
        List<Judge0TestcaseOutcome> testcaseResults
    ) {}

    @Override
    public ExecutionReportDto parseReport(String report) {
        try {
            Judge0ExecutionReport raw = objectMapper.readValue(report, Judge0ExecutionReport.class);

            List<TestcaseResult> testcaseResults = raw.testcaseResults().stream()
                    .map(r -> new TestcaseResult(
                            r.index(),
                            mapTestcaseStatus(r.statusId()),
                            r.input(),
                            r.expectedOutput(),
                            r.stdout(),
                            r.stdout(),
                            r.stderr(),
                            runtimeMs(r.time()),
                            memoryMb(r.memory()),
                            r.statusDescription()))
                    .toList();

            // A compile error fails every testcase identically, so the first one carries it.
            // Gated on the status: Judge0 also fills compile_output for compiles that merely
            // emitted warnings, and treating those as CE would fail a passing submission.
            String compileError = raw.testcaseResults().stream()
                    .filter(r -> mapOverallStatus(r.statusId()) == ExecutionStatus.CE)
                    .map(Judge0TestcaseOutcome::compileOutput)
                    .filter(c -> c != null && !c.isBlank())
                    .findFirst()
                    .orElse(null);

            String runtimeError = raw.testcaseResults().stream()
                    .filter(r -> mapOverallStatus(r.statusId()) == ExecutionStatus.RE)
                    .map(Judge0TestcaseOutcome::stderr)
                    .filter(s -> s != null && !s.isBlank())
                    .findFirst()
                    .orElse(null);

            // Status 6 already maps to CE, so the first non-ACC verdict covers compile errors too.
            ExecutionStatus overallStatus = raw.testcaseResults().stream()
                    .map(r -> mapOverallStatus(r.statusId()))
                    .filter(s -> s != ExecutionStatus.ACC)
                    .findFirst()
                    .orElse(ExecutionStatus.ACC);

            int totalCorrect = (int) testcaseResults.stream()
                    .filter(r -> r.status() == TestcaseStatus.PASSED)
                    .count();

            // Judge0 has no notion of our request types and reports every testcase the same way, so
            // the SUBMIT_CODE/RUN_CODE distinction the NSJAIL worker makes in its raw report is
            // drawn here instead. The two are mutually exclusive: a submission runs the full hidden
            // judge set and so discloses only the first failure, while a run returns its visible
            // testcases in full. Counts and the maxima below still come from the full list.
            boolean isSubmitCode = SubmissionType.SUBMIT_CODE.name().equals(raw.task());

            TestcaseResult failedTestcase = !isSubmitCode ? null
                    : testcaseResults.stream()
                            .filter(r -> r.status() != TestcaseStatus.PASSED)
                            .findFirst()
                            .orElse(null);

            int runtimeMs = testcaseResults.stream()
                    .map(TestcaseResult::runtimeMs)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);

            int memoryMb = testcaseResults.stream()
                    .map(TestcaseResult::memoryMb)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);

            return ExecutionReportDto.builder()
                    .executionId(raw.jobId())
                    .language(raw.language())
                    .task(raw.task())
                    .status(overallStatus)
                    .statusMsg(overallStatus.getStatusMessage())
                    .totalTestcases(testcaseResults.size())
                    .totalCorrect(totalCorrect)
                    .runtimeMs(runtimeMs)
                    .memoryMb(memoryMb)
                    .testcaseResults(isSubmitCode ? List.of() : testcaseResults)
                    .failedTestcase(failedTestcase)
                    .compileError(compileError)
                    .runtimeError(runtimeError)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Judge0 execution report", e);
        }
    }

    private static Integer runtimeMs(String seconds) {
        if (seconds == null || seconds.isBlank()) return null;
        try {
            return Math.round(Float.parseFloat(seconds) * 1000);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer memoryMb(Integer kilobytes) {
        return kilobytes == null ? null : kilobytes / 1024;
    }

    // Judge0 status ids: 3=Accepted, 4=Wrong Answer, 5=TLE, 6=Compilation Error, 7-12=Runtime Error, 13+=Internal
    private static ExecutionStatus mapOverallStatus(int statusId) {
        return switch (statusId) {
            case 3 -> ExecutionStatus.ACC;
            case 4 -> ExecutionStatus.WA;
            case 5 -> ExecutionStatus.TLE;
            case 6 -> ExecutionStatus.CE;
            case 7, 8, 9, 10, 11, 12 -> ExecutionStatus.RE;
            default -> ExecutionStatus.IE;
        };
    }

    private static TestcaseStatus mapTestcaseStatus(int statusId) {
        return switch (statusId) {
            case 3 -> TestcaseStatus.PASSED;
            case 4 -> TestcaseStatus.FAILED;
            case 5 -> TestcaseStatus.TIMEOUT;
            case 7, 8, 9, 10, 11, 12 -> TestcaseStatus.RUNTIME_ERROR;
            default -> TestcaseStatus.ERROR;
        };
    }
}
