import { describe, expect, it, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { renderWithProviders } from '../../test/testUtils';
import { PokemonDetailPage } from './PokemonDetailPage';
import { setToken, setStoredUserEmail } from '../auth/AuthContext';
import { sampleSynced } from '../../test/fixtures';
import { getLocalPokemon } from './localStore';

function renderDetail(route = '/pokemon/1') {
  return renderWithProviders(
    <Routes>
      <Route path="/pokemon/:idOrName" element={<PokemonDetailPage />} />
    </Routes>,
    { route },
  );
}

describe('PokemonDetailPage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('renders detail with image, description, stats and evolution chain', async () => {
    renderDetail('/pokemon/1');

    expect(await screen.findByRole('heading', { name: 'bulbasaur' })).toBeInTheDocument();
    expect(screen.getByText('A strange seed was planted on its back at birth.')).toBeInTheDocument();
    expect(screen.getByText('hp')).toBeInTheDocument();
    expect(screen.getByText('attack')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'ivysaur' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'bulbasaur' })).toHaveAttribute(
      'src',
      'https://example.com/bulbasaur-art.png',
    );
  });

  it('shows 404 state', async () => {
    renderDetail('/pokemon/missing');
    expect(await screen.findByRole('alert')).toHaveTextContent('Pokemon not found');
  });

  it('hides sync button when unauthenticated', async () => {
    renderDetail('/pokemon/1');
    await screen.findByRole('heading', { name: 'bulbasaur' });
    expect(screen.queryByRole('button', { name: 'Sync to local collection' })).not.toBeInTheDocument();
    expect(screen.getByText(/Login/)).toBeInTheDocument();
  });

  it('syncs pokemon to local registry when authenticated', async () => {
    setToken('test-token');
    setStoredUserEmail('ash@example.com');

    const user = userEvent.setup();
    renderDetail('/pokemon/1');

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Sync to local collection' })).toBeInTheDocument();
    });
    await user.click(screen.getByRole('button', { name: 'Sync to local collection' }));

    await waitFor(() => {
      expect(getLocalPokemon('ash@example.com', sampleSynced.localId)).toEqual(sampleSynced);
    });
    expect(screen.getByText('Pokemon synced to your local collection.')).toBeInTheDocument();
  });
});
