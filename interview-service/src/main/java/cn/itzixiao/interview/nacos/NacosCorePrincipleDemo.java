package cn.itzixiao.interview.nacos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Nacos 核心知识点详解 - 面试高频考点
 * 
 * <pre>
 * 涵盖内容：
 * 1. Nacos 概述（注册中心 + 配置中心）
 * 2. 服务注册与发现
 * 3. 配置管理
 * 4. 服务健康检查
 * 5. 负载均衡
 * 6. 多环境配置
 * 7. 配置热更新
 * </pre>
 * 
 * @author itzixiao
 */
public class NacosCorePrincipleDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    Nacos 核心知识点详解                                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝\n");
        
        // 第一部分：Nacos 概述
        demonstrateNacosOverview();
        
        // 第二部分：服务注册与发现
        demonstrateServiceRegistry();
        
        // 第三部分：配置管理
        demonstrateConfigManagement();
        
        // 第四部分：服务健康检查
        demonstrateHealthCheck();
        
        // 第五部分：负载均衡
        demonstrateLoadBalance();
        
        // 第六部分：多环境配置
        demonstrateMultiEnvironment();
        
        // 第七部分：配置热更新
        demonstrateHotReload();
        
        // 第八部分：高频面试题
        printInterviewQuestions();
    }
    
    // ==================== 第一部分：Nacos 概述 ====================
    
    private static void demonstrateNacosOverview() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第一部分：Nacos 概述                                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【1.1 什么是 Nacos？】\n");
        System.out.println("Nacos = Naming Service（命名服务）+ Configuration Service（配置服务）");
        System.out.println("阿里巴巴开源，用于服务发现、配置管理和服务管理\n");
        
        System.out.println("【1.2 Nacos 核心功能】\n");
        System.out.println("┌─────────────────┬────────────────────────────────────────────────────┐");
        System.out.println("│ 功能            │ 说明                                               │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────┤");
        System.out.println("│ 服务发现        │ 自动注册服务，支持 DNS 和 RPC 发现                      │");
        System.out.println("│ 服务注册        │ 提供者启动时注册到 Nacos                            │");
        System.out.println("│ 健康检查        │ 心跳检测，自动剔除不健康实例                        │");
        System.out.println("│ 配置管理        │ 集中管理配置，支持热更新                            │");
        System.out.println("│ 动态 DNS        │ 支持加权轮询、一致性哈希等路由策略                  │");
        System.out.println("│ 服务元数据      │ 支持标签、权重等元数据                              │");
        System.out.println("└─────────────────┴────────────────────────────────────────────────────┘\n");
        
        System.out.println("【1.3 Nacos 架构角色】\n");
        System.out.println("Provider（服务提供者）：");
        System.out.println("- 启动时向 Nacos 注册自己的网络地址");
        System.out.println("- 定期发送心跳维持健康状态\n");
        
        System.out.println("Consumer（服务消费者）：");
        System.out.println("- 从 Nacos 获取服务列表");
        System.out.println("- 缓存服务列表到本地");
        System.out.println("- 基于缓存进行负载均衡调用\n");
        
        System.out.println("Nacos Server（服务端）：");
        System.out.println("- 存储服务注册信息");
        System.out.println("- 健康检查");
        System.out.println("- 配置存储和管理\n");
        
        System.out.println("【1.4 Nacos vs Eureka vs Consul vs Zookeeper】\n");
        System.out.println("┌──────────────┬──────────┬──────────┬──────────┬──────────┐");
        System.out.println("│ 特性         │ Nacos    │ Eureka   │ Consul   │ ZK       │");
        System.out.println("├──────────────┼──────────┼──────────┼──────────┼──────────┤");
        System.out.println("│ CAP          │ CP/AP    │ AP       │ CP       │ CP       │");
        System.out.println("│ 服务健康检查 │ 心跳/TCP │ 心跳     │ 心跳/DNS │ 心跳     │");
        System.out.println("│ 负载均衡     │ 内置     │ 客户端   │ 内置     │ 客户端   │");
        System.out.println("│ 配置管理     │ ✓        │ ✗        │ ✓        │ ✗        │");
        System.out.println("│ 多数据中心   │ ✓        │ ✗        │ ✓        │ ✗        │");
        System.out.println("│ Spring Cloud │ ✓        │ ✓        │ ✓        │ ✓        │");
        System.out.println("└──────────────┴──────────┴──────────┴──────────┴──────────┘\n");
        
        System.out.println("【1.5 Nacos 数据模型】\n");
        System.out.println("Namespace（命名空间）：");
        System.out.println("- 隔离级别，用于多环境隔离（dev/test/prod）");
        System.out.println("- 默认 public\n");
        
        System.out.println("Group（分组）：");
        System.out.println("- 同一服务的不同分组（DEFAULT_GROUP）");
        System.out.println("- 用于灰度发布\n");
        
 System.out.println("Service（服务）：");
        System.out.println("- 具体的服务名称（user-service、order-service）");
        System.out.println("- 包含多个实例\n");
        
        System.out.println("Cluster（集群）：");
        System.out.println("- 同一机房或区域的实例集合");
        System.out.println("- 用于就近访问\n");
        
        System.out.println("Instance（实例）：");
        System.out.println("- 具体的服务实例（IP:Port）");
        System.out.println("- 包含权重、健康状态等元数据\n");
    }
    
    // ==================== 第二部分：服务注册与发现 ====================
    
    private static void demonstrateServiceRegistry() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第二部分：服务注册与发现                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【2.1 服务注册流程】\n");
        System.out.println("1. 服务提供者启动时，向 Nacos Server 发送注册请求");
        System.out.println("2. Nacos Server 存储服务信息（IP、Port、服务名等）");
        System.out.println("3. 服务消费者从 Nacos Server 拉取服务列表");
        System.out.println("4. 消费者缓存服务列表，并定期更新\n");
        
        System.out.println("【2.2 注册参数示例】\n");
        System.out.println("Properties properties = new Properties();");
        System.out.println("properties.setProperty(\"serverAddr\", \"127.0.0.1:8848\");");
        System.out.println("properties.setProperty(\"namespace\", \"public\");");
        System.out.println("properties.setProperty(\"group\", \"DEFAULT_GROUP\");");
        System.out.println("");
        System.out.println("// 注册服务");
        System.out.println("NamingService naming = NamingFactory.createNaming(properties);");
        System.out.println("naming.registerInstance(\"user-service\", \"192.168.1.100\", 8080);\n");
        
        System.out.println("【2.3 服务发现流程】\n");
        System.out.println("1. 消费者通过服务名查询实例列表");
        System.out.println("2. Nacos 返回健康的实例列表");
        System.out.println("3. 消费者基于负载均衡算法选择一个实例");
        System.out.println("4. 发起远程调用\n");
        
        System.out.println("【2.4 服务发现示例】\n");
        System.out.println("// 获取服务实例列表");
        System.out.println("List<Instance> instances = naming.getAllInstances(\"user-service\");");
        System.out.println("");
        System.out.println("// 只获取健康实例");
        System.out.println("List<Instance> healthyInstances = naming.selectInstances(\"user-service\", true);\n");
        
        System.out.println("【2.5 服务监听机制】\n");
        System.out.println("// 监听服务变化");
        System.out.println("naming.subscribe(\"user-service\", event -> {");
        System.out.println("    List<Instance> instances = ((InstancesChangeEvent) event).instances;");
        System.out.println("    System.out.println(\"服务实例变化：\" + instances.size());");
        System.out.println("});\n");
        
        System.out.println("【2.6 临时实例 vs 持久实例】\n");
        System.out.println("临时实例（Ephemeral）：");
        System.out.println("- 需要定期发送心跳");
        System.out.println("- 超时未心跳会被删除");
        System.out.println("- 适用于微服务场景\n");
        
        System.out.println("持久实例（Persistent）：");
        System.out.println("- 不需要心跳");
        System.out.println("- 不会被自动删除");
        System.out.println("- 适用于数据库等永久服务\n");
    }
    
    // ==================== 第三部分：配置管理 ====================
    
    private static void demonstrateConfigManagement() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第三部分：配置管理                                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【3.1 配置管理核心概念】\n");
        System.out.println("Data ID：");
        System.out.println("- 配置文件的唯一标识");
        System.out.println("- 格式：${prefix}-${spring.profile.active}.${file-extension}");
        System.out.println("- 例如：user-service-dev.yaml\n");
        
        System.out.println("Group：");
        System.out.println("- 配置文件的分组");
        System.out.println("- 默认 DEFAULT_GROUP");
        System.out.println("- 可用于隔离不同业务线\n");
        
        System.out.println("Namespace：");
        System.out.println("- 命名空间，用于环境隔离");
        System.out.println("- dev、test、prod 分别对应不同 namespace\n");
        
        System.out.println("【3.2 配置读取示例】\n");
        System.out.println("Properties properties = new Properties();");
        System.out.println("properties.setProperty(\"serverAddr\", \"127.0.0.1:8848\");");
        System.out.println("");
        System.out.println("// 获取配置服务");
        System.out.println("ConfigService config = NacosFactory.createConfigService(properties);");
        System.out.println("");
        System.out.println("// 读取配置");
        System.out.println("String content = config.getConfig(\"user-service.yaml\", \"DEFAULT_GROUP\", 5000);");
        System.out.println("");
        System.out.println("// 发布配置");
        System.out.println("config.publishConfig(\"user-service.yaml\", \"DEFAULT_GROUP\", content);\n");
        
        System.out.println("【3.3 配置优先级】\n");
        System.out.println("Spring Cloud Alibaba 配置加载优先级（从高到低）：");
        System.out.println("1. 命令行参数");
        System.out.println("2. 来自 dataId 为 ${spring.application.name}-${spring.profile.active}.yaml");
        System.out.println("3. 来自 dataId 为 ${spring.application.name}.yaml");
        System.out.println("4. 来自 dataId 为 ${spring.cloud.nacos.config.prefix}-${spring.profile.active}.yaml");
        System.out.println("5. 来自 dataId 为 ${spring.cloud.nacos.config.prefix}.yaml");
        System.out.println("6. 来自 dataId 为 ${spring.cloud.nacos.config.file-extension}");
        System.out.println("7. 本地配置文件（application.yml）\n");
        
        System.out.println("【3.4 配置共享】\n");
        System.out.println("// 通过 shared-configs 实现配置共享");
        System.out.println("spring:");
        System.out.println("  cloud:");
        System.out.println("    nacos:");
        System.out.println("      config:");
        System.out.println("        shared-configs[0]:");
        System.out.println("          data-id: common-config.yaml");
        System.out.println("          group: DEFAULT_GROUP");
        System.out.println("          refresh: true\n");
    }
    
    // ==================== 第四部分：服务健康检查 ====================
    
    private static void demonstrateHealthCheck() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第四部分：服务健康检查                                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【4.1 健康检查机制】\n");
        System.out.println("客户端主动上报（临时实例）：");
        System.out.println("- 服务实例定期发送心跳");
        System.out.println("- 默认 5 秒一次");
        System.out.println("- 超过 15 秒未心跳标记为不健康");
        System.out.println("- 超过 30 秒未心跳删除实例\n");
        
        System.out.println("服务端主动检测：");
        System.out.println("- TCP 连接检测");
        System.out.println("- HTTP 请求检测");
        System.out.println("- MySQL 连接检测\n");
        
        System.out.println("【4.2 心跳配置】\n");
        System.out.println("spring:");
        System.out.println("  cloud:");
        System.out.println("    nacos:");
        System.out.println("      discovery:");
        System.out.println("        # 心跳间隔（秒）");
        System.out.println("        heart-beat-interval: 5000");
        System.out.println("        # 心跳超时时间（秒）");
        System.out.println("        heart-beat-timeout: 15000");
        System.out.println("        # IP 保护时长（秒）");
        System.out.println("        ip-delete-timeout: 30000\n");
        
        System.out.println("【4.3 健康状态流转】\n");
        System.out.println("健康 → 不健康 → 删除");
        System.out.println("  ↓        ↓         ↓");
        System.out.println("正常    15 秒超时   30 秒超时");
        System.out.println("");
        System.out.println("保护阈值：");
        System.out.println("- 当健康实例比例低于阈值时，推送所有实例（包括不健康）");
        System.out.println("- 防止流量全部打到少数健康实例导致雪崩\n");
    }
    
    // ==================== 第五部分：负载均衡 ====================
    
    private static void demonstrateLoadBalance() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第五部分：负载均衡                                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【5.1 客户端负载均衡】\n");
        System.out.println("原理：");
        System.out.println("- 服务消费者维护服务列表");
        System.out.println("- 基于本地缓存选择实例");
        System.out.println("- Spring Cloud LoadBalancer\n");
        
        System.out.println("【5.2 负载均衡策略】\n");
        System.out.println("┌─────────────────┬────────────────────────────────────────────────────┐");
        System.out.println("│ 策略            │ 说明                                               │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────┤");
        System.out.println("│ 随机（Random）  │ 随机选择一个实例                                   │");
        System.out.println("│ 轮询（RoundRobin）│ 按顺序轮流选择                                     │");
        System.out.println("│ 权重（Weighted）│ 根据实例权重选择                                   │");
        System.out.println("│ 最小连接数      │ 选择当前连接数最少的实例                           │");
        System.out.println("│ 一致性 Hash     │ 相同参数的请求总是发到同一实例                     │");
        System.out.println("│ 本地优先        │ 优先选择同机房的实例                               │");
        System.out.println("└─────────────────┴────────────────────────────────────────────────────┘\n");
        
        System.out.println("【5.3 权重配置】\n");
        System.out.println("// 设置实例权重");
        System.out.println("Instance instance = new Instance();");
        System.out.println("instance.setIp(\"192.168.1.100\");");
        System.out.println("instance.setPort(8080);");
        System.out.println("instance.setWeight(2.0); // 权重是其他实例的 2 倍");
        System.out.println("naming.registerInstance(\"user-service\", instance);\n");
        
        System.out.println("【5.4 元数据过滤】\n");
        System.out.println("// 基于元数据筛选实例");
        System.out.println("Map<String, String> metadata = new HashMap<>();");
        System.out.println("metadata.put(\"version\", \"v1\");");
        System.out.println("metadata.put(\"region\", \"cn-hangzhou\");");
        System.out.println("");
        System.out.println("// 只选择 v1 版本的实例");
        System.out.println("List<Instance> instances = naming.selectInstances(\"user-service\", metadata, true);\n");
    }
    
    // ==================== 第六部分：多环境配置 ====================
    
    private static void demonstrateMultiEnvironment() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第六部分：多环境配置                                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【6.1 Namespace 隔离方案】\n");
        System.out.println("开发环境（dev）：");
        System.out.println("- Namespace ID: dev-001");
        System.out.println("- 配置：user-service-dev.yaml");
        System.out.println("- 服务：user-service (dev cluster)\n");
        
        System.out.println("测试环境（test）：");
        System.out.println("- Namespace ID: test-002");
        System.out.println("- 配置：user-service-test.yaml");
        System.out.println("- 服务：user-service (test cluster)\n");
        
        System.out.println("生产环境（prod）：");
        System.out.println("- Namespace ID: prod-003");
        System.out.println("- 配置：user-service-prod.yaml");
        System.out.println("- 服务：user-service (prod cluster)\n");
        
        System.out.println("【6.2 配置文件命名规范】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ 文件                          │ 用途                                 │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│ ${service-name}.yaml          │ 默认配置（所有环境共享）             │");
        System.out.println("│ ${service-name}-dev.yaml      │ 开发环境配置                         │");
        System.out.println("│ ${service-name}-test.yaml     │ 测试环境配置                         │");
        System.out.println("│ ${service-name}-prod.yaml     │ 生产环境配置                         │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
        
        System.out.println("【6.3 配置示例】\n");
        System.out.println("# application.yml");
        System.out.println("spring:");
        System.out.println("  profiles:");
        System.out.println("    active: dev  # 通过命令行切换：--spring.profiles.active=prod");
        System.out.println("  cloud:");
        System.out.println("    nacos:");
        System.out.println("      config:");
        System.out.println("        server-addr: 127.0.0.1:8848");
        System.out.println("        file-extension: yaml");
        System.out.println("        prefix: ${spring.application.name}");
        System.out.println("        namespace: ${spring.profiles.active}-001\n");
    }
    
    // ==================== 第七部分：配置热更新 ====================
    
    private static void demonstrateHotReload() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第七部分：配置热更新                                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【7.1 热更新原理】\n");
        System.out.println("1. 客户端监听配置变化（长轮询）");
        System.out.println("2. Nacos Server 配置变更时，通知客户端");
        System.out.println("3. 客户端重新拉取配置");
        System.out.println("4. 刷新 Spring 上下文中的 Bean\n");
        
        System.out.println("【7.2 监听器配置】\n");
        System.out.println("// 添加配置监听器");
        System.out.println("config.addListener(\"user-service.yaml\", \"DEFAULT_GROUP\", new Listener() {");
        System.out.println("    @Override");
        System.out.println("    public void receiveConfigInfo(String configInfo) {");
        System.out.println("        System.out.println(\"配置已更新：\" + configInfo);");
        System.out.println("        // 处理配置变更逻辑");
        System.out.println("    }");
        System.out.println("");
        System.out.println("    @Override");
        System.out.println("    public Executor getExecutor() {");
        System.out.println("        return null; // 使用内部线程池");
        System.out.println("    }");
        System.out.println("});\n");
        
        System.out.println("【7.3 @RefreshScope 注解】\n");
        System.out.println("@RestController");
        System.out.println("@RefreshScope  // 配置更新时自动刷新 Bean");
        System.out.println("public class ConfigController {");
        System.out.println("");
        System.out.println("    @Value(\"${config.version:1.0.0}\")");
        System.out.println("    private String configVersion;");
        System.out.println("");
        System.out.println("    @GetMapping(\"/config/version\")");
        System.out.println("    public String getConfigVersion() {");
        System.out.println("        return configVersion;");
        System.out.println("    }");
        System.out.println("}\n");
        
        System.out.println("【7.4 热更新注意事项】\n");
        System.out.println("✓ 适合热更新的配置：");
        System.out.println("- 业务开关（feature flag）");
        System.out.println("- 限流阈值");
        System.out.println("- 日志级别");
        System.out.println("- 超时时间\n");
        
        System.out.println("✗ 不适合热更新的配置：");
        System.out.println("- 数据库连接池核心参数");
        System.out.println("- 端口号");
        System.out.println("- 涉及 Bean 生命周期的配置\n");
    }
    
    // ==================== 第八部分：高频面试题 ====================
    
    private static void printInterviewQuestions() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第八部分：高频面试题                                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("==================== Nacos 基础概念 ====================\n");
        
        System.out.println("【问题 1】什么是 Nacos？它有哪些核心功能？");
        System.out.println("答：");
        System.out.println("Nacos = Naming Service + Configuration Service");
        System.out.println("核心功能：");
        System.out.println("1. 服务发现与服务注册");
        System.out.println("2. 健康检查");
        System.out.println("3. 配置管理（支持热更新）");
        System.out.println("4. 动态 DNS 服务");
        System.out.println("5. 服务元数据管理\n");
        
        System.out.println("【问题 2】Nacos 相比 Eureka 有什么优势？");
        System.out.println("答：");
        System.out.println("1. CAP 灵活切换（CP 或 AP），Eureka 只支持 AP");
        System.out.println("2. 支持配置管理，Eureka 不支持");
        System.out.println("3. 支持多数据中心");
        System.out.println("4. 更丰富的健康检查（TCP/HTTP/MySQL）");
        System.out.println("5. 内置负载均衡");
        System.out.println("6. 社区活跃（阿里背书）\n");
        
        System.out.println("【问题 3】Nacos 的数据模型是什么？");
        System.out.println("答：");
        System.out.println("四层结构：");
        System.out.println("1. Namespace（命名空间）- 环境隔离");
        System.out.println("2. Group（分组）- 业务分组");
        System.out.println("3. Service（服务）- 具体服务名");
        System.out.println("4. Instance（实例）- IP:Port\n");
        
        System.out.println("==================== 服务注册与发现 ====================\n");
        
        System.out.println("【问题 4】Nacos 服务注册流程是什么？");
        System.out.println("答：");
        System.out.println("1. 提供者启动时向 Nacos 发送注册请求");
        System.out.println("2. Nacos 存储服务信息（IP、Port、服务名等）");
        System.out.println("3. 提供者定期发送心跳（5 秒）");
        System.out.println("4. 消费者从 Nacos 拉取服务列表");
        System.out.println("5. 消费者缓存服务列表并定期更新\n");
        
        System.out.println("【问题 5】Nacos 如何保证高可用？");
        System.out.println("答：");
        System.out.println("1. Nacos Server 集群部署");
        System.out.println("2. 数据 Raft 协议同步（CP 模式）");
        System.out.println("3. 客户端缓存服务列表（本地容灾）");
        System.out.println("4. 自动故障转移");
        System.out.println("5. 保护阈值机制\n");
        
        System.out.println("【问题 6】临时实例和持久实例的区别？");
        System.out.println("答：");
        System.out.println("临时实例：");
        System.out.println("- 需要定期发送心跳");
        System.out.println("- 超时会被删除");
        System.out.println("- 适用于微服务\n");
        System.out.println("持久实例：");
        System.out.println("- 不需要心跳");
        System.out.println("- 不会被自动删除");
        System.out.println("- 适用于数据库等永久服务\n");
        
        System.out.println("==================== 配置管理 ====================\n");
        
        System.out.println("【问题 7】Nacos 配置管理的 DataID 组成规则？");
        System.out.println("答：");
        System.out.println("公式：${prefix}-${spring.profile.active}.${file-extension}");
        System.out.println("例如：user-service-dev.yaml");
        System.out.println("- prefix：默认为 spring.application.name");
        System.out.println("- active：当前激活的 profile（dev/test/prod）");
        System.out.println("- extension：配置文件类型（yaml/properties）\n");
        
        System.out.println("【问题 8】Nacos 配置加载优先级是什么？");
        System.out.println("答：");
        System.out.println("从高到低：");
        System.out.println("1. 命令行参数");
        System.out.println("2. ${spring.application.name}-${profile}.yaml");
        System.out.println("3. ${spring.application.name}.yaml");
        System.out.println("4. ${prefix}-${profile}.yaml");
        System.out.println("5. ${prefix}.yaml");
        System.out.println("6. 本地配置文件\n");
        
        System.out.println("【问题 9】Nacos 配置热更新的原理？");
        System.out.println("答：");
        System.out.println("1. 客户端长轮询监听配置变化");
        System.out.println("2. Nacos 配置变更时通知客户端");
        System.out.println("3. 客户端重新拉取配置");
        System.out.println("4. @RefreshScope 刷新 Bean");
        System.out.println("5. 新请求使用新配置\n");
        
        System.out.println("==================== 健康检查与负载均衡 ====================\n");
        
        System.out.println("【问题 10】Nacos 的健康检查机制？");
        System.out.println("答：");
        System.out.println("1. 客户端主动上报心跳（临时实例）");
        System.out.println("   - 默认 5 秒一次");
        System.out.println("   - 15 秒未心跳标记为不健康");
        System.out.println("   - 30 秒未心跳删除实例");
        System.out.println("2. 服务端主动检测");
        System.out.println("   - TCP 连接检测");
        System.out.println("   - HTTP 请求检测\n");
        
        System.out.println("【问题 11】什么是保护阈值？有什么作用？");
        System.out.println("答：");
        System.out.println("定义：健康实例比例的阈值（默认 0.2f）");
        System.out.println("作用：");
        System.out.println("- 当健康实例比例低于阈值时");
        System.out.println("- 推送所有实例（包括不健康）");
        System.out.println("- 防止流量集中导致雪崩\n");
        
        System.out.println("【问题 12】Nacos 支持哪些负载均衡策略？");
        System.out.println("答：");
        System.out.println("1. 随机（Random）");
        System.out.println("2. 轮询（RoundRobin）");
        System.out.println("3. 权重（Weighted）");
        System.out.println("4. 最小连接数");
        System.out.println("5. 一致性 Hash");
        System.out.println("6. 本地优先（同机房优先）\n");
        
        System.out.println("==================== 多环境与实战 ====================\n");
        
        System.out.println("【问题 13】如何使用 Nacos 实现多环境隔离？");
        System.out.println("答：");
        System.out.println("1. 通过 Namespace 隔离（dev/test/prod）");
        System.out.println("2. 每个环境对应不同的 Namespace ID");
        System.out.println("3. 配置文件命名：${service}-${env}.yaml");
        System.out.println("4. 通过 spring.profiles.active 切换环境\n");
        
        System.out.println("【问题 14】Nacos 配置共享如何实现？");
        System.out.println("答：");
        System.out.println("通过 shared-configs 配置：");
        System.out.println("spring:");
        System.out.println("  cloud:");
        System.out.println("    nacos:");
        System.out.println("      config:");
        System.out.println("        shared-configs[0]:");
        System.out.println("          data-id: common-config.yaml");
        System.out.println("          group: DEFAULT_GROUP");
        System.out.println("          refresh: true\n");
        
        System.out.println("【问题 15】@RefreshScope 的作用和原理？");
        System.out.println("答：");
        System.out.println("作用：配置更新时自动刷新 Bean");
        System.out.println("原理：");
        System.out.println("1. 创建代理 Bean");
        System.out.println("2. 配置变更时销毁原 Bean");
        System.out.println("3. 下次请求重新创建 Bean");
        System.out.println("4. 新 Bean 使用新配置\n");
        
        System.out.println("==========================================================================\n");
    }
}
