# TradeBall

<p align="center">
  <strong>Full-stack NBA fantasy basketball trade analysis powered by live player data and transparent 9-category valuation.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/JavaScript-ES6+-F7DF1E?logo=javascript&logoColor=black" alt="JavaScript" />
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white" alt="Redis" />
  <img src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/GitHub%20Actions-CI-2088FF?logo=githubactions&logoColor=white" alt="GitHub Actions" />
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="MIT License" />
</p>

---

## Demo

<p align="center">
  <img src="images/demo.gif" alt="TradeRank demo" width="900" />
</p>

> **TradeRank** is the user-facing application name; **TradeBall** is the repository and backend project name.

---

## Why I Built TradeBall

Last year, I joined a fantasy basketball league with friends and quickly became known for one thing: **making terrible trades**.

I was consistently near the bottom of the standings, and whenever a trade opportunity came up, I was basically guessing whether I was actually improving my team or making things worse.

I wanted something that could help me make smarter decisions instead of relying on gut feeling. So I built **TradeBall**.

TradeBall is a fantasy basketball trade analyzer that evaluates potential trades using the most up-to-date player statistics available. It compares players across the standard nine fantasy basketball categories and provides an overall evaluation of whether a trade improves my team.

What started as a personal tool to **help me survive my fantasy league** evolved into a full-stack application with user authentication, roster management, live NBA data, persistent storage, caching, and a production backend.

The goal is simple:

> **Stop making terrible trades. Start making trades that actually help me win.**

---

## Overview

TradeBall is a full-stack NBA fantasy basketball platform designed to help fantasy managers make more informed trade decisions.

Users can authenticate, manage their roster, search NBA players, build potential trades, and receive an explainable evaluation based on player performance across the standard nine-category fantasy basketball format.

The frontend provides the user experience, while the Spring Boot backend serves as the authoritative source for authentication, roster ownership, player data, fantasy valuation, trade scoring, and trade history.

---

## Features

### Fantasy & Trade Analysis

- Live NBA player and statistical data
- Player search and player detail views
- Fantasy value calculations
- 9-category trade evaluation
- Category-by-category gains and losses
- Overall trade fairness / value score
- Human-readable trade recommendations
- Trade history
- Roster-aware trade evaluation

### Accounts & Rosters

- User registration and login
- JWT-based authentication
- BCrypt password hashing
- Authenticated roster management
- Server-side ownership and authorization checks

### Backend & Infrastructure

- RESTful Spring Boot API
- PostgreSQL persistence
- Flyway database migrations
- Redis caching with Caffeine fallback
- NBA data synchronization endpoints
- OpenAPI / Swagger documentation
- Spring Boot Actuator health endpoints
- Docker-based development and deployment
- Automated testing with JUnit, Mockito, H2, and Testcontainers
- GitHub Actions CI

### Client

- Responsive HTML/CSS/JavaScript frontend
- API-driven UI with no duplicated trade-scoring logic
- Capacitor Android project for mobile packaging
- Production frontend/backend separation

---

## How TradeBall Evaluates a Trade

TradeBall uses a **Z-score-based fantasy valuation model** across nine standard categories:

| Category | Type |
|---|---|
| Points | Higher is better |
| Rebounds | Higher is better |
| Assists | Higher is better |
| Steals | Higher is better |
| Blocks | Higher is better |
| Three-Pointers | Higher is better |
| Field Goal % | Higher is better |
| Free Throw % | Higher is better |
| Turnovers | Lower is better |

The evaluation pipeline is handled by the backend:

```text
NBA Player Data
      ↓
Player Statistics
      ↓
9-Category Fantasy Valuation
      ↓
Incoming vs. Outgoing Players
      ↓
Category-Level Comparison
      ↓
Overall Trade Score
      ↓
Recommendation + Explanation
```

The frontend **does not independently calculate authoritative trade scores**. It sends the selected players to the backend and renders the returned analysis, keeping business logic centralized and consistent.

---

## Full-Stack Architecture

```text
                         ┌─────────────────────┐
                         │     TradeRank UI    │
                         │ HTML / CSS / JS     │
                         │     + Capacitor     │
                         └──────────┬──────────┘
                                    │
                              REST / JSON
                                    │
                                    ▼
                    ┌────────────────────────────┐
                    │     Spring Boot Backend     │
                    │       Java 21 / REST API    │
                    ├────────────────────────────┤
                    │ Auth / JWT / Security       │
                    │ Players / Stats             │
                    │ Rosters / Ownership         │
                    │ Fantasy Valuation           │
                    │ Trade Evaluation            │
                    │ Trade History               │
                    │ NBA Synchronization         │
                    └───────┬─────────┬──────────┘
                            │         │
                   ┌────────▼───┐ ┌──▼─────────┐
                   │ PostgreSQL  │ │   Redis    │
                   │ + Flyway    │ │   Cache    │
                   └─────────────┘ └────────────┘
                            │
                            │
                    ┌───────▼────────┐
                    │    NBA Data    │
                    │    Provider    │
                    └────────────────┘
```

### Backend

The backend is a production-oriented Spring Boot modular monolith. It exposes versioned REST endpoints under `/api/v1` and owns the application's core business logic.

Key API areas include:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me

GET  /api/v1/players
GET  /api/v1/players/search?q=
GET  /api/v1/players/{id}
GET  /api/v1/players/{id}/stats
GET  /api/v1/players/{id}/fantasy-value

CRUD  /api/v1/rosters...
POST  /api/v1/trades/evaluate
GET   /api/v1/trades

POST  /api/v1/admin/sync/players
POST  /api/v1/admin/sync/stats
GET   /api/v1/admin/sync/status
```

### Database

PostgreSQL stores persistent application state including users, rosters, players, statistics, fantasy values, and trade history.

Flyway manages versioned schema migrations while Hibernate validates the production schema rather than generating it automatically.

### Caching

Redis is used for server-side caching of frequently accessed data.

The backend can fall back to Caffeine for environments where Redis is unavailable, allowing local development without requiring the production cache infrastructure.

### NBA Data

NBA data access is isolated behind the backend's NBA client layer.

Production uses the configured live HTTP provider, while development supports a curated fallback snapshot for deterministic local development and testing.

---

## Authentication Flow

```text
User
 │
 ├── Register / Login
 │
 ▼
Spring Security
 │
 ├── BCrypt password verification
 │
 └── JWT access token
          │
          ▼
Frontend stores token
          │
          ▼
Authenticated API requests
          │
          ▼
Backend validates JWT + resource ownership
```

Protected operations such as roster management and trade evaluation require a valid JWT.

Authorization is enforced on the server rather than relying on frontend checks.

---

## Tech Stack

| Layer | Technologies |
|---|---|
| Frontend | HTML5, CSS3, JavaScript |
| Mobile | Capacitor, Android |
| Backend | Java 21, Spring Boot 3.3 |
| API | Spring Web, REST, JSON |
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL |
| Migrations | Flyway |
| Caching | Redis, Caffeine |
| Documentation | OpenAPI / Swagger |
| Testing | JUnit 5, Mockito, H2, Testcontainers |
| Infrastructure | Docker, Docker Compose |
| CI | GitHub Actions |
| Deployment | Render + managed PostgreSQL / Redis |

---

## Project Structure

```text
TradeBall/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── README.md
│
├── www/
│   ├── index.html
│   └── js/
│       └── tradeball-api.js
│
├── android/
├── images/
├── .github/
├── capacitor.config.json
├── package.json
└── README.md
```

---

## Running Locally

### Backend with Docker

```bash
cd backend
docker compose up --build
```

The API will be available at:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Health check:

```text
http://localhost:8080/actuator/health
```

### Backend without the application container

Start the infrastructure:

```bash
cd backend
docker compose up -d postgres redis
```

Configure the required environment variables, then run:

```bash
mvn spring-boot:run
```

### Frontend

The frontend API base URL is configured through `www/js/tradeball-api.js` and defaults to the deployed TradeBall backend in production.

The frontend is intentionally API-driven: player data, roster state, authentication, fantasy valuation, and trade analysis come from the backend.

---

## Testing

The backend includes unit and API-level tests covering core fantasy and application behavior.

```bash
cd backend
mvn clean test
```

Additional verification:

```bash
mvn verify
mvn -DskipTests package
```

Tests cover areas including:

- Fantasy Z-score calculations
- Trade scoring
- Buy-low / sell-high logic
- Roster authorization
- Authentication
- Player endpoints
- Roster endpoints
- Trade endpoints
- PostgreSQL-backed integration scenarios

---

## Production Deployment

TradeBall is structured so the frontend and backend can be deployed independently.

```text
Frontend
   │
   ▼
Cloud-hosted web client
   │
   │ HTTPS / REST
   ▼
Render — Spring Boot API
   │
   ├── Supabase PostgreSQL
   │
   └── Redis
```

Production configuration is supplied through environment variables rather than committed secrets.

The production backend uses a unique JWT secret, an explicit CORS allow-list, managed PostgreSQL, managed Redis, and live NBA data configuration.

The production NBA dataset is synchronized through protected admin endpoints rather than automatically performing a long-running synchronization during application startup.

---

## Design Principles

### Backend as the Source of Truth

All security-sensitive and business-critical logic lives on the server.

The frontend is a client of the API rather than a second implementation of the trade engine.

### Explainable Scoring

Trade recommendations are backed by category-level statistics instead of an opaque single-number model.

Managers can see which categories drive the evaluation.

### Production-Oriented Infrastructure

The project uses persistent PostgreSQL storage, migrations, caching, JWT security, containerization, health checks, automated tests, and CI rather than relying on local-only state.

### Development / Production Separation

Development fallback data and seeded development configuration are isolated from production behavior.

Production requires explicit configuration for databases, authentication secrets, CORS, caching, and live NBA data.

---

## Future Improvements

- More configurable league scoring settings
- Additional player valuation models
- More advanced trade recommendations
- Expanded historical analytics
- Additional fantasy league integrations
- Deeper mobile-specific functionality

---

## License

This project is licensed under the MIT License.