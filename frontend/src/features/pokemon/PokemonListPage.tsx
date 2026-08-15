import { useState } from 'react';
import { ErrorMessage, Pagination, Spinner } from '../../components/ui';
import { parseApiError } from '../../api/errors';
import { PokemonCard } from './PokemonCard';
import { usePokemonList } from './hooks';

export function PokemonListPage() {
  const [page, setPage] = useState(0);
  const size = 20;
  const { data, isLoading, isError, error } = usePokemonList(page, size);

  if (isLoading) {
    return <Spinner />;
  }

  if (isError) {
    return <ErrorMessage message={parseApiError(error).message} />;
  }

  return (
    <section>
      <h1 className="mb-6 text-3xl font-bold text-slate-900">Pokemon</h1>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {data?.content.map((pokemon) => (
          <PokemonCard key={pokemon.id} pokemon={pokemon} />
        ))}
      </div>
      <Pagination
        page={data?.page ?? 0}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
      />
    </section>
  );
}
