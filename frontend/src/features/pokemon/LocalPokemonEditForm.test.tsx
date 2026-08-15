import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../test/testUtils';
import { LocalPokemonEditForm } from './LocalPokemonEditForm';
import { buildDirtyPatch, validatePatch } from './localPokemonPatch';
import { sampleSynced } from '../../test/fixtures';

describe('LocalPokemonEditForm helpers', () => {
  it('builds patch with only dirty fields', () => {
    const patch = buildDirtyPatch(sampleSynced, 'Bulbasaur', 'Johto', 'starter');
    expect(patch).toEqual({
      region: 'Johto',
    });
  });

  it('blocks blank localName', () => {
    const errors = validatePatch({ localName: '   ' });
    expect(errors.localName).toBe('Local name must not be blank');
  });

  it('blocks more than 20 tags', () => {
    const tags = Array.from({ length: 21 }, (_, index) => `tag-${index}`);
    const errors = validatePatch({ internalTags: tags });
    expect(errors.internalTags).toBe('Internal tags must not exceed 20');
  });

  it('blocks empty patch', () => {
    const errors = validatePatch({});
    expect(errors.form).toBe('At least one editable field must be provided');
  });
});

describe('LocalPokemonEditForm', () => {
  it('blocks blank localName on submit', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <LocalPokemonEditForm pokemon={sampleSynced} onUpdated={vi.fn()} />,
    );

    const nameInput = screen.getByLabelText('Local name');
    await user.clear(nameInput);
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByText('Local name must not be blank')).toBeInTheDocument();
  });

  it('blocks empty patch', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <LocalPokemonEditForm pokemon={sampleSynced} onUpdated={vi.fn()} />,
    );

    await user.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(await screen.findByText('At least one editable field must be provided')).toBeInTheDocument();
  });

  it('binds server fieldErrors to inputs', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <LocalPokemonEditForm pokemon={sampleSynced} onUpdated={vi.fn()} />,
    );

    const nameInput = screen.getByLabelText('Local name');
    await user.clear(nameInput);
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByText('Local name must not be blank')).toBeInTheDocument();
  });

  it('calls onUpdated after successful save', async () => {
    const onUpdated = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(
      <LocalPokemonEditForm pokemon={sampleSynced} onUpdated={onUpdated} />,
    );

    const regionInput = screen.getByLabelText('Region');
    await user.clear(regionInput);
    await user.type(regionInput, 'Johto');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => {
      expect(onUpdated).toHaveBeenCalledWith(expect.objectContaining({ region: 'Johto' }));
    });
  });
});
