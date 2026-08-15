import type { SyncedPokemon, UpdatePokemonPatch } from '../../api/types';

function tagsToString(tags: string[]): string {
  return tags.join(', ');
}

function parseTags(input: string): string[] {
  return input
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean);
}

export function buildDirtyPatch(
  original: SyncedPokemon,
  localName: string,
  region: string,
  tagsInput: string,
): UpdatePokemonPatch {
  const patch: UpdatePokemonPatch = {};
  const trimmedName = localName.trim();
  const trimmedRegion = region.trim();
  const tags = parseTags(tagsInput);

  if (trimmedName !== original.localName) {
    patch.localName = trimmedName;
  }
  if (trimmedRegion !== (original.region ?? '')) {
    patch.region = trimmedRegion || null;
  }
  if (tagsToString(tags) !== tagsToString(original.internalTags)) {
    patch.internalTags = tags;
  }

  return patch;
}

export function validatePatch(patch: UpdatePokemonPatch): Record<string, string> {
  const errors: Record<string, string> = {};

  if (Object.keys(patch).length === 0) {
    errors.form = 'At least one editable field must be provided';
  }
  if (patch.localName !== undefined && !patch.localName?.trim()) {
    errors.localName = 'Local name must not be blank';
  }
  if (patch.internalTags && patch.internalTags.length > 20) {
    errors.internalTags = 'Internal tags must not exceed 20';
  }

  return errors;
}
