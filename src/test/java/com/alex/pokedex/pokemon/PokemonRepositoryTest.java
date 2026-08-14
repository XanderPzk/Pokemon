package com.alex.pokedex.pokemon;

import static org.assertj.core.api.Assertions.assertThat;

import com.alex.pokedex.support.PostgresTestSupport;
import java.util.List;
import java.util.Optional;
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
class PokemonRepositoryTest {

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        PostgresTestSupport.register(registry, "it_pokemon_repository");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired private PokemonRepository pokemonRepository;

    @Test
    void save_persistsPokemonWithCollections() {
        Pokemon pokemon =
                Pokemon.fromSnapshot(
                        new Pokemon.Snapshot(
                                25,
                                "pikachu",
                                "https://example.com/pikachu.png",
                                "Mouse Pokémon",
                                60,
                                List.of("static", "lightning-rod")));
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("Sparky"));
        request.setRegion(Optional.of("Kanto"));
        request.setInternalTags(Optional.of(List.of("starter", "electric")));
        pokemon.applyLocalEdits(request);

        Pokemon saved = pokemonRepository.save(pokemon);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getExternalId()).isEqualTo(25);
        assertThat(saved.getName()).isEqualTo("pikachu");
        assertThat(saved.getSpriteUrl()).isEqualTo("https://example.com/pikachu.png");
        assertThat(saved.getCategory()).isEqualTo("Mouse Pokémon");
        assertThat(saved.getMass()).isEqualTo(60);
        assertThat(saved.getAbilities()).containsExactly("static", "lightning-rod");
        assertThat(saved.getLocalName()).isEqualTo("Sparky");
        assertThat(saved.getRegion()).isEqualTo("Kanto");
        assertThat(saved.getInternalTags()).containsExactly("starter", "electric");

        Pokemon loaded = pokemonRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getExternalId()).isEqualTo(saved.getExternalId());
        assertThat(loaded.getName()).isEqualTo(saved.getName());
        assertThat(loaded.getSpriteUrl()).isEqualTo(saved.getSpriteUrl());
        assertThat(loaded.getCategory()).isEqualTo(saved.getCategory());
        assertThat(loaded.getMass()).isEqualTo(saved.getMass());
        assertThat(loaded.getAbilities()).isEqualTo(saved.getAbilities());
        assertThat(loaded.getLocalName()).isEqualTo(saved.getLocalName());
        assertThat(loaded.getRegion()).isEqualTo(saved.getRegion());
        assertThat(loaded.getInternalTags()).isEqualTo(saved.getInternalTags());
    }

    @Test
    void save_upsertsOnExternalIdWithoutCreatingDuplicate() {
        Pokemon initial =
                Pokemon.fromSnapshot(
                        new Pokemon.Snapshot(
                                1,
                                "bulbasaur",
                                "https://example.com/1.png",
                                "Seed Pokémon",
                                69,
                                List.of("overgrow")));
        PokemonController.UpdatePokemonRequest initialEdits =
                new PokemonController.UpdatePokemonRequest();
        initialEdits.setLocalName(Optional.of("Bulby"));
        initialEdits.setRegion(Optional.of("Kanto"));
        initialEdits.setInternalTags(Optional.of(List.of("starter")));
        initial.applyLocalEdits(initialEdits);
        Pokemon saved = pokemonRepository.save(initial);

        saved.refreshSnapshot(
                new Pokemon.Snapshot(
                        1,
                        "bulbasaur-updated",
                        "https://example.com/1-new.png",
                        "Seed Pokémon",
                        70,
                        List.of("overgrow", "chlorophyll")));
        PokemonController.UpdatePokemonRequest updatedEdits =
                new PokemonController.UpdatePokemonRequest();
        updatedEdits.setLocalName(Optional.of("Bulby"));
        updatedEdits.setRegion(Optional.of("Johto"));
        updatedEdits.setInternalTags(Optional.of(List.of("grass")));
        saved.applyLocalEdits(updatedEdits);

        Pokemon upserted = pokemonRepository.save(saved);

        assertThat(upserted.getId()).isEqualTo(saved.getId());
        assertThat(upserted.getName()).isEqualTo("bulbasaur-updated");
        assertThat(upserted.getSpriteUrl()).isEqualTo("https://example.com/1-new.png");
        assertThat(upserted.getMass()).isEqualTo(70);
        assertThat(upserted.getAbilities()).containsExactly("overgrow", "chlorophyll");
        assertThat(upserted.getLocalName()).isEqualTo("Bulby");
        assertThat(upserted.getRegion()).isEqualTo("Johto");
        assertThat(upserted.getInternalTags()).containsExactly("grass");

        assertThat(pokemonRepository.findByExternalId(1)).isPresent();
    }

    @Test
    void findByIdAndExternalId_returnEmptyWhenMissing() {
        assertThat(pokemonRepository.findById(999L)).isEmpty();
        assertThat(pokemonRepository.findByExternalId(999)).isEmpty();
    }

    @Test
    void save_afterLocalEdits_roundTripsClearedRegionAndReplacedTags() {
        Pokemon pokemon =
                Pokemon.fromSnapshot(
                        new Pokemon.Snapshot(
                                25,
                                "pikachu",
                                "https://example.com/pikachu.png",
                                "Mouse Pokémon",
                                60,
                                List.of("static")));
        PokemonController.UpdatePokemonRequest initialEdits =
                new PokemonController.UpdatePokemonRequest();
        initialEdits.setLocalName(Optional.of("Sparky"));
        initialEdits.setRegion(Optional.of("Kanto"));
        initialEdits.setInternalTags(Optional.of(List.of("starter", "electric")));
        pokemon.applyLocalEdits(initialEdits);
        Pokemon saved = pokemonRepository.save(pokemon);

        PokemonController.UpdatePokemonRequest patch = new PokemonController.UpdatePokemonRequest();
        patch.setRegion(Optional.empty());
        patch.setInternalTags(Optional.of(List.of("legendary")));
        saved.applyLocalEdits(patch);
        Pokemon updated = pokemonRepository.save(saved);

        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getRegion()).isNull();
        assertThat(updated.getInternalTags()).containsExactly("legendary");

        Pokemon loaded = pokemonRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getRegion()).isNull();
        assertThat(loaded.getInternalTags()).containsExactly("legendary");
        assertThat(loaded.getLocalName()).isEqualTo("Sparky");
    }
}
