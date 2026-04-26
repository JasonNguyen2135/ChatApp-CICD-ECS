variable "project_name" {}
variable "vpc_id" {}
variable "public_subnets" { type = list(string) }
variable "container_port" {}
variable "alb_sg_id" {} # Khai báo biến mới

resource "aws_lb" "main" {
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.alb_sg_id] # Dùng biến truyền vào
  subnets            = var.public_subnets
}

resource "aws_lb_target_group" "backend_tg" {
  name        = "${var.project_name}-tg"
  port        = var.container_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"
  
  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"
  
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend_tg.arn
  }
}

output "alb_dns_name" { value = aws_lb.main.dns_name }
output "target_group_arn" { value = aws_lb_target_group.backend_tg.arn }
