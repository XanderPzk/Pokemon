import { http, HttpResponse } from 'msw';
import { sampleDetail, samplePokemon, sampleSynced, sampleUser } from '../fixtures';

const baseUrl = '/api';

export const handlers = [
  http.get(`${baseUrl}/pokemon`, ({ request }) => {
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') ?? '0');
    const size = Number(url.searchParams.get('size') ?? '20');
    return HttpResponse.json({
      content: [samplePokemon],
      page,
      size,
      totalElements: 1,
      totalPages: 1,
    });
  }),

  http.get(`${baseUrl}/pokemon/:idOrName`, ({ params }) => {
    if (params.idOrName === 'missing') {
      return HttpResponse.json(
        {
          status: 404,
          error: 'NOT_FOUND',
          message: 'Pokemon not found',
          timestamp: new Date().toISOString(),
        },
        { status: 404 },
      );
    }
    return HttpResponse.json(sampleDetail);
  }),

  http.post(`${baseUrl}/pokemon/:idOrName/sync`, () => HttpResponse.json(sampleSynced)),

  http.patch(`${baseUrl}/pokemon/local/:localId`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    if (body.localName === '') {
      return HttpResponse.json(
        {
          status: 400,
          error: 'VALIDATION_ERROR',
          message: 'Validation failed',
          timestamp: new Date().toISOString(),
          fieldErrors: [{ field: 'localName', message: 'Local name must not be blank' }],
        },
        { status: 400 },
      );
    }
    return HttpResponse.json({
      ...sampleSynced,
      ...body,
    });
  }),

  http.post(`${baseUrl}/auth/login`, async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string };
    if (body.password === 'wrong') {
      return HttpResponse.json(
        {
          status: 401,
          error: 'UNAUTHORIZED',
          message: 'Invalid credentials',
          timestamp: new Date().toISOString(),
        },
        { status: 401 },
      );
    }
    return HttpResponse.json({ token: 'test-token' });
  }),

  http.post(`${baseUrl}/auth/register`, async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string };
    if (body.password.length < 8) {
      return HttpResponse.json(
        {
          status: 400,
          error: 'VALIDATION_ERROR',
          message: 'Validation failed',
          timestamp: new Date().toISOString(),
          fieldErrors: [{ field: 'password', message: 'Password must be at least 8 characters' }],
        },
        { status: 400 },
      );
    }
    return HttpResponse.json(sampleUser, { status: 201 });
  }),

  http.get(`${baseUrl}/auth/me`, ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) {
      return HttpResponse.json(
        {
          status: 401,
          error: 'UNAUTHORIZED',
          message: 'Authentication required',
          timestamp: new Date().toISOString(),
        },
        { status: 401 },
      );
    }
    return HttpResponse.json(sampleUser);
  }),
];
