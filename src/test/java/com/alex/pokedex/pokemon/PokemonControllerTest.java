package com.alex.pokedex.pokemon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alex.pokedex.auth.JwtTokenProvider;
import com.alex.pokedex.auth.User;
import com.alex.pokedex.common.ApiException;
import com.alex.pokedex.common.GlobalExceptionHandler;
import com.alex.pokedex.common.RequestLoggingFilter;
import com.alex.pokedex.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PokemonController.class)
@Import({GlobalExceptionHandler.class, RequestLoggingFilter.class, SecurityConfig.class})
class PokemonControllerTest {

    private static final String VALID_TOKEN = "valid-token";

    @Autowired private MockMvc mockMvc;

    @MockBean private PokemonService pokemonService;

    @MockBean private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUpJwtParser() {
        when(jwtTokenProvider.parse(VALID_TOKEN))
                .thenReturn(User.fromTokenClaims("user@example.com", User.Role.USER));
    }

    @Test
    void listPokemon_returnsPaginatedSummaries() throws Exception {
        when(pokemonService.list(1, 2)).thenReturn(samplePage());

        mockMvc.perform(get("/pokemon").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(1351))
                .andExpect(jsonPath("$.totalPages").value(676))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$.content[0].spriteUrl").value("https://example.com/1.png"))
                .andExpect(jsonPath("$.content[0].category").value("Seed Pokémon"))
                .andExpect(jsonPath("$.content[0].mass").value(69))
                .andExpect(jsonPath("$.content[0].abilities[0]").value("overgrow"))
                .andExpect(jsonPath("$.content[0].abilities[1]").value("chlorophyll"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].name").value("ivysaur"));
    }

    @Test
    void listPokemon_defaultsToFirstPageOfTwenty() throws Exception {
        when(pokemonService.list(0, 20)).thenReturn(emptyPage());

        mockMvc.perform(get("/pokemon")).andExpect(status().isOk());

        verify(pokemonService).list(0, 20);
    }

    @Test
    void listPokemon_rejectsZeroSize() throws Exception {
        mockMvc.perform(get("/pokemon").param("size", "0")).andExpect(status().isBadRequest());

        verify(pokemonService, never()).list(anyInt(), anyInt());
    }

    @Test
    void listPokemon_rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/pokemon").param("page", "-1")).andExpect(status().isBadRequest());

        verify(pokemonService, never()).list(anyInt(), anyInt());
    }

    @Test
    void listPokemon_rejectsSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/pokemon").param("size", "101")).andExpect(status().isBadRequest());

        verify(pokemonService, never()).list(anyInt(), anyInt());
    }

    @Test
    void listPokemon_rejectsNonNumericPage() throws Exception {
        mockMvc.perform(get("/pokemon").param("page", "abc")).andExpect(status().isBadRequest());

        verify(pokemonService, never()).list(anyInt(), anyInt());
    }

    @Test
    void getPokemonDetail_returnsFlatDetailByName() throws Exception {
        when(pokemonService.getByIdOrName("bulbasaur")).thenReturn(sampleDetail());

        mockMvc.perform(get("/pokemon/bulbasaur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("bulbasaur"))
                .andExpect(jsonPath("$.spriteUrl").value("https://example.com/1.png"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/1.png"))
                .andExpect(jsonPath("$.category").value("Seed Pokémon"))
                .andExpect(jsonPath("$.mass").value(69))
                .andExpect(jsonPath("$.abilities[0]").value("overgrow"))
                .andExpect(
                        jsonPath("$.description")
                                .value("A strange seed was planted on its back at birth."))
                .andExpect(jsonPath("$.stats[0].name").value("hp"))
                .andExpect(jsonPath("$.stats[0].baseStat").value(45))
                .andExpect(jsonPath("$.stats[1].name").value("attack"))
                .andExpect(jsonPath("$.stats[1].baseStat").value(49))
                .andExpect(jsonPath("$.evolutionChain[0].id").value(1))
                .andExpect(jsonPath("$.evolutionChain[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$.evolutionChain[1].id").value(2))
                .andExpect(jsonPath("$.evolutionChain[1].name").value("ivysaur"));
    }

    @Test
    void getPokemonDetail_returnsFlatDetailByNumericId() throws Exception {
        when(pokemonService.getByIdOrName("1")).thenReturn(sampleDetail());

        mockMvc.perform(get("/pokemon/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("bulbasaur"));

        verify(pokemonService).getByIdOrName("1");
    }

    @Test
    void getPokemonDetail_returns404WhenPokemonNotFoundInPokeApi() throws Exception {
        when(pokemonService.getByIdOrName("missingno"))
                .thenThrow(ApiException.notFound("Pokemon 'missingno' not found in PokeAPI"));

        mockMvc.perform(get("/pokemon/missingno"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Pokemon 'missingno' not found in PokeAPI"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getPokemonDetail_returns400WhenIdentifierIsInvalid() throws Exception {
        when(pokemonService.getByIdOrName(anyString()))
                .thenThrow(
                        ApiException.badRequest(
                                "Pokemon id or name must not be blank", "idOrName"));

        mockMvc.perform(get("/pokemon/{idOrName}", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Pokemon id or name must not be blank"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getPokemonDetail_serializesEmptyEvolutionChainAndNullDescription() throws Exception {
        PokemonController.PokemonDetailResponse detail =
                new PokemonController.PokemonDetailResponse(
                        132,
                        "ditto",
                        "ditto.png",
                        "ditto.png",
                        null,
                        40,
                        List.of(),
                        null,
                        List.of(),
                        List.of());
        when(pokemonService.getByIdOrName("ditto")).thenReturn(detail);

        mockMvc.perform(get("/pokemon/ditto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.stats").isEmpty())
                .andExpect(jsonPath("$.evolutionChain").isEmpty());
    }

    @Test
    void syncPokemon_returnsSyncedRecord() throws Exception {
        when(pokemonService.sync("pikachu")).thenReturn(sampleSyncedResponse());

        mockMvc.perform(
                        post("/pokemon/pikachu/sync")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localId").value(1))
                .andExpect(jsonPath("$.externalId").value(25))
                .andExpect(jsonPath("$.name").value("pikachu"))
                .andExpect(jsonPath("$.spriteUrl").value("https://example.com/pikachu.png"))
                .andExpect(jsonPath("$.category").value("Mouse Pokémon"))
                .andExpect(jsonPath("$.mass").value(60))
                .andExpect(jsonPath("$.abilities[0]").value("static"))
                .andExpect(jsonPath("$.localName").value("Sparky"))
                .andExpect(jsonPath("$.region").value("Kanto"))
                .andExpect(jsonPath("$.internalTags[0]").value("starter"))
                .andExpect(jsonPath("$.internalTags[1]").value("electric"));
    }

    @Test
    void syncPokemon_returns404WhenPokemonNotFoundInPokeApi() throws Exception {
        when(pokemonService.sync("missingno"))
                .thenThrow(ApiException.notFound("Pokemon 'missingno' not found in PokeAPI"));

        mockMvc.perform(
                        post("/pokemon/missingno/sync")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Pokemon 'missingno' not found in PokeAPI"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void syncPokemon_returns400WhenIdentifierIsInvalid() throws Exception {
        when(pokemonService.sync(anyString()))
                .thenThrow(
                        ApiException.badRequest(
                                "Pokemon id or name must not be blank", "idOrName"));

        mockMvc.perform(
                        post("/pokemon/{idOrName}/sync", "   ")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Pokemon id or name must not be blank"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void syncPokemon_withoutToken_returns401() throws Exception {
        mockMvc.perform(
                        post("/pokemon/pikachu/sync")
                                .header("X-Correlation-Id", "test-correlation-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.correlationId").value("test-correlation-401"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(pokemonService, never()).sync(anyString());
    }

    @Test
    void updateLocalPokemon_withoutToken_returns401() throws Exception {
        mockMvc.perform(
                        patch("/pokemon/local/1")
                                .header("X-Correlation-Id", "test-correlation-patch-401")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"localName\":\"Thunder\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.correlationId").value("test-correlation-patch-401"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(pokemonService, never()).update(anyLong(), any());
    }

    @Test
    void updateLocalPokemon_partialUpdate_returns200() throws Exception {
        when(pokemonService.update(eq(1L), any(PokemonController.UpdatePokemonRequest.class)))
                .thenReturn(
                        new PokemonController.SyncedPokemonResponse(
                                1L,
                                25,
                                "pikachu",
                                "https://example.com/pikachu.png",
                                "Mouse Pokémon",
                                60,
                                List.of("static"),
                                "Thunder",
                                "Kanto",
                                List.of("starter", "electric")));

        mockMvc.perform(
                        patch("/pokemon/local/1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"localName\":\"Thunder\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localId").value(1))
                .andExpect(jsonPath("$.localName").value("Thunder"))
                .andExpect(jsonPath("$.region").value("Kanto"));
    }

    @Test
    void updateLocalPokemon_explicitNullRegion_clearsRegion() throws Exception {
        when(pokemonService.update(eq(1L), any(PokemonController.UpdatePokemonRequest.class)))
                .thenReturn(
                        new PokemonController.SyncedPokemonResponse(
                                1L,
                                25,
                                "pikachu",
                                "https://example.com/pikachu.png",
                                "Mouse Pokémon",
                                60,
                                List.of("static"),
                                "Sparky",
                                null,
                                List.of("starter", "electric")));

        mockMvc.perform(
                        patch("/pokemon/local/1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"region\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.region").doesNotExist());
    }

    @Test
    void updateLocalPokemon_notFound_returns404Envelope() throws Exception {
        when(pokemonService.update(eq(999L), any(PokemonController.UpdatePokemonRequest.class)))
                .thenThrow(ApiException.notFound("Pokemon with id 999 not found"));

        mockMvc.perform(
                        patch("/pokemon/local/999")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"localName\":\"Thunder\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Pokemon with id 999 not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void updateLocalPokemon_localNameTooLong_returns400WithFieldErrors() throws Exception {
        String tooLongName = "a".repeat(101);

        mockMvc.perform(
                        patch("/pokemon/local/1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"localName\":\"" + tooLongName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("localName"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(pokemonService, never()).update(anyLong(), any());
    }

    @Test
    void updateLocalPokemon_unknownProperty_returns400() throws Exception {
        mockMvc.perform(
                        patch("/pokemon/local/1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"mass\":999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(pokemonService, never()).update(anyLong(), any());
    }

    @Test
    void updateLocalPokemon_malformedJson_returns400() throws Exception {
        mockMvc.perform(
                        patch("/pokemon/local/1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(pokemonService, never()).update(anyLong(), any());
    }

    @Test
    void updateLocalPokemon_emptyBody_returns400() throws Exception {
        when(pokemonService.update(eq(1L), any(PokemonController.UpdatePokemonRequest.class)))
                .thenThrow(
                        ApiException.badRequest(
                                "At least one editable field must be provided", null));

        mockMvc.perform(
                        patch("/pokemon/local/1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(
                        jsonPath("$.message").value("At least one editable field must be provided"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void updateLocalPokemon_nonNumericLocalId_returns400() throws Exception {
        mockMvc.perform(
                        patch("/pokemon/local/abc")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"localName\":\"Thunder\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(pokemonService, never()).update(anyLong(), any());
    }

    @Test
    void listPokemonWithWrongMethod_returns405() throws Exception {
        mockMvc.perform(post("/pokemon").header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void updateLocalPokemon_unsupportedMediaType_returns415() throws Exception {
        mockMvc.perform(
                        patch("/pokemon/local/1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("localName=Thunder"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(pokemonService, never()).update(anyLong(), any());
    }

    private static PokemonController.SyncedPokemonResponse sampleSyncedResponse() {
        return new PokemonController.SyncedPokemonResponse(
                1L,
                25,
                "pikachu",
                "https://example.com/pikachu.png",
                "Mouse Pokémon",
                60,
                List.of("static"),
                "Sparky",
                "Kanto",
                List.of("starter", "electric"));
    }

    private static PokemonController.PokemonDetailResponse sampleDetail() {
        return new PokemonController.PokemonDetailResponse(
                1,
                "bulbasaur",
                "https://example.com/1.png",
                "https://example.com/1.png",
                "Seed Pokémon",
                69,
                List.of("overgrow"),
                "A strange seed was planted on its back at birth.",
                List.of(
                        new PokemonController.PokemonStatResponse("hp", 45),
                        new PokemonController.PokemonStatResponse("attack", 49)),
                List.of(
                        new PokemonController.EvolutionStageResponse(
                                1, "bulbasaur", "https://example.com/1.png"),
                        new PokemonController.EvolutionStageResponse(
                                2, "ivysaur", "https://example.com/2.png")));
    }

    private static PokemonController.PageResponse<PokemonController.PokemonSummaryResponse>
            samplePage() {
        return new PokemonController.PageResponse<>(
                List.of(
                        new PokemonController.PokemonSummaryResponse(
                                1,
                                "bulbasaur",
                                "https://example.com/1.png",
                                "Seed Pokémon",
                                69,
                                List.of("overgrow", "chlorophyll")),
                        new PokemonController.PokemonSummaryResponse(
                                2,
                                "ivysaur",
                                "https://example.com/2.png",
                                "Seed Pokémon",
                                130,
                                List.of("overgrow"))),
                1,
                2,
                1351,
                676);
    }

    private static PokemonController.PageResponse<PokemonController.PokemonSummaryResponse>
            emptyPage() {
        return new PokemonController.PageResponse<>(List.of(), 0, 20, 0, 0);
    }
}
