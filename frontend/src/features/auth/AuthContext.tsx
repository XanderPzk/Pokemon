import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { AuthTokenResponse, LoginRequest, RegisterRequest, User } from '../../api/types';
import { apiClient } from '../../api/client';

const TOKEN_KEY = 'pokedex.auth.token';
const USER_KEY = 'pokedex.auth.user';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getStoredUserEmail(): string | null {
  return localStorage.getItem(USER_KEY);
}

export function setStoredUserEmail(email: string): void {
  localStorage.setItem(USER_KEY, email);
}

export function localPokemonStorageKey(userEmail: string): string {
  return `pokedex.localPokemon.${userEmail}`;
}

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

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  loginSuccess: (token: string, email: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const loadUser = useCallback(async () => {
    const token = getToken();
    if (!token) {
      setUser(null);
      setIsLoading(false);
      return;
    }
    try {
      const currentUser = await fetchCurrentUser();
      setUser(currentUser);
      setStoredUserEmail(currentUser.email);
    } catch {
      clearToken();
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadUser();
  }, [loadUser]);

  const loginSuccess = useCallback(async (token: string, email: string) => {
    setToken(token);
    setStoredUserEmail(email);
    const currentUser = await fetchCurrentUser();
    setUser(currentUser);
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isLoading,
      loginSuccess,
      logout,
    }),
    [user, isLoading, loginSuccess, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}

export function useAuthEmail(): string | null {
  return getStoredUserEmail();
}
