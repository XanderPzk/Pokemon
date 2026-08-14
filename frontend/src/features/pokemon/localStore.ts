import type { SyncedPokemon } from '../../api/types';
import { localPokemonStorageKey } from '../auth/AuthContext';

function readRegistry(userEmail: string): SyncedPokemon[] {
  const raw = localStorage.getItem(localPokemonStorageKey(userEmail));
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw) as SyncedPokemon[];
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed;
  } catch {
    return [];
  }
}

function writeRegistry(userEmail: string, items: SyncedPokemon[]): void {
  localStorage.setItem(localPokemonStorageKey(userEmail), JSON.stringify(items));
}

export function listLocalPokemon(userEmail: string): SyncedPokemon[] {
  return readRegistry(userEmail).sort((a, b) => a.localName.localeCompare(b.localName));
}

export function upsertLocalPokemon(userEmail: string, pokemon: SyncedPokemon): void {
  const items = readRegistry(userEmail);
  const index = items.findIndex((item) => item.localId === pokemon.localId);
  if (index >= 0) {
    items[index] = pokemon;
  } else {
    items.push(pokemon);
  }
  writeRegistry(userEmail, items);
}

export function removeLocalPokemon(userEmail: string, localId: number): void {
  const items = readRegistry(userEmail).filter((item) => item.localId !== localId);
  writeRegistry(userEmail, items);
}

export function getLocalPokemon(userEmail: string, localId: number): SyncedPokemon | undefined {
  return readRegistry(userEmail).find((item) => item.localId === localId);
}
