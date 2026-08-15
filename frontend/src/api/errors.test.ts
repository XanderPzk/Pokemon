import { describe, expect, it } from 'vitest';
import { parseApiError, isNotFoundError } from './errors';
import type { AxiosError } from 'axios';
import type { ErrorResponse } from './types';

function makeAxiosError(data: Partial<ErrorResponse>, status = 400): AxiosError<ErrorResponse> {
  return {
    isAxiosError: true,
    message: 'Request failed',
    response: {
      status,
      data: {
        status,
        error: data.error ?? 'ERROR',
        message: data.message ?? 'Something went wrong',
        timestamp: data.timestamp ?? new Date().toISOString(),
        fieldErrors: data.fieldErrors,
      },
      statusText: 'Bad Request',
      headers: {},
      config: {} as never,
    },
    config: {} as never,
    name: 'AxiosError',
    toJSON: () => ({}),
  };
}

describe('parseApiError', () => {
  it('maps error envelope to message and fieldErrors', () => {
    const error = makeAxiosError({
      message: 'Validation failed',
      fieldErrors: [
        { field: 'localName', message: 'Local name must not be blank' },
        { field: 'region', message: 'Invalid region' },
      ],
    });

    const parsed = parseApiError(error);
    expect(parsed.message).toBe('Validation failed');
    expect(parsed.status).toBe(400);
    expect(parsed.fieldErrors).toEqual({
      localName: 'Local name must not be blank',
      region: 'Invalid region',
    });
  });

  it('falls back for non-envelope errors', () => {
    const parsed = parseApiError({ message: 'Network Error' });
    expect(parsed.message).toBe('Network Error');
    expect(parsed.fieldErrors).toEqual({});
  });
});

describe('isNotFoundError', () => {
  it('returns true for 404 responses', () => {
    const error = makeAxiosError({ message: 'Not found' }, 404);
    expect(isNotFoundError(error)).toBe(true);
  });
});
