variable "project_name" {}
variable "db_password" {}
variable "jwt_secret" { default = "YourSuperSecretKeyForJwtAuthenticationWhichShouldBeLongEnough" }

resource "aws_secretsmanager_secret" "app_secrets" {
  name = "${var.project_name}-secrets-${random_id.id.hex}"
  recovery_window_in_days = 0
}
resource "random_id" "id" {
  byte_length = 4
}

resource "aws_secretsmanager_secret_version" "version" {
  secret_id     = aws_secretsmanager_secret.app_secrets.id
  secret_string = jsonencode({
    DB_PASSWORD = var.db_password
    JWT_SECRET  = var.jwt_secret
  })
}

output "secret_arn" { value = aws_secretsmanager_secret.app_secrets.arn }
