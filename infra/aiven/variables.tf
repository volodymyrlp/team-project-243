variable "aiven_api_token" {
  description = "Aiven API token; provide via: export TF_VAR_aiven_api_token=..."
  type        = string
  sensitive   = true
}

variable "aiven_project" {
  description = "Aiven project name from the Aiven console"
  type        = string
}

variable "service_name" {
  description = "Name of the managed MySQL service"
  type        = string
  default     = "travel-mysql"
}

variable "cloud_name" {
  description = "Aiven cloud/region; free tier only on DigitalOcean/UpCloud (do-fra = Frankfurt)"
  type        = string
  default     = "do-fra"
}

variable "plan" {
  description = "Aiven service plan; free-1-1gb is the free tier (1GB)"
  type        = string
  default     = "free-1-1gb"
}

variable "mysql_version" {
  description = "MySQL version; 8.4 is current (8.0 reached end of availability)"
  type        = string
  default     = "8.4"
}
