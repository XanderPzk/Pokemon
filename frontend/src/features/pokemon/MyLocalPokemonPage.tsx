import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuthEmail } from '../auth/AuthContext';
import { listLocalPokemon } from './localStore';
import { LocalPokemonEditForm } from './LocalPokemonEditForm';
import type { SyncedPokemon } from '../../api/types';

export function MyLocalPokemonPage() {
  const userEmail = useAuthEmail();
  const [refreshKey, setRefreshKey] = useState(0);

  const items = useMemo(() => {
    if (!userEmail) {
      return [];
    }
    return listLocalPokemon(userEmail);
    // refreshKey forces re-read after edits
  }, [userEmail, refreshKey]);

  function handleUpdated(_updated: SyncedPokemon) {
    setRefreshKey((current) => current + 1);
  }

  if (items.length === 0) {
    return (
      <section className="rounded-xl bg-white p-8 text-center shadow-sm ring-1 ring-slate-200">
        <h1 className="text-2xl font-bold text-slate-900">My local Pokemon</h1>
        <p className="mt-4 text-slate-600">
          You have not synced any Pokemon yet. Browse the catalog and sync your favorites.
        </p>
        <Link
          to="/"
          className="mt-6 inline-block rounded-md bg-poke-blue px-4 py-2 text-sm font-semibold text-white"
        >
          Browse Pokemon
        </Link>
      </section>
    );
  }

  return (
    <section>
      <h1 className="mb-6 text-3xl font-bold text-slate-900">My local Pokemon</h1>
      <div className="grid gap-6 lg:grid-cols-2">
        {items.map((pokemon) => (
          <LocalPokemonEditForm
            key={pokemon.localId}
            pokemon={pokemon}
            onUpdated={handleUpdated}
          />
        ))}
      </div>
    </section>
  );
}
