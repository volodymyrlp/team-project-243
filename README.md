# MriyaTrip — Team Project #243

A trip-planning web app. Users create trips, plan them day by day, add places,
and see them on a map.

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
- Frontend → **Vercel** — live at <https://team-project-243.vercel.app>
- Backend → **Render** — live at <https://travel-planner-backend-4tb0.onrender.com>
- Database → **Aiven (MySQL, free tier)** — schema applied by Liquibase

The backend is the API only; it has no UI. `SecurityConfig` leaves `/actuator/health`,
`/api/v1/auth/**`, `/swagger-ui/**` and `/error` open and requires a JWT for everything else, so
`/api/v1/trips` answers `401` without a token. CORS is read from `CORS_ALLOWED_ORIGINS`, which in
production is the Vercel domain, so the deployed frontend can call the API.

The free instances sleep when idle, so the first request can take 50 s or more. A scheduled
`Monitoring` workflow wakes the backend, staging and the frontend every 6 hours and records how
long each took, which also keeps the database from powering itself off. Deployment details, and
the two free-tier traps that already broke a release, live in [docs/DEPLOY.md](docs/DEPLOY.md).

## Repository layout (monorepo)
```
/backend    — Spring Boot API
/frontend   — React + Vite SPA
```

An interactive map of the architecture — the modules, the services they talk to and how the
pieces connect — is published at <https://foglamp.dev/scan/mriyatrip-lplqrt>. It is an unlisted
link and it expires on 27 November 2026.

## Local development
1. Copy `.env.example` → `.env` and fill in the values. Optional for a first run —
   compose falls back to local defaults for everything except `ORS_API_KEY`.
2. `docker compose up --build` — starts MySQL and the backend on <http://localhost:8080>.
   The first build takes a few minutes (Maven downloads the dependencies).
3. Check it is alive: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.
4. Frontend, in a second terminal: `cd frontend && npm ci && npm run dev` —
   Vite serves it on <http://localhost:5173>.

The `frontend` service in `docker-compose.yml` stays commented out: there is no
`frontend/Dockerfile` yet, and the Vite dev server with hot reload is the better local
experience anyway.

## Branches
- `main` — production (Render/Vercel auto-deploy)
- `dev` — staging

Work lands in `dev` first. Getting it from `dev` into `main` is a **manual** pull request —
nothing promotes itself. Deploys, on the other hand, are automatic: `render.yaml` pins each
service to a branch, so a push to `dev` redeploys staging and a push to `main` redeploys
production.

So a change that is merged but not yet released lives only in `dev`. GitHub opens the
repository on `main`, which is why it can look missing. Compare the two branches to see what
is waiting for a release: <https://github.com/volodymyrlp/team-project-243/compare/main...dev>

## Environment variables
See `.env.example`.

---
Data model: `User` 1→N `Trip` 1→N `Activity` (`place, date, time, notes, lat, lon`).
