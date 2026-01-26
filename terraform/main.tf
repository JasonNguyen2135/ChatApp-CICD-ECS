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
        { name = "SERVER_PORT", value = tostring(var.container_port) }
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
    subnets = [aws_subnet.public_1.id, aws_subnet.public_2.id]
    security_groups = [aws_security_group.ecs_sg.id]
    assign_public_ip = true
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.backend_tg.arn
    container_name   = "backend-container"
    container_port   = var.container_port
  }
}