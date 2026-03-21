# DevOps 知识点详解

## 📚 文档列表

#### 1. [01-Linux与Shell核心知识点详解.md](./01-Linux%E4%B8%8EShell%E6%A0%B8%E5%BF%83%E7%9F%A5%E8%AF%86%E7%82%B9%E8%AF%A6%E8%A7%A3.md)

- **内容：** Linux 文件权限、进程管理、网络命令、Shell 脚本编程
- **子主题**：
    - Linux：文件操作、权限管理、进程管理、日志查看
    - Shell：变量、条件判断、循环、函数、实战脚本
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐

---

#### 2. [02-Docker容器化详解.md](./02-Docker%E5%AE%B9%E5%99%A8%E5%8C%96%E8%AF%A6%E8%A7%A3.md)

- **内容：** Docker 核心概念、常用命令、Dockerfile 编写、Docker Compose
- **子主题**：
    - Docker：镜像、容器、仓库
    - Dockerfile：多阶段构建、最佳实践
    - Docker Compose：多容器编排
    - 网络模式：bridge、host、none
- **面试题：** 5+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

---

#### 3. [03-Jenkins-CICD详解.md](./03-Jenkins-CI/CD%E8%AF%A6%E8%A7%A3.md)

- **内容：** CI/CD 概念、Jenkins Pipeline、触发方式
- **子主题**：
    - CI/CD：持续集成、持续交付、持续部署
    - Pipeline：声明式语法、阶段定义
    - 触发方式：手动、定时、Webhook、轮询
- **面试题：** 3+ 道
- **重要程度：** ⭐⭐⭐⭐

---

#### 4. [04-容器化与云原生详解.md](./04-%E5%AE%B9%E5%99%A8%E5%8C%96%E4%B8%8E%E4%BA%91%E5%8E%9F%E7%94%9F%E8%AF%A6%E8%A7%A3.md)

- **内容：** Kubernetes、Helm、CI/CD 流水线完整实战
- **面试题：** 30+ 道（含详细解答）
- **重要程度：** ⭐⭐⭐⭐⭐
- **代码示例：** `interview-microservices-parent/interview-containerization/`

---

#### 5. [05-监控与可观测性详解.md](./05-%E7%9B%91%E6%8E%A7%E4%B8%8E%E5%8F%AF%E8%A7%82%E6%B5%8B%E6%80%A7%E8%AF%A6%E8%A7%A3.md)

- **内容：** Prometheus、Grafana、SkyWalking、ELK、健康检查与自愈
- **子主题**：
    - Prometheus：指标采集、PromQL、告警规则
    - Grafana：可视化 Dashboard
    - SkyWalking：APM 链路追踪
    - ELK：日志平台
    - K8s：健康检查探针、HPA 自动扩缩容
- **代码示例：** `interview-microservices-parent/interview-observability/`
- **面试题：** 20+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

---

#### 6. [06-性能调优全链路详解.md](./06-%E6%80%A7%E8%83%BD%E8%B0%83%E4%BC%98%E5%85%A8%E9%93%BE%E8%B7%AF%E8%AF%A6%E8%A7%A3.md)

- **内容：** JVM 调优、MySQL 优化、Redis 优化、Tomcat 调优、Linux 系统优化
- **子主题**：
    - JVM：GC 日志分析、MAT/JProfiler 工具、内存泄漏排查
    - MySQL：Explain 执行计划、慢查询优化、锁等待分析
    - Redis：大 Key/热 Key 处理、集群性能监控
    - Tomcat：线程池配置、NIO 连接器优化
    - Linux：CPU/内存/磁盘 IO、网络参数优化
- **代码示例：** `interview-microservices-parent/interview-performance-tuning/`
- **面试题：** 38+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

---

#### 7. [07-Service Mesh 服务网格详解.md](./07-Service-Mesh%E6%9C%8D%E5%8A%A1%E7%BD%91%E6%A0%BC%E8%AF%A6%E8%A7%A3.md)

- **内容：** Istio 架构、Sidecar 模式、流量管理、策略执行、可观测性增强
- **子主题**：
    - Istio：控制平面（Pilot/Mixer/Citadel/Galley）、数据平面（Envoy）
    - Sidecar：透明代理、自动注入、iptables 拦截
    - 流量管理：VirtualService、DestinationRule、Gateway、金丝雀发布、蓝绿部署
    - 安全：mTLS 双向认证、JWT 认证、RBAC 授权、限流策略
    - 可观测性：Prometheus 指标、Jaeger 链路追踪、访问日志
- **代码示例：** `interview-microservices-parent/interview-service-mesh/`（~200 行代码 + 配置）
- **面试题：** 36+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

---

#### 8. [08-Tomcat详解.md](./08-Tomcat%E8%AF%A6%E8%A7%A3.md) ⭐ NEW

- **内容：** Tomcat 架构、连接器模式、线程模型、类加载机制、调优实战
- **子主题**：
    - 架构组件：Server、Service、Connector、Engine、Host、Context、Wrapper
    - 连接器模式：BIO、NIO、APR 对比与配置
    - 线程池：配置参数、线程数计算公式
    - 类加载机制：打破双亲委派模型
    - 调优实战：内存配置、连接器调优、性能监控
- **代码示例：** `interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/tomcat/`
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐

---

#### 9. [09-Nginx详解.md](./09-Nginx%E8%AF%A6%E8%A7%A3.md) ⭐ NEW

- **内容：** Nginx 架构、负载均衡、反向代理、HTTPS 配置、高可用方案
- **子主题**：
    - 架构：Master-Worker 进程模型、事件驱动机制
    - 负载均衡：轮询、权重、IP Hash、最少连接策略
    - 反向代理：配置详解、动静分离
    - HTTPS：SSL/TLS 配置、证书管理
    - 联合部署：Nginx + Tomcat 架构、SSL 终止
- **面试题：** 7+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

---

## 📊 统计信息

- **文档数：** 9 个
- **面试题总数：** 170+ 道
- **代码示例：**
    - 容器化：`interview-microservices-parent/interview-containerization/`（~2000 行配置 + 代码）
    - 监控可观测性：`interview-microservices-parent/interview-observability/`（~1500 行代码 + 配置）
    - 性能调优：`interview-microservices-parent/interview-performance-tuning/`（~500 行代码 + 配置）
    - Service Mesh：`interview-microservices-parent/interview-service-mesh/`（~200 行代码 + 配置）

---

## 🎯 学习建议

### 基础篇（1 周）

**第 1-2 天：Linux 基础**

1. **常用命令**
    - ps、top、netstat、lsof
    - grep、awk、sed
    - chmod、chown

2. **性能排查**
    - CPU 使用率分析
    - 内存占用查看
    - 磁盘 IO 监控

**第 3-4 天：Shell 脚本**

1. **基础语法**
    - 变量、条件判断、循环
    - 函数定义与调用

2. **实战脚本**
    - Java 应用部署脚本
    - 日志分析脚本

**第 5-7 天：Docker 容器化**

1. **镜像管理**
    - Dockerfile 编写
    - 镜像构建优化

2. **容器操作**
    - 启动、停止、删除
    - 端口映射、数据卷挂载

3. **Docker Compose**
    - 多容器编排
    - 服务依赖管理

---

### 进阶篇（4 周）

**第 1 周：Kubernetes 入门**

1. **K8s 架构**
    - Control Plane 组件
    - Worker Node 组件

2. **核心资源**
    - Pod、Deployment、Service
    - ConfigMap、Secret
    - Namespace

3. **健康检查**
    - livenessProbe
    - readinessProbe
    - startupProbe

**第 2 周：Kubernetes 进阶**

1. **自动扩缩容**
    - HPA 配置
    - 自定义指标

2. **高级调度**
    - 节点选择器
    - 亲和性与反亲和性
    - 污点与容忍

3. **有状态应用**
    - StatefulSet
    - PersistentVolume

**第 3 周：Helm 与 CI/CD**

1. **Helm 包管理**
    - Chart 开发
    - 多环境管理

2. **CI/CD流水线**
    - Jenkins Pipeline
    - GitLab CI
    - 部署策略（蓝绿、金丝雀）

**第 4 周：监控与可观测性**

1. **Prometheus 监控**
    - 指标采集（Exporter）
    - PromQL 查询语言
    - AlertManager 告警

2. **Grafana 可视化**
    - 数据源配置
    - Dashboard 创建

3. **SkyWalking APM**
    - 链路追踪原理
    - Java Agent 配置

---

### 高级篇（2 周）

**第 1 周：性能调优**

1. **JVM 调优**
    - GC 日志分析
    - 内存泄漏排查

2. **MySQL 优化**
    - Explain 执行计划
    - 慢查询优化

3. **Tomcat 调优**
    - 线程池配置
    - NIO 连接器优化

**第 2 周：Service Mesh**

1. **Istio 架构**
    - 控制平面与数据平面
    - Sidecar 模式

2. **流量管理**
    - 金丝雀发布
    - 蓝绿部署

---

## 🔗 跨模块关联

### 前置知识

- ✅ **[SpringBoot](../06-SpringBoot与自动装配/README.md)** - 应用打包部署
- ✅ **[Linux 基础](./01-Linux与Shell核心知识点详解.md)** - 基本命令操作

### 后续进阶

- 📚 **[Gateway](../07-SpringCloud微服务/README.md)** - Nginx vs Gateway
- 📚 **[分布式系统](../14-分布式系统/README.md)** - 容器化部署
- 📚 **[Service Mesh](./07-Service-Mesh服务网格详解.md)** - 服务网格进阶（Istio、Sidecar、流量管理）

### 知识点对应

| DevOps | 应用场景 |
|--------|---------||
| Linux | 生产环境运维 |
| Shell | 自动化脚本编写 |
| Docker | 应用容器化部署 |
| Kubernetes | 容器编排与管理 |
| Helm | K8s 应用包管理 |
| CI/CD | 自动化构建与部署 |
| Nginx | 反向代理、负载均衡 |
| Jenkins | CI/CD 自动化 |
| Service Mesh | 下一代微服务治理（Istio、Sidecar） |
| Prometheus | 应用监控、告警 |
| Grafana | 数据可视化 |
| SkyWalking | APM 链路追踪 |
| ELK | 日志采集与分析 |

---

## 💡 高频面试题 Top 20

### Linux/Shell 相关

1. **如何查看某个端口被哪个进程占用？**
2. **chmod 755 是什么意思？**
3. **kill 和 kill -9 的区别？**
4. **如何让程序后台运行，且关闭终端后继续运行？**
5. **Shell 中单引号和双引号的区别？**

### Docker 相关

6. **Docker 的优势是什么？与虚拟机的区别？**
7. **Dockerfile 的常用指令有哪些？**
8. **如何优化 Docker 镜像大小？**
9. **Docker 的网络模式有哪些？**
10. **COPY 和 ADD 的区别？**

### Kubernetes 相关

11. **Kubernetes 的核心组件有哪些？**
12. **Pod 的生命周期？**
13. **Deployment 的工作原理？**
14. **Service 的类型和使用场景？**
15. **HPA 的工作原理？**

### CI/CD相关

16. **什么是 CI/CD？**
17. **Jenkins Pipeline 有哪两种语法？**
18. **如何实现灰度发布？**
19. **蓝绿部署 vs 金丝雀发布？**

### Service Mesh 相关

20. **Istio 的核心组件有哪些？**

---

## 🛠️ 实战技巧

### Dockerfile 示例

```dockerfile
# 多阶段构建优化版
FROM maven:3.8-openjdk-8 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM openjdk:8-jre-slim
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /app
COPY --from=builder /app/target/app.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC"
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Kubernetes Deployment 示例

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: interview-app
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: interview-app
  template:
    metadata:
      labels:
        app: interview-app
    spec:
      containers:
      - name: interview-app
        image: interview-containerization:1.0.0
        ports:
        - containerPort: 8080
        resources:
          requests:
            cpu: "250m"
            memory: "512Mi"
          limits:
            cpu: "500m"
            memory: "1Gi"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

## 📖 推荐学习顺序

```
Linux 基础
   ↓
Shell 脚本
   ↓
Docker 基础
   ↓
Dockerfile 编写
   ↓
Docker Compose 编排
   ↓
Kubernetes 入门
   ↓
Pod、Deployment、Service
   ↓
ConfigMap、Secret
   ↓
HPA 自动扩缩容
   ↓
Helm 包管理
   ↓
Jenkins CI/CD
   ↓
部署策略（蓝绿、金丝雀）
   ↓
监控与可观测性
   ↓
Service Mesh
```

---

## 📈 更新日志

### v5.0 - 2026-03-21（本次更新）

- ✅ 拆分《01-DevOps核心知识点详解.md》为 3 个独立文档
- ✅ 新增《01-Linux与Shell核心知识点详解.md》
- ✅ 新增《02-Docker容器化详解.md》
- ✅ 新增《03-Jenkins-CI/CD详解.md》
- ✅ 拆分《08-Tomcat与Nginx详解.md》为两个独立文档
- ✅ 新增《08-Tomcat详解.md》- Tomcat 架构、连接器、类加载机制
- ✅ 新增《09-Nginx详解.md》- Nginx 架构、负载均衡、反向代理
- ✅ 更新文档编号和索引
- ✅ 更新统计信息（8 个文档，155+ 面试题）

### v4.0 - 2026-03-21

- ✅ 新增《06-Tomcat与Nginx详解》文档（已拆分）
- ✅ 包含 Tomcat 架构、连接器模式、线程模型详解
- ✅ 包含 Nginx 进程模型、负载均衡、反向代理配置
- ✅ 配套代码：Tomcat 监控、性能统计拦截器

### v3.0 - 2026-03-15

- ✅ 新增《监控与可观测性详解》文档
- ✅ 新增 interview-observability 示例模块
- ✅ 补充 Prometheus、Grafana、SkyWalking、ELK 完整示例

---

**维护者：** itzixiao  
**最后更新：** 2026-03-21  
**问题反馈：** 欢迎提 Issue 或 PR
