# 📱 Enterprise Real-time Chat Application (Cloud-Native & Distributed)

Một ứng dụng nhắn tin thời gian thực hoàn chỉnh (End-to-End) với kiến trúc chuẩn Enterprise, chạy trên nền tảng **AWS ECS Fargate**. Dự án không chỉ tập trung vào tính năng chat mà còn giải quyết các bài toán về **Khả năng mở rộng (Scalability)**, **Bảo mật (Security)** và **Infrastructure as Code (IaC)**.

## 🏗️ System Architecture (Kiến trúc hệ thống)

Hệ thống được thiết kế theo mô hình Stateless Architecture, cho phép mở rộng không giới hạn (Horizontal Scaling).

<img width="1024" height="658" alt="image" src="https://github.com/user-attachments/assets/076e3517-9503-43d3-a9c3-75b3c018835d" />

### 🚀 Điểm nhấn kiến trúc (Key Highlights):
1.  **Distributed WebSocket with Redis:** Sử dụng **Amazon ElastiCache (Redis)** làm Message Broker. Điều này cho phép tin nhắn được đồng bộ hóa tức thì giữa tất cả các ECS Tasks, giải quyết bài toán đa server.
2.  **Stateless Storage (AWS S3):** File và ảnh được lưu trữ vĩnh viễn trên **AWS S3**, đảm bảo dữ liệu không bị mất khi container khởi động lại.
3.  **Enterprise Security:** 
    *   Xác thực người dùng qua **JWT (JSON Web Token)** tự xây dựng trên Backend Java.
    *   Quản lý thông tin nhạy cảm (DB Password, JWT Secret) qua **AWS Secrets Manager**.
4.  **Database:** Chuyển đổi từ H2 sang **AWS RDS PostgreSQL** chuyên nghiệp, đặt trong Private Subnet để bảo mật tối đa.

---

## 🛠️ Tech Stack (Công nghệ sử dụng)

### Backend (Server)
* **Framework:** Spring Boot 4.x (Java 21)
* **Security:** Spring Security & JWT
* **Real-time:** WebSocket (STOMP) & Redis Pub/Sub Relay
* **Database:** PostgreSQL (AWS RDS)
* **Cloud SDK:** AWS SDK v2 (S3, Secrets Manager)

### Frontend (Mobile)
* **Platform:** Android (Kotlin/Jetpack Compose)
* **Networking:** Retrofit (REST), StompClient (WebSocket)
* **Auth:** Custom JWT Authentication (Gỡ bỏ phụ thuộc Firebase Auth)

### DevOps & Cloud Infrastructure (IaC)
* **Cloud:** AWS (ECS Fargate, ALB, S3, Redis, RDS, Secrets Manager)
* **IaC:** **Terraform (Module-based Architecture)**
* **CI/CD:** GitHub Actions (Automated Build & Deploy)

---

## ✨ Features (Tính năng nâng cao)
* ✅ **High Availability:** Tự động mở rộng từ 1-3 tasks dựa trên CPU/RAM nhờ **ECS Auto Scaling**.
* ✅ **Stateless Backend:** Dễ dàng bảo trì và nâng cấp không gây gián đoạn (Zero-downtime).
* ✅ **Secure by Design:** Toàn bộ DB và Cache đặt trong Private Subnet, truy cập qua Security Group layer.
* ✅ **Infrastructure as Code:** Khởi tạo toàn bộ tài nguyên AWS chỉ với 1 câu lệnh Terraform.
* ✅ **Distributed Pub/Sub:** Đảm bảo tin nhắn được phân phối chính xác trong môi trường nhiều server.

---

## 📂 Project Structure (Cấu trúc dự án)
```bash
├── chatapp_backend/       # Backend Spring Boot (Java 21)
│   ├── src/main/java/     # JWT Auth, S3 Service, Redis Sync logic
│   └── Dockerfile         # Dockerized Backend
├── chatapp_mobile/        # Android App (Kotlin)
│   └── app/               # Logic Retrofit & WebSocket Client
├── terraform/             # Infrastructure as Code (Professional Modules)
│   ├── modules/
│   │   ├── vpc/           # Network (Public/Private Subnets)
│   │   ├── ecs/           # Cluster, Service, AutoScaling, IAM
│   │   ├── rds/           # PostgreSQL Database
│   │   ├── redis/         # ElastiCache Broker
│   │   └── s3/            # Assets Storage
│   ├── main.tf            # Root Module orchestrator
│   └── variables.tf
└── .github/workflows/     # CI/CD Pipeline
    ├── backend-cd.yml     # Build & Push ECR, Deploy ECS
    └── android-ci.yml     # Build Android APK Artifact
```

---

## 🚀 Deployment (Hướng dẫn triển khai)

1. **Khởi tạo hạ tầng:**
   ```bash
   cd terraform
   terraform init
   terraform apply
   ```
2. **Deploy Backend:** Đẩy code lên nhánh `main`, GitHub Actions sẽ tự động Deploy lên ECS.
3. **Build App:** APK sẽ được tự động tạo trong tab Actions của GitHub, sẵn sàng để cài đặt.
