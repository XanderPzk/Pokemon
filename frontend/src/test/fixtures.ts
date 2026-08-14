import type { PokemonDetail, PokemonSummary, SyncedPokemon, User } from '../api/types';

export const samplePokemon: PokemonSummary = {
  id: 1,
  name: 'bulbasaur',
  spriteUrl: 'https://example.com/bulbasaur.png',
  category: 'Seed Pokemon',
  mass: 69,
  abilities: ['overgrow', 'chlorophyll'],
};

export const sampleDetail: PokemonDetail = {
  ...samplePokemon,
  imageUrl: 'https://example.com/bulbasaur-art.png',
  description: 'A strange seed was planted on its back at birth.',
  stats: [
    { name: 'hp', baseStat: 45 },
    { name: 'attack', baseStat: 49 },
  ],
  evolutionChain: [
    { id: 1, name: 'bulbasaur', spriteUrl: 'https://example.com/bulbasaur.png' },
    { id: 2, name: 'ivysaur', spriteUrl: 'https://example.com/ivysaur.png' },
  ],
};

export const sampleSynced: SyncedPokemon = {
  localId: 10,
  externalId: 1,
  name: 'bulbasaur',
  spriteUrl: 'https://example.com/bulbasaur.png',
  category: 'Seed Pokemon',
  mass: 69,
  abilities: ['overgrow', 'chlorophyll'],
  localName: 'Bulbasaur',
  region: 'Kanto',
  internalTags: ['starter'],
};

export const sampleUser: User = {
  id: 1,
  email: 'ash@example.com',
  role: 'USER',
  createdAt: '2026-01-01T00:00:00Z',
};
