import type { AxiosError } from 'axios';
import type { ErrorResponse } from './types';

export interface ParsedApiError {
  message: string;
  status: number;
  fieldErrors: Record<string, string>;
}

export function parseApiError(error: unknown): ParsedApiError {
  const axiosError = error as AxiosError<ErrorResponse>;
  const data = axiosError.response?.data;

  if (data && typeof data.message === 'string') {
    const fieldErrors: Record<string, string> = {};
    for (const fieldError of data.fieldErrors ?? []) {
      fieldErrors[fieldError.field] = fieldError.message;
    }
    return {
      message: data.message,
      status: data.status ?? axiosError.response?.status ?? 500,
      fieldErrors,
    };
  }

  return {
    message: axiosError.message || 'Unexpected error',
    status: axiosError.response?.status ?? 500,
    fieldErrors: {},
  };
}

export function isNotFoundError(error: unknown): boolean {
  return parseApiError(error).status === 404;
}
