package com.alex.pokedex.pokemon.web;

import com.alex.pokedex.config.OpenApiConfig;
import com.alex.pokedex.pokemon.service.PokemonService;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Pokemon", description = "Browse, sync, and locally edit Pokemon records")
@RestController
@RequestMapping("/pokemon")
public class PokemonController {

    public record PageResponse<T>(
            List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record PokemonSummaryResponse(
            int id,
            String name,
            String spriteUrl,
            String category,
            int mass,
            List<String> abilities) {}

    public record PokemonStatResponse(String name, int baseStat) {}

    public record EvolutionStageResponse(int id, String name, String spriteUrl) {}

    public record PokemonDetailResponse(
            int id,
            String name,
            String spriteUrl,
            String imageUrl,
            String category,
            int mass,
            List<String> abilities,
            String description,
            List<PokemonStatResponse> stats,
            List<EvolutionStageResponse> evolutionChain) {}

    public record SyncedPokemonResponse(
            Long localId,
            int externalId,
            String name,
            String spriteUrl,
            String category,
            int mass,
            List<String> abilities,
            String localName,
            String region,
            List<String> internalTags) {}

    /**
     * PATCH body for local Pokemon edits. Plain class (not a record) so Jackson can distinguish
     * omitted properties (field stays null) from explicit null (field set to Optional.empty()).
     */
    @JsonIgnoreProperties(ignoreUnknown = false)
    public static class UpdatePokemonRequest {

        private Optional<@Size(max = 100) String> localName;

        private Optional<String> region;

        private Optional<List<@Size(max = 50) String>> internalTags;

        @JsonAnySetter
        public void rejectUnknownProperty(String name, Object value) {
            throw new IllegalArgumentException("Unknown property: " + name);
        }

        public Optional<String> getLocalName() {
            return localName;
        }

        public void setLocalName(Optional<String> localName) {
            this.localName = localName;
        }

        public Optional<String> getRegion() {
            return region;
        }

        public void setRegion(Optional<String> region) {
            this.region = region;
        }

        public Optional<List<String>> getInternalTags() {
            return internalTags;
        }

        public void setInternalTags(Optional<List<String>> internalTags) {
            this.internalTags = internalTags;
        }
    }

    private final PokemonService pokemonService;

    public PokemonController(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    @Operation(summary = "List Pokemon from PokeAPI with pagination")
    @GetMapping
    public PageResponse<PokemonSummaryResponse> listPokemon(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return pokemonService.list(page, size);
    }

    @Operation(summary = "Get Pokemon detail from PokeAPI")
    @GetMapping("/{idOrName}")
    public PokemonDetailResponse getPokemonDetail(@PathVariable String idOrName) {
        return pokemonService.getByIdOrName(idOrName);
    }

    @Operation(summary = "Sync a Pokemon snapshot into local storage")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/{idOrName}/sync")
    public SyncedPokemonResponse syncPokemon(@PathVariable String idOrName) {
        return pokemonService.sync(idOrName);
    }

    @Operation(summary = "Patch locally editable fields on a synced Pokemon record")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PatchMapping("/local/{localId}")
    public SyncedPokemonResponse updateLocalPokemon(
            @PathVariable Long localId, @Valid @RequestBody UpdatePokemonRequest request) {
        return pokemonService.update(localId, request);
    }
}
