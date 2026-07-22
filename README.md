# Team Project #243 — Travel Planner

A trip-planning web app (similar to [Wanderlog](https://wanderlog.com/)).
Users create trips, plan them day by day, add places, and see them on a map.

## Stack
- **Backend:** Spring Boot 4.1.0 (Java 17, Maven) — REST API, packaged as a JAR
- **Database:** MySQL + Liquibase (migrations)
- **Frontend:** React + Vite — SPA with React Router (Node 24)
- **Auth:** token-based
- **Free external APIs:**
  - OpenStreetMap tiles — map (Leaflet, frontend)
  - Nominatim — geocoding (place name → coordinates, called by the backend; coordinates cached in the DB)
  - OpenRouteService / OSRM — routing between places

## Hosting
- Frontend → **Vercel**
- Backend → **Render**
- Database → **Aiven (MySQL, free tier)**

## Repository layout (monorepo)
```
/backend    — Spring Boot API
/frontend   — React + Vite SPA
```

## Local development
1. Copy `.env.example` → `.env` and fill in the values.
2. `docker compose up` — starts MySQL (the `backend`/`frontend` services stay commented out until their code exists).

## Branches
- `main` — production (Render/Vercel auto-deploy)
- `dev` — staging

## Environment variables
See `.env.example`.

---
Data model: `User` 1→N `Trip` 1→N `Activity` (`place, date, time, notes, lat, lon`).
