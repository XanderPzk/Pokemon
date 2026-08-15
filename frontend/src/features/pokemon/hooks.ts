import type { SyncedPokemon, UpdatePokemonPatch } from '../../api/types';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getStoredUserEmail } from '../auth/AuthContext';
import {
  fetchPokemonDetail,
  fetchPokemonList,
  syncPokemon,
  updateLocalPokemon,
} from './pokemonApi';
import { upsertLocalPokemon } from './localStore';

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
