# 📱 Real-time Chat Application (Cloud-Native & Mobile)
Một ứng dụng nhắn tin thời gian thực hoàn chỉnh (End-to-End) bao gồm Backend chạy trên **AWS ECS Fargate** và Mobile Client trên **Android**. Dự án áp dụng quy trình **DevOps/CI/CD** tự động hóa hoàn toàn từ khâu Build đến Deploy.
## 🏗️ System Architecture (Kiến trúc hệ thống)
Hệ thống được thiết kế theo mô hình Microservices-ready, sử dụng Containerization và Infrastructure as Code.
<img width="1024" height="658" alt="image" src="https://github.com/user-attachments/assets/076e3517-9503-43d3-a9c3-75b3c018835d" />
### 🚀 Luồng hoạt động (Workflow):
1.  **Developer** đẩy code lên GitHub.
2.  **GitHub Actions** tự động kích hoạt 2 luồng song song:
    * **Backend:** Build Docker Image -> Push lên AWS ECR -> Deploy lên AWS ECS Cluster.
    * **Mobile:** Inject biến môi trường (DNS) -> Build APK Debug -> Upload Artifact.
3.  **User** tải file APK về máy, kết nối qua **Load Balancer (ALB)** để chat thời gian thực.
---
## 🛠️ Tech Stack (Công nghệ sử dụng)
### Backend (Server)
* **Framework:** Spring Boot 3.x (Java 17/21)
* **Protocol:** WebSocket (STOMP) & REST API
* **Database:** H2 Database (Dev) / RDS (Prod)
* **Container:** Docker
### Frontend (Mobile)
* **Platform:** Android (Java/Kotlin)
* **Networking:** Retrofit (HTTP), StompClient (WebSocket)
* **Features:** Real-time messaging, Image upload, Emoji reaction.
### DevOps & Cloud (Infrastructure)
* **Cloud Provider:** AWS (Amazon Web Services)
* **Orchestration:** AWS ECS (Elastic Container Service) - Fargate (Serverless)
* **Load Balancing:** Application Load Balancer (ALB)
* **IaC (Infrastructure as Code):** Terraform
* **CI/CD:** GitHub Actions
---
## ✨ Features (Tính năng chính)
* ✅ **Real-time Messaging:** Nhắn tin tức thời với độ trễ thấp qua WebSocket.
* ✅ **Auto-Scaling:** Backend tự động phục hồi nếu gặp sự cố nhờ AWS ECS.
* ✅ **Infrastructure as Code:** Toàn bộ hạ tầng mạng, server được khởi tạo bằng Terraform (không click chuột thủ công).
* ✅ **Automated Build:** Tự động tạo file APK có sẵn cấu hình Server mỗi khi có code mới.
* ✅ **Secure Deployment:** Sử dụng Docker Container để đảm bảo môi trường nhất quán.
---
## 📂 Project Structure (Cấu trúc dự án)
```bash
├── chatapp_backend/       # Source code Backend Spring Boot
│   ├── src/
│   ├── Dockerfile         # Cấu hình Docker image
│   └── build.gradle
├── chatapp_mobile/        # Source code Android App
│   ├── app/
│   └── build.gradle
├── terraform/             # Mã nguồn Infrastructure as Code
│   ├── main.tf            # Cấu hình AWS (ECS, ALB, VPC, SG)
│   └── variables.tf
└── .github/workflows/     # Cấu hình CI/CD Pipeline
    ├── deploy-backend.yml # Build & Deploy Backend lên AWS
    └── build-android.yml  # Build Android APK
