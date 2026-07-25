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

## Notes
- Free tier: 1 GB RAM, single node, no backups; the service powers off after inactivity.
- The API token is passed via the `TF_VAR_aiven_api_token` env var — never commit it.
- State is local for now (gitignored). Move to a remote backend before the team shares applies.
- The database password exists only in the local `terraform.tfstate`; if it is lost, reset it
  from the Aiven console rather than trying to recover the file.
