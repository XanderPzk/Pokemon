package com.alex.pokedex.pokemon;

import com.alex.pokedex.common.ApiException;
import com.alex.pokedex.pokemon.PokeApiDtos.EvolutionChainResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonListResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonResponse;
import com.alex.pokedex.pokemon.PokeApiDtos.PokemonSpeciesResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PokemonService {

    private final PokeApiHttpClient pokeApiHttpClient;
    private final PokemonRepository pokemonRepository;

    public PokemonService(
            PokeApiHttpClient pokeApiHttpClient, PokemonRepository pokemonRepository) {
        this.pokeApiHttpClient = pokeApiHttpClient;
        this.pokemonRepository = pokemonRepository;
    }

    public PokemonController.PageResponse<PokemonController.PokemonSummaryResponse> list(
            int page, int size) {
        if (size < 1) {
            throw ApiException.badRequest("Requested page size must be at least 1", "size");
        }
        if (page < 0) {
            throw ApiException.badRequest("Requested page must not be negative", "page");
        }

        PokemonListResponse list = pokeApiHttpClient.fetchList(page * size, size);
        return PokeApiMapper.toPageResponse(
                list.results().stream()
                        .map(entry -> summaryOf(PokeApiMapper.resourceId(entry.url())))
                        .toList(),
                page,
                size,
                list.count());
    }

    public PokemonController.PokemonDetailResponse getByIdOrName(String idOrName) {
        PokemonResponse pokemon = pokeApiHttpClient.fetchPokemon(normalize(idOrName));
        PokemonSpeciesResponse species = speciesOf(pokemon);
        EvolutionChainResponse chain = evolutionChainOf(species);
        return PokeApiMapper.toDetailResponse(
                pokemon, species, chain, evolutionStages(chain, pokemon));
    }

    @Transactional
    public PokemonController.SyncedPokemonResponse sync(String idOrName) {
        PokemonController.PokemonDetailResponse detail = getByIdOrName(idOrName);
        Pokemon.Snapshot snapshot =
                new Pokemon.Snapshot(
                        detail.id(),
                        detail.name(),
                        detail.spriteUrl(),
                        detail.category(),
                        detail.mass(),
                        detail.abilities());

        Pokemon pokemon =
                pokemonRepository
                        .findByExternalId(snapshot.externalId())
                        .map(
                                existing -> {
                                    existing.refreshSnapshot(snapshot);
                                    return existing;
                                })
                        .orElseGet(() -> Pokemon.fromSnapshot(snapshot));

        return toSyncedResponse(pokemonRepository.save(resolveEntity(pokemon)));
    }

    @Transactional
    public PokemonController.SyncedPokemonResponse update(
            Long localId, PokemonController.UpdatePokemonRequest request) {
        Pokemon pokemon =
                pokemonRepository
                        .findById(localId)
                        .orElseThrow(
                                () ->
                                        ApiException.notFound(
                                                "Pokemon with id " + localId + " not found"));

        pokemon.applyLocalEdits(request);
        return toSyncedResponse(pokemonRepository.save(pokemon));
    }

    private Pokemon resolveEntity(Pokemon pokemon) {
        if (pokemon.getId() != null) {
            return pokemonRepository.findById(pokemon.getId()).orElse(pokemon);
        }
        return pokemonRepository.findByExternalId(pokemon.getExternalId()).orElse(pokemon);
    }

    private PokemonController.PokemonSummaryResponse summaryOf(int pokemonId) {
        PokemonResponse pokemon = pokeApiHttpClient.fetchPokemon(String.valueOf(pokemonId));
        return PokeApiMapper.toSummaryResponse(pokemon, speciesOf(pokemon));
    }

    private PokemonSpeciesResponse speciesOf(PokemonResponse pokemon) {
        if (pokemon.species() == null) {
            return null;
        }
        return pokeApiHttpClient.fetchSpecies(PokeApiMapper.resourceId(pokemon.species().url()));
    }

    private EvolutionChainResponse evolutionChainOf(PokemonSpeciesResponse species) {
        if (species == null || species.evolutionChain() == null) {
            return null;
        }
        return pokeApiHttpClient.fetchEvolutionChain(
                PokeApiMapper.resourceId(species.evolutionChain().url()));
    }

    private Map<Integer, PokemonResponse> evolutionStages(
            EvolutionChainResponse chain, PokemonResponse alreadyFetched) {
        Map<Integer, PokemonResponse> bySpeciesId = new LinkedHashMap<>();
        bySpeciesId.put(alreadyFetched.id(), alreadyFetched);
        PokeApiMapper.evolutionSpeciesIds(chain)
                .forEach(
                        speciesId ->
                                bySpeciesId.computeIfAbsent(
                                        speciesId,
                                        id -> pokeApiHttpClient.fetchPokemon(String.valueOf(id))));
        return bySpeciesId;
    }

    private static String normalize(String idOrName) {
        if (idOrName == null || idOrName.isBlank()) {
            throw ApiException.badRequest("Pokemon id or name must not be blank", "idOrName");
        }
        return idOrName.trim().toLowerCase(Locale.ROOT);
    }

    static PokemonController.SyncedPokemonResponse toSyncedResponse(Pokemon pokemon) {
        return new PokemonController.SyncedPokemonResponse(
                pokemon.getId(),
                pokemon.getExternalId(),
                pokemon.getName(),
                pokemon.getSpriteUrl(),
                pokemon.getCategory(),
                pokemon.getMass(),
                pokemon.getAbilities(),
                pokemon.getLocalName(),
                pokemon.getRegion(),
                pokemon.getInternalTags());
    }
}
