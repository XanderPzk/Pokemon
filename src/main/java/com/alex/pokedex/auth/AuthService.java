package com.alex.pokedex.auth;

import com.alex.pokedex.common.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public User register(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw ApiException.conflict("Email already registered: " + normalizedEmail);
        }
        String passwordHash = passwordEncoder.encode(rawPassword);
        return userRepository.save(User.newUser(normalizedEmail, passwordHash));
    }

    @Transactional(readOnly = true)
    public String login(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(() -> ApiException.unauthorized(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw ApiException.unauthorized(INVALID_CREDENTIALS_MESSAGE);
        }

        return jwtTokenProvider.issueToken(user);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
