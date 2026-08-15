package com.alex.pokedex.pokemon.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alex.pokedex.common.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PokemonTest {

    private static Pokemon.Snapshot sampleSnapshot() {
        return new Pokemon.Snapshot(
                25,
                "pikachu",
                "https://example.com/pikachu.png",
                "Mouse Pokémon",
                60,
                List.of("static"));
    }

    @Test
    void fromSnapshot_createsPokemonWithExternalId() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());

        assertThat(pokemon.getId()).isNull();
        assertThat(pokemon.getExternalId()).isEqualTo(25);
        assertThat(pokemon.getName()).isEqualTo("pikachu");
        assertThat(pokemon.getLocalName()).isEqualTo("pikachu");
        assertThat(pokemon.getRegion()).isNull();
        assertThat(pokemon.getInternalTags()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rename_blankName_throwsApiException(String blankName) {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());

        assertThatThrownBy(
                        () ->
                                pokemon.applyLocalEdits(
                                        new Pokemon.LocalEdits(
                                                Optional.ofNullable(blankName), null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Local name must not be blank");
    }

    @Test
    void rename_validName_updatesLocalName() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        pokemon.applyLocalEdits(new Pokemon.LocalEdits(Optional.of("Raichu"), null, null));

        assertThat(pokemon.getLocalName()).isEqualTo("Raichu");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void updateLocalFields_blankLocalName_throws(String blankName) {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());

        assertThatThrownBy(
                        () ->
                                pokemon.applyLocalEdits(
                                        new Pokemon.LocalEdits(
                                                Optional.ofNullable(blankName),
                                                Optional.of("Kanto"),
                                                Optional.of(List.of("starter")))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Local name must not be blank");
    }

    @Test
    void updateLocalFields_validValues_applied() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        pokemon.applyLocalEdits(
                new Pokemon.LocalEdits(
                        Optional.of("Sparky"),
                        Optional.of("Kanto"),
                        Optional.of(List.of("starter", "electric"))));

        assertThat(pokemon.getLocalName()).isEqualTo("Sparky");
        assertThat(pokemon.getRegion()).isEqualTo("Kanto");
        assertThat(pokemon.getInternalTags()).containsExactly("starter", "electric");
    }

    @Test
    void updateLocalFields_nullTags_clearsTags() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        pokemon.applyLocalEdits(
                new Pokemon.LocalEdits(
                        Optional.of("Sparky"),
                        Optional.of("Kanto"),
                        Optional.of(List.of("starter"))));
        pokemon.applyLocalEdits(
                new Pokemon.LocalEdits(
                        Optional.of("Sparky"), Optional.of("Johto"), Optional.empty()));

        assertThat(pokemon.getRegion()).isEqualTo("Johto");
        assertThat(pokemon.getInternalTags()).isEmpty();
    }

    @Test
    void updateLocalFields_nullTagElement_throws() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        List<String> tagsWithNull = new ArrayList<>();
        tagsWithNull.add("ok");
        tagsWithNull.add(null);

        assertThatThrownBy(
                        () ->
                                pokemon.applyLocalEdits(
                                        new Pokemon.LocalEdits(
                                                Optional.of("Sparky"),
                                                Optional.of("Kanto"),
                                                Optional.of(tagsWithNull))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not contain null");
    }

    @Test
    void refreshSnapshot_updatesPokeApiFieldsAndPreservesLocalCustomFields() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        Pokemon.Snapshot refreshedSnapshot =
                new Pokemon.Snapshot(
                        25,
                        "pikachu-updated",
                        "https://example.com/new.png",
                        "Electric Mouse Pokémon",
                        65,
                        List.of("static", "lightning-rod"));

        pokemon.refreshSnapshot(refreshedSnapshot);

        assertThat(pokemon.getName()).isEqualTo("pikachu-updated");
        assertThat(pokemon.getSpriteUrl()).isEqualTo("https://example.com/new.png");
        assertThat(pokemon.getCategory()).isEqualTo("Electric Mouse Pokémon");
        assertThat(pokemon.getMass()).isEqualTo(65);
        assertThat(pokemon.getAbilities()).containsExactly("static", "lightning-rod");
        assertThat(pokemon.getLocalName()).isEqualTo("Sparky");
        assertThat(pokemon.getRegion()).isEqualTo("Kanto");
        assertThat(pokemon.getInternalTags()).containsExactly("starter", "electric");
    }

    @Test
    void refreshSnapshot_differentExternalId_throwsApiException() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        Pokemon.Snapshot otherPokemon =
                new Pokemon.Snapshot(
                        26,
                        "raichu",
                        "https://example.com/raichu.png",
                        "Mouse Pokémon",
                        300,
                        List.of("static"));

        assertThatThrownBy(() -> pokemon.refreshSnapshot(otherPokemon))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("external id");
    }

    @Test
    void applyLocalEdits_partialUpdate_onlyChangesProvidedFields() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        pokemon.applyLocalEdits(new Pokemon.LocalEdits(Optional.of("Thunder"), null, null));

        assertThat(pokemon.getLocalName()).isEqualTo("Thunder");
        assertThat(pokemon.getRegion()).isEqualTo("Kanto");
        assertThat(pokemon.getInternalTags()).containsExactly("starter", "electric");
    }

    @Test
    void applyLocalEdits_explicitNullRegion_clearsRegion() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        pokemon.applyLocalEdits(new Pokemon.LocalEdits(null, Optional.empty(), null));

        assertThat(pokemon.getRegion()).isNull();
        assertThat(pokemon.getLocalName()).isEqualTo("Sparky");
    }

    @Test
    void applyLocalEdits_explicitNullInternalTags_clearsTags() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        pokemon.applyLocalEdits(new Pokemon.LocalEdits(null, null, Optional.empty()));

        assertThat(pokemon.getInternalTags()).isEmpty();
        assertThat(pokemon.getLocalName()).isEqualTo("Sparky");
    }

    @Test
    void applyLocalEdits_localNameIsTrimmed() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        pokemon.applyLocalEdits(new Pokemon.LocalEdits(Optional.of("  Thunder  "), null, null));

        assertThat(pokemon.getLocalName()).isEqualTo("Thunder");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void applyLocalEdits_blankLocalName_throws(String blankName) {
        Pokemon pokemon = samplePokemonWithLocalFields();

        assertThatThrownBy(
                        () ->
                                pokemon.applyLocalEdits(
                                        new Pokemon.LocalEdits(
                                                Optional.ofNullable(blankName), null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Local name must not be blank");
    }

    @Test
    void applyLocalEdits_allFieldsAbsent_throws() {
        Pokemon pokemon = samplePokemonWithLocalFields();

        assertThatThrownBy(() -> pokemon.applyLocalEdits(new Pokemon.LocalEdits(null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("At least one editable field");
    }

    @Test
    void applyLocalEdits_nullTagElement_throws() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        List<String> tagsWithNull = new ArrayList<>();
        tagsWithNull.add("ok");
        tagsWithNull.add(null);

        assertThatThrownBy(
                        () ->
                                pokemon.applyLocalEdits(
                                        new Pokemon.LocalEdits(
                                                null, null, Optional.of(tagsWithNull))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not contain null");
    }

    @Test
    void applyLocalEdits_tooManyTags_throws() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        List<String> tooManyTags = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tooManyTags.add("tag-" + i);
        }

        assertThatThrownBy(
                        () ->
                                pokemon.applyLocalEdits(
                                        new Pokemon.LocalEdits(
                                                null, null, Optional.of(tooManyTags))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not exceed");
    }

    @Test
    void applyLocalEdits_doesNotModifySnapshotFields() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        pokemon.applyLocalEdits(
                new Pokemon.LocalEdits(Optional.of("Thunder"), Optional.of("Johto"), null));

        assertThat(pokemon.getName()).isEqualTo("pikachu");
        assertThat(pokemon.getMass()).isEqualTo(60);
        assertThat(pokemon.getExternalId()).isEqualTo(25);
        assertThat(pokemon.getAbilities()).containsExactly("static");
    }

    private static Pokemon samplePokemonWithLocalFields() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        pokemon.applyLocalEdits(
                new Pokemon.LocalEdits(
                        Optional.of("Sparky"),
                        Optional.of("Kanto"),
                        Optional.of(List.of("starter", "electric"))));
        return pokemon;
    }
}
