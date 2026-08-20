# TradeBall

<p align="center">
  <strong>A full-stack fantasy basketball trade analysis platform powered by live NBA data and transparent heuristic scoring.</strong>
</p>

<p align="center">

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

</p>

---

## Overview

TradeBall is a full-stack fantasy basketball application that helps managers evaluate trades using live NBA statistics, historical player performance, and a transparent heuristic scoring system.

Instead of returning only a single trade score, TradeBall explains **why** a trade is favorable or unfavorable by analyzing player value across the standard nine-category fantasy basketball format.

The application combines a responsive web frontend with a production-oriented Spring Boot backend responsible for authentication, roster persistence, player data, fantasy valuation, trade evaluation, trade history, caching, and NBA data synchronization.

---

## Features

- Live NBA player statistics
- NBA data synchronization
- Development fallback for NBA data
- Fantasy roster management
- Player search and comparison
- Trade evaluation
- Category-by-category trade analysis
- Fantasy player valuation
- Human-readable trade recommendations
- Trade history persistence
- JWT authentication
- BCrypt password hashing
- Server-side authorization
- PostgreSQL persistence
- Flyway database migrations
- Redis caching
- Local cache fallback
- Swagger / OpenAPI documentation
- Health monitoring through Spring Boot Actuator
- Automated CI testing with GitHub Actions

---

## How TradeBall Works

TradeBall evaluates players using a custom **Z-score-inspired heuristic model** across nine standard fantasy basketball categories:

- Points
- Rebounds
- Assists
- Steals
- Blocks
- Three-Point Field Goals
- Field Goal Percentage
- Free Throw Percentage
- Turnovers

The evaluation engine:

1. Analyzes player statistics.
2. Calculates fantasy value across each category.
3. Compares the players involved in a trade.
4. Identifies category-level gains and losses.
5. Calculates an overall trade score.
6. Generates a contextual recommendation explaining the result.

The backend is authoritative for fantasy valuation and trade scoring. The frontend renders the API results rather than independently recreating the scoring logic.

---

# Architecture

```text
                         TradeBall
                            │
             ┌──────────────┴──────────────┐
             │                             │
       Web Frontend                  Spring Boot API
          (www/)                         (backend/)
             │                             │
             │                    ┌────────┼────────┐
             │                    │        │        │
             │               PostgreSQL  Redis   NBA API
             │                    │        │
             │                  Flyway   Cache
             │
             └──────── REST API ───────────┘