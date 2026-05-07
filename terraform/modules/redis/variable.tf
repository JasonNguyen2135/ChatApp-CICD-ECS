variable "project_name" {}
variable "vpc_id" {}
variable "private_subnets" { type = list(string) }
variable "ecs_sg_id" {} 