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
