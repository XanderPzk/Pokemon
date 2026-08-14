package com.alex.pokedex.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.alex.pokedex.auth.User;
import com.alex.pokedex.auth.UserRepository;
import com.alex.pokedex.pokemon.Pokemon;
import com.alex.pokedex.pokemon.PokemonRepository;
import com.alex.pokedex.support.PostgresTestSupport;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
@Transactional
@EnabledIf("com.alex.pokedex.support.PostgresTestSupport#isAvailable")
class DemoSeedDataTest {

    static final String DEMO_EMAIL = "demo@example.com";
    static final String DEMO_PASSWORD = "demo1234";
    static final List<Integer> SEEDED_EXTERNAL_IDS = List.of(1, 4, 7);

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        PostgresTestSupport.register(registry, "it_demo_seed");
    }

    @Autowired private UserRepository userRepository;
    @Autowired private PokemonRepository pokemonRepository;
    @Autowired private DataSource dataSource;

    @Test
    void demoProfileSeedsUserWithValidPasswordHash() {
        User user =
                userRepository
                        .findByEmail(DEMO_EMAIL)
                        .orElseThrow(() -> new AssertionError("Demo user not seeded"));

        assertThat(user.getRole()).isEqualTo(User.Role.USER);
        assertThat(new BCryptPasswordEncoder().matches(DEMO_PASSWORD, user.getPasswordHash()))
                .isTrue();
    }

    @Test
    void demoProfileSeedsSyncedPokemonWithOrderedCollections() {
        for (int externalId : SEEDED_EXTERNAL_IDS) {
            Pokemon pokemon =
                    pokemonRepository
                            .findByExternalId(externalId)
                            .orElseThrow(
                                    () ->
                                            new AssertionError(
                                                    "Pokemon external_id="
                                                            + externalId
                                                            + " not seeded"));

            assertThat(pokemon.getLocalName()).isNotBlank();
            assertThat(pokemon.getRegion()).isNotBlank();
            assertThat(pokemon.getAbilities()).doesNotContainNull().isNotEmpty();
            assertThat(pokemon.getInternalTags()).doesNotContainNull().isNotEmpty();
        }

        Pokemon bulbasaur = pokemonRepository.findByExternalId(1).orElseThrow();
        assertThat(bulbasaur.getAbilities()).containsExactly("overgrow", "chlorophyll");
        assertThat(bulbasaur.getInternalTags()).containsExactly("starter", "kanto");
    }

    @Test
    void seedScriptIsIdempotent() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        int usersBefore = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        int pokemonBefore = jdbc.queryForObject("SELECT COUNT(*) FROM pokemon", Integer.class);
        int abilitiesBefore =
                jdbc.queryForObject("SELECT COUNT(*) FROM pokemon_ability", Integer.class);
        int tagsBefore =
                jdbc.queryForObject("SELECT COUNT(*) FROM pokemon_internal_tag", Integer.class);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/seed/R__seed_demo_data.sql"));
        populator.execute(dataSource);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class))
                .isEqualTo(usersBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pokemon", Integer.class))
                .isEqualTo(pokemonBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pokemon_ability", Integer.class))
                .isEqualTo(abilitiesBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pokemon_internal_tag", Integer.class))
                .isEqualTo(tagsBefore);
    }
}
