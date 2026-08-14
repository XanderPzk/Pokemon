import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ErrorMessage, Spinner } from '../../components/ui';
import { isNotFoundError, parseApiError } from '../../api/client';
import { useAuth } from '../auth/AuthContext';
import { EvolutionChain } from './EvolutionChain';
import { formatMassKg, usePokemonDetail, useSyncPokemon } from './hooks';
import { StatBar } from './StatBar';

export function PokemonDetailPage() {
  const { idOrName = '' } = useParams();
  const { isAuthenticated } = useAuth();
  const { data, isLoading, isError, error } = usePokemonDetail(idOrName);
  const syncMutation = useSyncPokemon();
  const [syncMessage, setSyncMessage] = useState<string | null>(null);

  if (isLoading) {
    return <Spinner />;
  }

  if (isError) {
    if (isNotFoundError(error)) {
      return <ErrorMessage message="Pokemon not found" />;
    }
    return <ErrorMessage message={parseApiError(error).message} />;
  }

  if (!data) {
    return null;
  }

  async function handleSync() {
    setSyncMessage(null);
    try {
      await syncMutation.mutateAsync(idOrName);
      setSyncMessage('Pokemon synced to your local collection.');
    } catch (err) {
      setSyncMessage(parseApiError(err).message);
    }
  }

  return (
    <article className="space-y-8">
      <div>
        <Link to="/" className="text-sm font-medium text-poke-blue">
          ← Back to list
        </Link>
      </div>

      <div className="grid gap-8 lg:grid-cols-[280px_1fr]">
        <img
          src={data.imageUrl}
          alt={data.name}
          className="mx-auto h-64 w-64 object-contain"
        />
        <div>
          <h1 className="text-3xl font-bold capitalize text-slate-900">{data.name}</h1>
          <p className="mt-2 text-slate-600">{data.category}</p>
          <p className="mt-1 text-slate-600">Mass: {formatMassKg(data.mass)}</p>
          <p className="mt-1 text-slate-600">Abilities: {data.abilities.join(', ')}</p>
          <p className="mt-4 text-slate-700">{data.description}</p>

          {isAuthenticated ? (
            <div className="mt-6">
              <button
                type="button"
                onClick={() => void handleSync()}
                disabled={syncMutation.isPending}
                className="rounded-md bg-poke-red px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                {syncMutation.isPending ? 'Syncing...' : 'Sync to local collection'}
              </button>
              {syncMessage && <p className="mt-2 text-sm text-slate-600">{syncMessage}</p>}
            </div>
          ) : (
            <p className="mt-6 text-sm text-slate-600">
              <Link to="/login" className="font-medium text-poke-blue">
                Login
              </Link>{' '}
              to sync this Pokemon locally.
            </p>
          )}
        </div>
      </div>

      <section>
        <h2 className="mb-4 text-xl font-semibold text-slate-900">Stats</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          {data.stats.map((stat) => (
            <StatBar key={stat.name} name={stat.name} baseStat={stat.baseStat} />
          ))}
        </div>
      </section>

      <EvolutionChain stages={data.evolutionChain} />
    </article>
  );
}
