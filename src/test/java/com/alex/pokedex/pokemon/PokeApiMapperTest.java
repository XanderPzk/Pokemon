package com.alex.pokedex.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alex.pokedex.common.ApiException;
import com.alex.pokedex.pokemon.PokeApiDtos.EvolutionChainResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.NamedResource;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonSpeciesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PokeApiMapperTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static <T> T fixture(String name, Class<T> type) {
        try (InputStream in =
                PokeApiMapperTest.class.getResourceAsStream("/pokeapi/" + name + ".json")) {
            assertThat(in).as("fixture %s must exist on the test classpath", name).isNotNull();
            return JSON.readValue(in, type);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read fixture " + name, e);
        }
    }

    private static PokemonResponse pokemon(int id) {
        return fixture("pokemon-" + id, PokemonResponse.class);
    }

    private static PokemonSpeciesResponse species(int id) {
        return fixture("pokemon-species-" + id, PokemonSpeciesResponse.class);
    }

    @Test
    void toSummaryResponse_mapsBulbasaurFromRealPayload() {
        PokemonController.PokemonSummaryResponse summary =
                PokeApiMapper.toSummaryResponse(pokemon(1), species(1));

        assertThat(summary.id()).isEqualTo(1);
        assertThat(summary.name()).isEqualTo("bulbasaur");
        assertThat(summary.spriteUrl())
                .isEqualTo(
                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
        assertThat(summary.category()).isEqualTo("Seed Pokémon");
        assertThat(summary.mass()).isEqualTo(69);
        assertThat(summary.abilities()).containsExactly("overgrow", "chlorophyll");
    }

    @Test
    void toSummaryResponse_mapsCharmanderFromRealPayload() {
        PokemonController.PokemonSummaryResponse summary =
                PokeApiMapper.toSummaryResponse(pokemon(4), species(4));

        assertThat(summary.id()).isEqualTo(4);
        assertThat(summary.name()).isEqualTo("charmander");
        assertThat(summary.category()).isEqualTo("Lizard Pokémon");
        assertThat(summary.mass()).isEqualTo(85);
        assertThat(summary.abilities()).containsExactly("blaze", "solar-power");
    }

    @Test
    void toDetailResponse_collapsesFlavorTextControlCharactersIntoSingleSpaces() {
        PokemonController.PokemonDetailResponse detail =
                PokeApiMapper.toDetailResponse(
                        pokemon(1),
                        species(1),
                        fixture("evolution-chain-1", EvolutionChainResponse.class),
                        Map.of(1, pokemon(1), 2, pokemon(2), 3, pokemon(3)));

        assertThat(detail.description())
                .isEqualTo(
                        "A strange seed was planted on its back at birth. The plant sprouts and"
                                + " grows with this POKéMON.")
                .doesNotContain("\n")
                .doesNotContain("\f");
    }

    @Test
    void toDetailResponse_mapsStatsInPayloadOrder() {
        PokemonController.PokemonDetailResponse detail =
                PokeApiMapper.toDetailResponse(
                        pokemon(1),
                        species(1),
                        fixture("evolution-chain-1", EvolutionChainResponse.class),
                        Map.of(1, pokemon(1)));

        assertThat(detail.stats())
                .containsExactly(
                        new PokemonController.PokemonStatResponse("hp", 45),
                        new PokemonController.PokemonStatResponse("attack", 49),
                        new PokemonController.PokemonStatResponse("defense", 49),
                        new PokemonController.PokemonStatResponse("special-attack", 65),
                        new PokemonController.PokemonStatResponse("special-defense", 65),
                        new PokemonController.PokemonStatResponse("speed", 45));
        assertThat(detail.imageUrl()).isEqualTo(detail.spriteUrl());
    }

    @Test
    void toDetailResponse_flattensEvolutionChainWithSprites() {
        PokemonController.PokemonDetailResponse detail =
                PokeApiMapper.toDetailResponse(
                        pokemon(1),
                        species(1),
                        fixture("evolution-chain-1", EvolutionChainResponse.class),
                        Map.of(1, pokemon(1), 2, pokemon(2), 3, pokemon(3)));

        assertThat(detail.evolutionChain())
                .extracting(
                        PokemonController.EvolutionStageResponse::id,
                        PokemonController.EvolutionStageResponse::name)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "bulbasaur"),
                        org.assertj.core.groups.Tuple.tuple(2, "ivysaur"),
                        org.assertj.core.groups.Tuple.tuple(3, "venusaur"));
        assertThat(detail.evolutionChain())
                .extracting(PokemonController.EvolutionStageResponse::spriteUrl)
                .allSatisfy(url -> assertThat(url).endsWith(".png"));
    }

    @Test
    void toDetailResponse_leavesEvolutionSpriteNullWhenStageWasNotFetched() {
        PokemonController.PokemonDetailResponse detail =
                PokeApiMapper.toDetailResponse(
                        pokemon(1),
                        species(1),
                        fixture("evolution-chain-1", EvolutionChainResponse.class),
                        Map.of(1, pokemon(1)));

        assertThat(detail.evolutionChain()).hasSize(3);
        assertThat(detail.evolutionChain().get(1).spriteUrl()).isNull();
        assertThat(detail.evolutionChain().get(2).spriteUrl()).isNull();
    }

    @Test
    void evolutionStages_traverseBranchingChainDepthFirst() {
        EvolutionChainResponse.ChainLink deepest =
                new EvolutionChainResponse.ChainLink(speciesRef("d", 4), List.of());
        EvolutionChainResponse.ChainLink branchOne =
                new EvolutionChainResponse.ChainLink(speciesRef("b", 2), List.of(deepest));
        EvolutionChainResponse.ChainLink branchTwo =
                new EvolutionChainResponse.ChainLink(speciesRef("c", 3), List.of());
        EvolutionChainResponse chain =
                new EvolutionChainResponse(
                        new EvolutionChainResponse.ChainLink(
                                speciesRef("a", 1), List.of(branchOne, branchTwo)));

        PokemonController.PokemonDetailResponse detail =
                PokeApiMapper.toDetailResponse(pokemon(1), species(1), chain, Map.of());

        assertThat(detail.evolutionChain())
                .extracting(PokemonController.EvolutionStageResponse::name)
                .containsExactly("a", "b", "d", "c");
        assertThat(PokeApiMapper.evolutionSpeciesIds(chain)).containsExactly(1, 2, 4, 3);
    }

    @Test
    void toSummaryResponse_returnsNullCategoryWhenNoEnglishGenusExists() {
        PokemonSpeciesResponse japaneseOnly =
                new PokemonSpeciesResponse(
                        List.of(new PokemonSpeciesResponse.Genus("たねポケモン", language("ja"))),
                        List.of(
                                new PokemonSpeciesResponse.FlavorText(
                                        "日本語", language("ja"), language("red"))),
                        new NamedResource(null, "https://pokeapi.co/api/v2/evolution-chain/1/"));

        PokemonController.PokemonSummaryResponse summary =
                PokeApiMapper.toSummaryResponse(pokemon(1), japaneseOnly);

        assertThat(summary.category()).isNull();
        assertThat(
                        PokeApiMapper.toDetailResponse(
                                        pokemon(1),
                                        japaneseOnly,
                                        new EvolutionChainResponse(null),
                                        Map.of())
                                .description())
                .isNull();
    }

    @Test
    void toSummaryResponse_toleratesMissingSpritesAbilitiesAndStats() {
        PokemonResponse bare = new PokemonResponse(7, "squirtle", 90, null, null, null, null);

        PokemonController.PokemonSummaryResponse summary =
                PokeApiMapper.toSummaryResponse(bare, species(1));
        PokemonController.PokemonDetailResponse detail =
                PokeApiMapper.toDetailResponse(bare, species(1), null, Map.of());

        assertThat(summary.spriteUrl()).isNull();
        assertThat(summary.abilities()).isEmpty();
        assertThat(detail.stats()).isEmpty();
        assertThat(detail.evolutionChain()).isEmpty();
    }

    @Test
    void toDetailResponse_skipsIncompleteSlotsAndBlankFlavorText() {
        PokemonResponse response =
                new PokemonResponse(
                        5,
                        "charmeleon",
                        190,
                        new PokemonResponse.Sprites("charmeleon.png"),
                        List.of(
                                new PokemonResponse.AbilitySlot(null),
                                new PokemonResponse.AbilitySlot(new NamedResource("blaze", null))),
                        List.of(
                                new PokemonResponse.StatSlot(1, null),
                                new PokemonResponse.StatSlot(58, new NamedResource("hp", null))),
                        null);
        PokemonSpeciesResponse speciesResponse =
                new PokemonSpeciesResponse(
                        List.of(),
                        List.of(
                                new PokemonSpeciesResponse.FlavorText(
                                        "   ", language("en"), language("red")),
                                new PokemonSpeciesResponse.FlavorText(
                                        "Real text\fhere", language("en"), language("blue"))),
                        null);

        PokemonController.PokemonDetailResponse detail =
                PokeApiMapper.toDetailResponse(response, speciesResponse, null, Map.of());

        assertThat(detail.abilities()).containsExactly("blaze");
        assertThat(detail.category()).isNull();
        assertThat(detail.stats())
                .containsExactly(new PokemonController.PokemonStatResponse("hp", 58));
        assertThat(detail.description()).isEqualTo("Real text here");
    }

    @Test
    void resourceId_extractsTrailingIdWithAndWithoutTrailingSlash() {
        assertThat(PokeApiMapper.resourceId("https://pokeapi.co/api/v2/pokemon-species/7/"))
                .isEqualTo(7);
        assertThat(PokeApiMapper.resourceId("https://pokeapi.co/api/v2/evolution-chain/12"))
                .isEqualTo(12);
    }

    @Test
    void resourceId_rejectsBlankAndNonNumericUrls() {
        assertThatThrownBy(() -> PokeApiMapper.resourceId(null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> PokeApiMapper.resourceId("  ")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> PokeApiMapper.resourceId("https://pokeapi.co/api/v2/pokemon/abc"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unexpected PokeAPI resource url");
    }

    @Test
    void toPageResponse_roundsTotalPagesUp() {
        PokemonController.PageResponse<String> result =
                PokeApiMapper.toPageResponse(List.of("a", "b"), 0, 2, 1351);

        assertThat(result.content()).containsExactly("a", "b");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(1351);
        assertThat(result.totalPages()).isEqualTo(676);
    }

    private static NamedResource speciesRef(String name, int id) {
        return new NamedResource(name, "https://pokeapi.co/api/v2/pokemon-species/" + id + "/");
    }

    private static NamedResource language(String name) {
        return new NamedResource(name, null);
    }
}
