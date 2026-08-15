import { FormEvent, useMemo, useState } from 'react';
import type { SyncedPokemon } from '../../api/types';
import { ErrorMessage } from '../../components/ui';
import { parseApiError } from '../../api/errors';
import { buildDirtyPatch, validatePatch } from './localPokemonPatch';
import { useUpdateLocalPokemon } from './hooks';

interface LocalPokemonEditFormProps {
  pokemon: SyncedPokemon;
  onUpdated: (pokemon: SyncedPokemon) => void;
}

function tagsToString(tags: string[]): string {
  return tags.join(', ');
}

export function LocalPokemonEditForm({ pokemon, onUpdated }: LocalPokemonEditFormProps) {
  const [localName, setLocalName] = useState(pokemon.localName);
  const [region, setRegion] = useState(pokemon.region ?? '');
  const [tagsInput, setTagsInput] = useState(tagsToString(pokemon.internalTags));
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const updateMutation = useUpdateLocalPokemon();

  const patch = useMemo(
    () => buildDirtyPatch(pokemon, localName, region, tagsInput),
    [pokemon, localName, region, tagsInput],
  );

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setServerError(null);

    const validationErrors = validatePatch(patch);
    if (Object.keys(validationErrors).length > 0) {
      setClientErrors(validationErrors);
      return;
    }

    setClientErrors({});
    try {
      const updated = await updateMutation.mutateAsync({ localId: pokemon.localId, patch });
      onUpdated(updated);
    } catch (error) {
      const parsed = parseApiError(error);
      setServerError(parsed.message);
      setClientErrors(parsed.fieldErrors);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 rounded-xl bg-white p-4 ring-1 ring-slate-200">
      <h3 className="text-lg font-semibold capitalize text-slate-900">{pokemon.name}</h3>
      <div>
        <label htmlFor={`localName-${pokemon.localId}`} className="mb-1 block text-sm font-medium">
          Local name
        </label>
        <input
          id={`localName-${pokemon.localId}`}
          value={localName}
          onChange={(event) => setLocalName(event.target.value)}
          aria-invalid={Boolean(clientErrors.localName)}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
        {clientErrors.localName && (
          <p className="mt-1 text-sm text-red-600">{clientErrors.localName}</p>
        )}
      </div>
      <div>
        <label htmlFor={`region-${pokemon.localId}`} className="mb-1 block text-sm font-medium">
          Region
        </label>
        <input
          id={`region-${pokemon.localId}`}
          value={region}
          onChange={(event) => setRegion(event.target.value)}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      </div>
      <div>
        <label htmlFor={`tags-${pokemon.localId}`} className="mb-1 block text-sm font-medium">
          Internal tags (comma-separated)
        </label>
        <input
          id={`tags-${pokemon.localId}`}
          value={tagsInput}
          onChange={(event) => setTagsInput(event.target.value)}
          aria-invalid={Boolean(clientErrors.internalTags)}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
        {clientErrors.internalTags && (
          <p className="mt-1 text-sm text-red-600">{clientErrors.internalTags}</p>
        )}
      </div>
      {clientErrors.form && <ErrorMessage message={clientErrors.form} />}
      {serverError && <ErrorMessage message={serverError} />}
      <button
        type="submit"
        disabled={updateMutation.isPending}
        className="rounded-md bg-poke-blue px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
      >
        {updateMutation.isPending ? 'Saving...' : 'Save changes'}
      </button>
    </form>
  );
}
