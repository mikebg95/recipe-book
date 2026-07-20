# Recipe Book — Backend

A small REST API for managing recipes. A recipe has a name, a description, an **ordered** list of preparation steps, and a list of ingredients (name, unit, quantity). You can list recipes, view one in full, create, update, and delete them.

> Project 2 of a self-directed engineering roadmap. Its purpose is to practise **JPA / Hibernate relationship modelling** and a **design-first OpenAPI** workflow — so a few choices here are deliberate learning constraints rather than what I'd reach for in production (see [Design notes](#design-notes)).

![Java](https://img.shields.io/badge/Java-26-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-336791)
![Build](https://img.shields.io/badge/build-Maven-C71A36)

---

## Features

- **List recipes** — returns a lightweight summary of each recipe (name, description, and how many steps and ingredients it has), backed by a projection query rather than loading full aggregates.
- **View a recipe** — full detail, with steps returned in order.
- **Create / update** — a recipe is created or replaced as a whole, including its steps and ingredients. Steps are renumbered automatically.
- **Delete** — removes the recipe and, with it, its steps and ingredients.

Steps and ingredients are **owned by their recipe** — there is no shared ingredient catalogue. This is a single aggregate, not three independent resources.

---

## Tech stack

| Concern | Choice |
|---|---|
| Language | Java 26 |
| Framework | Spring Boot 4.1 / Spring Framework 7 |
| Persistence | JPA / Hibernate via a hand-written `EntityManager` repository — **no Spring Data** (deliberate, see below) |
| Database | PostgreSQL 18 |
| Migrations | Flyway |
| API | Design-first **OpenAPI 3.1** — request/response models are generated from the spec by `openapi-generator`; entity ↔ DTO mappers are hand-written |
| Docs | springdoc + Swagger UI (served in the `dev` profile) |
| Testing | JUnit 6 · Mockito · AssertJ · Testcontainers (real PostgreSQL, never H2) · JaCoCo |
| Architecture guard | ArchUnit |

---

## Design notes

What this project deliberately demonstrates:

- **Aggregate root.** `Recipe` is the only entity with a repository. `Step` and `Ingredient` have no independent lifecycle — they are reached, saved, and deleted only through their recipe. `cascade = ALL` + `orphanRemoval = true` mirror that ownership in JPA, and the schema enforces it too (`ON DELETE CASCADE`, `NOT NULL` foreign keys).
- **Design-first API.** `docs/api/openapi.yaml` is the source of truth. The build generates the DTOs and API interfaces from it, so the contract can't silently drift from the code.
- **Hand-written repository (a learning constraint).** The repository is implemented directly against `EntityManager` on purpose — the point of this project is to see what an ORM does by hand before letting Spring Data generate it in a later project. It is *not* that Spring Data was unavailable.
- **JPA correctness details:** `SEQUENCE` id generation (`allocationSize = 50`, matching the DB `INCREMENT BY`), `@Version` optimistic locking, id-based `equals` with a constant `hashCode`, `open-in-view = false`, and a dedicated projection (`RecipeSummary`) for the list screen so it never over-fetches.
- **Data integrity in the database, not only the app:** `CHECK (quantity > 0)`, case-insensitive uniqueness on recipe and ingredient names, and *deferrable* unique constraints on step ordering so a whole recipe can be re-saved in one transaction.
- **RFC 9457 error handling.** A single `@RestControllerAdvice` returns `application/problem+json` for validation (400), not-found (404), conflicts (409), optimistic-lock failures (409), with a catch-all backstop that never leaks internals.
- **Layered architecture, enforced.** ArchUnit fails the build if the controller touches the persistence layer or an entity, if `@Transactional` escapes the service layer, on field injection, JPA leaking outside the persistence packages, or package cycles.

---

## API

All endpoints are served under `/api/v1`.

| Method | Path | Description | Success |
|---|---|---|---|
| `GET` | `/recipes` | List all recipes as summaries (name, description, step/ingredient counts) | `200` |
| `POST` | `/recipes` | Create a recipe | `201` + `Location` |
| `GET` | `/recipes/{id}` | Get one recipe in full | `200` |
| `PUT` | `/recipes/{id}` | Replace a recipe | `200` |
| `DELETE` | `/recipes/{id}` | Delete a recipe | `204` |

Errors are returned as `application/problem+json` (RFC 9457): `400` for invalid input, `404` when a recipe doesn't exist, `409` on a duplicate name or a concurrent modification.

---

## Getting started

### Prerequisites

- JDK 26
- Docker (for the test suite's Testcontainers, and for a local database if you don't have one)

### Run it

The `dev` profile expects a PostgreSQL instance on `localhost:5432/recipe-book`. The quickest way to get one:

```bash
docker run --name recipe-db -e POSTGRES_DB=recipe-book \
  -e POSTGRES_USER=recipe -e POSTGRES_PASSWORD=recipe \
  -p 5432:5432 -d postgres:18
```

Then start the app (Flyway applies the migrations on startup):

```bash
export DB_USERNAME=recipe DB_PASSWORD=recipe
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The API is served under `http://localhost:8080/api/v1`. In the `dev` profile, Swagger UI is available at `http://localhost:8080/swagger-ui.html` and the committed spec at `http://localhost:8080/openapi.yaml`.

### Profiles

| Profile | Purpose |
|---|---|
| `dev` | Local development — verbose logging, Swagger UI on, permissive CORS. |
| `test` | Test runs. |
| `prod` | All config via environment variables, API docs off, Flyway `clean` disabled. |

---

## Testing

The full backend test pyramid, hand-written and exhaustive (~100 test methods):

| Layer | Tooling |
|---|---|
| Unit | JUnit 6 + Mockito — service, mapper, and config logic |
| Web slice | `@WebMvcTest` — the controller in isolation |
| Persistence slice | `@DataJpaTest` — mappings, relationships, cascade/orphan behaviour |
| Integration | `@SpringBootTest` + Testcontainers — the real app against real PostgreSQL |

```bash
./mvnw test      # unit + slice tests
./mvnw verify    # everything, including Testcontainers integration tests (Docker required)
```

`verify` also produces a merged JaCoCo report at `target/site/jacoco/index.html`.

> Testing here is intentionally exhaustive — this project is early enough in the roadmap that over-practice is the goal. Real projects (and later projects here) test more leanly.

---

## Project structure

```
src/main/java/dev/michaelgoldman/recipebookbackend/
├── controller/     # REST endpoints (DTO in, DTO out)
├── service/        # business logic, transaction boundaries
├── repository/     # hand-written EntityManager implementation + projection
├── entity/         # JPA entities (Recipe aggregate)
├── mapper/         # hand-written entity ↔ DTO mapping
├── exception/      # domain exceptions + RFC 9457 handler
├── validation/     # custom Bean Validation constraints
└── config/         # web, CORS, Jackson

src/main/resources/db/migration/    # Flyway migrations
docs/api/openapi.yaml                # API contract (source of truth)
```

The `api` / `api.model` packages are generated from the OpenAPI spec at build time and are not committed source.

---

## Scope

This repository is the **backend only**. Authentication, containerisation, CI/CD, and deployment are out of scope here by design — they belong to later projects in the roadmap. Architecture and database diagrams (C4 via Structurizr, schema via DBML) live in the sibling [`docs/`](../docs) directory.