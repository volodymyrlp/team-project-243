# Team Project #243 — Travel Planner

A trip-planning web app (similar to [Wanderlog](https://wanderlog.com/)).
Users create trips, plan them day by day, add places, and see them on a map.

## Stack
- **Backend:** Spring Boot 3.4.5 (Java 17, Maven) — REST API, packaged as a JAR
- **Database:** MySQL + Liquibase (migrations)
- **Frontend:** React + Vite — SPA with React Router (Node 24)
- **Auth:** token-based
- **Free external APIs:**
  - OpenStreetMap tiles — map (Leaflet, frontend)
  - Nominatim — geocoding (place name → coordinates, called by the backend; coordinates cached in the DB)
  - OpenRouteService / OSRM — routing between places

## Hosting
- Frontend → **Vercel** — not deployed yet
- Backend → **Render** — live at <https://travel-planner-backend-4tb0.onrender.com>
- Database → **Aiven (MySQL, free tier)**

The backend is the API only; it has no UI, and every path except `/actuator/health` currently
answers `401`. The free instance sleeps when idle, so the first request can take 50 s or more.
Frontend work should point `VITE_API_URL` at the URL above. Deployment details live in
[docs/DEPLOY.md](docs/DEPLOY.md).

## Repository layout (monorepo)
```
/backend    — Spring Boot API
/frontend   — React + Vite SPA
```

## Local development
1. Copy `.env.example` → `.env` and fill in the values. Optional for a first run —
   compose falls back to local defaults for everything except `ORS_API_KEY`.
2. `docker compose up --build` — starts MySQL and the backend on <http://localhost:8080>.
   The first build takes a few minutes (Maven downloads the dependencies).
3. Check it is alive: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.

The `frontend` service stays commented out until its code exists.

## Branches
- `main` — production (Render/Vercel auto-deploy)
- `dev` — staging

## Environment variables
See `.env.example`.

---
Data model: `User` 1→N `Trip` 1→N `Activity` (`place, date, time, notes, lat, lon`).
