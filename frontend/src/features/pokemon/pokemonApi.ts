import type {
  PageResponse,
  PokemonDetail,
  PokemonSummary,
  SyncedPokemon,
  UpdatePokemonPatch,
} from '../../api/types';
import { apiClient } from '../../api/client';

export async function fetchPokemonList(page: number, size: number) {
  const { data } = await apiClient.get<PageResponse<PokemonSummary>>('/pokemon', {
    params: { page, size },
  });
  return data;
}

export async function fetchPokemonDetail(idOrName: string) {
  const { data } = await apiClient.get<PokemonDetail>(`/pokemon/${idOrName}`);
  return data;
}

export async function syncPokemon(idOrName: string) {
  const { data } = await apiClient.post<SyncedPokemon>(`/pokemon/${idOrName}/sync`);
  return data;
}

export async function updateLocalPokemon(localId: number, patch: UpdatePokemonPatch) {
  const { data } = await apiClient.patch<SyncedPokemon>(`/pokemon/local/${localId}`, patch);
  return data;
}
