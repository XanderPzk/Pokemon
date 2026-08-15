import { Link } from 'react-router-dom';
import type { PokemonSummary } from '../../api/types';
import { formatMassKg } from './format';

interface PokemonCardProps {
  pokemon: PokemonSummary;
}

export function PokemonCard({ pokemon }: PokemonCardProps) {
  return (
    <article className="rounded-xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
      <Link to={`/pokemon/${pokemon.id}`} className="block">
        <div className="flex items-center gap-4">
          <img
            src={pokemon.spriteUrl}
            alt={pokemon.name}
            className="h-20 w-20 object-contain"
          />
          <div>
            <h2 className="text-lg font-semibold capitalize text-slate-900">{pokemon.name}</h2>
            <p className="text-sm text-slate-600">{pokemon.category}</p>
            <p className="text-sm text-slate-600">Mass: {formatMassKg(pokemon.mass)}</p>
            <p className="text-sm text-slate-600">
              Abilities: {pokemon.abilities.join(', ')}
            </p>
          </div>
        </div>
      </Link>
    </article>
  );
}
