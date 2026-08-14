package com.alex.pokedex.pokemon;

import com.alex.pokedex.common.ApiException;
import com.alex.pokedex.pokemon.PokeApiDtos.EvolutionChainResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.NamedResource;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonSpeciesResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Maps raw PokeAPI payloads onto controller response types. Pure functions, no Spring. */
public final class PokeApiMapper {

    private static final String ENGLISH = "en";

    private PokeApiMapper() {}

    public static PokemonController.PokemonSummaryResponse toSummaryResponse(
            PokemonResponse pokemon, PokemonSpeciesResponse species) {
        return new PokemonController.PokemonSummaryResponse(
                pokemon.id(),
                pokemon.name(),
                spriteUrl(pokemon),
                englishGenus(species),
                pokemon.weight(),
                abilities(pokemon));
    }

    public static PokemonController.PokemonDetailResponse toDetailResponse(
            PokemonResponse pokemon,
            PokemonSpeciesResponse species,
            EvolutionChainResponse chain,
            Map<Integer, PokemonResponse> stageSprites) {
        PokemonController.PokemonSummaryResponse summary = toSummaryResponse(pokemon, species);
        return new PokemonController.PokemonDetailResponse(
                summary.id(),
                summary.name(),
                summary.spriteUrl(),
                spriteUrl(pokemon),
                summary.category(),
                summary.mass(),
                summary.abilities(),
                englishDescription(species),
                stats(pokemon),
                evolutionStages(chain, stageSprites));
    }

    public static <T> PokemonController.PageResponse<T> toPageResponse(
            List<T> content, int page, int size, long count) {
        return new PokemonController.PageResponse<>(
                content, page, size, count, (int) Math.ceilDiv(count, size));
    }

    /**
     * Extracts the trailing numeric id from a PokeAPI resource URL, e.g. {@code
     * .../pokemon-species/7/} yields {@code 7}.
     */
    public static int resourceId(String url) {
        if (url == null || url.isBlank()) {
            throw ApiException.badRequest("PokeAPI resource url must not be blank", "url");
        }
        String trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        String lastSegment = trimmed.substring(trimmed.lastIndexOf('/') + 1);
        try {
            return Integer.parseInt(lastSegment);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Unexpected PokeAPI resource url: " + url, "url");
        }
    }

    /** Species ids for every stage in the chain, in traversal order. */
    public static List<Integer> evolutionSpeciesIds(EvolutionChainResponse chain) {
        if (chain == null || chain.chain() == null) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        collectSpeciesIds(chain.chain(), ids);
        return List.copyOf(ids);
    }

    private static String spriteUrl(PokemonResponse pokemon) {
        return pokemon.sprites() == null ? null : pokemon.sprites().frontDefault();
    }

    private static List<String> abilities(PokemonResponse pokemon) {
        if (pokemon.abilities() == null) {
            return List.of();
        }
        return pokemon.abilities().stream()
                .filter(slot -> slot.ability() != null)
                .map(slot -> slot.ability().name())
                .toList();
    }

    private static List<PokemonController.PokemonStatResponse> stats(PokemonResponse pokemon) {
        if (pokemon.stats() == null) {
            return List.of();
        }
        return pokemon.stats().stream()
                .filter(slot -> slot.stat() != null)
                .map(
                        slot ->
                                new PokemonController.PokemonStatResponse(
                                        slot.stat().name(), slot.baseStat()))
                .toList();
    }

    private static String englishGenus(PokemonSpeciesResponse species) {
        if (species == null || species.genera() == null) {
            return null;
        }
        return species.genera().stream()
                .filter(genus -> isEnglish(genus.language()))
                .map(PokemonSpeciesResponse.Genus::genus)
                .findFirst()
                .orElse(null);
    }

    private static String englishDescription(PokemonSpeciesResponse species) {
        if (species == null || species.flavorTextEntries() == null) {
            return null;
        }
        return species.flavorTextEntries().stream()
                .filter(entry -> isEnglish(entry.language()))
                .map(PokemonSpeciesResponse.FlavorText::flavorText)
                .filter(text -> text != null && !text.isBlank())
                .map(PokeApiMapper::normalizeWhitespace)
                .findFirst()
                .orElse(null);
    }

    private static String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static boolean isEnglish(NamedResource language) {
        return language != null && ENGLISH.equals(language.name());
    }

    private static List<PokemonController.EvolutionStageResponse> evolutionStages(
            EvolutionChainResponse chain, Map<Integer, PokemonResponse> stageSprites) {
        if (chain == null || chain.chain() == null) {
            return List.of();
        }
        List<PokemonController.EvolutionStageResponse> stages = new ArrayList<>();
        collectStages(chain.chain(), stageSprites, stages);
        return List.copyOf(stages);
    }

    private static void collectStages(
            EvolutionChainResponse.ChainLink link,
            Map<Integer, PokemonResponse> stageSprites,
            List<PokemonController.EvolutionStageResponse> stages) {
        if (link.species() != null) {
            int speciesId = resourceId(link.species().url());
            PokemonResponse pokemon = stageSprites.get(speciesId);
            stages.add(
                    new PokemonController.EvolutionStageResponse(
                            speciesId,
                            link.species().name(),
                            pokemon == null ? null : spriteUrl(pokemon)));
        }
        if (link.evolvesTo() != null) {
            link.evolvesTo().forEach(child -> collectStages(child, stageSprites, stages));
        }
    }

    private static void collectSpeciesIds(
            EvolutionChainResponse.ChainLink link, List<Integer> ids) {
        if (link.species() != null) {
            ids.add(resourceId(link.species().url()));
        }
        if (link.evolvesTo() != null) {
            link.evolvesTo().forEach(child -> collectSpeciesIds(child, ids));
        }
    }
}
