provider "aiven" {
  api_token = var.aiven_api_token
}

resource "aiven_mysql" "travel" {
  project      = var.aiven_project
  service_name = var.service_name
  cloud_name   = var.cloud_name
  plan         = var.plan

  mysql_user_config {
    mysql_version = var.mysql_version

    mysql {
      sql_require_primary_key = false
    }
  }
}

resource "aiven_mysql_database" "travel" {
  project       = var.aiven_project
  service_name  = aiven_mysql.travel.service_name
  database_name = "travel"
}

resource "aiven_mysql_database" "travel_staging" {
  project       = var.aiven_project
  service_name  = aiven_mysql.travel.service_name
  database_name = "travel_staging"
}

check "mysql_service_is_running" {
  data "aiven_mysql" "current" {
    project      = var.aiven_project
    service_name = var.service_name
  }

  assert {
    condition     = data.aiven_mysql.current.state == "RUNNING"
    error_message = "The Aiven MySQL service is not RUNNING. The free plan powers it off after roughly a day without connections, and while it is off its DNS record is withdrawn, so the backend cannot start at all. See the troubleshooting section of docs/DEPLOY.md for the command that powers it back on."
  }
}
