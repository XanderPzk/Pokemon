package com.alex.pokedex.pokemon;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = PokeApiTestApplication.class)
@ActiveProfiles({"test", "pokeapi-test"})
class PokeApiCacheTest {

    private static final WireMockServer WIREMOCK =
            new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @BeforeAll
    static void startWireMock() {
        WIREMOCK.start();
    }

    @AfterAll
    static void stopWireMock() {
        WIREMOCK.stop();
    }

    @DynamicPropertySource
    static void pokeApiBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("pokeapi.base-url", () -> WIREMOCK.baseUrl() + "/api/v2");
    }

    @Autowired private PokemonService pokemonService;
    @Autowired private CacheManager cacheManager;
    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean private PokemonRepository pokemonRepository;

    @BeforeEach
    void resetState() {
        WIREMOCK.resetAll();
        circuitBreakerRegistry.circuitBreaker("pokeApi").reset();
        cacheManager
                .getCacheNames()
                .forEach(
                        name -> {
                            var cache = cacheManager.getCache(name);
                            if (cache != null) {
                                cache.clear();
                            }
                        });
    }

    @Test
    void list_secondCallUsesCacheWithoutReInvokingUpstream() {
        stubList(0, 2, "pokemon-list-offset0-limit2");
        stubJson("/api/v2/pokemon/1", "pokemon-1");
        stubJson("/api/v2/pokemon/2", "pokemon-2");
        stubJson("/api/v2/pokemon-species/1", "pokemon-species-1");
        stubJson("/api/v2/pokemon-species/2", "pokemon-species-2");

        pokemonService.list(0, 2);
        pokemonService.list(0, 2);

        WIREMOCK.verify(
                exactly(1),
                getRequestedFor(urlPathEqualTo("/api/v2/pokemon"))
                        .withQueryParam("offset", equalTo("0"))
                        .withQueryParam("limit", equalTo("2")));
        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/1")));
        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/2")));
        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon-species/1")));
        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon-species/2")));
    }

    @Test
    void getByIdOrName_secondCallUsesCacheWithoutReInvokingEvolutionChain() {
        stubJson("/api/v2/pokemon/bulbasaur", "pokemon-1");
        stubJson("/api/v2/pokemon-species/1", "pokemon-species-1");
        stubJson("/api/v2/evolution-chain/1", "evolution-chain-1");
        stubJson("/api/v2/pokemon/2", "pokemon-2");
        stubJson("/api/v2/pokemon/3", "pokemon-3");

        pokemonService.getByIdOrName("bulbasaur");
        pokemonService.getByIdOrName("bulbasaur");

        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/bulbasaur")));
        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon-species/1")));
        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/evolution-chain/1")));
    }

    @Test
    void getByIdOrName_reusesEntriesWarmedByListCall() {
        stubList(0, 2, "pokemon-list-offset0-limit2");
        stubJson("/api/v2/pokemon/1", "pokemon-1");
        stubJson("/api/v2/pokemon/2", "pokemon-2");
        stubJson("/api/v2/pokemon-species/1", "pokemon-species-1");
        stubJson("/api/v2/pokemon-species/2", "pokemon-species-2");
        stubJson("/api/v2/evolution-chain/1", "evolution-chain-1");
        stubJson("/api/v2/pokemon/3", "pokemon-3");

        pokemonService.list(0, 2);

        int pokemon1Requests = requestCount("/api/v2/pokemon/1");
        int species1Requests = requestCount("/api/v2/pokemon-species/1");
        int pokemon2Requests = requestCount("/api/v2/pokemon/2");

        PokemonController.PokemonDetailResponse detail = pokemonService.getByIdOrName("1");

        assertThat(detail.name()).isEqualTo("bulbasaur");
        assertThat(requestCount("/api/v2/pokemon/1")).isEqualTo(pokemon1Requests);
        assertThat(requestCount("/api/v2/pokemon-species/1")).isEqualTo(species1Requests);
        assertThat(requestCount("/api/v2/pokemon/2")).isEqualTo(pokemon2Requests);
        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/evolution-chain/1")));
        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/3")));
    }

    private int requestCount(String urlPath) {
        return WIREMOCK.findAll(getRequestedFor(urlPathEqualTo(urlPath))).size();
    }

    @Test
    void list_servesFromCacheWhenCircuitIsOpen() {
        stubList(0, 2, "pokemon-list-offset0-limit2");
        stubJson("/api/v2/pokemon/1", "pokemon-1");
        stubJson("/api/v2/pokemon/2", "pokemon-2");
        stubJson("/api/v2/pokemon-species/1", "pokemon-species-1");
        stubJson("/api/v2/pokemon-species/2", "pokemon-species-2");

        PokemonController.PageResponse<PokemonController.PokemonSummaryResponse> first =
                pokemonService.list(0, 2);
        assertThat(first.content()).hasSize(2);

        WIREMOCK.stubFor(
                get(urlPathEqualTo("/api/v2/pokemon/missingno"))
                        .willReturn(aResponse().withStatus(500)));
        for (int call = 0; call < 5; call++) {
            assertThatThrownBy(() -> pokemonService.getByIdOrName("missingno"))
                    .isInstanceOf(Exception.class);
        }
        assertThat(circuitBreakerRegistry.circuitBreaker("pokeApi").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        int requestsBeforeCachedRead = WIREMOCK.getAllServeEvents().size();
        PokemonController.PageResponse<PokemonController.PokemonSummaryResponse> cached =
                pokemonService.list(0, 2);

        assertThat(cached.content()).hasSize(2);
        assertThat(cached.content().get(0).name()).isEqualTo("bulbasaur");
        assertThat(WIREMOCK.getAllServeEvents()).hasSize(requestsBeforeCachedRead);
    }

    @Test
    void list_rejectsCallWhenCircuitIsOpenAndCacheIsCold() {
        WIREMOCK.stubFor(
                get(urlPathEqualTo("/api/v2/pokemon")).willReturn(aResponse().withStatus(500)));

        for (int call = 0; call < 5; call++) {
            assertThatThrownBy(() -> pokemonService.list(0, 2)).isInstanceOf(Exception.class);
        }
        assertThat(circuitBreakerRegistry.circuitBreaker("pokeApi").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> pokemonService.list(0, 2))
                .isInstanceOf(CallNotPermittedException.class);
    }

    private void stubList(int offset, int limit, String fixture) {
        WIREMOCK.stubFor(
                get(urlPathEqualTo("/api/v2/pokemon"))
                        .withQueryParam("offset", equalTo(String.valueOf(offset)))
                        .withQueryParam("limit", equalTo(String.valueOf(limit)))
                        .willReturn(jsonResponse(fixture)));
    }

    private void stubJson(String urlPath, String fixture) {
        WIREMOCK.stubFor(get(urlPathEqualTo(urlPath)).willReturn(jsonResponse(fixture)));
    }

    private static ResponseDefinitionBuilder jsonResponse(String fixture) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(fixtureBody(fixture));
    }

    private static String fixtureBody(String fixture) {
        try (InputStream in =
                PokeApiCacheTest.class.getResourceAsStream("/pokeapi/" + fixture + ".json")) {
            if (in == null) {
                throw new IllegalStateException("Missing fixture " + fixture);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read fixture " + fixture, e);
        }
    }
}
