# Pokedex API

RESTful Pokemon API built with Java 21, Spring Boot, and TDD. Integrates with [PokeAPI](https://pokeapi.co/) for live browse/detail data and supports local persistence with custom fields (sync + patch).

```bash
git clone https://github.com/XanderPzk/Pokemon.git
cd Pokemon
```

Includes a React + TypeScript frontend for browsing, authentication, and editing locally synced Pokemon.

## Quick start (Docker — recommended)

From a clean clone:

```bash
docker compose up --build
```

| Service   | URL                          |
|-----------|------------------------------|
| Frontend  | http://localhost:5173        |
| API       | http://localhost:8080        |
| Swagger UI| http://localhost:8080/swagger-ui.html |
| OpenAPI   | http://localhost:8080/v3/api-docs     |

The `demo` Spring profile seeds a pre-populated database on first boot (see [Demo credentials](#demo-credentials)).

## Local development

### Prerequisites

- Java 21
- Maven 3.9+
- Docker (PostgreSQL via Compose, Testcontainers in tests)
- Node.js 20+ (frontend only)

### Backend

1. Start PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

2. Run the API (without demo seed):

   ```bash
   mvn spring-boot:run
   ```

   To run with demo seed data locally:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=demo
   ```

3. Verify health:

   ```bash
   curl http://localhost:8080/health
   ```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server (port 5173) proxies `/api/*` to `http://localhost:8080`, stripping the `/api` prefix — the same rewrite nginx performs in Docker.

### Tests and quality checks

```bash
mvn verify
```

Apply Spotless formatting locally:

```bash
mvn spotless:apply
```

Frontend tests:

```bash
cd frontend && npm test
```

## Demo credentials

Available when the `demo` profile is active (`docker compose up` enables this automatically):

| Field    | Value              |
|----------|--------------------|
| Email    | `demo@example.com` |
| Password | `demo1234`         |

Pre-synced local Pokemon in PostgreSQL (query via Swagger or sync from the UI to populate the frontend cache):

| External ID | Name       | Local name  | Region | Local DB id (fresh seed)* |
|-------------|------------|-------------|--------|---------------------------|
| 1           | bulbasaur  | Bulby       | Kanto  | 1                         |
| 4           | charmander | Ember       | Kanto  | 2                         |
| 7           | squirtle   | Shellshock  | Kanto  | 3                         |

\*Local ids are sequential on a fresh database; existing data may differ.

Each has `starter` and `kanto` internal tags plus PokeAPI-accurate abilities.

> **Note:** The frontend "My local Pokemon" page reads from browser localStorage, not the server. After logging in, sync a Pokemon from its detail page (or call `PATCH /pokemon/local/{localId}` via Swagger) to interact with seeded records in the UI.

## Environment variables

| Variable | Used by | Default | Description |
|----------|---------|---------|-------------|
| `POSTGRES_DB` | postgres service | `pokedex` | Database name |
| `POSTGRES_USER` | postgres service | `pokedex` | Database user |
| `POSTGRES_PASSWORD` | postgres service | `pokedex` | Database password |
| `JWT_SECRET` | api service | `pokedex-dev-secret-key-at-least-32-bytes-long` | HMAC secret for JWT signing (min 32 bytes) |
| `SPRING_PROFILES_ACTIVE` | api service | _(none locally)_ | Set to `demo` in Docker Compose to enable seed data |
| `SPRING_DATASOURCE_URL` | api service | `jdbc:postgresql://localhost:5432/pokedex` | JDBC URL (overridden in Compose) |
| `SPRING_DATASOURCE_USERNAME` | api service | `pokedex` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | api service | `pokedex` | Database password |
| `VITE_API_BASE_URL` | frontend build | `/api` | Axios base URL (browser-relative in production) |

Copy `.env.example` to `.env` to override Compose defaults.

## API endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/health` | Public | Liveness probe |
| `GET` | `/actuator/health` | Public | Spring Actuator health |
| `POST` | `/auth/register` | Public | Register `{ email, password }` → 201 |
| `POST` | `/auth/login` | Public | Login → `{ token }` |
| `GET` | `/auth/me` | **JWT** | Current user profile |
| `GET` | `/pokemon?page=&size=` | Public | Paginated PokeAPI browse (cached) |
| `GET` | `/pokemon/{idOrName}` | Public | PokeAPI detail (stats, description, evolution) |
| `POST` | `/pokemon/{idOrName}/sync` | **JWT** | Persist PokeAPI snapshot locally |
| `PATCH` | `/pokemon/local/{localId}` | **JWT** | Edit local fields (`localName`, `region`, `internalTags`) |

Protected routes require `Authorization: Bearer <token>`.

Error responses use a consistent envelope:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Pokemon with id 999 not found",
  "timestamp": "2026-08-13T18:00:00Z",
  "correlationId": "..."
}
```

## Architecture

Layered Spring Boot, organized by feature with explicit web/service/repository sub-packages:

```
com.alex.pokedex/
├── common/       # ApiException, GlobalExceptionHandler, RequestLoggingFilter
├── config/       # Security, OpenAPI, PokeApi RestClient
├── auth/
│   ├── web/          # AuthController
│   ├── service/      # AuthService, JwtTokenProvider
│   └── repository/   # User, UserRepository
├── pokemon/
│   ├── web/          # PokemonController
│   ├── service/      # PokemonService, PokeApiMapper
│   ├── repository/   # Pokemon, PokemonRepository
│   └── pokeapi/      # PokeApiHttpClient, PokeApiDtos, PokeApiServerException
└── HealthController, PokedexApplication
```

**Two explicit data modes:**

- **Live/Proxy mode** — `GET /pokemon` and `GET /pokemon/{id}` read through a cached PokeAPI client. No local persistence involved.
- **Sync mode** — `POST /pokemon/{id}/sync` fetches from PokeAPI (or cache) and upserts a local snapshot. `PATCH /pokemon/local/{id}` only ever touches synced local records.

A separate **`genai-task-api/`** module provides a standalone Task CRUD API (port 8081) as the GenAI Tools deliverable.

## Design decisions

| Topic | Decision | Rationale |
|-------|----------|-----------|
| Relational store | **PostgreSQL** (Docker Compose); **H2** for fast unit/slice tests | Realistic for prod, fast for local dev |
| Live vs local | Two explicit modes (see above) | Clean separation between read-through cache and system-of-record |
| Auth | **JWT** (stateless, Spring Security) | Standard, easy to demo, no session store |
| Frontend | **React + TypeScript + Vite**, TanStack Query, Tailwind | Fast setup, strong typing, widely understood |
| Caching | **Spring Cache + Caffeine** (in-memory, 10 min TTL) | Satisfies caching requirement cheaply; Redis noted as scale-up path |
| Local edits | **PATCH** (not PUT) | Partial updates to locally editable fields only |
| Demo seed | **Repeatable Flyway script** in `db/seed/`, gated by `demo` profile | Keeps test suite clean; `docker compose up` is pre-populated |
| Mass units | **Hectograms** (PokeAPI `weight` as-is) | Local records always agree with upstream |

### PokeAPI client specifics

- **Listing costs `1 + 2n` upstream requests.** PokeAPI's list resource returns only names/urls; each card is enriched with `pokemon` + `pokemon-species` calls. Caffeine cache sits in front of the client.
- **Resilience policies sit on `PokeApiHttpClient`.** Spring applies them through a proxy; per-request granularity means a failure retries one request instead of replaying a whole composition.
- **Circuit breaker wraps retry.** `circuitBreakerAspectOrder` is pinned ahead of `retryAspectOrder` in `application.yml` so the breaker records one failure per logical call, not per retry attempt.
- **Ids are re-resolved against the configured base URL.** Cross-references in PokeAPI payloads are absolute urls; the client extracts the trailing id rather than following upstream-chosen hosts.

## Testing policy

PokeAPI is **never** called directly in automated tests — the client is exercised against WireMock stubs with captured real JSON in `src/test/resources/pokeapi/`.

Test pyramid target:

| Layer | ~Share | Examples |
|-------|--------|----------|
| Unit | 70% | Entity validation, services with mocked dependencies |
| Integration/slice | 20% | `@WebMvcTest`, WireMock client tests, Testcontainers Postgres |
| End-to-end | 10% | `@SpringBootTest` smoke tests |

Testcontainers tests use `@Testcontainers(disabledWithoutDocker = true)` so CI without Docker skips gracefully.

## Demo walkthrough (10–15 min)

1. **`docker compose up --build`** — show the full stack starting with seeded data.
2. **Browse** — open http://localhost:5173, paginate the Pokemon list (live PokeAPI, cached).
3. **Detail** — click a Pokemon; show stats, description, evolution chain.
4. **Auth** — log in as `demo@example.com` / `demo1234`.
5. **Local Pokemon (API)** — call `PATCH /pokemon/local/1` via Swagger to edit Bulby's tags; show the seeded row in the DB.
6. **Sync + edit (UI)** — sync a Pokemon from the detail page; it appears under **My local Pokemon**; patch a local name or tag.
7. **Swagger** — http://localhost:8080/swagger-ui.html for interactive API exploration.

## Known limitations

- **In-memory cache only** — Caffeine is per-process; restarting the API clears it. Redis would be the production scale-up.
- **No rate-limit handling beyond retry/circuit-breaker** — heavy listing pages can still hit PokeAPI hard on cold cache.
- **`GET /auth/me` returns partial profile** — `id` and `createdAt` are null (JWT carries email + role only).
- **No role-based authorization** — `ADMIN` vs `USER` is stored but all protected routes require only authentication.
- **Frontend local Pokemon list** — reads from browser localStorage keyed by synced records; not a server-side "list all local" endpoint.
- **Demo seed is profile-gated** — running without `demo` profile gives an empty local database.

## GenAI Task API

Standalone Task CRUD module in [`genai-task-api/`](genai-task-api/) (port **8081**):

```bash
cd genai-task-api
mvn spring-boot:run
```

See [`genai-task-api/README.md`](genai-task-api/README.md) for endpoints and demo credentials.
