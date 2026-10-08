package com.codinglemonsbackend.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Prod logging is nothing but properties, so when they rot nothing fails - stdout just stops being
 * JSON and the log agent ships text it can't parse. This boots an empty context on the real prod
 * properties and proves every stdout line is JSON carrying the service name and the MDC.
 */
@ExtendWith(OutputCaptureExtension.class)
class ProdLogFormatTest {

    @Configuration(proxyBeanMethods = false)
    static class NoBeans {}

    @Test
    void prodWritesOnlyJsonToStdout(CapturedOutput output) throws Exception {
        SpringApplication app = new SpringApplication(NoBeans.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = app.run("--spring.profiles.active=prod")) {
            MDC.put("requestId", "abc12345");
            LoggerFactory.getLogger("probe").info("probe line");
        } finally {
            MDC.clear();
        }

        ObjectMapper json = new ObjectMapper();
        Map<?, ?> probe = null;
        for (String line : output.getOut().lines().filter(l -> !l.isBlank()).toList()) {
            Map<?, ?> event = json.readValue(line, Map.class);   // throws on the first non-JSON line
            if ("probe line".equals(event.get("message"))) probe = event;
        }
        assertNotNull(probe, "the probe line never reached stdout:\n" + output.getOut());
        assertEquals("codinglemon-backend", probe.get("service"));
        assertEquals("abc12345", probe.get("requestId"));
        assertEquals("INFO", probe.get("level"));
    }
}