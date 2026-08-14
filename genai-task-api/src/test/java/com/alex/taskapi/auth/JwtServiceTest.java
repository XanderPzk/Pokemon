package com.alex.taskapi.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtProperties properties =
            new JwtProperties(
                    "task-api-test-secret-key-at-least-32-bytes-long",
                    "genai-task-api-test",
                    Duration.ofMinutes(30));

    private final JwtService jwtService = new JwtService(properties);

    @Test
    void generateAndParseToken_roundTrip() {
        String token = jwtService.generateToken("user@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtService.parseEmail(token)).isEqualTo("user@example.com");
    }
}
