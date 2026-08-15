package com.alex.pokedex.pokemon.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alex.pokedex.common.ApiException;
import com.alex.pokedex.pokemon.repository.PokemonRepository;
import com.alex.pokedex.pokemon.service.PokemonService;
import com.alex.pokedex.pokemon.web.PokemonController;
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
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = PokeApiTestApplication.class)
@ActiveProfiles({"test", "pokeapi-test"})
class PokeApiClientTest {

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
    @Autowired private PokeApiHttpClient httpClient;
    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired private org.springframework.cache.CacheManager cacheManager;

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
    void httpClient_isProxiedSoResilienceAnnotationsActuallyApply() {
        assertThat(AopUtils.isAopProxy(httpClient))
                .as("without an AOP proxy every retry and circuit breaker test below is vacuous")
                .isTrue();
    }

    @Test
    void list_mapsPaginatedSummariesFromRealPayloads() {
        stubList(0, 2, "pokemon-list-offset0-limit2");
        stubJson("/api/v2/pokemon/1", "pokemon-1");
        stubJson("/api/v2/pokemon/2", "pokemon-2");
        stubJson("/api/v2/pokemon-species/1", "pokemon-species-1");
        stubJson("/api/v2/pokemon-species/2", "pokemon-species-2");

        PokemonController.PageResponse<PokemonController.PokemonSummaryResponse> result =
                pokemonService.list(0, 2);

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(1351);
        assertThat(result.totalPages()).isEqualTo(676);
        assertThat(result.content()).hasSize(2);

        PokemonController.PokemonSummaryResponse bulbasaur = result.content().get(0);
        assertThat(bulbasaur.id()).isEqualTo(1);
        assertThat(bulbasaur.name()).isEqualTo("bulbasaur");
        assertThat(bulbasaur.category()).isEqualTo("Seed Pokémon");
        assertThat(bulbasaur.mass()).isEqualTo(69);
        assertThat(bulbasaur.spriteUrl()).endsWith("/pokemon/1.png");
        assertThat(bulbasaur.abilities()).containsExactly("overgrow", "chlorophyll");

        PokemonController.PokemonSummaryResponse ivysaur = result.content().get(1);
        assertThat(ivysaur.id()).isEqualTo(2);
        assertThat(ivysaur.name()).isEqualTo("ivysaur");
        assertThat(ivysaur.mass()).isEqualTo(130);
    }

    @Test
    void list_translatesPageAndSizeIntoOffsetAndLimit() {
        stubList(40, 20, "pokemon-list-offset0-limit2");
        stubJson("/api/v2/pokemon/1", "pokemon-1");
        stubJson("/api/v2/pokemon/2", "pokemon-2");
        stubJson("/api/v2/pokemon-species/1", "pokemon-species-1");
        stubJson("/api/v2/pokemon-species/2", "pokemon-species-2");

        pokemonService.list(2, 20);

        WIREMOCK.verify(
                exactly(1),
                getRequestedFor(urlPathEqualTo("/api/v2/pokemon"))
                        .withQueryParam("offset", equalTo("40"))
                        .withQueryParam("limit", equalTo("20")));
    }

    @Test
    void list_rejectsInvalidPagingWithoutCallingPokeApi() {
        assertThatThrownBy(() -> pokemonService.list(0, 0))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("size");
        assertThatThrownBy(() -> pokemonService.list(-1, 20))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("page");

        assertThat(WIREMOCK.getAllServeEvents()).isEmpty();
    }

    @Test
    void getByIdOrName_composesPokemonSpeciesAndEvolutionChain() {
        stubDetailFixtures();

        PokemonController.PokemonDetailResponse detail = pokemonService.getByIdOrName("bulbasaur");

        assertThat(detail.name()).isEqualTo("bulbasaur");
        assertThat(detail.category()).isEqualTo("Seed Pokémon");
        assertThat(detail.mass()).isEqualTo(69);
        assertThat(detail.imageUrl()).endsWith("/pokemon/1.png");
        assertThat(detail.description())
                .isEqualTo(
                        "A strange seed was planted on its back at birth. The plant sprouts and"
                                + " grows with this POKéMON.");
        assertThat(detail.stats()).hasSize(6);
        assertThat(detail.evolutionChain())
                .extracting(PokemonController.EvolutionStageResponse::name)
                .containsExactly("bulbasaur", "ivysaur", "venusaur");
        assertThat(detail.evolutionChain())
                .allSatisfy(stage -> assertThat(stage.spriteUrl()).endsWith(".png"));
    }

    @Test
    void getByIdOrName_reusesTheAlreadyFetchedPokemonForItsOwnEvolutionStage() {
        stubDetailFixtures();

        pokemonService.getByIdOrName("bulbasaur");

        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/bulbasaur")));
        WIREMOCK.verify(exactly(0), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/1")));
        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/2")));
        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/3")));
    }

    @Test
    void getByIdOrName_rejectsBlankIdentifierWithoutCallingPokeApi() {
        assertThatThrownBy(() -> pokemonService.getByIdOrName("   "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> pokemonService.getByIdOrName(null))
                .isInstanceOf(ApiException.class);

        assertThat(WIREMOCK.getAllServeEvents()).isEmpty();
    }

    @Test
    void getByIdOrName_lowercasesTheIdentifierBeforeCallingPokeApi() {
        stubDetailFixtures();

        pokemonService.getByIdOrName("BulbaSaur");

        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/bulbasaur")));
    }

    @Test
    void getByIdOrName_skipsSpeciesAndChainWhenPokemonHasNoSpeciesReference() {
        WIREMOCK.stubFor(
                get(urlPathEqualTo("/api/v2/pokemon/ditto"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                {"id":132,"name":"ditto","weight":40,
                                                 "sprites":{"front_default":"ditto.png"}}
                                                """)));

        PokemonController.PokemonDetailResponse detail = pokemonService.getByIdOrName("ditto");

        assertThat(detail.name()).isEqualTo("ditto");
        assertThat(detail.category()).isNull();
        assertThat(detail.description()).isNull();
        assertThat(detail.stats()).isEmpty();
        assertThat(detail.evolutionChain()).isEmpty();
        assertThat(WIREMOCK.getAllServeEvents()).hasSize(1);
    }

    @Test
    void getByIdOrName_throwsNotFoundOn404WithoutRetrying() {
        WIREMOCK.stubFor(
                get(urlPathEqualTo("/api/v2/pokemon/missingno"))
                        .willReturn(aResponse().withStatus(404).withBody("Not Found")));

        assertThatThrownBy(() -> pokemonService.getByIdOrName("missingno"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("missingno");

        WIREMOCK.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/missingno")));
    }

    @Test
    void getByIdOrName_retriesServerErrorsThenSucceeds() {
        String url = "/api/v2/pokemon/bulbasaur";
        WIREMOCK.stubFor(
                get(urlPathEqualTo(url))
                        .inScenario("flaky")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(aResponse().withStatus(500))
                        .willSetStateTo("second"));
        WIREMOCK.stubFor(
                get(urlPathEqualTo(url))
                        .inScenario("flaky")
                        .whenScenarioStateIs("second")
                        .willReturn(aResponse().withStatus(503))
                        .willSetStateTo("third"));
        WIREMOCK.stubFor(
                get(urlPathEqualTo(url))
                        .inScenario("flaky")
                        .whenScenarioStateIs("third")
                        .willReturn(jsonResponse("pokemon-1")));
        stubSpeciesAndChain();

        PokemonController.PokemonDetailResponse detail = pokemonService.getByIdOrName("bulbasaur");

        assertThat(detail.name()).isEqualTo("bulbasaur");
        WIREMOCK.verify(exactly(3), getRequestedFor(urlPathEqualTo(url)));
    }

    @Test
    void getByIdOrName_propagatesServerErrorAfterRetriesAreExhausted() {
        WIREMOCK.stubFor(
                get(urlPathEqualTo("/api/v2/pokemon/bulbasaur"))
                        .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> pokemonService.getByIdOrName("bulbasaur"))
                .isInstanceOf(PokeApiServerException.class);

        WIREMOCK.verify(exactly(3), getRequestedFor(urlPathEqualTo("/api/v2/pokemon/bulbasaur")));
    }

    @Test
    void getByIdOrName_retriesReadTimeoutThenSucceeds() {
        String url = "/api/v2/pokemon/bulbasaur";
        WIREMOCK.stubFor(
                get(urlPathEqualTo(url))
                        .inScenario("slow")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(jsonResponse("pokemon-1").withFixedDelay(1500))
                        .willSetStateTo("fast"));
        WIREMOCK.stubFor(
                get(urlPathEqualTo(url))
                        .inScenario("slow")
                        .whenScenarioStateIs("fast")
                        .willReturn(jsonResponse("pokemon-1")));
        stubSpeciesAndChain();

        PokemonController.PokemonDetailResponse detail = pokemonService.getByIdOrName("bulbasaur");

        assertThat(detail.name()).isEqualTo("bulbasaur");
        WIREMOCK.verify(exactly(2), getRequestedFor(urlPathEqualTo(url)));
    }

    @Test
    void getByIdOrName_opensCircuitBreakerAfterRepeatedServerErrors() {
        String url = "/api/v2/pokemon/bulbasaur";
        WIREMOCK.stubFor(get(urlPathEqualTo(url)).willReturn(aResponse().withStatus(500)));

        for (int call = 0; call < 5; call++) {
            assertThatThrownBy(() -> pokemonService.getByIdOrName("bulbasaur"))
                    .isInstanceOf(PokeApiServerException.class);
        }

        assertThat(circuitBreakerRegistry.circuitBreaker("pokeApi").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        int requestsBeforeRejection = WIREMOCK.getAllServeEvents().size();
        assertThatThrownBy(() -> pokemonService.getByIdOrName("bulbasaur"))
                .isInstanceOf(CallNotPermittedException.class);

        assertThat(WIREMOCK.getAllServeEvents()).hasSize(requestsBeforeRejection);
    }

    private void stubDetailFixtures() {
        WIREMOCK.stubFor(
                get(urlPathEqualTo("/api/v2/pokemon/bulbasaur"))
                        .willReturn(jsonResponse("pokemon-1")));
        stubSpeciesAndChain();
    }

    private void stubSpeciesAndChain() {
        stubJson("/api/v2/pokemon-species/1", "pokemon-species-1");
        stubJson("/api/v2/evolution-chain/1", "evolution-chain-1");
        stubJson("/api/v2/pokemon/2", "pokemon-2");
        stubJson("/api/v2/pokemon/3", "pokemon-3");
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
                PokeApiClientTest.class.getResourceAsStream("/pokeapi/" + fixture + ".json")) {
            if (in == null) {
                throw new IllegalStateException("Missing fixture " + fixture);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read fixture " + fixture, e);
        }
    }
}
