# Deployment runbook — Team Project #243

Production hosting: **frontend → Vercel**, **backend → Render**, **database → Aiven (MySQL)**.
All three are free tier. This document is the step-by-step guide the DevOps side follows.

## Status

| Piece | State |
| --- | --- |
| Database — Aiven MySQL | live, schema applied by Liquibase |
| Backend — Render | live, see [Deployed service](#deployed-service) |
| Frontend — Vercel | live, see [Frontend on Vercel](#frontend-on-vercel) |

| | URL |
| --- | --- |
| Frontend | <https://team-project-243.vercel.app> |
| Backend | <https://travel-planner-backend-4tb0.onrender.com> |

Two things on the backend still block the frontend from talking to it:

- **`/api/**` answers `401`.** The app defines no `SecurityFilterChain` of its own, so Spring
  Security's default chain guards every path with generated-password basic auth.
- **CORS is not implemented.** `CORS_ALLOWED_ORIGINS` exists on Render, but no code reads it, so
  it is still the `http://localhost:5173` placeholder. Change it to
  `https://team-project-243.vercel.app` in the same deploy that adds a `CorsConfigurationSource`
  — changing it earlier only burns a rebuild on a variable nothing reads.

The backend must keep `spring-boot-starter-actuator` on the classpath, because Render's health
check hits `/actuator/health`.

**That endpoint answers without authentication only because Spring Boot's
`ManagementWebSecurityAutoConfiguration` applies while the app defines no `SecurityFilterChain`
bean of its own.** The moment one is added, it has to `permitAll` on `/actuator/health` —
otherwise the endpoint starts returning 401, Render marks the service unhealthy, and production
goes down. This is not hypothetical: `GET /` already returns 401, so the default security chain
is active.

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
   | `SPRING_DATASOURCE_USERNAME` | `avnadmin` | Aiven service |
   | `SPRING_DATASOURCE_PASSWORD` | *(secret)* | `cd infra/aiven && terraform output -raw mysql_password` |
   | `NOMINATIM_USER_AGENT` | `team243-travel-planner (contact: <real email>)` | Nominatim policy requires a contact |
   | `ORS_API_KEY` | *(secret)* | OpenRouteService account |
   | `CORS_ALLOWED_ORIGINS` | `https://team-project-243.vercel.app` | still the `http://localhost:5173` placeholder — see [Status](#status) |

   `SPRING_DATASOURCE_URL` is deliberately **not** in that list: it is a plain `value:` in
   `render.yaml`, different per service (`/travel` for production, `/travel_staging` for staging).
   The host, port and database name are not secrets — they are printed in this document — and
   keeping the URL in git means the difference between the two environments shows up in a PR
   diff instead of living in two browser tabs. On 2026-07-25 that value silently changed on the
   staging service and staging applied a migration straight to the production database; see
   [Troubleshooting](#troubleshooting).

4. Apply → Render builds `backend/Dockerfile`, deploys, and starts health checks against
   `/actuator/health`. The first build takes 5–8 minutes, most of it `dependency:go-offline`.
   Later builds reuse cached layers and land in 2–3 minutes.

### Deployed service

| | |
| --- | --- |
| URL | <https://travel-planner-backend-4tb0.onrender.com> |
| Service ID | `srv-d9htdbbeo5us73dl8o3g` |
| Blueprint ID | `exs-d9ht5nr7uimc73am85vg` |
| Branch | `main` (auto-deploys on push) |

The URL carries a `-4tb0` suffix because `*.onrender.com` subdomains are unique across all
of Render and `travel-planner-backend` was already taken. The suffix-less hostname belongs to
someone else — always use the URL above.

**Reading deploy status:** trust the coloured badge (`Live` / `Failed`), not the "Deployed on …"
line — that line is printed for failed deploys too. The commit shown next to the repo and branch
at the top of the service page is the one actually serving traffic.

### Port

The container listens on `$PORT` (Render injects it). The Dockerfile entrypoint passes
`--server.port=${PORT:-8080}`, so nothing extra is needed in the Spring config.

### Cold start

The free instance spins down when idle. The first request after that takes 50 s or more —
measured up to ~2.5 minutes when the request arrives before the instance is back. Warm,
`/actuator/health` answers in ~0.2 s. Anything calling the API has to tolerate this, and demos
should warm the service up beforehand. The keep-alive workflow below also keeps it warm.

## Keep-alive

`.github/workflows/keep-alive.yml` calls `/actuator/health` every 6 hours, and can be run by hand
from the Actions tab (`workflow_dispatch`).

It exists because **both** free tiers fall asleep, and only one of them wakes up on its own:

| Service | Sleeps after | Wakes by itself? |
| --- | --- | --- |
| Render web service | ~15 min idle | yes, on the first HTTP request |
| Aiven MySQL | ~24 h without connections | **no** — its DNS record is withdrawn, so nothing can reach it |

One HTTP request covers both, because the actuator health check queries the database on every
call — so the workflow needs no database credentials as secrets.

**The schedule only runs from the default branch (`main`).** A keep-alive sitting on `dev` does
nothing at all.

A failing run means the backend is down; the step prints the command to power the database back on.

## Smoke tests

`.github/workflows/smoke.yml` runs on every push to `main` and checks production from the
outside: backend health, that `/api/**` answers without a 5xx, that the frontend serves the app,
that a deep link reaches the SPA, and that hashed assets are still served as assets.

It first asks the Render API how the deploy **for this exact commit** ended, and fails if that
deploy did not go live. That check is the point of the workflow. Render keeps the previous
version serving when a deploy fails, so endpoint checks alone would stay green while the release
never reached production — which is exactly what happened on 2026-07-25, with every GitHub check
green and a failed deploy in Render.

This needs one repository secret:

| Secret | Where to get it |
| --- | --- |
| `RENDER_API_KEY` | Render dashboard → Account Settings → API Keys → Create API Key |

Without it the deploy check is skipped and only the endpoint checks run, so the workflow is
still useful but no longer catches a failed deploy. When a commit changes nothing under
`backend/`, Render creates no deploy at all and the check times out after 15 minutes and moves
on — that is expected, not a failure.

## Staging

`render.yaml` declares a second web service, `travel-planner-backend-staging`, that tracks the
`dev` branch. Everything else about it matches production: same Dockerfile, same region, free
plan, same health check path.

It has its own database on the **same** Aiven service — `travel_staging`, declared in
`infra/aiven/main.tf`. The free plan allows only one MySQL service, so staging shares the server
but not the schema. Liquibase applies the same changelogs to both.

After the Blueprint syncs, the staging service needs its `sync: false` env vars filled in the
Render dashboard, the same way production was set up. `SPRING_DATASOURCE_URL` is not among them —
it comes from `render.yaml` and already points at `travel_staging`. `CORS_ALLOWED_ORIGINS` is set
to `http://localhost:5173`: with no stable Vercel domain for the `dev` branch, staging's real use
is as a deployed backend for local frontend work.

On the Vercel side, `dev` already produces preview deployments. To give it a stable address,
assign a domain to the branch in Settings → Domains → Add Existing (for example
`team-project-243-dev.vercel.app`) and set its Git branch to `dev`.

Staging costs free-tier instance hours and sleeps like production. The keep-alive workflow does
**not** ping it, on purpose — waking a staging service every 6 hours would burn hours for nothing.

## Frontend on Vercel

Live at <https://team-project-243.vercel.app>. Vercel also builds a preview deployment for every
pull request and posts the link as a PR comment.

Project settings (Vercel → Settings → Build and Deployment):

| Setting | Value | Why |
| --- | --- | --- |
| Root Directory | `frontend` | monorepo — without it Vercel looks for `package.json` in the repo root |
| Framework Preset | Vite | |
| Build Command | `npm run build` (Override on) | the preset's own `vite build` **skips `tsc -b`**, so type errors that fail CI would still reach production |
| Output Directory | `dist` | |
| Install Command | `npm ci` (Override on) | installs exactly the lockfile, same as CI |
| Node.js Version | 24.x | matches `node-version` in the CI `frontend-build` job |

Environment variable: `VITE_API_URL` = `https://travel-planner-backend-4tb0.onrender.com`, for
Production and Preview. It is **not** a secret and must not be marked as one — Vite inlines every
`VITE_*` variable into the client bundle, so anyone can read it in DevTools. Never put a real
secret behind a `VITE_` name.

`frontend/vercel.json` rewrites `/(.*)` to `/index.html` so client-side routes survive a page
reload or a deep link. Rewrites only fire when no static file matches the path, so hashed assets
under `/assets/` are still served as themselves.

## CORS

The Spring Boot backend must allow the Vercel origin. Read it from `CORS_ALLOWED_ORIGINS`
and apply it in a `WebMvcConfigurer` (or Spring Security CORS config):

- allowed origins: value of `CORS_ALLOWED_ORIGINS`
- allowed methods: `GET, POST, PUT, DELETE, OPTIONS`
- allow credentials: only if token auth needs it

## Secrets — where they live

- **DB password:** only in the local Terraform state (`infra/aiven/terraform.tfstate`, gitignored).
  Retrieve with `terraform output -raw mysql_password`. Set it directly in Render, never in git.
- **Aiven API token:** `infra/aiven/terraform.tfvars`, gitignored via `*.tfvars`. Create it in the
  Aiven console → Profile → Authentication → Application tokens.
- **ORS API key:** register at <https://openrouteservice.org/dev/#/signup>, create a token,
  set it as `ORS_API_KEY` in Render. Never commit it.
- **Nominatim:** no key, but the `User-Agent` must carry a real contact email (usage policy).

## Auto-deploy

- `main` → production (Render + Vercel redeploy on push to `main`).
- `dev` → staging (`travel-planner-backend-staging` on Render, preview deployments on Vercel).

Render's auto-deploy is scoped by `rootDir: backend` in `render.yaml`: a push to `main` that
changes nothing under `backend/` produces no deploy and no event at all. So the commit shown as
`Live` may legitimately be older than `main`. That is not drift and needs no fixing, as long as
no backend file changed since.

## Troubleshooting

Both of these already cost a failed production deploy. They come from the free tiers rather than
from the code, and neither can be reproduced locally.

### `UnknownHostException` on the Aiven host, or `NXDOMAIN`

The database is **powered off**, not deleted. Aiven withdraws the DNS record and nulls
`service_host`/`service_port` while a service is off, which looks exactly like a deleted service.

`terraform plan` is no help here: it reports `No changes` and keeps `state = RUNNING`, because
`state` is not part of our configuration, so the provider does not treat it as drift. Plan
verifies configuration, not liveness.

Check the real state, then power it on:

```bash
TOKEN=$(awk -F'"' '/aiven_api_token/{print $2}' infra/aiven/terraform.tfvars)

curl -s -H "Authorization: aivenv1 $TOKEN" \
  https://api.aiven.io/v1/project/mr-b549/service/travel-mysql

curl -s -X PUT -H "Authorization: aivenv1 $TOKEN" -H "Content-Type: application/json" \
  -d '{"powered": true}' \
  https://api.aiven.io/v1/project/mr-b549/service/travel-mysql
```

It goes `REBUILDING` → `RUNNING` in one to two minutes and comes back on the **same** hostname,
port and password, so nothing in Render needs changing. Then redeploy the backend.

### `Unable to create or change a table without a primary key`

Aiven ships with `sql_require_primary_key = ON`. Liquibase creates its own `DATABASECHANGELOG`
bookkeeping table **without** a primary key, so the very first migration attempt is rejected and
the backend exits with status 1. Our own tables all have primary keys; only Liquibase's does not.

Already fixed declaratively in `infra/aiven/main.tf`:

```hcl
mysql_user_config {
  mysql_version = var.mysql_version

  mysql {
    sql_require_primary_key = false
  }
}
```

The local `mysql:8.4` in `docker-compose.yml` has this setting off, which is why rehearsing a
release locally cannot catch it. Verify against the real database with
`SELECT @@global.sql_require_primary_key;` — it must return `0`.

### Staging wrote to the production database

Happened on 2026-07-25. Ihor's `05-insert-dummy-user.sql` was merged into `dev`, staging deployed
it, and Liquibase applied the changeset to **`travel`** instead of `travel_staging`: production
gained a `test@example.com` row and a `DATABASECHANGELOG` entry for a changeset that did not exist
in `main`. Staging's own database got nothing.

The cause was staging's `SPRING_DATASOURCE_URL` pointing at `/travel`. It had been correct — the
first staging deploy created the tables in `travel_staging` — and changed at some point after,
either by a manual edit or by a Blueprint re-sync. Nobody could tell which, and that is the point:
a value living only in the dashboard leaves no history.

That is why the URL now lives in `render.yaml` as a plain `value:`. If it ever drifts again, the
next Blueprint sync restores it from git.

To check which database a service is really writing to, compare changelogs:

```sql
SELECT ID, DATEEXECUTED FROM travel.DATABASECHANGELOG ORDER BY ORDEREXECUTED;
SELECT ID, DATEEXECUTED FROM travel_staging.DATABASECHANGELOG ORDER BY ORDEREXECUTED;
```

The timestamps say which deploy touched which schema. Production was cleaned by deleting the stray
`users` row and its `DATABASECHANGELOG` entry, so the database matched the deployed code again.

## Branches / environments summary

| Branch | Backend (Render) | Frontend (Vercel) | Database |
| --- | --- | --- | --- |
| `main` | `travel-planner-backend` | production deployment | Aiven `travel` |
| `dev` | `travel-planner-backend-staging` | preview deployments | Aiven `travel_staging` |

Both databases live on the same Aiven service, because the free plan allows only one.
