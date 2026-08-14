package com.alex.pokedex.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alex.pokedex.common.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void register_hashesPasswordAndPersistsNormalizedUser() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User registered = authService.register("  User@Example.COM  ", "secret");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-secret");
        assertThat(saved.getRole()).isEqualTo(User.Role.USER);
        assertThat(registered.getEmail()).isEqualTo("user@example.com");
        assertThat(registered.getPasswordHash()).isEqualTo("hashed-secret");
    }

    @Test
    void register_duplicateEmail_throwsWithoutSaving() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("user@example.com", "secret"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Email already registered");

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsToken() {
        User user = User.newUser("user@example.com", "hashed-secret");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed-secret")).thenReturn(true);
        when(jwtTokenProvider.issueToken(org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn("jwt-token");

        String token = authService.login("user@example.com", "secret");

        assertThat(token).isEqualTo("jwt-token");
        verify(jwtTokenProvider).issueToken(user);
    }

    @Test
    void login_unknownEmail_throwsWithoutIssuingToken() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.login("user@example.com", "secret"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid email or password");

        verify(passwordEncoder, never())
                .matches(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(jwtTokenProvider, never()).issueToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void login_wrongPassword_throwsWithoutIssuingToken() {
        User user = User.newUser("user@example.com", "hashed-secret");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-secret")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("user@example.com", "wrong"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtTokenProvider, never()).issueToken(org.mockito.ArgumentMatchers.any());
    }
}
