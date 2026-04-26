output "alb_dns_name" {
  description = "DNS name of the Load Balancer"
  value       = module.alb.alb_dns_name
}

output "db_endpoint" {
  description = "The connection endpoint for the RDS instance"
  value       = module.rds.db_endpoint
}

output "redis_endpoint" {
  description = "The endpoint of the Redis cluster"
  value       = module.redis.redis_endpoint
}

output "ecr_repository_url" {
  description = "The URL of the ECR repository"
  value       = module.ecs.ecr_repository_url
}
