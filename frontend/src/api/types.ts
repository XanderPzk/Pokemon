export type Role = 'USER' | 'ADMIN';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PokemonSummary {
  id: number;
  name: string;
  spriteUrl: string;
  category: string;
  mass: number;
  abilities: string[];
}

export interface PokemonStat {
  name: string;
  baseStat: number;
}

export interface EvolutionStage {
  id: number;
  name: string;
  spriteUrl: string;
}

export interface PokemonDetail extends PokemonSummary {
  imageUrl: string;
  description: string;
  stats: PokemonStat[];
  evolutionChain: EvolutionStage[];
}

export interface SyncedPokemon {
  localId: number;
  externalId: number;
  name: string;
  spriteUrl: string;
  category: string;
  mass: number;
  abilities: string[];
  localName: string;
  region: string | null;
  internalTags: string[];
}

export interface User {
  id: number | null;
  email: string;
  role: Role;
  createdAt: string | null;
}

export interface AuthTokenResponse {
  token: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  correlationId?: string;
  fieldErrors?: FieldError[];
}

export interface UpdatePokemonPatch {
  localName?: string | null;
  region?: string | null;
  internalTags?: string[] | null;
}
