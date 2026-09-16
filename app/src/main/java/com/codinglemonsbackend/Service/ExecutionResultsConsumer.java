package com.codinglemonsbackend.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ExecutionResultEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * The single consumer of finished executions, whichever executor produced them.
 *
 * Reads through a consumer group with manual acknowledgement: an entry stays pending until its
 * side effects have committed, so a crash mid-processing replays rather than silently losing a
 * user's submission. Entries that keep failing are moved aside instead of being retried forever.
 */
@Slf4j
@Component
public class ExecutionResultsConsumer {

    private static final int PENDING_SCAN_LIMIT = 100;

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final ExecutionResultProcessor resultProcessor;
    private final String resultsStream;
    private final String deadLetterStream;
    private final String consumerGroup;
    private final String consumerName;
    private final Duration retryAfter;
    private final long maxDeliveries;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    public ExecutionResultsConsumer(
            RedisConnectionFactory redisConnectionFactory,
            RedisService redisService,
            ObjectMapper objectMapper,
            ExecutionResultProcessor resultProcessor,
            @Value("${queue.execution-results.stream}") String resultsStream,
            @Value("${queue.execution-results.dead-letter-stream:stream:execution-results-dlq}") String deadLetterStream,
            @Value("${queue.execution-results.group:backend-result-processors}") String consumerGroup,
            @Value("${queue.execution-results.retry-after-seconds:60}") long retryAfterSeconds,
            @Value("${queue.execution-results.max-deliveries:5}") long maxDeliveries) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.resultProcessor = resultProcessor;
        this.resultsStream = resultsStream;
        this.deadLetterStream = deadLetterStream;
        this.consumerGroup = consumerGroup;
        this.consumerName = "backend-" + UUID.randomUUID().toString().substring(0, 8);
        this.retryAfter = Duration.ofSeconds(retryAfterSeconds);
        this.maxDeliveries = maxDeliveries;
    }

    @PostConstruct
    void start() {
        ensureConsumerGroup();

        container = StreamMessageListenerContainer.create(
                redisConnectionFactory,
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .build());

        // Spring cancels the subscription on the first read error by default, which would leave
        // this container "running" but permanently deaf after any Redis blip or failover - and
        // results would then pile up unprocessed with nothing to signal it. Keep reading instead.
        StreamMessageListenerContainer.ConsumerStreamReadRequest<String> readRequest =
                StreamMessageListenerContainer.StreamReadRequest
                        .builder(StreamOffset.create(resultsStream, ReadOffset.lastConsumed()))
                        .consumer(Consumer.from(consumerGroup, consumerName))
                        .autoAcknowledge(false)
                        .cancelOnError(throwable -> false)
                        .errorHandler((Throwable throwable) ->
                                log.error("Error reading execution results from {} - will keep polling",
                                        resultsStream, throwable))
                        .build();

        container.register(readRequest, this::onRecord);
        container.start();

        log.info("Consuming execution results from {} as {}/{}", resultsStream, consumerGroup, consumerName);
    }

    /**
     * Idempotent: Redis answers BUSYGROUP when the group is already there. Called again if the
     * group ever goes missing, which happens when Redis restarts without persistence.
     */
    private void ensureConsumerGroup() {
        try {
            redisService.createConsumerGroup(resultsStream, consumerGroup);
        } catch (Exception e) {
            // Not fatal at boot: the retry sweep re-attempts this, so a Redis outage during a
            // restart does not have to take the whole application down with it.
            log.error("Could not create consumer group {} on {}", consumerGroup, resultsStream, e);
        }
    }

    @PreDestroy
    void stop() {
        if (container != null) container.stop();
    }

    /**
     * Acknowledges only after processing returns. Anything thrown leaves the entry pending for
     * {@link #retryFailedResults()} to pick up.
     */
    private void onRecord(MapRecord<String, String, String> record) {
        try {
            handle(record);
            redisService.acknowledge(resultsStream, consumerGroup, record.getId());
        } catch (Exception e) {
            log.error("Execution result {} failed to process - leaving it pending for retry", record.getId(), e);
        }
    }

    private void handle(MapRecord<String, String, String> record) throws Exception {
        String body = record.getValue().get("body");
        if (body == null) {
            log.warn("Execution result {} has no 'body' field - nothing to process", record.getId());
            return;
        }
        resultProcessor.process(objectMapper.readValue(body, ExecutionResultEnvelope.class));
    }

    /**
     * Retries entries whose processing did not complete, and retires the ones that never will.
     * Also picks up work left pending by an instance that died, since XPENDING spans the group.
     */
    @Scheduled(fixedDelayString = "${queue.execution-results.retry-interval-ms:60000}")
    public void retryFailedResults() {
        PendingMessages pending;
        try {
            pending = redisService.getPendingMessages(resultsStream, consumerGroup, PENDING_SCAN_LIMIT);
        } catch (Exception e) {
            log.warn("Could not read pending execution results: {}", e.getMessage());
            // The group can disappear under us if Redis restarts without persistence; recreating
            // it here is what gets the reader consuming again once Redis is back.
            ensureConsumerGroup();
            return;
        }
        if (pending == null || pending.isEmpty()) return;

        for (PendingMessage message : pending) {
            RecordId id = message.getId();
            if (message.getTotalDeliveryCount() >= maxDeliveries) {
                deadLetter(id, message.getTotalDeliveryCount());
            } else if (message.getElapsedTimeSinceLastDelivery().compareTo(retryAfter) >= 0) {
                claim(id).forEach(this::onRecord);
            }
        }
    }

    private List<MapRecord<String, String, String>> claim(RecordId id) {
        List<MapRecord<String, String, String>> claimed =
                redisService.claimPending(resultsStream, consumerGroup, consumerName, retryAfter, id);
        return claimed == null ? List.of() : claimed;
    }

    private void deadLetter(RecordId id, long deliveries) {
        for (MapRecord<String, String, String> record
                : redisService.claimPending(resultsStream, consumerGroup, consumerName, Duration.ZERO, id)) {
            Map<String, String> fields = record.getValue();
            if (!fields.isEmpty()) redisService.addToStream(deadLetterStream, fields);
        }
        redisService.acknowledge(resultsStream, consumerGroup, id);
        log.error("Execution result {} failed {} times - moved to {}", id, deliveries, deadLetterStream);
    }
}
