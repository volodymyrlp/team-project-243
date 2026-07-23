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
  description = "Aiven cloud/region; the free tier is AWS-only"
  type        = string
  default     = "aws-eu-central-1"
}

variable "plan" {
  description = "Aiven service plan; free-1 is the free tier"
  type        = string
  default     = "free-1"
}

variable "mysql_version" {
  description = "MySQL major version"
  type        = string
  default     = "8"
}
