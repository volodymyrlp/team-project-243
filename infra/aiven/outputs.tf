output "mysql_host" {
  description = "MySQL host"
  value       = aiven_mysql.travel.service_host
}

output "mysql_port" {
  description = "MySQL port"
  value       = aiven_mysql.travel.service_port
}

output "mysql_user" {
  description = "MySQL admin username"
  value       = aiven_mysql.travel.service_username
}

output "mysql_password" {
  description = "MySQL admin password"
  value       = aiven_mysql.travel.service_password
  sensitive   = true
}

output "mysql_service_uri" {
  description = "Full MySQL connection URI"
  value       = aiven_mysql.travel.service_uri
  sensitive   = true
}

output "spring_datasource_url" {
  description = "JDBC URL for Spring Boot (SPRING_DATASOURCE_URL)"
  value       = "jdbc:mysql://${aiven_mysql.travel.service_host}:${aiven_mysql.travel.service_port}/travel?sslMode=REQUIRED"
}
