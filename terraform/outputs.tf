output "ecr_url" { value = aws_ecr_repository.repo.repository_url }
output "alb_dns" { value = aws_lb.main.dns_name }