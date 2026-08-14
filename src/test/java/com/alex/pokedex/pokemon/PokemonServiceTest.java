package com.alex.pokedex.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alex.pokedex.common.ApiException;
import com.alex.pokedex.pokemon.PokeApiDtos.EvolutionChainResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.NamedResource;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonListResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonSpeciesResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PokemonServiceTest {

    @Mock private PokeApiHttpClient pokeApiHttpClient;

    @Mock private PokemonRepository pokemonRepository;

    private PokemonService pokemonService;

    @BeforeEach
    void setUp() {
        pokemonService = new PokemonService(pokeApiHttpClient, pokemonRepository);
    }

    @Test
    void list_delegatesToPokeApiAndMapsSummaries() {
        PokemonListResponse listResponse =
                new PokemonListResponse(
                        1351,
                        List.of(
                                new PokemonListResponse.Entry(
                                        "bulbasaur", "https://pokeapi.co/api/v2/pokemon/1/"),
                                new PokemonListResponse.Entry(
                                        "ivysaur", "https://pokeapi.co/api/v2/pokemon/2/")));
        when(pokeApiHttpClient.fetchList(2, 2)).thenReturn(listResponse);
        when(pokeApiHttpClient.fetchPokemon("1")).thenReturn(pokemon(1, "bulbasaur", 69));
        when(pokeApiHttpClient.fetchPokemon("2")).thenReturn(pokemon(2, "ivysaur", 130));
        when(pokeApiHttpClient.fetchSpecies(1)).thenReturn(species(1, "Seed Pokémon"));
        when(pokeApiHttpClient.fetchSpecies(2)).thenReturn(species(2, "Seed Pokémon"));

        PokemonController.PageResponse<PokemonController.PokemonSummaryResponse> result =
                pokemonService.list(1, 2);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(1351);
        assertThat(result.totalPages()).isEqualTo(676);
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).name()).isEqualTo("bulbasaur");
        verify(pokeApiHttpClient).fetchList(2, 2);
    }

    @Test
    void list_rejectsInvalidSize() {
        assertThatThrownBy(() -> pokemonService.list(0, 0))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("size");

        verify(pokeApiHttpClient, never()).fetchList(any(Integer.class), any(Integer.class));
    }

    @Test
    void getByIdOrName_delegatesToPokeApiClient() {
        when(pokeApiHttpClient.fetchPokemon("bulbasaur"))
                .thenReturn(
                        new PokemonResponse(
                                1,
                                "bulbasaur",
                                69,
                                new PokemonResponse.Sprites("https://example.com/1.png"),
                                List.of(
                                        new PokemonResponse.AbilitySlot(
                                                new NamedResource("overgrow", null))),
                                List.of(),
                                new NamedResource(
                                        "bulbasaur",
                                        "https://pokeapi.co/api/v2/pokemon-species/1/")));
        when(pokeApiHttpClient.fetchSpecies(1))
                .thenReturn(
                        new PokemonSpeciesResponse(
                                List.of(
                                        new PokemonSpeciesResponse.Genus(
                                                "Seed Pokémon", new NamedResource("en", null))),
                                List.of(),
                                null));

        PokemonController.PokemonDetailResponse result = pokemonService.getByIdOrName("bulbasaur");

        assertThat(result.name()).isEqualTo("bulbasaur");
        assertThat(result.category()).isEqualTo("Seed Pokémon");
        verify(pokeApiHttpClient).fetchPokemon("bulbasaur");
    }

    @Test
    void getByIdOrName_propagatesNotFound() {
        when(pokeApiHttpClient.fetchPokemon("missingno"))
                .thenThrow(ApiException.notFound("Pokemon 'missingno' not found in PokeAPI"));

        assertThatThrownBy(() -> pokemonService.getByIdOrName("missingno"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("missingno");
    }

    @Test
    void getByIdOrName_rejectsBlankIdentifier() {
        assertThatThrownBy(() -> pokemonService.getByIdOrName("   "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not be blank");

        verify(pokeApiHttpClient, never()).fetchPokemon(any());
    }

    @Test
    void sync_firstSync_createsRecordFromSnapshotWithDefaultLocalName() {
        stubDetailFixtures("pikachu");
        when(pokemonRepository.findByExternalId(25)).thenReturn(Optional.empty());
        when(pokemonRepository.save(any(Pokemon.class)))
                .thenAnswer(
                        invocation -> {
                            Pokemon saved = invocation.getArgument(0);
                            org.springframework.test.util.ReflectionTestUtils.setField(
                                    saved, "id", 1L);
                            return saved;
                        });

        PokemonController.SyncedPokemonResponse result = pokemonService.sync("pikachu");

        assertThat(result.localId()).isEqualTo(1L);
        assertThat(result.externalId()).isEqualTo(25);
        assertThat(result.name()).isEqualTo("pikachu");
        assertThat(result.localName()).isEqualTo("pikachu");
        assertThat(result.region()).isNull();
        assertThat(result.internalTags()).isEmpty();

        ArgumentCaptor<Pokemon> captor = ArgumentCaptor.forClass(Pokemon.class);
        verify(pokemonRepository).save(captor.capture());
        assertThat(captor.getValue().getLocalName()).isEqualTo("pikachu");
    }

    @Test
    void sync_resync_refreshesSnapshotAndPreservesCustomFields() {
        Pokemon existing = syncedPokemonWithLocalFields();
        org.springframework.test.util.ReflectionTestUtils.setField(existing, "id", 1L);
        stubRefreshedDetailFixtures("pikachu");
        when(pokemonRepository.findByExternalId(25)).thenReturn(Optional.of(existing));
        when(pokemonRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(pokemonRepository.save(existing)).thenReturn(existing);

        PokemonController.SyncedPokemonResponse result = pokemonService.sync("pikachu");

        assertThat(result.localId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("pikachu-updated");
        assertThat(result.spriteUrl()).isEqualTo("https://example.com/new.png");
        assertThat(result.category()).isEqualTo("Electric Mouse Pokémon");
        assertThat(result.mass()).isEqualTo(65);
        assertThat(result.abilities()).containsExactly("static", "lightning-rod");
        assertThat(result.localName()).isEqualTo("Sparky");
        assertThat(result.region()).isEqualTo("Kanto");
        assertThat(result.internalTags()).containsExactly("starter", "electric");
        verify(pokemonRepository).save(existing);
    }

    @Test
    void sync_pokemonNotFoundInPokeApi_propagatesException() {
        when(pokeApiHttpClient.fetchPokemon("missingno"))
                .thenThrow(ApiException.notFound("Pokemon 'missingno' not found in PokeAPI"));

        assertThatThrownBy(() -> pokemonService.sync("missingno"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("missingno");

        verify(pokemonRepository, never()).save(any());
    }

    @Test
    void update_existingPokemon_appliesEditsAndSaves() {
        Pokemon existing = syncedPokemonWithLocalFields();
        org.springframework.test.util.ReflectionTestUtils.setField(existing, "id", 1L);
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("Thunder"));
        request.setRegion(Optional.of("Johto"));
        when(pokemonRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(pokemonRepository.save(existing)).thenReturn(existing);

        PokemonController.SyncedPokemonResponse result = pokemonService.update(1L, request);

        assertThat(result.localName()).isEqualTo("Thunder");
        assertThat(result.region()).isEqualTo("Johto");
        assertThat(result.internalTags()).containsExactly("starter", "electric");
        verify(pokemonRepository).save(existing);
    }

    @Test
    void update_missingPokemon_throwsNotFoundAndDoesNotSave() {
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("Thunder"));
        when(pokemonRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pokemonService.update(999L, request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Pokemon with id 999 not found");

        verify(pokemonRepository, never()).save(any());
    }

    @Test
    void update_validationFailure_propagatesAndDoesNotSave() {
        Pokemon existing = syncedPokemonWithLocalFields();
        org.springframework.test.util.ReflectionTestUtils.setField(existing, "id", 1L);
        when(pokemonRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(
                        () ->
                                pokemonService.update(
                                        1L, new PokemonController.UpdatePokemonRequest()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("At least one editable field");

        verify(pokemonRepository, never()).save(any());
    }

    private void stubDetailFixtures(String idOrName) {
        when(pokeApiHttpClient.fetchPokemon(idOrName))
                .thenReturn(
                        new PokemonResponse(
                                25,
                                "pikachu",
                                60,
                                new PokemonResponse.Sprites("https://example.com/pikachu.png"),
                                List.of(
                                        new PokemonResponse.AbilitySlot(
                                                new NamedResource("static", null))),
                                List.of(),
                                new NamedResource(
                                        "pikachu",
                                        "https://pokeapi.co/api/v2/pokemon-species/25/")));
        when(pokeApiHttpClient.fetchSpecies(25))
                .thenReturn(
                        new PokemonSpeciesResponse(
                                List.of(
                                        new PokemonSpeciesResponse.Genus(
                                                "Mouse Pokémon", new NamedResource("en", null))),
                                List.of(),
                                new NamedResource(
                                        "chain", "https://pokeapi.co/api/v2/evolution-chain/1/")));
        when(pokeApiHttpClient.fetchEvolutionChain(1)).thenReturn(new EvolutionChainResponse(null));
    }

    private void stubRefreshedDetailFixtures(String idOrName) {
        when(pokeApiHttpClient.fetchPokemon(idOrName))
                .thenReturn(
                        new PokemonResponse(
                                25,
                                "pikachu-updated",
                                65,
                                new PokemonResponse.Sprites("https://example.com/new.png"),
                                List.of(
                                        new PokemonResponse.AbilitySlot(
                                                new NamedResource("static", null)),
                                        new PokemonResponse.AbilitySlot(
                                                new NamedResource("lightning-rod", null))),
                                List.of(),
                                new NamedResource(
                                        "pikachu",
                                        "https://pokeapi.co/api/v2/pokemon-species/25/")));
        when(pokeApiHttpClient.fetchSpecies(25))
                .thenReturn(
                        new PokemonSpeciesResponse(
                                List.of(
                                        new PokemonSpeciesResponse.Genus(
                                                "Electric Mouse Pokémon",
                                                new NamedResource("en", null))),
                                List.of(),
                                null));
    }

    private static Pokemon syncedPokemonWithLocalFields() {
        Pokemon pokemon =
                Pokemon.fromSnapshot(
                        new Pokemon.Snapshot(
                                25,
                                "pikachu",
                                "https://example.com/pikachu.png",
                                "Mouse Pokémon",
                                60,
                                List.of("static")));
        PokemonController.UpdatePokemonRequest request =
                new PokemonController.UpdatePokemonRequest();
        request.setLocalName(Optional.of("Sparky"));
        request.setRegion(Optional.of("Kanto"));
        request.setInternalTags(Optional.of(List.of("starter", "electric")));
        pokemon.applyLocalEdits(request);
        return pokemon;
    }

    private static PokemonResponse pokemon(int id, String name, int weight) {
        return new PokemonResponse(
                id,
                name,
                weight,
                new PokemonResponse.Sprites("https://example.com/" + id + ".png"),
                List.of(new PokemonResponse.AbilitySlot(new NamedResource("overgrow", null))),
                List.of(),
                new NamedResource(name, "https://pokeapi.co/api/v2/pokemon-species/" + id + "/"));
    }

    private static PokemonSpeciesResponse species(int id, String category) {
        return new PokemonSpeciesResponse(
                List.of(new PokemonSpeciesResponse.Genus(category, new NamedResource("en", null))),
                List.of(),
                null);
    }
}
