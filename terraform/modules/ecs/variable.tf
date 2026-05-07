variable "project_name" {}
variable "vpc_id" {}
variable "public_subnets" { type = list(string) }
variable "container_port" {}
variable "ecs_sg_id" {}
variable "target_group_arn" {}
variable "cpu" {}
variable "memory" { }
variable "aws_region" {}
variable "db_endpoint" {}
variable "db_name" {}
variable "db_username" {}
variable "secret_arn" {}
variable "s3_bucket_name" {}
variable "redis_endpoint" {}