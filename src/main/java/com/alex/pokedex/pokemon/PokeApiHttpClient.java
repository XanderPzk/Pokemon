package com.alex.pokedex.pokemon;

import com.alex.pokedex.common.ApiException;
import com.alex.pokedex.pokemon.PokeApiDtos.EvolutionChainResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonSpeciesResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.URI;
import java.util.function.Function;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * Single-request access to PokeAPI, and the layer the resilience policies are attached to.
 *
 * <p>The policies live here rather than on a composing adapter for two reasons. Spring applies them
 * through a proxy, so a composed method calling its own helpers on {@code this} would bypass them
 * for every call but the first. And retrying at this granularity re-issues only the request that
 * failed, instead of replaying an entire multi-request composition.
 */
@Component
@Retry(name = "pokeApi")
@CircuitBreaker(name = "pokeApi")
public class PokeApiHttpClient {

    private final RestClient restClient;

    PokeApiHttpClient(RestClient pokeApiRestClient) {
        this.restClient = pokeApiRestClient;
    }

    @Cacheable(value = "pokeApiList", key = "#offset + '-' + #limit")
    public PokeApiDtos.PokemonListResponse fetchList(int offset, int limit) {
        return fetch(
                uri ->
                        uri.path("/pokemon")
                                .queryParam("offset", offset)
                                .queryParam("limit", limit)
                                .build(),
                "Pokemon list at offset " + offset,
                PokeApiDtos.PokemonListResponse.class);
    }

    @Cacheable(value = "pokeApiPokemon", key = "#idOrName")
    public PokemonResponse fetchPokemon(String idOrName) {
        return fetch(
                uri -> uri.path("/pokemon/{idOrName}").build(idOrName),
                "Pokemon '" + idOrName + "'",
                PokemonResponse.class);
    }

    @Cacheable(value = "pokeApiSpecies", key = "#speciesId")
    public PokemonSpeciesResponse fetchSpecies(int speciesId) {
        return fetch(
                uri -> uri.path("/pokemon-species/{id}").build(speciesId),
                "Pokemon species " + speciesId,
                PokemonSpeciesResponse.class);
    }

    @Cacheable(value = "pokeApiEvolutionChain", key = "#chainId")
    public EvolutionChainResponse fetchEvolutionChain(int chainId) {
        return fetch(
                uri -> uri.path("/evolution-chain/{id}").build(chainId),
                "Evolution chain " + chainId,
                EvolutionChainResponse.class);
    }

    private <T> T fetch(Function<UriBuilder, URI> uriFunction, String resource, Class<T> type) {
        return restClient
                .get()
                .uri(uriFunction)
                .retrieve()
                .onStatus(
                        status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        (request, response) -> {
                            throw ApiException.notFound(resource + " not found in PokeAPI");
                        })
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            throw new PokeApiServerException(
                                    "PokeAPI returned "
                                            + response.getStatusCode().value()
                                            + " for "
                                            + resource);
                        })
                .body(type);
    }
}
