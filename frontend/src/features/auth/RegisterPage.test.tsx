import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { renderWithProviders } from '../../test/testUtils';
import { server } from '../../setupTests';
import { RegisterPage } from './RegisterPage';

describe('RegisterPage', () => {
  it('validates password length client-side', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText('Email'), 'ash@example.com');
    await user.type(screen.getByLabelText('Password'), 'short');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('Password must be at least 8 characters')).toBeInTheDocument();
  });

  it('binds server fieldErrors to inputs', async () => {
    server.use(
      http.post('/api/auth/register', () =>
        HttpResponse.json(
          {
            status: 400,
            error: 'VALIDATION_ERROR',
            message: 'Validation failed',
            timestamp: new Date().toISOString(),
            fieldErrors: [{ field: 'email', message: 'Email must be valid' }],
          },
          { status: 400 },
        ),
      ),
    );

    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText('Email'), 'ash@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('Email must be valid')).toBeInTheDocument();
  });
});
