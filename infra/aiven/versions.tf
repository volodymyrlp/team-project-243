terraform {
  required_version = ">= 1.9"

  cloud {
    organization = "volodymyrlp"

    workspaces {
      name = "team-project-243-aiven"
    }
  }

  required_providers {
    aiven = {
      source  = "aiven/aiven"
      version = ">= 4.0.0, < 5.0.0"
    }
  }
}
