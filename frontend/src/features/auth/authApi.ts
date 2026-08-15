import type { AuthTokenResponse, LoginRequest, RegisterRequest, User } from '../../api/types';
import { apiClient } from '../../api/client';

export async function login(request: LoginRequest) {
  const { data } = await apiClient.post<AuthTokenResponse>('/auth/login', request);
  return data;
}

export async function register(request: RegisterRequest) {
  const { data } = await apiClient.post<User>('/auth/register', request);
  return data;
}

export async function fetchCurrentUser() {
  const { data } = await apiClient.get<User>('/auth/me');
  return data;
}
