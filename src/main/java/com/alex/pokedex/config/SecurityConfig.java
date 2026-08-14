package com.alex.pokedex.config;

import com.alex.pokedex.auth.JwtTokenProvider;
import com.alex.pokedex.common.GlobalExceptionHandler;
import com.alex.pokedex.common.RequestLoggingFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableConfigurationProperties(SecurityConfig.JwtProperties.class)
public class SecurityConfig {

    @ConfigurationProperties(prefix = "security.jwt")
    public record JwtProperties(String secret, String issuer, Duration expiration) {}

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper)
            throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtTokenProvider);

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers("/health", "/actuator/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        .requestMatchers("/auth/register", "/auth/login")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/pokemon/**")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/pokemon/*/sync")
                                        .authenticated()
                                        .requestMatchers(HttpMethod.PATCH, "/pokemon/local/**")
                                        .authenticated()
                                        .requestMatchers("/auth/me")
                                        .authenticated()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(
                                                (request, response, authException) ->
                                                        writeErrorResponse(
                                                                objectMapper,
                                                                response,
                                                                HttpStatus.UNAUTHORIZED,
                                                                "UNAUTHORIZED",
                                                                "Authentication required"))
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) ->
                                                        writeErrorResponse(
                                                                objectMapper,
                                                                response,
                                                                HttpStatus.FORBIDDEN,
                                                                "FORBIDDEN",
                                                                "Access denied")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    static class JwtAuthFilter extends OncePerRequestFilter {

        private static final String BEARER_PREFIX = "Bearer ";

        private final JwtTokenProvider jwtTokenProvider;

        JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
            this.jwtTokenProvider = jwtTokenProvider;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                    String token = authorization.substring(BEARER_PREFIX.length()).trim();
                    if (!token.isEmpty()) {
                        try {
                            var user = jwtTokenProvider.parse(token);
                            var authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            user.getEmail(),
                                            null,
                                            java.util.List.of(
                                                    new SimpleGrantedAuthority(
                                                            "ROLE_" + user.getRole().name())));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } catch (RuntimeException ignored) {
                            SecurityContextHolder.clearContext();
                        }
                    }
                }
            }

            filterChain.doFilter(request, response);
        }
    }

    private static void writeErrorResponse(
            ObjectMapper objectMapper,
            HttpServletResponse response,
            HttpStatus status,
            String error,
            String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        GlobalExceptionHandler.ErrorResponse body =
                new GlobalExceptionHandler.ErrorResponse(
                        status.value(),
                        error,
                        message,
                        java.time.Instant.now(),
                        MDC.get(RequestLoggingFilter.MDC_KEY));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
