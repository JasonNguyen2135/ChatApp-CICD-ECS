variable "aws_region" { description = "Region AWS" }
variable "project_name" { description = "Tên dự án" }
variable "container_port" { description = "Port của Spring Boot" }
variable "cpu" { description = "CPU Fargate" }
variable "memory" { description = "RAM Fargate" }

variable "db_name" {
  description = "Database name"
  default     = "chatappdb"
}

variable "db_username" {
  description = "Database admin username"
  default     = "dbadmin"
}

variable "db_password" {
  description = "Database admin password"
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT Secret Key"
  sensitive   = true
}