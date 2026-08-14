import { describe, expect, it, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { renderWithProviders } from '../../test/testUtils';
import { server } from '../../setupTests';
import { PokemonListPage } from './PokemonListPage';
import { samplePokemon } from '../../test/fixtures';

describe('PokemonListPage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('renders pokemon cards with sprite, category, mass and abilities', async () => {
    renderWithProviders(<PokemonListPage />);

    expect(await screen.findByText('bulbasaur')).toBeInTheDocument();
    expect(screen.getByText('Seed Pokemon')).toBeInTheDocument();
    expect(screen.getByText('Mass: 6.9 kg')).toBeInTheDocument();
    expect(screen.getByText('Abilities: overgrow, chlorophyll')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'bulbasaur' })).toHaveAttribute(
      'src',
      samplePokemon.spriteUrl,
    );
  });

  it('shows loading state initially', () => {
    renderWithProviders(<PokemonListPage />);
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument();
  });

  it('shows error state on failure', async () => {
    server.use(
      http.get('/api/pokemon', () =>
        HttpResponse.json(
          {
            status: 500,
            error: 'INTERNAL_ERROR',
            message: 'Server exploded',
            timestamp: new Date().toISOString(),
          },
          { status: 500 },
        ),
      ),
    );

    renderWithProviders(<PokemonListPage />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Server exploded');
  });

  it('advances pagination', async () => {
    server.use(
      http.get('/api/pokemon', ({ request }) => {
        const url = new URL(request.url);
        const page = Number(url.searchParams.get('page') ?? '0');
        return HttpResponse.json({
          content: [{ ...samplePokemon, name: page === 0 ? 'bulbasaur' : 'ivysaur' }],
          page,
          size: 20,
          totalElements: 40,
          totalPages: 2,
        });
      }),
    );

    const user = userEvent.setup();
    renderWithProviders(<PokemonListPage />);

    expect(await screen.findByText('bulbasaur')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(await screen.findByText('ivysaur')).toBeInTheDocument();
  });
});
