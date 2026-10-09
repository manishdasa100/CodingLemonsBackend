package com.codinglemonsbackend.Filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

class RequestLoggingFilterTest {
    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final Logger accessLogger = (Logger) LoggerFactory.getLogger("web.access");
    private final ListAppender<ILoggingEvent> accessLog = new ListAppender<>() {
        @Override
        protected void append(ILoggingEvent event) {
            // Logback reads the MDC lazily; snapshot it before the filter clears it.
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    };

    @BeforeEach
    void captureAccessLog() {
        accessLog.start();
        accessLogger.addAppender(accessLog);
    }

    @AfterEach
    void release() {
        accessLogger.detachAppender(accessLog);
    }

    @Test
    void everyLineOfARequestSharesTheIdItsResponseCarries() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, String> mdcInsideTheRequest = new HashMap<>();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/problem/7"), response, (req, res) -> {
            mdcInsideTheRequest.putAll(MDC.getCopyOfContextMap());
            ((HttpServletResponse) res).setStatus(404);
        });

        String requestId = response.getHeader("X-Request-ID");
        assertNotNull(requestId);
        assertEquals(requestId, mdcInsideTheRequest.get("requestId"));
        assertEquals(1, accessLog.list.size(), "exactly one access line per request");
        Map<String, String> accessLine = accessLog.list.get(0).getMDCPropertyMap();
        assertEquals(requestId, accessLine.get("requestId"));
        assertEquals("404", accessLine.get("status"));
        assertMdcCleared();
    }

    @Test
    void aRequestThatBlowsUpIsLoggedAsTheFiveHundredItBecomes() {
        assertThrows(ServletException.class, () -> filter.doFilter(
                new MockHttpServletRequest("POST", "/api/v1/submission/submit"), new MockHttpServletResponse(),
                (req, res) -> { throw new ServletException("boom"); }));

        assertEquals("500", accessLog.list.get(0).getMDCPropertyMap().get("status"));
        assertMdcCleared();
    }

    /** Request threads are pooled: anything left behind would be stamped on the next request. */
    private static void assertMdcCleared() {
        Map<String, String> leftover = MDC.getCopyOfContextMap();
        assertTrue(leftover == null || leftover.isEmpty(), "MDC leaked past the request: " + leftover);
    }
}
