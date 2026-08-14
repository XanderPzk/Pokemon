import type {
  PageResponse,
  PokemonDetail,
  PokemonSummary,
  SyncedPokemon,
  UpdatePokemonPatch,
} from '../../api/types';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import { upsertLocalPokemon } from './localStore';
import { getStoredUserEmail } from '../auth/AuthContext';

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

export function usePokemonList(page: number, size: number) {
  return useQuery({
    queryKey: ['pokemon', 'list', page, size],
    queryFn: () => fetchPokemonList(page, size),
  });
}

export function usePokemonDetail(idOrName: string) {
  return useQuery({
    queryKey: ['pokemon', 'detail', idOrName],
    queryFn: () => fetchPokemonDetail(idOrName),
    enabled: Boolean(idOrName),
  });
}

export function useSyncPokemon() {
  const queryClient = useQueryClient();
  const userEmail = getStoredUserEmail();

  return useMutation({
    mutationFn: (idOrName: string) => syncPokemon(idOrName),
    onSuccess: (data: SyncedPokemon) => {
      if (userEmail) {
        upsertLocalPokemon(userEmail, data);
      }
      queryClient.invalidateQueries({ queryKey: ['pokemon'] });
    },
  });
}

export function useUpdateLocalPokemon() {
  const userEmail = getStoredUserEmail();

  return useMutation({
    mutationFn: ({ localId, patch }: { localId: number; patch: UpdatePokemonPatch }) =>
      updateLocalPokemon(localId, patch),
    onSuccess: (data: SyncedPokemon) => {
      if (userEmail) {
        upsertLocalPokemon(userEmail, data);
      }
    },
  });
}

export function formatMassKg(massHectograms: number): string {
  return `${(massHectograms / 10).toFixed(1)} kg`;
}

export function pokemonSummaryFromDetail(detail: PokemonDetail): PokemonSummary {
  return {
    id: detail.id,
    name: detail.name,
    spriteUrl: detail.spriteUrl,
    category: detail.category,
    mass: detail.mass,
    abilities: detail.abilities,
  };
}
