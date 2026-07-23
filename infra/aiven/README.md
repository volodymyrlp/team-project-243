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

## Notes
- Free tier: 1 GB RAM, single node, no backups; the service powers off after inactivity.
- The API token is passed via the `TF_VAR_aiven_api_token` env var — never commit it.
- State is local for now (gitignored). Move to a remote backend before the team shares applies.
