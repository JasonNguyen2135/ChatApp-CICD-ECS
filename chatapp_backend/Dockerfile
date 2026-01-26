# Stage 1: Build - Sử dụng JDK 21 để biên dịch code
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy file cấu hình Maven để tận dụng Docker cache
COPY pom.xml .

# Tải các thư viện cần thiết (dependencies)
RUN mvn dependency:go-offline

# Copy source code vào container
COPY src ./src

# Build ra file .jar và bỏ qua unit tests để tiết kiệm thời gian
RUN mvn clean package -DskipTests

# Stage 2: Run - Sử dụng JRE 21 siêu nhẹ để chạy ứng dụng
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy file .jar từ giai đoạn build sang
COPY --from=builder /app/target/*.jar app.jar

# Backend chạy trên cổng 8080
EXPOSE 8080

# Lệnh khởi chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]