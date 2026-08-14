package com.alex.pokedex.auth;

import com.alex.pokedex.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Register, login, and inspect the authenticated user")
@RestController
@RequestMapping("/auth")
public class AuthController {

    public record LoginRequest(
            @NotBlank(message = "Email must not be blank") @Email(message = "Email must be valid")
                    String email,
            @NotBlank(message = "Password must not be blank") String password) {}

    public record RegisterRequest(
            @NotBlank(message = "Email must not be blank") @Email(message = "Email must be valid")
                    String email,
            @NotBlank(message = "Password must not be blank")
                    @Size(min = 8, message = "Password must be at least 8 characters")
                    String password) {}

    public record TokenResponse(String token) {}

    public record UserResponse(Long id, String email, User.Role role, Instant createdAt) {}

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@jakarta.validation.Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.email(), request.password());
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    @Operation(summary = "Authenticate and receive a JWT")
    @PostMapping("/login")
    public TokenResponse login(@jakarta.validation.Valid @RequestBody LoginRequest request) {
        return new TokenResponse(authService.login(request.email(), request.password()));
    }

    @Operation(summary = "Get the authenticated user profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User.Role role =
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .map(authority -> User.Role.valueOf(authority.substring("ROLE_".length())))
                        .findFirst()
                        .orElse(User.Role.USER);
        return new UserResponse(null, authentication.getName(), role, null);
    }
}
