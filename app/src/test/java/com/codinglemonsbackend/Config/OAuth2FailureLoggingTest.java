package com.codinglemonsbackend.Config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class OAuth2FailureLoggingTest {
    @Test
    void anEmailConflictIsLoggedByItsCodeNotTheAddress() throws Exception {
        OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler();
        ReflectionTestUtils.setField(handler, "frontendRedirectUrl", "http://localhost:5173/oauth2/callback");
        Logger logger = (Logger) LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            handler.onAuthenticationFailure(new MockHttpServletRequest(), new MockHttpServletResponse(),
                    new OAuth2AuthenticationException(new OAuth2Error("email_conflict"),
                            "An account with email ada@example.com already exists."));

            String logged = appender.list.get(0).getFormattedMessage();
            assertTrue(logged.contains("email_conflict"), logged);
            assertFalse(logged.contains("@"), "the user's email reached the log: " + logged);
        } finally {
            logger.detachAppender(appender);
        }
    }
}
