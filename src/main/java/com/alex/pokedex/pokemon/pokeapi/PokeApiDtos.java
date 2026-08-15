package com.alex.pokedex.pokemon.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public final class PokeApiDtos {

    private PokeApiDtos() {}

    /** The `{ "name": ..., "url": ... }` shape PokeAPI uses for every cross-reference. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NamedResource(String name, String url) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PokemonListResponse(long count, java.util.List<Entry> results) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Entry(String name, String url) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PokemonResponse(
            int id,
            String name,
            int weight,
            Sprites sprites,
            java.util.List<AbilitySlot> abilities,
            java.util.List<StatSlot> stats,
            NamedResource species) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Sprites(
                @com.fasterxml.jackson.annotation.JsonProperty("front_default")
                        String frontDefault) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record AbilitySlot(NamedResource ability) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record StatSlot(
                @com.fasterxml.jackson.annotation.JsonProperty("base_stat") int baseStat,
                NamedResource stat) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PokemonSpeciesResponse(
            java.util.List<Genus> genera,
            @com.fasterxml.jackson.annotation.JsonProperty("flavor_text_entries")
                    java.util.List<FlavorText> flavorTextEntries,
            @com.fasterxml.jackson.annotation.JsonProperty("evolution_chain")
                    NamedResource evolutionChain) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Genus(String genus, NamedResource language) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record FlavorText(
                @com.fasterxml.jackson.annotation.JsonProperty("flavor_text") String flavorText,
                NamedResource language,
                NamedResource version) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvolutionChainResponse(ChainLink chain) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ChainLink(
                NamedResource species,
                @com.fasterxml.jackson.annotation.JsonProperty("evolves_to")
                        java.util.List<ChainLink> evolvesTo) {}
    }
}
