package com.alex.pokedex.pokemon.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.alex.pokedex.support.PostgresTestSupport;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
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
class PokemonBatchFetchTest {

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        PostgresTestSupport.register(registry, "it_pokemon_batch_fetch");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired private PokemonRepository pokemonRepository;

    @Autowired private EntityManager entityManager;

    @BeforeEach
    void seedPokemon() {
        for (int id = 1; id <= 5; id++) {
            Pokemon pokemon =
                    Pokemon.fromSnapshot(
                            new Pokemon.Snapshot(
                                    id,
                                    "pokemon-" + id,
                                    "https://example.com/" + id + ".png",
                                    "Category " + id,
                                    50 + id,
                                    List.of("ability-" + id)));
            pokemon.applyLocalEdits(
                    new Pokemon.LocalEdits(
                            Optional.of("Local " + id),
                            Optional.of("Region " + id),
                            Optional.of(List.of("tag-" + id))));
            pokemonRepository.save(pokemon);
        }
        entityManager.flush();
        entityManager.clear();
        statistics().clear();
    }

    @Test
    void findAll_usesBatchFetchForElementCollections() {
        List<Pokemon> loaded = pokemonRepository.findAll();
        entityManager.flush();

        assertThat(loaded).hasSize(5);
        assertThat(statistics().getPrepareStatementCount()).isLessThan(6);
    }

    private Statistics statistics() {
        return entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
    }
}
