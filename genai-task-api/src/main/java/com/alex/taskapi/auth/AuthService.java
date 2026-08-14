package com.alex.taskapi.auth;

import com.alex.taskapi.auth.dto.AuthTokenResponse;
import com.alex.taskapi.auth.dto.LoginRequest;
import com.alex.taskapi.auth.dto.RegisterRequest;
import com.alex.taskapi.common.DuplicateEmailException;
import com.alex.taskapi.common.InvalidCredentialsException;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Email already registered: " + request.email());
        }

        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        AppUser saved = userRepository.save(user);

        return Map.of(
                "id", saved.getId(),
                "email", saved.getEmail(),
                "createdAt", saved.getCreatedAt());
    }

    public AuthTokenResponse login(LoginRequest request) {
        AppUser user =
                userRepository
                        .findByEmail(request.email())
                        .filter(
                                existing ->
                                        passwordEncoder.matches(
                                                request.password(), existing.getPasswordHash()))
                        .orElseThrow(
                                () -> new InvalidCredentialsException("Invalid email or password"));

        return new AuthTokenResponse(jwtService.generateToken(user.getEmail()));
    }

    public Map<String, String> currentUser(String email) {
        return Map.of("email", email);
    }
}
