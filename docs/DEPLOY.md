# Deployment runbook — Team Project #243

Production hosting: **frontend → Vercel**, **backend → Render**, **database → Aiven (MySQL)**.
All three are free tier. This document is the step-by-step guide the DevOps side follows
once the backend and frontend skeletons exist in the repo.

## Prerequisites

Deployment cannot start until:

1. `backend/` contains a buildable Spring Boot app (`pom.xml` + `src/`), with:
   - `spring-boot-starter-actuator` on the classpath (Render health check hits `/actuator/health`);
   - a `management.endpoints.web.exposure.include=health` and `management.endpoint.health.probes.enabled=true`
     so `/actuator/health` answers without auth.
2. `frontend/` contains a buildable Vite app (`package.json` with a `build` script producing `dist/`).
3. An OpenRouteService API key is registered (see "Secrets" below).

Until then only the database (Aiven) is live.

## Architecture

```
Browser ──> Vercel (React SPA, static)
   │
   └── VITE_API_URL ──> Render (Spring Boot, Docker) ──> Aiven MySQL (do-fra)
                             │
                             └── Nominatim / OpenRouteService (outbound HTTPS)
```

## Backend on Render (Blueprint)

The repo ships a `render.yaml` Blueprint at the root, so Render reads the whole service
definition from git (Infrastructure as Code) instead of clicking through the dashboard.

1. Render dashboard → **New → Blueprint** → connect the GitHub repo `volodymyrlp/team-project-243`.
2. Render detects `render.yaml` and proposes the `travel-planner-backend` web service.
3. Fill in the env vars marked `sync: false` (they are secrets, never in git):

   | Env var | Value | Where it comes from |
   | --- | --- | --- |
   | `SPRING_DATASOURCE_URL` | `jdbc:mysql://travel-mysql-mr-b549.d.aivencloud.com:18032/travel?sslMode=REQUIRED` | Aiven service (see `infra/aiven`) |
   | `SPRING_DATASOURCE_USERNAME` | `avnadmin` | Aiven service |
   | `SPRING_DATASOURCE_PASSWORD` | *(secret)* | `cd infra/aiven && terraform output -raw mysql_password` |
   | `NOMINATIM_USER_AGENT` | `team243-travel-planner (contact: <real email>)` | Nominatim policy requires a contact |
   | `ORS_API_KEY` | *(secret)* | OpenRouteService account |
   | `CORS_ALLOWED_ORIGINS` | the Vercel URL, e.g. `https://team-project-243.vercel.app` | filled after the frontend is deployed |

4. Apply → Render builds `backend/Dockerfile`, deploys, and starts health checks against
   `/actuator/health`. First successful deploy prints the public URL
   (`https://travel-planner-backend.onrender.com`).

### Port

The container listens on `$PORT` (Render injects it). The Dockerfile entrypoint passes
`--server.port=${PORT:-8080}`, so nothing extra is needed in the Spring config.

### Cold start

Render's free web service sleeps after ~15 minutes of no traffic and takes ~30–60 s to wake.
Options: accept it for the demo, or add an external uptime pinger hitting `/actuator/health`.

## Frontend on Vercel

1. Vercel → **Add New → Project** → import `volodymyrlp/team-project-243`.
2. **Root Directory:** `frontend`.
3. Framework preset: **Vite** (Build Command `npm run build`, Output Directory `dist`).
4. Environment variable: `VITE_API_URL` = the Render backend URL from the step above.
5. Deploy → Vercel prints the public URL. Put that URL into the backend's
   `CORS_ALLOWED_ORIGINS` on Render and redeploy the backend once.

## CORS

The Spring Boot backend must allow the Vercel origin. Read it from `CORS_ALLOWED_ORIGINS`
and apply it in a `WebMvcConfigurer` (or Spring Security CORS config):

- allowed origins: value of `CORS_ALLOWED_ORIGINS`
- allowed methods: `GET, POST, PUT, DELETE, OPTIONS`
- allow credentials: only if token auth needs it

## Secrets — where they live

- **DB password:** only in the local Terraform state (`infra/aiven/terraform.tfstate`, gitignored).
  Retrieve with `terraform output -raw mysql_password`. Set it directly in Render, never in git.
- **ORS API key:** register at <https://openrouteservice.org/dev/#/signup>, create a token,
  set it as `ORS_API_KEY` in Render. Never commit it.
- **Nominatim:** no key, but the `User-Agent` must carry a real contact email (usage policy).

## Auto-deploy

- `main` → production (Render + Vercel redeploy on push to `main`).
- `dev` → staging (optional second Render/Vercel environment later).

## Branches / environments summary

| Branch | Backend (Render) | Frontend (Vercel) | Database |
| --- | --- | --- | --- |
| `main` | production service | production deployment | Aiven `travel` |
| `dev` | staging (optional) | preview deployments | shared Aiven for now |
