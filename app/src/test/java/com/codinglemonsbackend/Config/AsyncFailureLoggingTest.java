package com.codinglemonsbackend.Config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * The whole point of the AsyncConfigurer is wiring: if Spring does not pick it up, nothing fails
 * visibly and the code still looks correct. So this boots a real context and proves an @Async void
 * method that throws actually reaches the handler - and that the handler does not spill the user's
 * source code while doing it.
 */
@SpringJUnitConfig(classes = { AsyncConfig.class, AsyncFailureLoggingTest.FailingListener.class })
class AsyncFailureLoggingTest {

    private static final String USER_CODE = "class Solution { /* NOT-FOR-THE-LOGS */ }";

    @Autowired
    private FailingListener failingListener;

    @Test
    void asyncVoidFailureIsLoggedWithContextButWithoutUserCode() {
        Logger asyncConfigLogger = (Logger) LoggerFactory.getLogger(AsyncConfig.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        asyncConfigLogger.addAppender(appender);

        try {
            failingListener.onSubmitCodeCompleted(new SubmitCodeCompletedEvent(this, null,
                    SubmissionMetadata.builder()
                            .submissionJobId("job-99")
                            .username("ada")
                            .problemId(7)
                            .userCode(USER_CODE)
                            .build(),
                    true));

            String logged = awaitSingleMessage(appender);

            assertTrue(logged.contains("FailingListener.onSubmitCodeCompleted"), logged);
            assertTrue(logged.contains("job=job-99"), logged);
            assertTrue(logged.contains("user=ada"), logged);
            assertTrue(logged.contains("problem=7"), logged);
            assertFalse(logged.contains("NOT-FOR-THE-LOGS"),
                    "the handler leaked the user's submitted code into the log: " + logged);
        } finally {
            asyncConfigLogger.detachAppender(appender);
        }
    }

    /** The throw happens on a pool thread, so the log arrives slightly after the call returns. */
    private String awaitSingleMessage(ListAppender<ILoggingEvent> appender) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            List<ILoggingEvent> events = List.copyOf(appender.list);
            if (!events.isEmpty()) return events.get(0).getFormattedMessage();
            Thread.onSpinWait();
        }
        return fail("the async failure was swallowed - nothing reached the exception handler");
    }

    @Component
    static class FailingListener {
        @Async("applicationAsyncExecutor")
        public void onSubmitCodeCompleted(SubmitCodeCompletedEvent event) {
            throw new IllegalStateException("listener blew up");
        }
    }
}
