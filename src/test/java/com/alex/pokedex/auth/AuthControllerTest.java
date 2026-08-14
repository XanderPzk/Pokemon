package com.alex.pokedex.auth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alex.pokedex.common.ApiException;
import com.alex.pokedex.common.GlobalExceptionHandler;
import com.alex.pokedex.config.SecurityConfig;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {

    private static final String VALID_TOKEN = "valid-token";

    @Autowired private MockMvc mockMvc;

    @MockBean private AuthService authService;

    @MockBean private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUpJwtParser() {
        when(jwtTokenProvider.parse(VALID_TOKEN))
                .thenReturn(User.fromTokenClaims("user@example.com", User.Role.USER));
    }

    @Test
    void register_returns201WithoutPasswordHash() throws Exception {
        User registered = User.newUser("user@example.com", "hashed-secret");
        ReflectionTestUtils.setField(registered, "id", 1L);
        ReflectionTestUtils.setField(
                registered, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        when(authService.register(anyString(), anyString())).thenReturn(registered);

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_invalidEmail_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"not-an-email\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void register_shortPassword_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(anyString(), anyString()))
                .thenThrow(ApiException.conflict("Email already registered: user@example.com"));

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(
                        jsonPath("$.message").value("Email already registered: user@example.com"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void login_returnsToken() throws Exception {
        when(authService.login(anyString(), anyString())).thenReturn("jwt-token");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_badCredentials_returns401Envelope() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(ApiException.unauthorized("Invalid email or password"));

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"user@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void me_withValidToken_returns200() throws Exception {
        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
