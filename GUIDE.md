# 💬 Hướng Dẫn Vận Hành Dự Án Chat App (AWS ECS & CI/CD)

Hệ thống Chat thời gian thực được xây dựng với kiến trúc Cloud-Native, tự động hóa hạ tầng bằng Terraform và triển khai lên AWS.

## 🏗 1. Cấu trúc hệ thống
- **Backend**: Spring Boot + WebSocket + Redis (để sync tin nhắn).
- **Mobile**: Android Application (Kotlin).
- **Infrastructure (IaC)**: Terraform quản lý VPC, ECS Fargate, RDS (Postgres), ElastiCache (Redis), S3, ALB.
- **CI/CD**: GitHub Actions (Tự động build Docker Backend & Build APK cho Android).

## 🛠 2. Triển khai hạ tầng (AWS)
1. Cài đặt Terraform và AWS CLI.
2. Tại thư mục `terraform/`:
   ```bash
   terraform init
   git plan
   terraform apply -auto-approve
   ```

## 🚀 3. Quy trình CI/CD
### Cho Backend:
- Tự động Build Docker image.
- Đẩy lên AWS ECR.
- Deploy lên AWS ECS Fargate thông qua Service Discovery.

### Cho Mobile:
- Tự động Build file APK từ mã nguồn Android.
- Lưu trữ file APK vào GitHub Artifacts để tải về.

## 📡 4. Công nghệ nổi bật
- **Real-time**: Sử dụng WebSocket kết hợp với Redis Pub/Sub để nhắn tin tức thì.
- **Storage**: Lưu trữ ảnh/file trên AWS S3.
- **Security**: AWS Secrets Manager quản lý các thông tin nhạy cảm.

## 🔑 5. Cấu hình Secrets
Đảm bảo GitHub Secrets đã có: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `DOCKER_USERNAME` (nếu dùng Docker Hub).
