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

## State lives in HCP Terraform

Migrated on 2026-07-26 from a local `terraform.tfstate` to the HCP Terraform workspace
`volodymyrlp/team-project-243-aiven`, free tier. The state is versioned and locked during applies,
and it is no longer one file on one laptop — which was the single point of failure in our
infrastructure: losing it would have made Terraform forget the database exists and taken the only
copy of its password with it.

`terraform.tfstate` is still in the working directory as the pre-migration snapshot. It is
gitignored and can be deleted once the remote is trusted.

### The state is locked now, which is new

Remote state is locked for the duration of every operation, so a killed command can leave the
lock behind and every later command fails with `Error acquiring the state lock`. The easiest way
to cause this is piping a plan into something that closes early:

```bash
terraform plan | head -4      # head exits, plan gets SIGPIPE, lock survives
terraform plan > plan.txt     # do this instead
```

Releasing it — note that HCP's lock ID is the **workspace name**, not the UUID the error prints
under `Lock Info`:

```bash
terraform force-unlock -force "volodymyrlp/team-project-243-aiven"
```

Only force-unlock a lock you recognise as your own. `Who` in the error message says which machine
and user holds it; if that is not you, someone is mid-apply and breaking their lock can corrupt
the state.

### How it was set up (repeat these steps for a new environment)

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

7. **Switch the workspace to local execution: Settings → General → Execution Mode → Local.**
   A CLI-driven workspace defaults to **Remote** execution, which is not what the name suggests:
   Terraform packs the working directory up and runs the plan on HashiCorp's servers, and that
   directory contains `terraform.tfvars` with the Aiven API token. With `Local`, HCP stores only
   the state and plans run on your machine, so the token never leaves it.

   Keep `Remote` instead only if someone else needs to run plans without local credentials. Then
   the token belongs in a workspace variable marked sensitive, and `terraform.tfvars` belongs in
   `.terraformignore`.

## Notes
- Free tier: 1 GB RAM, single node, no backups; the service powers off after inactivity.
- The API token is passed via the `TF_VAR_aiven_api_token` env var — never commit it.
- The database password exists only in `terraform.tfstate`; if it is lost, reset it
  from the Aiven console rather than trying to recover the file.
- `check "mysql_service_is_running"` in `main.tf` asserts the service is powered on. `plan`
  verifies configuration, not liveness — without that assertion it happily reports
  `No changes` while the database is off and the backend cannot start.
