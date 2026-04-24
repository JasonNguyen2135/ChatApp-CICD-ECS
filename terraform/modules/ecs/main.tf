variable "project_name" {}
variable "vpc_id" {}
variable "public_subnets" { type = list(string) }
variable "container_port" {}
variable "alb_sg_id" {}
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

resource "aws_ecr_repository" "repo" {
  name = "${var.project_name}-repo"
  force_delete = true
}

resource "aws_ecs_cluster" "main" { name = "${var.project_name}-cluster" }

resource "aws_iam_role" "ecs_exec_role" {
  name = "${var.project_name}-exec-role"
  assume_role_policy = jsonencode({ Version="2012-10-17", Statement=[{Action="sts:AssumeRole", Effect="Allow", Principal={Service="ecs-tasks.amazonaws.com"}}] })
}

resource "aws_iam_role_policy_attachment" "ecs_policy" {
  role       = aws_iam_role.ecs_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecs_extra_policy" {
  name = "${var.project_name}-extra-policy"
  role = aws_iam_role.ecs_exec_role.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"]
        Resource = "arn:aws:s3:::${var.s3_bucket_name}/*"
      },
      {
        Effect = "Allow"
        Action = ["secretsmanager:GetSecretValue"]
        Resource = [var.secret_arn]
      }
    ]
  })
}

resource "aws_security_group" "ecs_sg" {
  name   = "${var.project_name}-ecs-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port       = var.container_port
    to_port         = var.container_port
    protocol        = "tcp"
    security_groups = [var.alb_sg_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_ecs_task_definition" "backend_task" {
  family                   = "${var.project_name}-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = aws_iam_role.ecs_exec_role.arn

  container_definitions = jsonencode([{
    name      = "backend-container"
    image     = "${aws_ecr_repository.repo.repository_url}:latest"
    essential = true
    portMappings = [{ containerPort = var.container_port, hostPort = var.container_port }]
    environment = [
        { name = "SERVER_PORT", value = tostring(var.container_port) },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${var.db_endpoint}/${var.db_name}" },
        { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
        { name = "SPRING_JPA_HIBERNATE_DDL_AUTO", value = "update" },
        { name = "AWS_S3_BUCKET", value = var.s3_bucket_name },
        { name = "AWS_REGION", value = var.aws_region },
        { name = "REDIS_HOST", value = var.redis_endpoint },
        { name = "SECRET_ARN", value = var.secret_arn }
    ]
    logConfiguration = {
        logDriver = "awslogs"
        options = {
            "awslogs-group" = "/ecs/${var.project_name}", "awslogs-region" = var.aws_region, "awslogs-stream-prefix" = "ecs", "awslogs-create-group" = "true"
        }
    }
  }])
}

resource "aws_ecs_service" "backend_service" {
  name            = "${var.project_name}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend_task.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  network_configuration {
    subnets = var.public_subnets
    security_groups = [aws_security_group.ecs_sg.id]
    assign_public_ip = true
  }
  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "backend-container"
    container_port   = var.container_port
  }
}

resource "aws_appautoscaling_target" "ecs_target" {
  max_capacity       = 3
  min_capacity       = 1
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.backend_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_policy_cpu" {
  name               = "cpu-autoscaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 70.0
  }
}

output "ecs_sg_id" { value = aws_security_group.ecs_sg.id }
