# Docker 容器化详解

## 一、Docker 核心概念

### 1.1 镜像（Image）

- 只读模板，包含运行应用所需的一切
- 由多个层（Layer）组成，共享相同层

### 1.2 容器（Container）

- 镜像的运行实例
- 可读写层 + 镜像层
- 生命周期：创建 → 运行 → 暂停 → 停止 → 删除

### 1.3 仓库（Registry）

- 存储和分发镜像的服务
- Docker Hub（公共）、Harbor（私有）

---

## 二、Docker 常用命令

### 2.1 镜像操作

```bash
docker images            # 列出本地镜像
docker pull nginx:latest # 拉取镜像
docker build -t myapp:v1 .  # 构建镜像
docker push myapp:v1     # 推送镜像
docker rmi image_id      # 删除镜像
docker tag old:v1 new:v1 # 给镜像打标签
docker save -o file.tar image  # 导出镜像
docker load -i file.tar  # 导入镜像
```

### 2.2 容器操作

```bash
docker run -d -p 8080:80 --name web nginx  # 后台运行容器
  -d: 后台运行
  -p: 端口映射（主机：容器）
  --name: 容器名称
  -v: 挂载卷（主机目录：容器目录）
  -e: 环境变量
  --restart=always: 自动重启

docker ps                # 查看运行中的容器
docker ps -a             # 查看所有容器
docker start/stop/restart container  # 启停容器
docker rm container      # 删除容器
docker exec -it container bash  # 进入容器
docker logs -f container # 查看容器日志
docker inspect container # 查看容器详情
docker cp file container:/path  # 复制文件到容器
```

---

## 三、Dockerfile 编写

### 3.1 Spring Boot 应用 Dockerfile 示例

```dockerfile
# 基础镜像
FROM openjdk:17-jdk-slim

# 维护者信息
LABEL maintainer="itzixiao@example.com"

# 设置工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime

# 复制 JAR 包（利用 Docker 层缓存）
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx512m"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 四、Docker Compose

### 4.1 docker-compose.yml 示例

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
    depends_on:
      - mysql
      - redis
    networks:
      - app-network

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - app-network

  redis:
    image: redis:7
    networks:
      - app-network

volumes:
  mysql-data:

networks:
  app-network:
```

### 4.2 Docker Compose 命令

```bash
docker-compose up -d      # 启动所有服务
docker-compose down       # 停止并删除容器
docker-compose ps         # 查看服务状态
docker-compose logs -f    # 查看日志
docker-compose exec app bash  # 进入容器
docker-compose restart app    # 重启服务
```

---

## 五、Docker 网络

### 5.1 网络模式

- **bridge（默认）**：容器通过虚拟网桥通信
- **host**：容器使用主机网络（性能最好，无隔离）
- **none**：无网络
- **container**：共享其他容器的网络

```bash
docker network ls                     # 列出网络
docker network create mynet           # 创建网络
docker run --network=mynet ...        # 指定网络
```

---

## 六、高频面试题汇总

### Docker 面试题

#### **问题 1：Docker 镜像和容器的区别？**

**答：**

- 镜像：只读模板，包含运行应用所需的文件系统和配置
- 容器：镜像的运行实例，有独立的可写层
- 类比：镜像是类，容器是对象

#### **问题 2：COPY 和 ADD 的区别？**

**答：**

- COPY: 只复制本地文件到镜像
- ADD: 除了复制，还支持：
    1. 自动解压 tar 包
    2. 从 URL 下载文件
- 推荐使用 COPY，更明确

#### **问题 3：CMD 和 ENTRYPOINT 的区别？**

**答：**

- CMD: 容器启动时的默认命令，可被 docker run 参数覆盖
- ENTRYPOINT: 固定的执行程序，docker run 参数作为参数传递
- 最佳实践：ENTRYPOINT 定义可执行程序，CMD 定义默认参数

#### **问题 4：如何减小 Docker 镜像体积？**

**答：**

1. 使用更小的基础镜像（alpine）
2. 多阶段构建（multi-stage build）
3. 合并 RUN 命令，减少层数
4. 清理不必要的文件（apt clean）
5. 使用.dockerignore 排除不需要的文件

#### **问题 5：Docker 网络模式有哪些？**

**答：**

- bridge（默认）：虚拟网桥，容器通过 NAT 访问外网
- host：使用主机网络，性能最好但无隔离
- none：无网络
- container：共享其他容器的网络

---

## 总结

本文详细介绍了 Docker 容器化的核心知识点：

1. **核心概念**：镜像、容器、仓库
2. **常用命令**：镜像操作、容器操作
3. **Dockerfile**：编写规范、最佳实践
4. **Docker Compose**：多容器编排
5. **网络模式**：bridge、host、none、container

每个部分都配有高频面试题及参考答案，帮助理解和应对面试。

---

**维护者：** itzixiao  
**最后更新：** 2026-03-21  
**问题反馈：** 欢迎提 Issue 或 PR
