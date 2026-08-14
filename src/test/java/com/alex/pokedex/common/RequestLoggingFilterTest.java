package com.alex.pokedex.common;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUpAppender() {
        logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        MDC.clear();
    }

    @Test
    void generatesCorrelationIdWhenHeaderAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, noopChain());

        assertThat(response.getHeader(RequestLoggingFilter.HEADER_NAME)).isNotBlank();
        assertThat(MDC.get(RequestLoggingFilter.MDC_KEY)).isNull();
    }

    @Test
    void reusesValidInboundCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestLoggingFilter.HEADER_NAME, "client-trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringChain = new AtomicReference<>();

        filter.doFilterInternal(
                request,
                response,
                (req, res) -> mdcDuringChain.set(MDC.get(RequestLoggingFilter.MDC_KEY)));

        assertThat(mdcDuringChain.get()).isEqualTo("client-trace-123");
        assertThat(response.getHeader(RequestLoggingFilter.HEADER_NAME))
                .isEqualTo("client-trace-123");
        assertThat(MDC.get(RequestLoggingFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesMalformedInboundCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestLoggingFilter.HEADER_NAME, "bad\nid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, noopChain());

        assertThat(response.getHeader(RequestLoggingFilter.HEADER_NAME))
                .isNotEqualTo("bad\nid")
                .isNotBlank();
    }

    @Test
    void resolveCorrelationId_acceptsValidCharacters() {
        assertThat(RequestLoggingFilter.resolveCorrelationId("trace.123_abc-XYZ"))
                .isEqualTo("trace.123_abc-XYZ");
    }

    @Test
    void resolveCorrelationId_rejectsInvalidCharacters() {
        assertThat(RequestLoggingFilter.resolveCorrelationId("bad id")).matches("^[0-9a-f-]{36}$");
    }

    @Test
    void logsRequestSummaryWithoutHeadersOrBody() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("Authorization", "Bearer secret-token");
        request.setContent("{\"password\":\"super-secret\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> response.setStatus(200));

        String logMessage = appender.list.getFirst().getFormattedMessage();
        assertThat(logMessage).contains("POST /auth/login");
        assertThat(logMessage).contains("status=200");
        assertThat(logMessage).contains("durationMs=");
        assertThat(logMessage).doesNotContain("secret-token");
        assertThat(logMessage).doesNotContain("super-secret");
        assertThat(logMessage).doesNotContain("Authorization");
        assertThat(logMessage).doesNotContain("password");
    }

    private static FilterChain noopChain() {
        return (ServletRequest request, ServletResponse response) -> {};
    }
}
