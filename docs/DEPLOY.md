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
| Backend, staging | <https://travel-planner-backend-staging.onrender.com> |

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
bean of its own.** The moment one is added, it has to `permitAll` on `/actuator/health` and
`/actuator/info` —
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
   | `SPRING_DATASOURCE_PASSWORD` | *(secret)* | `cd infra/aiven && terraform output -raw mysql_password` |
   | `NOMINATIM_USER_AGENT` | `team243-travel-planner (contact: <real email>)` | Nominatim policy requires a contact |
   | `ORS_API_KEY` | *(secret)* | OpenRouteService account |

   Three variables, and only two of them are real secrets. `NOMINATIM_USER_AGENT` stays out of git
   for a different reason: Nominatim's policy requires a genuine contact address in it, and a
   personal email in a public repository is an invitation to scrapers.

   Everything else — `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `NOMINATIM_URL`,
   `CORS_ALLOWED_ORIGINS` — is a plain `value:` in `render.yaml`, different per service where it
   needs to be (`/travel` vs `/travel_staging`, the Vercel origin vs `http://localhost:5173`).
   None of them is secret, and keeping them in git means the difference between the two
   environments shows up in a PR diff instead of living in two browser tabs. On 2026-07-25 the
   datasource URL silently changed on the staging service and staging applied a migration straight
   to the production database; see [Troubleshooting](#troubleshooting).

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
still useful but no longer catches a failed deploy.

The deploy check runs only when the push actually changed something under `backend/`, decided by
`git diff HEAD^ HEAD -- backend/`. That mirrors Render's own `rootDir` rule: a docs-only release
produces no deploy at all, and demanding one would fail every such release.

**A better version of this check is available to the backend side.** If the application published
the commit it was built from, smoke could ask production directly which commit it is running — no
platform API, no secret, and it would keep working on any host. Render already injects
`RENDER_GIT_COMMIT` into the running service, so it takes two things in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  info:
    env:
      enabled: true

info:
  commit: ${RENDER_GIT_COMMIT:local}
```

and a `permitAll` on `/actuator/info` in the `SecurityFilterChain`, since Spring Boot's default
management security exposes **only** `/actuator/health` anonymously — with the config alone the
endpoint answers `401`. Verified locally: with the config and basic auth it returns
`{"commit":"local"}`. When that lands, switch this check to `/actuator/info` and drop
`RENDER_API_KEY` entirely.

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

## Rollback

We can ship in three minutes. Undoing is not symmetric, and the asymmetry is the whole point of
this section: **code rolls back, a database schema does not — unless somebody wrote the way back
when they wrote the way forward.**

### Backend

Render dashboard → the service → **Deploys** → **Rollback** on the last known good deploy. It
redeploys that image; no rebuild, so it is fast. The `Rollback` button is greyed out on the deploy
that is currently live.

Equivalent from git: revert the offending commit on `main` and let auto-deploy carry it. Slower,
but it keeps `main` honest — after a dashboard rollback, production runs something that is no
longer what `main` says. Use the button to stop the bleeding, then revert in git so the two agree.

### Frontend

Vercel dashboard → **Deployments** → the previous production deployment → **Promote to
Production**. Static files, so it is close to instant. Same rule applies: follow it with a git
revert, or `main` and production disagree.

### Database

**This is the one that will hurt, and it was rehearsed on staging on 2026-07-25. It does not
work. Not "partially" — at all.**

```
Rolling Back Changeset: db/changelog/changes/05-insert-dummy-user.sql::insert-dummy-user
ERROR: RollbackFailedException
Liquibase does not support automatic rollback generation for raw sql changes
```

Every changeset we have is `--liquibase formatted sql`, and for a raw SQL change Liquibase cannot
infer a reverse — not for `INSERT`, and not for `CREATE TABLE` either. There is no partial
capability to fall back on: **the database currently cannot be rolled back by one step.**

The failure is at least clean. Liquibase refused before touching anything, and staging came out
with the same five changesets and the same row it went in with. A rollback attempt during a real
incident would waste the minutes it takes to discover this, and nothing more.

That is survivable while the schema only grows and holds no real data. It stops being survivable
the moment users exist.

**Convention to adopt — every changeset carries its own reverse:**

```sql
--liquibase formatted sql
--changeset author:add-notes-to-trips
ALTER TABLE trips ADD COLUMN notes TEXT;
--rollback ALTER TABLE trips DROP COLUMN notes;
```

With that in place, this rolls the last change back on a service. It is the exact command from the
rehearsal, so it works as written — the two non-obvious parts cost three failed attempts to find:
the Liquibase images ship **no** JDBC drivers, and overriding the entrypoint loses the default
search path.

```bash
PW=$(cd infra/aiven && terraform output -raw mysql_password)

docker run --rm --entrypoint /bin/sh \
  -v "$PWD/backend/src/main/resources:/liquibase/changelog" \
  liquibase/liquibase:4.29 -c \
  "lpm add mysql --global && liquibase \
    --search-path=/liquibase/changelog \
    --url='jdbc:mysql://travel-mysql-mr-b549.d.aivencloud.com:18032/travel_staging?sslMode=REQUIRED' \
    --username=avnadmin --password='$PW' \
    --changeLogFile=db/changelog/db.changelog-master.yaml \
    rollbackCount 1"
```

The `4.29` tag matches the `liquibase-core` version the application itself runs, so a rehearsal
behaves like the real thing.

Point it at `travel_staging` and never at `travel` until it has succeeded there. A rollback that
has never been tried is a plan, not a capability — ours turned out to be a plan.

**When a migration is the problem, the order matters:** roll the database back *first*, then the
code. The other order leaves the old application talking to a schema it was never built for, and
`ddl-auto: validate` will refuse to start it — turning a bad release into an outage.

### What cannot be rolled back at all

Data that a migration destroyed between two backups. The free Aiven plan has **no backups of its
own**, which is why we take our own — see below. They run daily, so the worst case is losing up to
a day of writes. Any changeset that drops a column or a table on production is still close to a
one-way door and should be reviewed as one.

## Backups

`.github/workflows/backup.yml` dumps both schemas every night at 03:00 UTC, and can be run by hand
from the Actions tab. Aiven's free plan takes no backups at all, so these are the only copies that
exist.

Each run dumps `travel` and `travel_staging`, **restores both into a throwaway MySQL 8.4 container
and checks them**, then compresses and uploads them as a workflow artifact kept for 90 days. The
verification is the point: a dump nobody has restored is a file, not a backup. The run fails if a
schema comes back with fewer tables than expected or with an empty Liquibase changelog — a dump
that carries structure but no state would otherwise look perfectly healthy.

Two things learned while building it, both of which cost a failed attempt:

- `mysqldump` against Aiven emits `SET @@GLOBAL.GTID_PURGED` by default, and that makes the dump
  refuse to load into a server with its own GTID state. `--set-gtid-purged=OFF` removes it.
- The dump must run in a `mysql:8.4` container rather than with whatever client the runner has, so
  client and server versions match.

### Restoring

1. Actions → the `Backup` run you want → download the `mysql-backup-<date>` artifact.
2. `gunzip travel_staging-<date>.sql.gz`
3. Restore into **staging first**, always:

   ```bash
   export MYSQL_PWD=$(cd infra/aiven && terraform output -raw mysql_password)
   docker run --rm -i -e MYSQL_PWD -v "$PWD:/w" -w /w mysql:8.4 \
     mysql -h travel-mysql-mr-b549.d.aivencloud.com -P 18032 -u avnadmin \
       --ssl-mode=REQUIRED < travel_staging-<date>.sql
   ```

4. Check what came back — table count, `DATABASECHANGELOG`, and whatever data mattered — before
   even considering the same command against `travel`.

**Rehearsed on `travel_staging` on 2026-07-26, and it works.** The restore came back with the same
6 tables, 5 changesets and the same row it went in with, and the staging backend answered `UP`
afterwards. The detail that proves the restore actually did something rather than quietly doing
nothing: `information_schema.tables.CREATE_TIME` for `users` moved to the time of the restore. Check
that field if you ever need to confirm a restore really landed.

The dump contains `DROP TABLE IF EXISTS` for every table it recreates, so a restore **overwrites**
the target schema. Restoring `travel` is a destructive act on production and should be treated
like one: know what you are overwriting, and why.

### What these backups do not protect against

They live in the same GitHub organisation as the code, so they cover data loss, not the loss of the
account. Retention is 90 days. And if Aiven has powered the database off at 03:00 UTC, the dump
fails — deliberately, since a silent skip would leave a gap nobody notices.

## Alerting — what we would actually find out about

Honest inventory, because "we have monitoring" is easy to believe and expensive to be wrong about.

| Event | How we learn | Delay |
| --- | --- | --- |
| A deploy fails | Render emails on failure (workspace default), and Smoke fails on the release commit | minutes |
| Production is down | The Keep-alive workflow fails and GitHub emails | up to 6 hours |
| The Aiven database powered itself off | Same — Keep-alive goes red, because the health check queries the database | up to 6 hours |
| A migration corrupted data | Nothing tells us | never, until someone notices |

The six-hour worst case is a deliberate trade: a shorter interval wakes the free Render instance
more often and burns the monthly instance-hour budget for very little. If the project ever gets
real users, that is the number to revisit first.

There is no paging, no on-call and no dashboard, and for a student project that is the right
amount of machinery. What matters is that the table above is written down, so nobody assumes a
safety net that is not there.

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
