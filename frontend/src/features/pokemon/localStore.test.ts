import { describe, expect, it, beforeEach } from 'vitest';
import {
  getLocalPokemon,
  listLocalPokemon,
  removeLocalPokemon,
  upsertLocalPokemon,
} from './localStore';
import { sampleSynced } from '../../test/fixtures';

const userEmail = 'ash@example.com';

describe('localStore', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('upserts by localId', () => {
    upsertLocalPokemon(userEmail, sampleSynced);
    const updated = { ...sampleSynced, localName: 'Updated Name' };
    upsertLocalPokemon(userEmail, updated);

    const items = listLocalPokemon(userEmail);
    expect(items).toHaveLength(1);
    expect(items[0].localName).toBe('Updated Name');
  });

  it('lists entries sorted by localName', () => {
    upsertLocalPokemon(userEmail, { ...sampleSynced, localId: 1, localName: 'Zapdos' });
    upsertLocalPokemon(userEmail, { ...sampleSynced, localId: 2, localName: 'Articuno' });

    const items = listLocalPokemon(userEmail);
    expect(items.map((item) => item.localName)).toEqual(['Articuno', 'Zapdos']);
  });

  it('removes an entry', () => {
    upsertLocalPokemon(userEmail, sampleSynced);
    removeLocalPokemon(userEmail, sampleSynced.localId);
    expect(listLocalPokemon(userEmail)).toHaveLength(0);
  });

  it('gets a single entry', () => {
    upsertLocalPokemon(userEmail, sampleSynced);
    expect(getLocalPokemon(userEmail, sampleSynced.localId)).toEqual(sampleSynced);
  });

  it('tolerates corrupt JSON', () => {
    localStorage.setItem(`pokedex.localPokemon.${userEmail}`, '{not-json');
    expect(listLocalPokemon(userEmail)).toEqual([]);
  });

  it('scopes registry per user email', () => {
    upsertLocalPokemon(userEmail, sampleSynced);
    expect(listLocalPokemon('misty@example.com')).toEqual([]);
    expect(listLocalPokemon(userEmail)).toHaveLength(1);
  });
});
