# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

MriyaTrip — a trip-planning web app. Monorepo: Spring Boot API, React SPA, Terraform for the
database, GitHub Actions for everything operational.

## Commands

### Backend (from `backend/`)
```bash
mvn -B verify                                    # compile, test, Checkstyle — what CI runs
mvn test -Dtest=ApplicationTests                 # one test class
mvn test -Dtest=ApplicationTests#contextLoads    # one test method
```

### Frontend (from `frontend/`)
```bash
npm ci          # install exactly what the lockfile pins
npm run dev     # Vite on http://localhost:5173
npm run build   # tsc -b && vite build  (type checking is part of the build)
npm run lint    # ESLint — a required check
```

### Local stack
```bash
cp .env.example .env
docker compose up --build                      # MySQL 8.4 + backend on :8080
curl http://localhost:8080/actuator/health     # {"status":"UP"}
```

Frontend against the deployed staging backend, which is the only way to exercise a real API from a
local browser:
```bash
cd frontend && VITE_API_URL=https://travel-planner-backend-staging.onrender.com npm run dev
```

### Checking a deployed environment
```bash
curl -sS -D - --max-time 400 https://travel-planner-backend-4tb0.onrender.com/actuator/health
```
One request at a time and a generous timeout. Several concurrent probes make a waking free-tier
instance look dead.

## Conventions

**Branches are environments.** `dev` deploys to staging, `main` deploys to production, and merging
*is* the deploy — there is no separate button. Feature branches go to `dev` first; `dev` reaches
`main` only through a release pull request opened by hand.

**Releasing.** Merge a release with a merge commit, never a squash — squashing a release rewrote the
commits and left the branches permanently diverged. Before merging, take a probe on production that
distinguishes the old code from the new, and repeat it after. `/actuator/health` cannot do this:
Render keeps the previous container serving when a deploy fails, so health stays 200 through a
failed release. Also confirm every `sync: false` variable the new code reads is already set on both
Render services — the app aborts at startup on an unresolved placeholder, and since Vercel deploys
independently, the frontend can move while the backend does not.

**Migrations are one-way.** Every changeset is raw formatted SQL, so `liquibase rollback` cannot
generate a reversal — this was rehearsed, it does not work — and the Aiven free tier takes no
backups of its own. The nightly `backup.yml` artifact is the only recovery path. Rehearse a
migration on staging before it reaches production, and treat anything destructive as irreversible.

**A slow response is not an outage.** A cold start takes 170–230 seconds and looks like silence:
TCP and TLS complete instantly while no HTTP bytes arrive. A real outage answers *fast*, with a
status code and an `x-render-routing` header (`no-deploy`, `hibernate-wake-error`). One
`curl -D -` tells them apart.

**Scheduled workflows run from the default branch only,** so a cron added on `dev` stays dead until
released. GitHub schedules are best-effort and get delayed or dropped under load without an incident
being reported — a missing monitoring run is not evidence of an outage, and a green one last night
is not evidence the service was up this morning.

**Do not edit another role's files to finish your own change.** The backend belongs to the backend
developers and the frontend to the frontend developer; hand over the recipe instead of committing
into their area.

**Commits and PRs.** Commit messages are plain sentences describing the effect, not `type(scope):`
prefixes — for example "Move the scheduled runs off the top of the hour". PR descriptions are short:
what changed and why. Merged branches are kept, never deleted.

## Architecture

```
backend/      Spring Boot 3.4.5, Java 17, Maven. Package root: travelplanner
frontend/     React 19 + Vite 8 + TypeScript + SCSS, SPA with React Router
infra/aiven   Terraform for the managed MySQL service (state on HCP Terraform)
infra/storage Script that creates the Supabase buckets
docs/         DEPLOY.md (runbook), STORAGE.md (uploads)
render.yaml   Render Blueprint describing both backend services
```

**Backend layering.** `controller` → `service` → `repository` (Spring Data JPA) over `entity`, with
`dto` records crossing the boundary and MapStruct mappers converting between them. `config` holds
Spring configuration, `security` the JWT filter and authentication service, `exception` the domain
exceptions plus a global handler that turns them into HTTP responses.

**Auth** is a stateless JWT. `JwtAuthenticationFilter` populates the security context from the
`Authorization` header before Spring Security's own filter. `SecurityConfig` permits
`/actuator/health`, `/api/v1/auth/**`, `/api/v1/landing/**`, `/api/v1/trips/catalog`,
`/swagger-ui/**` and `/error` anonymously; everything else needs a token, so `/api/v1/trips` answers
`401` without one. `JWT_SECRET` deliberately has **no fallback** in `application.yml` — a default in
a public repo would let anyone forge tokens, and a missing variable must fail the boot loudly rather
than start with a known key.

**External integrations.** `NominatimClientService` geocodes place names and the coordinates are
cached on the row, so the 1 request/second limit is rarely hit. `StorageService` presigns S3 URLs
against Supabase for both upload and download, so files move between the browser and storage
directly and a private bucket still renders.

**Deployment topology.** Backend on Render as Docker with `rootDir: backend` — a push to `main` that
touches nothing under `backend/` produces no deploy at all, so "Live" showing an older commit than
`main` is normal, not drift. Frontend on Vercel, root directory `frontend`, build command
`npm run build` rather than the Vite preset's bare `vite build`, so type errors cannot reach
production. Database is MySQL on Aiven: production is `travel`, staging is `travel_staging`.

The shape of a service (branch, region, root directory, health check path) lives in `render.yaml`
and is owned by git — editing it in the dashboard creates drift that a later Blueprint sync
reverts. Secrets are the mirror image: declared `sync: false` and typed into the dashboard, where a
sync will not overwrite them.

**CORS.** `SecurityConfig` reads `CORS_ALLOWED_ORIGINS` and calls `setAllowedOrigins` — exact string
matching, no patterns. Production allows exactly the one Vercel domain, and every Vercel preview
deployment gets its own hostname, so **login always fails from a preview** with `Failed to fetch`:
the preflight answers 403 with no CORS headers and the browser never surfaces the status to
JavaScript. Test on the production domain or locally against staging.

**CI.** `ci.yml` runs on `main` and `dev`. A `detect` job checks whether `backend/pom.xml` and
`frontend/package.json` exist and gates the build jobs on its output, so a job arms itself once the
code it tests appears. `migration-check` applies `main`'s changesets to an empty MySQL container and
then the branch's changesets on top, testing a migration against the history production already has.
Alongside it: `smoke.yml` polls the Render API until the deploy for that commit is live and only
then asserts against it, `env-check.yml` compares the placeholders in `application.yml` against the
variables actually set on both Render services, `backup.yml` dumps both databases nightly and
restores each into a throwaway container before keeping it, and `monitoring.yml` probes both
backends, the frontend and Supabase on a schedule.
