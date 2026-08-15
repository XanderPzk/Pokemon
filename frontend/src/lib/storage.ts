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
