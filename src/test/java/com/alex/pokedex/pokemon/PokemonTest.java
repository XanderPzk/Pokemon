package com.alex.pokedex.pokemon;

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
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.ofNullable(blankName));

        assertThatThrownBy(() -> pokemon.applyLocalEdits(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Local name must not be blank");
    }

    @Test
    void rename_validName_updatesLocalName() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("Raichu"));

        pokemon.applyLocalEdits(request);

        assertThat(pokemon.getLocalName()).isEqualTo("Raichu");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void updateLocalFields_blankLocalName_throws(String blankName) {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.ofNullable(blankName));
        request.setRegion(Optional.of("Kanto"));
        request.setInternalTags(Optional.of(List.of("starter")));

        assertThatThrownBy(() -> pokemon.applyLocalEdits(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Local name must not be blank");
    }

    @Test
    void updateLocalFields_validValues_applied() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("Sparky"));
        request.setRegion(Optional.of("Kanto"));
        request.setInternalTags(Optional.of(List.of("starter", "electric")));

        pokemon.applyLocalEdits(request);

        assertThat(pokemon.getLocalName()).isEqualTo("Sparky");
        assertThat(pokemon.getRegion()).isEqualTo("Kanto");
        assertThat(pokemon.getInternalTags()).containsExactly("starter", "electric");
    }

    @Test
    void updateLocalFields_nullTags_clearsTags() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        PokemonController.UpdatePokemonRequest initial =
                new PokemonController.UpdatePokemonRequest();
        initial.setLocalName(Optional.of("Sparky"));
        initial.setRegion(Optional.of("Kanto"));
        initial.setInternalTags(Optional.of(List.of("starter")));
        pokemon.applyLocalEdits(initial);

        PokemonController.UpdatePokemonRequest clearTags =
                new PokemonController.UpdatePokemonRequest();
        clearTags.setLocalName(Optional.of("Sparky"));
        clearTags.setRegion(Optional.of("Johto"));
        clearTags.setInternalTags(Optional.empty());
        pokemon.applyLocalEdits(clearTags);

        assertThat(pokemon.getRegion()).isEqualTo("Johto");
        assertThat(pokemon.getInternalTags()).isEmpty();
    }

    @Test
    void updateLocalFields_nullTagElement_throws() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        List<String> tagsWithNull = new ArrayList<>();
        tagsWithNull.add("ok");
        tagsWithNull.add(null);
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("Sparky"));
        request.setRegion(Optional.of("Kanto"));
        request.setInternalTags(Optional.of(tagsWithNull));

        assertThatThrownBy(() -> pokemon.applyLocalEdits(request))
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
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("Thunder"));

        pokemon.applyLocalEdits(request);

        assertThat(pokemon.getLocalName()).isEqualTo("Thunder");
        assertThat(pokemon.getRegion()).isEqualTo("Kanto");
        assertThat(pokemon.getInternalTags()).containsExactly("starter", "electric");
    }

    @Test
    void applyLocalEdits_explicitNullRegion_clearsRegion() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setRegion(Optional.empty());

        pokemon.applyLocalEdits(request);

        assertThat(pokemon.getRegion()).isNull();
        assertThat(pokemon.getLocalName()).isEqualTo("Sparky");
    }

    @Test
    void applyLocalEdits_explicitNullInternalTags_clearsTags() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setInternalTags(Optional.empty());

        pokemon.applyLocalEdits(request);

        assertThat(pokemon.getInternalTags()).isEmpty();
        assertThat(pokemon.getLocalName()).isEqualTo("Sparky");
    }

    @Test
    void applyLocalEdits_localNameIsTrimmed() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("  Thunder  "));

        pokemon.applyLocalEdits(request);

        assertThat(pokemon.getLocalName()).isEqualTo("Thunder");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void applyLocalEdits_blankLocalName_throws(String blankName) {
        Pokemon pokemon = samplePokemonWithLocalFields();
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.ofNullable(blankName));

        assertThatThrownBy(() -> pokemon.applyLocalEdits(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Local name must not be blank");
    }

    @Test
    void applyLocalEdits_allFieldsAbsent_throws() {
        Pokemon pokemon = samplePokemonWithLocalFields();

        assertThatThrownBy(
                        () -> pokemon.applyLocalEdits(new PokemonController.UpdatePokemonRequest()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("At least one editable field");
    }

    @Test
    void applyLocalEdits_nullTagElement_throws() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        List<String> tagsWithNull = new ArrayList<>();
        tagsWithNull.add("ok");
        tagsWithNull.add(null);
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setInternalTags(Optional.of(tagsWithNull));

        assertThatThrownBy(() -> pokemon.applyLocalEdits(request))
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
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setInternalTags(Optional.of(tooManyTags));

        assertThatThrownBy(() -> pokemon.applyLocalEdits(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not exceed");
    }

    @Test
    void applyLocalEdits_doesNotModifySnapshotFields() {
        Pokemon pokemon = samplePokemonWithLocalFields();
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("Thunder"));
        request.setRegion(Optional.of("Johto"));

        pokemon.applyLocalEdits(request);

        assertThat(pokemon.getName()).isEqualTo("pikachu");
        assertThat(pokemon.getMass()).isEqualTo(60);
        assertThat(pokemon.getExternalId()).isEqualTo(25);
        assertThat(pokemon.getAbilities()).containsExactly("static");
    }

    private static Pokemon samplePokemonWithLocalFields() {
        Pokemon pokemon = Pokemon.fromSnapshot(sampleSnapshot());
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("Sparky"));
        request.setRegion(Optional.of("Kanto"));
        request.setInternalTags(Optional.of(List.of("starter", "electric")));
        pokemon.applyLocalEdits(request);
        return pokemon;
    }
}
