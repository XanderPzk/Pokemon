import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { User } from '../../api/types';
import {
  clearToken,
  getStoredUserEmail,
  getToken,
  setStoredUserEmail,
  setToken,
} from '../../lib/storage';
import { fetchCurrentUser } from './authApi';

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

export {
  clearToken,
  getStoredUserEmail,
  getToken,
  localPokemonStorageKey,
  setStoredUserEmail,
  setToken,
} from '../../lib/storage';

export { login, register } from './authApi';
