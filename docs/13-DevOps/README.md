# DevOps 知识点详解

## 📚 文档列表

#### 1. [01-DevOps核心知识点详解.md](./01-DevOps%E6%A0%B8%E5%BF%83%E7%9F%A5%E8%AF%86%E7%82%B9%E8%AF%A6%E8%A7%A3.md)
- **内容：** Linux、Docker、Jenkins、Nginx 等运维知识
- **面试题：** 20+ 道
- **重要程度：** ⭐⭐⭐⭐

#### 2. [02-容器化与云原生详解.md](./02-%E5%AE%B9%E5%99%A8%E5%8C%96%E4%B8%8E%E4%BA%91%E5%8E%9F%E7%94%9F%E8%AF%A6%E8%A7%A3.md)
- **内容：** Docker、Kubernetes、Helm、CI/CD 流水线完整实战
- **面试题：** 30+ 道（含详细解答）
- **重要程度：** ⭐⭐⭐⭐⭐
- **代码示例：** `interview-microservices-parent/interview-containerization/`

#### 3. [03-监控与可观测性详解.md](./03-%E7%9B%91%E6%8E%A7%E4%B8%8E%E5%8F%AF%E8%A7%82%E6%B5%8B%E6%80%A7%E8%AF%A6%E8%A7%A3.md)
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

#### 4. [04-性能调优全链路详解.md](./04-%E6%80%A7%E8%83%BD%E8%B0%83%E4%BC%98%E5%85%A8%E9%93%BE%E8%B7%AF%E8%AF%A6%E8%A7%A3.md)
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

#### 5. [05-Service Mesh 服务网格详解.md](./05-Service-Mesh%E6%9C%8D%E5%8A%A1%E7%BD%91%E6%A0%BC%E8%AF%A6%E8%A7%A3.md)
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

## 📊 统计信息

- **文档数：** 5 个
- **面试题总数：** 144+ 道
- **代码示例：** 
  - 容器化：`interview-microservices-parent/interview-containerization/`（~2000 行配置 + 代码）
  - 监控可观测性：`interview-microservices-parent/interview-observability/`（~1500 行代码 + 配置）
  - 性能调优：`interview-microservices-parent/interview-performance-tuning/`（~500 行代码 + 配置）
  - Service Mesh：`interview-microservices-parent/interview-service-mesh/`（~200 行代码 + 配置）

---

## 🎯 学习建议

### 容器化与云原生（4 周）

**第 1 周：Docker 基础**
1. **Docker 安装与配置**
   - Docker Desktop / Docker Engine
   - 镜像仓库配置

2. **镜像构建**
   - Dockerfile 编写
   - 多阶段构建优化
   - 最佳实践

3. **容器操作**
   - 启动、停止、删除
   - 端口映射、数据卷挂载
   - 网络配置

4. **Docker Compose**
   - 多容器编排
   - 服务依赖管理

**第 2 周：Kubernetes 入门**
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

**第 3 周：Kubernetes 进阶**
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

**第 4 周：Helm 与 CI/CD**
1. **Helm 包管理**
   - Chart 开发
   - 多环境管理

2. **CI/CD流水线**
   - GitLab CI
   - Jenkins Pipeline
   - 部署策略（蓝绿、金丝雀）

### Linux 基础（2 天）
1. **常用命令**
   - ps、top、netstat、lsof
   - grep、awk、sed
   - chmod、chown

2. **性能排查**
   - CPU 使用率分析
   - 内存占用查看
   - 磁盘 IO 监控

### Docker 容器化（2 天）
1. **镜像管理**
   - Dockerfile 编写
   - 镜像构建优化

2. **容器操作**
   - 启动、停止、删除
   - 端口映射、数据卷挂载

### Nginx 配置（1 天）
1. **反向代理**
   - upstream 配置
   - 负载均衡策略

2. **静态资源服务**
   - gzip 压缩
   - 缓存配置

### CI/CD（1 天）
1. **Jenkins 流水线**
   - 构建、测试、部署
   - 自动化发布

### 监控与可观测性（1 周）

**第 1-2 天：Prometheus 监控**
1. **Prometheus 架构**
   - 指标采集（Exporter）
   - PromQL 查询语言
   - AlertManager 告警

2. **Spring Boot 集成**
   - Spring Boot Actuator
   - Micrometer 指标采集
   - 自定义业务指标

**第 3-4 天：Grafana 可视化**
1. **Dashboard 配置**
   - 数据源配置
   - 面板创建
   - 告警通知

**第 5 天：SkyWalking APM**
1. **链路追踪原理**
   - Trace、Span 概念
   - Java Agent 配置
   - 性能瓶颈分析

**第 6-7 天：ELK 日志平台**
1. **Filebeat 日志采集**
2. **Logstash 数据处理**
3. **Kibana 可视化**

---

## 🔗 跨模块关联

### 前置知识
- ✅ **[SpringBoot](../05-SpringBoot与自动装配/README.md)** - 应用打包部署
- ✅ **[Linux 基础](./01-DevOps核心知识点详解.md)** - 基本命令操作

### 后续进阶
- 📚 **[Gateway](../06-SpringCloud微服务/README.md)** - Nginx vs Gateway
- 📚 **[分布式系统](../12-分布式系统/README.md)** - 容器化部署
- 📚 **[Service Mesh](05-Service-Mesh服务网格详解.md)** - 服务网格进阶（Istio、Sidecar、流量管理）

### 知识点对应
| DevOps | 应用场景 |
|--------|---------||
| Docker | 应用容器化部署 |
| Kubernetes | 容器编排与管理 |
| Helm | K8s 应用包管理 |
| CI/CD | 自动化构建与部署 |
| Nginx | 反向代理、负载均衡 |
| Jenkins | CI/CD 自动化 |
| Service Mesh | 下一代微服务治理（Istio、Sidecar） |
| Linux | 生产环境运维 |
| Prometheus | 应用监控、告警 |
| Grafana | 数据可视化 |
| SkyWalking | APM 链路追踪 |
| ELK | 日志采集与分析 |

---

## 💡 高频面试题 Top 20

### Docker 相关
1. **Docker 的优势是什么？与虚拟机的区别？**
2. **Dockerfile 的常用指令有哪些？**
3. **如何优化 Docker 镜像大小？**
4. **Docker 的网络模式有哪些？**
5. **Docker 数据卷的作用？**
6. **多阶段构建的优势？**
7. **Docker Compose 的作用？**
8. **容器与宿主机的网络通信原理？**

### Kubernetes 相关
9. **Kubernetes 的核心组件有哪些？**
10. **Pod 的生命周期？**
11. **Deployment 的工作原理？**
12. **Service 的类型和使用场景？**
13. **ConfigMap 和 Secret 的区别？**
14. **HPA 的工作原理？**
15. **K8s 如何实现服务发现？**
16. **滚动更新的过程？**
17. **Pod 探针的种类和作用？**

### CI/CD相关
18. **CI/CD 的流程是怎样的？**
19. **如何实现灰度发布？**
20. **蓝绿部署 vs 金丝雀发布？**

### Helm 相关
21. **Helm 的核心概念？**
22. **Chart 的结构？**
23. **Helm 多环境管理？**

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

### HPA 自动扩缩容示例
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: interview-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: interview-app-deployment
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
    scaleUp:
      stabilizationWindowSeconds: 0
```

---

## 📖 推荐学习顺序

```
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
CI/CD流水线
   ↓
部署策略（蓝绿、金丝雀）
```

---

## 📈 更新日志

### v3.0 - 2026-03-15
- ✅ 新增《监控与可观测性详解》文档
- ✅ 新增 interview-observability 示例模块
- ✅ 补充 Prometheus、Grafana、SkyWalking、ELK 完整示例
- ✅ 补充健康检查与自愈配置
- ✅ 更新高频面试题（70+ 道）
- ✅ 完善学习路线和实战技巧

### v2.1 - 2026-03-08
- ✅ 新增《容器化与云原生详解》文档
- ✅ 新增 interview-containerization 示例模块
- ✅ 补充 Docker、K8s、Helm、CI/CD完整示例
- ✅ 更新高频面试题（30+ 道）
- ✅ 完善学习路线和实战技巧

### v2.0 - 2026-03-08
- ✅ 新增跨模块关联章节
- ✅ 补充 20+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本
- ✅ 基础 DevOps 文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15  
**问题反馈：** 欢迎提 Issue 或 PR
