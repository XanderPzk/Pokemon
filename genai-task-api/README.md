# GenAI Task API

Standalone Task CRUD REST API on port **8081**, secured with JWT authentication. Tasks are scoped to the authenticated user.

## Demo credentials

| Email | Password |
|-------|----------|
| `demo@example.com` | `demo1234` |

On startup, the demo user and two sample tasks are seeded automatically.

## Quick start

```bash
cd genai-task-api
mvn spring-boot:run
```

The API listens on `http://localhost:8081`.

### Docker

```bash
docker build -t genai-task-api .
docker run -p 8081:8081 genai-task-api
```

Health check: `GET /actuator/health`

## Authentication

Register or log in to obtain a JWT, then send it on protected requests:

```
Authorization: Bearer <token>
```

### Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/auth/register` | No | Create account |
| `POST` | `/auth/login` | No | Obtain JWT |
| `GET` | `/auth/me` | Yes | Current user profile |

**Register / login body**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Login response**

```json
{
  "token": "<jwt>"
}
```

## Tasks

All task endpoints require authentication. Users only see and modify their own tasks.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/tasks?page=0&size=20` | List tasks (paginated) |
| `POST` | `/tasks` | Create task |
| `GET` | `/tasks/{id}` | Get task by id |
| `PUT` | `/tasks/{id}` | Update task |
| `DELETE` | `/tasks/{id}` | Delete task |

**Create task body**

```json
{
  "title": "Write docs",
  "description": "Document the API",
  "status": "TODO",
  "dueDate": "2026-12-31"
}
```

`status` is one of `TODO`, `IN_PROGRESS`, or `DONE`. `dueDate` must be today or in the future.

**Task response**

```json
{
  "id": 1,
  "title": "Write docs",
  "description": "Document the API",
  "status": "TODO",
  "dueDate": "2026-12-31",
  "ownerEmail": "demo@example.com",
  "createdAt": "2026-08-14T12:00:00Z",
  "updatedAt": "2026-08-14T12:00:00Z",
  "completedAt": null
}
```

When a task status changes to `DONE`, `completedAt` is set automatically.

Accessing another user's task returns **404 Not Found**.

## Error envelope

Errors use a consistent JSON shape:

```json
{
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed",
  "timestamp": "2026-08-14T12:00:00Z",
  "fieldErrors": [
    { "field": "title", "message": "must not be blank" }
  ]
}
```

## Tests

```bash
mvn test          # unit and slice tests
mvn verify        # tests + Spotless + JaCoCo report
```

Test coverage includes:

- `TaskApiEndToEndTest` — register, login, CRUD, cross-user isolation
- `AuthControllerTest`, `JwtServiceTest` — authentication
- `TaskControllerTest`, `TaskServiceTest` — task operations

## Architecture

This module uses conventional layered Spring (controller/service/repository), matching the main app's structure. Kept as a separate Maven module for the GenAI Tools deliverable.
