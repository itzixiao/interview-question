# ========================
# Dockerfile 示例详解
# ========================
# 用途：将 Spring Boot 应用容器化
# 作者：itzixiao
# 时间：2026-03-15

# ===========================================
# 第一部分：基础版 Dockerfile（适合初学者）
# ===========================================

# 使用 OpenJDK 8 作为基础镜像
FROM openjdk:8-jdk-alpine

# 设置维护者信息
LABEL maintainer="itzixiao@example.com"

# 设置工作目录
WORKDIR /app

# 复制打包好的 jar 文件到容器中
# 注意：实际使用时需要先用 mvn package 打包
COPY target/interview-containerization-1.0.0.jar app.jar

# 暴露应用端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]

# ===========================================
# 第二部分：优化版 Dockerfile（生产推荐）
# ===========================================
# 优化点：
# 1. 使用多阶段构建减少镜像大小
# 2. 分层缓存优化构建速度
# 3. 使用更小的基础镜像
# 4. 添加健康检查

# 第一阶段：构建阶段
FROM maven:3.8-openjdk-8 AS builder
WORKDIR /app
# 复制 pom.xml 利用 Docker 缓存层
COPY pom.xml .
# 下载依赖（如果 pom.xml 没变化，使用缓存）
RUN mvn dependency:go-offline -B
# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 第二阶段：运行阶段
FROM openjdk:8-jre-slim

# 创建非 root 用户（安全最佳实践）
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/interview-containerization-1.0.0.jar app.jar

# 更改文件所有者
RUN chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# 添加 JVM 参数优化
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC"

# 健康检查（每 30 秒检查一次）
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ===========================================
# 第三部分：极简版 Dockerfile（Alpine）
# ===========================================
# 优势：镜像体积最小（约 100MB）
# 注意：Alpine 使用 musl libc，某些 native 库可能不兼容

# FROM alpine:3.18
# RUN apk add --no-cache openjdk8 curl
# WORKDIR /app
# COPY target/interview-containerization-1.0.0.jar app.jar
# EXPOSE 8080
# HEALTHCHECK --interval=30s --timeout=3s \
#     CMD curl -f http://localhost:8080/health || exit 1
# ENTRYPOINT ["java", "-jar", "app.jar"]

# ===========================================
# 构建和使用说明
# ===========================================
# 1. 构建项目：mvn clean package
# 2. 构建镜像：docker build -t interview-containerization:1.0.0 .
# 3. 运行容器：docker run -p 8080:8080 interview-containerization:1.0.0
# 4. 查看日志：docker logs -f <container_id>
# 5. 进入容器：docker exec -it <container_id> sh
# 6. 停止容器：docker stop <container_id>
# 7. 删除容器：docker rm <container_id>
# 8. 删除镜像：docker rmi interview-containerization:1.0.0
