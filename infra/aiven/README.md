# Aiven MySQL (Terraform)

Provisions the managed MySQL database (staging/production) for the travel planner
on Aiven's free tier.

## Prerequisites
- Aiven account and a project (https://console.aiven.io)
- Aiven API token (Profile -> Tokens)
- Terraform >= 1.9

## Usage
```bash
export TF_VAR_aiven_api_token="<your-aiven-token>"
cp terraform.tfvars.example terraform.tfvars   # set aiven_project
terraform init
terraform plan
terraform apply
```

## Outputs
- `mysql_host`, `mysql_port`, `mysql_user`
- `mysql_password` (sensitive), `mysql_service_uri` (sensitive)
- `spring_datasource_url` — ready to paste into the Render backend env vars

Read a sensitive value: `terraform output -raw mysql_password`.

## Rotating the API token

The token only authenticates Terraform against the Aiven API — rotating it does **not**
touch the database or its password, so it is safe to do at any time.

1. https://console.aiven.io -> Profile -> Authentication -> Application tokens.
2. Revoke the old token, then generate a new one.
3. Re-export it and confirm Terraform still authenticates and sees no drift:

   ```bash
    export TF_VAR_aiven_api_token="<new-token>"
   terraform plan   # expected: "No changes. Your infrastructure matches the configuration."
   ```

Note the leading space before `export`: with zsh's `HIST_IGNORE_SPACE` option the command
is kept out of `~/.zsh_history`, so the token is not left in plain text on disk.

## State is local, and that is the biggest risk here

`terraform.tfstate` lives on one laptop and is gitignored. Two consequences, and the second is
worse than it sounds:

1. Nobody else can run `apply`. Terraform is effectively a single-person tool on this project.
2. **If that file is lost, Terraform forgets the database exists.** The next `apply` tries to
   create `travel-mysql` from scratch, fails because the name is taken, and the real service
   becomes unmanaged. The database password lives only in this file too — losing it means
   resetting the password in the Aiven console and updating Render.

There is no backup of it anywhere. That is the single point of failure in our infrastructure.

### Moving to HCP Terraform (free)

HCP Terraform (formerly Terraform Cloud) stores state remotely, keeps a version history and locks
it during applies, and its free tier covers a team this size at no cost.

**Someone with the account has to do steps 1–3 — they create credentials, which is not
something to hand off.**

1. Sign up at <https://app.terraform.io> and create an organisation.
2. Create a workspace of type **CLI-driven workflow** named `team-project-243-aiven`.
3. Run `terraform login` and paste the token it asks for. It is stored in
   `~/.terraform.d/credentials.tfrc.json`, never in the repo.
4. Add the `cloud` block to `versions.tf`, replacing `<org>`:

   ```hcl
   terraform {
     cloud {
       organization = "<org>"

       workspaces {
         name = "team-project-243-aiven"
       }
     }
   }
   ```

5. Migrate the existing state — Terraform asks for confirmation and uploads the local file:

   ```bash
   terraform init
   ```

6. Confirm nothing drifted, then keep the local file as a one-time backup somewhere off the
   repository until you trust the remote:

   ```bash
   terraform plan   # expected: "No changes."
   ```

7. Set `TF_VAR_aiven_api_token` as a workspace variable in HCP, or keep passing it locally —
   the CLI-driven workflow runs plans on your machine either way.

## Notes
- Free tier: 1 GB RAM, single node, no backups; the service powers off after inactivity.
- The API token is passed via the `TF_VAR_aiven_api_token` env var — never commit it.
- The database password exists only in `terraform.tfstate`; if it is lost, reset it
  from the Aiven console rather than trying to recover the file.
- `check "mysql_service_is_running"` in `main.tf` asserts the service is powered on. `plan`
  verifies configuration, not liveness — without that assertion it happily reports
  `No changes` while the database is off and the backend cannot start.
