import { Link } from 'react-router-dom';
import type { EvolutionStage } from '../../api/types';

interface EvolutionChainProps {
  stages: EvolutionStage[];
}

export function EvolutionChain({ stages }: EvolutionChainProps) {
  if (stages.length === 0) {
    return null;
  }

  return (
    <section aria-label="Evolution chain">
      <h2 className="mb-4 text-xl font-semibold text-slate-900">Evolution</h2>
      <ol className="flex flex-wrap items-center gap-4">
        {stages.map((stage, index) => (
          <li key={stage.id} className="flex items-center gap-4">
            <Link
              to={`/pokemon/${stage.id}`}
              aria-label={stage.name}
              className="flex flex-col items-center rounded-lg bg-white p-3 shadow-sm ring-1 ring-slate-200"
            >
              <img src={stage.spriteUrl} alt="" className="h-16 w-16 object-contain" />
              <span className="mt-2 text-sm capitalize text-slate-700">{stage.name}</span>
            </Link>
            {index < stages.length - 1 && (
              <span aria-hidden="true" className="text-slate-400">
                →
              </span>
            )}
          </li>
        ))}
      </ol>
    </section>
  );
}
