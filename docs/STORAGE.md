# Storage for user-uploaded images (TP-149)

User avatars and trip cover images live in **Supabase Storage**, not in the database and not
on the Render disk. Render's filesystem is ephemeral — anything written there disappears on
the next deploy — so uploads have to go somewhere else.

## Why Supabase and not S3

The ticket left the provider open ("AWS S3 / GCP Bucket"). Supabase Storage was chosen because
it is the only candidate that is free **without a card on file**: 1 GB of files and 5 GB of
outbound traffic per month. It speaks the S3 protocol, so the backend uses the ordinary AWS SDK
and only the endpoint differs. If the project ever outgrows the free tier, moving to real S3 or
Cloudflare R2 is an endpoint change, not a rewrite.

The trade-off: a free Supabase project **pauses after 7 days without activity**, and a paused
project serves nothing — every uploaded image becomes unreachable at once. That is the same
trap Aiven and Render already sprang on us.

The catch is what counts as activity: Supabase measures **database** requests, not storage
traffic. Uploading and serving images all week does not reset the timer. So the project needs
a one-row table that a scheduled job reads, even though we use none of its Postgres otherwise.

Run this once in the project's SQL editor:

```sql
create table if not exists public.keepalive (
  id int primary key,
  touched_at timestamptz default now()
);
insert into public.keepalive (id) values (1) on conflict (id) do nothing;
alter table public.keepalive enable row level security;
create policy "anon can read keepalive" on public.keepalive
  for select to anon using (true);
```

The `Monitoring` workflow then reads that row every six hours. It needs the repository secret
`SUPABASE_ANON_KEY` — the anon/publishable key, which is safe to expose by design since it is
the key browsers would carry. Without the secret the step prints a warning and passes, so the
workflow does not turn red over it; the project simply is not being kept awake.

## Bucket

Two private buckets: `user-images` for production and `user-images-staging` for staging. They
are separate for the same reason the two databases are — staging once wrote its migrations into
the production schema because both pointed at the same place, and a shared bucket would let
staging uploads land among real users' files.

Be aware the separation here is weaker than it is for the databases. Supabase's S3 access keys
are scoped to the whole project, not to one bucket, so staging's credentials *can* reach the
production bucket. Only configuration keeps them apart. Enforcing it properly would mean a
second Supabase project, which would consume both free-plan project slots and double the
pause-watching below.

Both buckets were created through the dashboard, under Storage → Buckets → New bucket, with the
settings in the table. `infra/storage/create-bucket.sh` holds the same settings in code: it is
there to re-apply or verify them, not as the only way to create a bucket. It needs a legacy
`service_role` JWT — the 64-character S3 secret is a different credential and the Storage REST
API rejects it with `Invalid Compact JWS`, which is a confusing way to say "wrong kind of key".

| Setting | Value | Why |
| --- | --- | --- |
| Public | `false` | Objects are read through signed URLs, so nothing is world-readable by guessing a name. |
| File size limit | 5 MB | Enforced by the bucket itself, not only by the browser. |
| Allowed MIME types | `image/jpeg`, `image/png`, `image/webp` | Stops the bucket being used as a general file host. |

The bucket is not managed by Terraform. The official `supabase/supabase` provider has no
storage-bucket resource, and the only provider that does is a third-party one at version 0.1.x —
too fragile a dependency for a single resource that is created once and never changes.

## Upload flow

The image bytes never pass through the backend. Render's free instance has 512 MB of memory,
sleeps when idle, and takes over three minutes to wake — it must not be in the upload path.

1. Browser asks the backend for a signed upload URL, sending the file's content type and size.
2. Backend checks the JWT, validates type and size, and returns a presigned `PUT` URL plus the
   object key it minted.
3. Browser `PUT`s the file straight to Supabase Storage.
4. Browser tells the backend the upload succeeded; the backend stores the key.

## Object keys

```
avatars/<user_id>/<uuid>.<ext>
trip-covers/<trip_id>/<uuid>.<ext>
```

The key is minted by the backend, never by the client — otherwise a caller could overwrite
another user's avatar by choosing its key.

**Store the key in the database, not the full URL.** `users.avatar_url` currently holds a URL,
which bakes the storage host into the data: changing provider or domain would mean rewriting
every row. Since our Liquibase changesets carry no `--rollback`, a data migration is a one-way
door. Store `avatars/<user_id>/<uuid>.webp` and build the URL at read time.

## What the backend needs to implement

This is the boundary: the bucket, its settings, the credentials and the environment wiring are
DevOps. The endpoints below are backend work.

- `POST /api/v1/uploads/presign` — authenticated. Takes content type and size, rejects anything
  outside the allowed types or over 5 MB, mints the key, returns `{ uploadUrl, key }`.
- Signed URLs must be short-lived; a few minutes is plenty for one upload.
- Reads: generate a signed `GET` URL from the stored key when serving the profile or trip, or
  proxy through a redirect. Do not make the bucket public to avoid this.
- Every presign must be tied to the authenticated user. An unauthenticated presign endpoint is
  free file hosting for strangers.

## Environment variables

Declared in `render.yaml` as `sync: false`, values entered in the Render dashboard under
Environment. They are never committed.

| Variable | Meaning |
| --- | --- |
| `SUPABASE_S3_ENDPOINT` | `https://gvuchidiqyxjrpzjsnjb.storage.supabase.co/storage/v1/s3` |
| `SUPABASE_S3_REGION` | `eu-west-1` |
| `SUPABASE_S3_ACCESS_KEY_ID` | S3 access key, server-side only |
| `SUPABASE_S3_SECRET_ACCESS_KEY` | S3 secret, server-side only |
| `SUPABASE_STORAGE_BUCKET` | `user-images` |

These credentials are server-side only. They must never reach the frontend bundle — the browser
only ever sees a presigned URL, which is scoped to one object and expires.

**Two different hosts, and mixing them up costs an afternoon.** The S3 protocol lives on a
dedicated subdomain, `<project-ref>.storage.supabase.co`, while the Storage REST API used by
`create-bucket.sh` lives on the ordinary project host, `<project-ref>.supabase.co`. Both answer
on `/storage/v1/...`, and pointing the S3 client at the ordinary host fails with a signature
error rather than a clear "wrong address".

Two things trip up the S3 client against Supabase. Path-style addressing has to be switched on
(`forcePathStyle` in the AWS SDK, `pathStyleAccessEnabled` in the Java v2 builder), because the
bucket is a path segment rather than a subdomain. And the endpoint above is the dedicated
storage host: the older `https://<ref>.supabase.co/storage/v1/s3` form still answers, but the
dashboard hands out the `storage.supabase.co` one, so that is what we configure.

## No server-side image processing

Supabase's image transformation feature is Pro-only, so nothing resizes or re-encodes uploads
for us. The project-wide ceiling is 50 MB on the free plan; our buckets cap at 5 MB, which is
the limit that actually applies.

That leaves the frontend responsible for downscaling before upload — a photo straight from a
phone camera is several megabytes, and a handful of them would eat a noticeable share of the
1 GB. Downscale to something like 1600 px on the long edge and encode as WebP in the browser,
then upload. The 5 MB bucket limit is the backstop for when that is skipped, not the plan.

## Staying inside the free tier

- The 5 MB bucket limit and the MIME whitelist cap how fast storage fills.
- Orphaned objects — uploaded, but the follow-up call that saves the key never arrived — should
  be swept periodically, otherwise they accumulate against the 1 GB with nothing pointing at them.
- 5 GB of outbound traffic per month is the ceiling that will bind first if images are served
  uncached. Prefer long-lived signed URLs on the read side over regenerating per page view.
