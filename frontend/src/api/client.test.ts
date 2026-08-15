import { describe, expect, it, beforeEach } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../setupTests';
import { apiClient } from './client';
import { clearToken, getToken, setToken } from '../lib/storage';

describe('apiClient', () => {
  beforeEach(() => {
    clearToken();
  });

  it('attaches Authorization header when token exists', async () => {
    setToken('abc123');
    let authHeader: string | null = null;

    server.use(
      http.get('/api/auth/me', ({ request }) => {
        authHeader = request.headers.get('Authorization');
        return HttpResponse.json({ id: 1, email: 'test@example.com', role: 'USER', createdAt: null });
      }),
    );

    await apiClient.get('/auth/me');
    expect(authHeader).toBe('Bearer abc123');
  });

  it('omits Authorization header when no token', async () => {
    let authHeader: string | null = 'unset';

    server.use(
      http.get('/api/auth/me', ({ request }) => {
        authHeader = request.headers.get('Authorization');
        return HttpResponse.json({ id: 1, email: 'test@example.com', role: 'USER', createdAt: null });
      }),
    );

    await apiClient.get('/auth/me');
    expect(authHeader).toBeNull();
  });

  it('clears token on 401 response', async () => {
    setToken('expired');

    server.use(
      http.get('/api/auth/me', () =>
        HttpResponse.json(
          {
            status: 401,
            error: 'UNAUTHORIZED',
            message: 'Authentication required',
            timestamp: new Date().toISOString(),
          },
          { status: 401 },
        ),
      ),
    );

    await expect(apiClient.get('/auth/me')).rejects.toThrow();
    expect(getToken()).toBeNull();
  });
});
