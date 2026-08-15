package com.alex.pokedex.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alex.pokedex.auth.repository.User;
import com.alex.pokedex.config.SecurityConfig.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "pokedex-test-secret-key-at-least-32-bytes-long";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider =
                new JwtTokenProvider(
                        new JwtProperties(SECRET, "pokedex-test", Duration.ofHours(1)));
    }

    @Test
    void issueToken_roundTripsSubjectAndRole() {
        User user = User.newUser("user@example.com", "hashed-secret");

        String token = jwtTokenProvider.issueToken(user);
        User parsed = jwtTokenProvider.parse(token);

        assertThat(parsed.getEmail()).isEqualTo("user@example.com");
        assertThat(parsed.getRole()).isEqualTo(User.Role.USER);
    }

    @Test
    void parse_rejectsTamperedSignature() {
        String token =
                jwtTokenProvider.issueToken(User.newUser("user@example.com", "hashed-secret"));
        String[] parts = token.split("\\.", 3);
        char[] signature = parts[2].toCharArray();
        signature[0] = signature[0] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + new String(signature);

        assertThatThrownBy(() -> jwtTokenProvider.parse(tampered))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void parse_rejectsExpiredToken() {
        JwtTokenProvider expiredProvider =
                new JwtTokenProvider(
                        new JwtProperties(SECRET, "pokedex-test", Duration.ofSeconds(-1)));
        String token =
                expiredProvider.issueToken(User.newUser("user@example.com", "hashed-secret"));

        assertThatThrownBy(() -> jwtTokenProvider.parse(token))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void parse_rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtTokenProvider.parse("not-a-jwt"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void parse_rejectsTokenSignedWithDifferentSecret() {
        String otherSecret = "another-test-secret-key-at-least-32-bytes";
        String token =
                Jwts.builder()
                        .subject("user@example.com")
                        .issuer("pokedex-test")
                        .claim(JwtTokenProvider.ROLE_CLAIM, User.Role.USER.name())
                        .issuedAt(Date.from(Instant.now()))
                        .expiration(Date.from(Instant.now().plusSeconds(3600)))
                        .signWith(Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8)))
                        .compact();

        assertThatThrownBy(() -> jwtTokenProvider.parse(token))
                .isInstanceOf(RuntimeException.class);
    }
}
