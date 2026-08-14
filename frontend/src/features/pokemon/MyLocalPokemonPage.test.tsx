import { describe, expect, it, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../test/testUtils';
import { MyLocalPokemonPage } from './MyLocalPokemonPage';
import { upsertLocalPokemon } from './localStore';
import { sampleSynced } from '../../test/fixtures';
import { setStoredUserEmail, setToken } from '../auth/AuthContext';

describe('MyLocalPokemonPage', () => {
  beforeEach(() => {
    localStorage.clear();
    setToken('test-token');
    setStoredUserEmail('ash@example.com');
  });

  it('shows empty state when registry is empty', async () => {
    renderWithProviders(<MyLocalPokemonPage />, { route: '/my-pokemon' });
    expect(await screen.findByText(/You have not synced any Pokemon yet/)).toBeInTheDocument();
  });

  it('renders registry entries', async () => {
    upsertLocalPokemon('ash@example.com', sampleSynced);
    renderWithProviders(<MyLocalPokemonPage />, { route: '/my-pokemon' });

    expect(await screen.findByDisplayValue('Bulbasaur')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Kanto')).toBeInTheDocument();
  });

  it('updates registry after successful edit', async () => {
    upsertLocalPokemon('ash@example.com', sampleSynced);
    const user = userEvent.setup();
    renderWithProviders(<MyLocalPokemonPage />, { route: '/my-pokemon' });

    const regionInput = await screen.findByDisplayValue('Kanto');
    await user.clear(regionInput);
    await user.type(regionInput, 'Johto');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => {
      expect(screen.getByDisplayValue('Johto')).toBeInTheDocument();
    });
  });
});
