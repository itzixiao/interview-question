# Nacos 核心知识点详解

## 一、Nacos 概述

### 1.1 什么是 Nacos？

**Nacos** = **N**aming **S**ervice（命名服务）+ **Co**nfiguration **S**ervice（配置服务）

阿里巴巴开源的分布式系统基础设施，用于：
- 服务发现
- 配置管理
- 服务管理

### 1.2 核心功能

| 功能 | 说明 |
|------|------|
| 服务发现 | 自动注册服务，支持 DNS 和 RPC 发现 |
| 服务注册 | 提供者启动时注册到 Nacos |
| 健康检查 | 心跳检测，自动剔除不健康实例 |
| 配置管理 | 集中管理配置，支持热更新 |
| 动态 DNS | 支持加权轮询、一致性哈希等路由策略 |
| 服务元数据 | 支持标签、权重等元数据 |

### 1.3 Nacos 架构角色

**Provider（服务提供者）：**
- 启动时向 Nacos 注册自己的网络地址
- 定期发送心跳维持健康状态

**Consumer（服务消费者）：**
- 从 Nacos 获取服务列表
- 缓存服务列表到本地
- 基于缓存进行负载均衡调用

**Nacos Server（服务端）：**
- 存储服务注册信息
- 健康检查
- 配置存储和管理

### 1.4 Nacos vs Eureka vs Consul vs Zookeeper

| 特性 | Nacos | Eureka | Consul | ZK |
|------|-------|--------|--------|-----|
| CAP | CP/AP | AP | CP | CP |
| 服务健康检查 | 心跳/TCP | 心跳 | 心跳/DNS | 心跳 |
| 负载均衡 | 内置 | 客户端 | 内置 | 客户端 |
| 配置管理 | ✓ | ✗ | ✓ | ✗ |
| 多数据中心 | ✓ | ✗ | ✓ | ✗ |
| Spring Cloud | ✓ | ✓ | ✓ | ✓ |

---

## 二、Nacos 数据模型

### 2.1 Namespace（命名空间）

- **隔离级别**，用于多环境隔离（dev/test/prod）
- 默认：`public`
- 通过 UUID 标识不同命名空间

**示例：**
```yaml
spring:
  cloud:
    nacos:
      discovery:
        namespace: dev-001  # 开发环境
```

### 2.2 Group（分组）

- 同一服务的不同分组
- 默认：`DEFAULT_GROUP`
- 用于灰度发布、业务线隔离

**示例：**
```yaml
spring:
  cloud:
    nacos:
      discovery:
        group: GROUP_V1  # v1 版本服务
```

### 2.3 Service（服务）

- 具体的服务名称
- 例如：`user-service`、`order-service`
- 包含多个实例

### 2.4 Cluster（集群）

- 同一机房或区域的实例集合
- 用于就近访问
- 例如：`BEIJING`、`HANGZHOU`

### 2.5 Instance（实例）

- 具体的服务实例（IP:Port）
- 包含权重、健康状态等元数据

**完整层级：**
```
Namespace (dev-001)
  └── Group (DEFAULT_GROUP)
       └── Service (user-service)
            └── Cluster (BEIJING)
                 └── Instance (192.168.1.100:8080)
```

---

## 三、服务注册与发现

### 3.1 服务注册流程

```
1. 服务提供者启动 → 向 Nacos Server 发送注册请求
2. Nacos Server → 存储服务信息（IP、Port、服务名等）
3. 提供者 → 定期发送心跳（5 秒）
4. 消费者 → 从 Nacos 拉取服务列表
5. 消费者 → 缓存服务列表并定期更新
```

### 3.2 注册参数示例

```java
Properties properties = new Properties();
properties.setProperty("serverAddr", "127.0.0.1:8848");
properties.setProperty("namespace", "public");
properties.setProperty("group", "DEFAULT_GROUP");

// 注册服务
NamingService naming = NamingFactory.createNaming(properties);
naming.registerInstance("user-service", "192.168.1.100", 8080);
```

### 3.3 服务发现流程

```java
// 获取服务实例列表
List<Instance> instances = naming.getAllInstances("user-service");

// 只获取健康实例
List<Instance> healthyInstances = naming.selectInstances("user-service", true);

// 基于元数据筛选
Map<String, String> metadata = new HashMap<>();
metadata.put("version", "v1");
List<Instance> v1Instances = naming.selectInstances("user-service", metadata, true);
```

### 3.4 服务监听机制

```java
// 监听服务变化
naming.subscribe("user-service", event -> {
    List<Instance> instances = ((InstancesChangeEvent) event).instances;
    System.out.println("服务实例变化：" + instances.size());
});
```

### 3.5 临时实例 vs 持久实例

| 类型 | 心跳 | 自动删除 | 适用场景 |
|------|------|----------|----------|
| 临时实例 | 需要 | 是 | 微服务 |
| 持久实例 | 不需要 | 否 | 数据库等永久服务 |

**配置方式：**
```yaml
spring:
  cloud:
    nacos:
      discovery:
        ephemeral: true  # true=临时实例，false=持久实例
```

---

## 四、配置管理

### 4.1 配置管理核心概念

#### Data ID
- 配置文件的唯一标识
- **格式：** `${prefix}-${spring.profile.active}.${file-extension}`
- **例如：** `user-service-dev.yaml`

#### Group
- 配置文件的分组
- 默认：`DEFAULT_GROUP`
- 可用于隔离不同业务线

#### Namespace
- 命名空间，用于环境隔离
- dev、test、prod 分别对应不同 namespace

### 4.2 配置读取示例

```java
Properties properties = new Properties();
properties.setProperty("serverAddr", "127.0.0.1:8848");

// 获取配置服务
ConfigService config = NacosFactory.createConfigService(properties);

// 读取配置
String content = config.getConfig("user-service.yaml", "DEFAULT_GROUP", 5000);

// 发布配置
config.publishConfig("user-service.yaml", "DEFAULT_GROUP", content);
```

### 4.3 配置加载优先级

Spring Cloud Alibaba 配置加载优先级（**从高到低**）：

1. 命令行参数
2. 来自 dataId 为 `${spring.application.name}-${spring.profile.active}.yaml`
3. 来自 dataId 为 `${spring.application.name}.yaml`
4. 来自 dataId 为 `${spring.cloud.nacos.config.prefix}-${spring.profile.active}.yaml`
5. 来自 dataId 为 `${spring.cloud.nacos.config.prefix}.yaml`
6. 来自 dataId 为 `${spring.cloud.nacos.config.file-extension}`
7. 本地配置文件（application.yml）

### 4.4 配置共享

通过 `shared-configs` 实现配置共享：

```yaml
spring:
  cloud:
    nacos:
      config:
        shared-configs:
          - data-id: common-config.yaml
            group: DEFAULT_GROUP
            refresh: true  # 支持热更新
```

---

## 五、服务健康检查

### 5.1 健康检查机制

#### 客户端主动上报（临时实例）
- 服务实例定期发送心跳
- **默认：** 5 秒一次
- **超时：** 15 秒未心跳标记为不健康
- **删除：** 30 秒未心跳删除实例

#### 服务端主动检测
- TCP 连接检测
- HTTP 请求检测
- MySQL 连接检测

### 5.2 心跳配置

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # 心跳间隔（毫秒）
        heart-beat-interval: 5000
        # 心跳超时时间（毫秒）
        heart-beat-timeout: 15000
        # IP 保护时长（毫秒）
        ip-delete-timeout: 30000
```

### 5.3 健康状态流转

```
健康 → 不健康 → 删除
 ↓      ↓         ↓
正常  15 秒超时   30 秒超时
```

### 5.4 保护阈值

**定义：** 健康实例比例的阈值（默认 0.2f）

**作用：**
- 当健康实例比例低于阈值时
- 推送所有实例（包括不健康）
- 防止流量集中导致雪崩

**配置：**
```yaml
spring:
  cloud:
    nacos:
      discovery:
        protect-threshold: 0.2  # 保护阈值
```

---

## 六、负载均衡

### 6.1 客户端负载均衡

**原理：**
- 服务消费者维护服务列表
- 基于本地缓存选择实例
- Spring Cloud LoadBalancer

### 6.2 负载均衡策略

| 策略 | 说明 |
|------|------|
| 随机（Random） | 随机选择一个实例 |
| 轮询（RoundRobin） | 按顺序轮流选择 |
| 权重（Weighted） | 根据实例权重选择 |
| 最小连接数 | 选择当前连接数最少的实例 |
| 一致性 Hash | 相同参数的请求总是发到同一实例 |
| 本地优先 | 优先选择同机房的实例 |

### 6.3 权重配置

```java
// 设置实例权重
Instance instance = new Instance();
instance.setIp("192.168.1.100");
instance.setPort(8080);
instance.setWeight(2.0); // 权重是其他实例的 2 倍
naming.registerInstance("user-service", instance);
```

### 6.4 元数据过滤

```java
// 基于元数据筛选实例
Map<String, String> metadata = new HashMap<>();
metadata.put("version", "v1");
metadata.put("region", "cn-hangzhou");

// 只选择 v1 版本的实例
List<Instance> instances = naming.selectInstances("user-service", metadata, true);
```

---

## 七、多环境配置

### 7.1 Namespace 隔离方案

| 环境 | Namespace ID | 配置文件 | 服务集群 |
|------|-------------|---------|---------|
| 开发 | dev-001 | user-service-dev.yaml | dev cluster |
| 测试 | test-002 | user-service-test.yaml | test cluster |
| 生产 | prod-003 | user-service-prod.yaml | prod cluster |

### 7.2 配置文件命名规范

| 文件 | 用途 |
|------|------|
| `${service-name}.yaml` | 默认配置（所有环境共享） |
| `${service-name}-dev.yaml` | 开发环境配置 |
| `${service-name}-test.yaml` | 测试环境配置 |
| `${service-name}-prod.yaml` | 生产环境配置 |

### 7.3 配置示例

```yaml
# application.yml
spring:
  profiles:
    active: dev  # 通过命令行切换：--spring.profiles.active=prod
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml
        prefix: ${spring.application.name}
        namespace: ${spring.profiles.active}-001
```

---

## 八、配置热更新

### 8.1 热更新原理

```
1. 客户端监听配置变化（长轮询）
2. Nacos Server 配置变更时，通知客户端
3. 客户端重新拉取配置
4. 刷新 Spring 上下文中的 Bean
```

### 8.2 监听器配置

```java
// 添加配置监听器
config.addListener("user-service.yaml", "DEFAULT_GROUP", new Listener() {
    @Override
    public void receiveConfigInfo(String configInfo) {
        System.out.println("配置已更新：" + configInfo);
        // 处理配置变更逻辑
    }
    
    @Override
    public Executor getExecutor() {
        return null; // 使用内部线程池
    }
});
```

### 8.3 @RefreshScope 注解

```java
@RestController
@RefreshScope  // 配置更新时自动刷新 Bean
public class ConfigController {
    
    @Value("${config.version:1.0.0}")
    private String configVersion;
    
    @GetMapping("/config/version")
    public String getConfigVersion() {
        return configVersion;
    }
}
```

### 8.4 热更新注意事项

**✓ 适合热更新的配置：**
- 业务开关（feature flag）
- 限流阈值
- 日志级别
- 超时时间

**✗ 不适合热更新的配置：**
- 数据库连接池核心参数
- 端口号
- 涉及 Bean 生命周期的配置

---

## 九、高频面试题

**问题 1：什么是 Nacos？它有哪些核心功能？**

**答：**

**Nacos** = **N**aming **S**ervice + **Co**nfiguration **S**ervice

**核心功能：**
1. 服务发现与服务注册
2. 健康检查
3. 配置管理（支持热更新）
4. 动态 DNS 服务
5. 服务元数据管理

**问题 2:Nacos 相比 Eureka 有什么优势？**

**答：**

1. **CAP 灵活切换**：Nacos 支持 CP 或 AP，Eureka 只支持 AP
2. **配置管理**：Nacos 支持，Eureka 不支持
3. **多数据中心**：Nacos 支持，Eureka 不支持
4. **健康检查**：Nacos 更丰富（TCP/HTTP/MySQL）
5. **负载均衡**：Nacos 内置，Eureka 需结合 Ribbon
6. **社区活跃**：阿里背书，更新快

**问题 3:Nacos 的数据模型是什么？**

**答：**

四层结构：

1. **Namespace（命名空间）** - 环境隔离
2. **Group（分组）** - 业务分组
3. **Service（服务）** - 具体服务名
4. **Instance（实例）** - IP:Port

**示例：**
```
Namespace: dev-001
  └── Group: DEFAULT_GROUP
       └── Service: user-service
            └── Instance: 192.168.1.100:8080
```

**问题 4:Nacos 服务注册流程是什么？**

**答：**

1. 提供者启动时向 Nacos 发送注册请求
2. Nacos 存储服务信息（IP、Port、服务名等）
3. 提供者定期发送心跳（5 秒）
4. 消费者从 Nacos 拉取服务列表
5. 消费者缓存服务列表并定期更新

**问题 5:Nacos 如何保证高可用？**

**答：**

1. **Nacos Server 集群部署**
2. **数据 Raft 协议同步**（CP 模式）
3. **客户端缓存服务列表**（本地容灾）
4. **自动故障转移**
5. **保护阈值机制**（防止雪崩）

**问题 6：临时实例和持久实例的区别？**

**答：**

**临时实例：**
- 需要定期发送心跳
- 超时会被删除
- 适用于微服务

**持久实例：**
- 不需要心跳
- 不会被自动删除
- 适用于数据库等永久服务

**问题 7:Nacos 配置管理的 DataID 组成规则？**

**答：**

**公式：** `${prefix}-${spring.profile.active}.${file-extension}`

**例如：** `user-service-dev.yaml`

- **prefix：** 默认为 `spring.application.name`
- **active：** 当前激活的 profile（dev/test/prod）
- **extension：** 配置文件类型（yaml/properties）

**问题 8:Nacos 配置加载优先级是什么？**

**答：**

从高到低：

1. 命令行参数
2. `${spring.application.name}-${profile}.yaml`
3. `${spring.application.name}.yaml`
4. `${prefix}-${profile}.yaml`
5. `${prefix}.yaml`
6. 本地配置文件

**问题 9:Nacos 配置热更新的原理？**

**答：**

1. **客户端长轮询监听**配置变化
2. **Nacos 配置变更**时通知客户端
3. **客户端重新拉取**配置
4. **@RefreshScope 刷新** Bean
5. **新请求使用新配置**

**问题 10:Nacos 的健康检查机制？**

**答：**

**1. 客户端主动上报（临时实例）：**
- 默认 5 秒一次心跳
- 15 秒未心跳标记为不健康
- 30 秒未心跳删除实例

**2. 服务端主动检测：**
- TCP 连接检测
- HTTP 请求检测

**问题 11：什么是保护阈值？有什么作用？**

**答：**

**定义：** 健康实例比例的阈值（默认 0.2f）

**作用：**
- 当健康实例比例低于阈值时
- 推送所有实例（包括不健康）
- 防止流量集中导致雪崩

**问题 12:Nacos 支持哪些负载均衡策略？**

**答：**

1. 随机（Random）
2. 轮询（RoundRobin）
3. 权重（Weighted）
4. 最小连接数
5. 一致性 Hash
6. 本地优先（同机房优先）

**问题 13：如何使用 Nacos 实现多环境隔离？**

**答：**

1. 通过 **Namespace** 隔离（dev/test/prod）
2. 每个环境对应不同的 Namespace ID
3. 配置文件命名：`${service}-${env}.yaml`
4. 通过 `spring.profiles.active` 切换环境

**问题 14:Nacos 配置共享如何实现？**

**答：**

通过 `shared-configs` 配置：

```yaml
spring:
  cloud:
    nacos:
      config:
        shared-configs:
          - data-id: common-config.yaml
            group: DEFAULT_GROUP
            refresh: true
```

**问题 15:@RefreshScope的作用和原理？**

**答：**

**作用：** 配置更新时自动刷新 Bean

**原理：**
1. 创建代理 Bean
2. 配置变更时销毁原 Bean
3. 下次请求重新创建 Bean
4. 新 Bean 使用新配置

---

## 十、最佳实践

### 10.1 服务注册配置

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: ${spring.profiles.active}
        group: DEFAULT_GROUP
        # 服务实例名称
        instance-enabled: true
        # 元数据
        metadata:
          version: v1.0.0
          region: cn-hangzhou
```

### 10.2 配置中心配置

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: ${spring.profiles.active}
        file-extension: yaml
        # 配置前缀
        prefix: ${spring.application.name}
        # 配置共享
        shared-configs:
          - data-id: common-config.yaml
            refresh: true
```

### 10.3 监控与告警

- 监控 Nacos Server CPU/内存
- 监控服务注册数量
- 监控配置变更频率
- 设置健康检查失败告警

---

**最后更新：** 2026-03-07  
**维护者：** itzixiao
