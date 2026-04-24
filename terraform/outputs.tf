output "alb_dns_name" {
  description = "DNS của Load Balancer để truy cập ứng dụng"
  value       = module.alb.alb_dns_name
}

output "db_endpoint" {
  description = "Endpoint của cơ sở dữ liệu RDS"
  value       = module.rds.db_endpoint
}
