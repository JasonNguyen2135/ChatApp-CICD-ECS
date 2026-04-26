module "vpc" {
  source       = "./modules/vpc"
  project_name = var.project_name
  aws_region   = var.aws_region
}

module "alb" {
  source         = "./modules/alb"
  project_name   = var.project_name
  vpc_id         = module.vpc.vpc_id
  public_subnets = module.vpc.public_subnets
  container_port = var.container_port
  alb_sg_id      = aws_security_group.alb_sg.id # Truyền từ root
}

module "s3" {
  source       = "./modules/s3"
  project_name = var.project_name
}

module "secrets" {
  source       = "./modules/secrets"
  project_name = var.project_name
  db_password  = var.db_password
}

module "redis" {
  source          = "./modules/redis"
  project_name    = var.project_name
  vpc_id          = module.vpc.vpc_id
  private_subnets = module.vpc.private_subnets
  ecs_sg_id       = aws_security_group.ecs_sg.id # Truyền từ root
}

module "rds" {
  source          = "./modules/rds"
  project_name    = var.project_name
  vpc_id          = module.vpc.vpc_id
  private_subnets = module.vpc.private_subnets
  db_name         = var.db_name
  db_username     = var.db_username
  db_password     = var.db_password
  ecs_sg_id       = aws_security_group.ecs_sg.id # Truyền từ root
}

module "ecs" {
  source           = "./modules/ecs"
  project_name     = var.project_name
  vpc_id           = module.vpc.vpc_id
  public_subnets   = module.vpc.public_subnets
  container_port   = var.container_port
  ecs_sg_id        = aws_security_group.ecs_sg.id # Truyền từ root
  target_group_arn = module.alb.target_group_arn
  cpu              = var.cpu
  memory           = var.memory
  aws_region       = var.aws_region
  db_endpoint      = module.rds.db_endpoint
  db_name          = var.db_name
  db_username      = var.db_username
  secret_arn       = module.secrets.secret_arn
  s3_bucket_name   = module.s3.bucket_name
  redis_endpoint   = module.redis.redis_endpoint
}
