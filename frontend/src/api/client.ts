import axios, { type AxiosError } from 'axios';
import { QueryClient } from '@tanstack/react-query';
import { clearToken, getToken } from '../features/auth/AuthContext';
import type { ErrorResponse } from './types';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api';

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearToken();
    }
    return Promise.reject(error);
  },
);

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 60_000,
      refetchOnWindowFocus: false,
    },
  },
});

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
