package com.alex.pokedex.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.alex.pokedex.support.PostgresTestSupport;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnabledIf("com.alex.pokedex.support.PostgresTestSupport#isAvailable")
class UserRepositoryTest {

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        PostgresTestSupport.register(registry, "it_user_repository");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired private UserRepository userRepository;

    @Test
    void save_assignsIdAndCreatedAt() {
        User user = User.newUser("save@example.com", "hashed-secret");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("save@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-secret");
        assertThat(saved.getRole()).isEqualTo(User.Role.USER);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByEmail_roundTripsHashAndRole() {
        User saved = userRepository.save(User.newUser("find@example.com", "hashed-secret"));

        User loaded = userRepository.findByEmail("find@example.com").orElseThrow();

        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getEmail()).isEqualTo("find@example.com");
        assertThat(loaded.getPasswordHash()).isEqualTo("hashed-secret");
        assertThat(loaded.getRole()).isEqualTo(User.Role.USER);
        assertThat(loaded.getCreatedAt().truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(saved.getCreatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void existsByEmail_returnsTrueWhenPresentAndFalseWhenMissing() {
        userRepository.save(User.newUser("exists@example.com", "hashed-secret"));

        assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }
}
