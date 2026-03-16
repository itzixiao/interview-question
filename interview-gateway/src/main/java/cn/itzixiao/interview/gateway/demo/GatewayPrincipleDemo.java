package cn.itzixiao.interview.gateway.demo;

/**
 * Spring Cloud Gateway 核心原理详解 - 教学型示例
 * <p>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                      Spring Cloud Gateway 架构图                            │
 * │                                                                             │
 * │   ┌─────────┐                                                               │
 * │   │  Client │                                                               │
 * │   └────┬────┘                                                               │
 * │        │                                                                    │
 * │        ↓                                                                    │
 * │   ┌────────────────────────────────────────────────────────────────────┐   │
 * │   │                     Gateway Handler Mapping                        │   │
 * │   │                     (匹配路由 Route)                               │   │
 * │   └────────────────────────────────────────────────────────────────────┘   │
 * │        │                                                                    │
 * │        ↓                                                                    │
 * │   ┌────────────────────────────────────────────────────────────────────┐   │
 * │   │                     Gateway Web Handler                            │   │
 * │   │                     (执行过滤器链)                                  │   │
 * │   └────────────────────────────────────────────────────────────────────┘   │
 * │        │                                                                    │
 * │        ↓                                                                    │
 * │   ┌────────────────────────────────────────────────────────────────────┐   │
 * │   │                      Filter Chain (过滤器链)                       │   │
 * │   │                                                                    │   │
 * │   │   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐             │   │
 * │   │   │ GlobalFilter│ → │GatewayFilter│ → │GatewayFilter│ → ...       │   │
 * │   │   │   (全局)    │   │  (路由级)   │   │  (路由级)   │             │   │
 * │   │   └─────────────┘   └─────────────┘   └─────────────┘             │   │
 * │   │                                                                    │   │
 * │   │        Pre Filters (请求前处理)                                    │   │
 * │   │              ↓                                                     │   │
 * │   │        Proxy Request (代理请求到下游服务)                          │   │
 * │   │              ↓                                                     │   │
 * │   │        Post Filters (响应后处理)                                   │   │
 * │   │                                                                    │   │
 * │   └────────────────────────────────────────────────────────────────────┘   │
 * │        │                                                                    │
 * │        ↓                                                                    │
 * │   ┌─────────────┐                                                          │
 * │   │  Downstream │                                                          │
 * │   │   Service   │                                                          │
 * │   └─────────────┘                                                          │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * <p>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                           核心概念                                          │
 * │                                                                             │
 * │   Route（路由）：                                                           │
 * │   - Gateway 的基本构建模块                                                  │
 * │   - 由 ID、目标 URI、断言集合、过滤器集合组成                               │
 * │                                                                             │
 * │   Predicate（断言）：                                                       │
 * │   - 匹配 HTTP 请求的条件（路径、方法、Header、参数等）                       │
 * │   - Java 8 函数式接口 Predicate<ServerWebExchange>                          │
 * │                                                                             │
 * │   Filter（过滤器）：                                                        │
 * │   - 对请求和响应进行处理                                                    │
 * │   - 分为 GatewayFilter（路由级）和 GlobalFilter（全局）                     │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * <p>
 * 注意：本文件为原理讲解文档，实际过滤器代码请参考 filter 包下的实现类
 */
public class GatewayPrincipleDemo {

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                                                                   █");
        System.out.println("█            Spring Cloud Gateway 核心原理详解                      █");
        System.out.println("█                                                                   █");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println();

        // 第一部分：Gateway 概述
        demonstrateGatewayOverview();

        // 第二部分：核心组件
        demonstrateCoreComponents();

        // 第三部分：路由断言
        demonstratePredicates();

        // 第四部分：内置过滤器
        demonstrateBuiltInFilters();

        // 第五部分：过滤器执行流程
        demonstrateFilterFlow();

        // 第六部分：动态路由
        demonstrateDynamicRouting();

        // 第七部分：高可用架构
        demonstrateHighAvailability();

        // 第八部分：高频面试题
        interviewQuestions();
    }

    // ==================== 第一部分：Gateway 概述 ====================

    private static void demonstrateGatewayOverview() {
        System.out.println("========== 1. Gateway 概述 ==========\n");

        System.out.println("【什么是 API 网关？】");
        System.out.println("  API 网关是微服务架构的入口，统一接收所有客户端请求");
        System.out.println("  主要功能：路由转发、认证鉴权、限流熔断、日志监控等\n");

        System.out.println("【为什么选择 Spring Cloud Gateway？】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  与 Zuul 1.x 对比：                                             │");
        System.out.println("│  ┌───────────────┬──────────────────┬──────────────────┐       │");
        System.out.println("│  │   特性        │  Zuul 1.x        │  Gateway         │       │");
        System.out.println("│  ├───────────────┼──────────────────┼──────────────────┤       │");
        System.out.println("│  │  IO 模型      │  同步阻塞        │  异步非阻塞      │       │");
        System.out.println("│  │  底层框架     │  Servlet         │  WebFlux/Netty   │       │");
        System.out.println("│  │  性能         │  较低            │  高（约1.6倍）   │       │");
        System.out.println("│  │  长连接       │  不支持          │  支持 WebSocket  │       │");
        System.out.println("│  │  编程模型     │  命令式          │  响应式          │       │");
        System.out.println("│  │  Spring 生态  │  兼容            │  原生支持        │       │");
        System.out.println("│  └───────────────┴──────────────────┴──────────────────┘       │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【Gateway 核心特性】");
        System.out.println("  1. 基于 Spring WebFlux 构建，响应式非阻塞");
        System.out.println("  2. 动态路由：支持注册中心动态发现服务");
        System.out.println("  3. 内置断言和过滤器工厂");
        System.out.println("  4. 集成 Hystrix/Sentinel 实现熔断限流");
        System.out.println("  5. 集成 Spring Cloud LoadBalancer 负载均衡");
        System.out.println("  6. 支持路径重写、请求/响应修改");
        System.out.println("  7. 支持 WebSocket 长连接\n");
    }

    // ==================== 第二部分：核心组件 ====================

    private static void demonstrateCoreComponents() {
        System.out.println("========== 2. 核心组件 ==========\n");

        System.out.println("【Route（路由）】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  路由是 Gateway 的基本构建单元，每个路由包含：                  │");
        System.out.println("│  - ID：唯一标识                                                 │");
        System.out.println("│  - URI：目标服务地址（可以是 lb://service-name 或具体地址）    │");
        System.out.println("│  - Predicates：断言集合，匹配请求的条件                        │");
        System.out.println("│  - Filters：过滤器集合，处理请求和响应                         │");
        System.out.println("│                                                                 │");
        System.out.println("│  YAML 配置示例：                                                │");
        System.out.println("│  spring:                                                        │");
        System.out.println("│    cloud:                                                       │");
        System.out.println("│      gateway:                                                   │");
        System.out.println("│        routes:                                                  │");
        System.out.println("│          - id: user-service                                     │");
        System.out.println("│            uri: lb://user-service                               │");
        System.out.println("│            predicates:                                          │");
        System.out.println("│              - Path=/api/user/**                                │");
        System.out.println("│            filters:                                             │");
        System.out.println("│              - StripPrefix=1                                    │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【Predicate（断言）】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  断言用于匹配 HTTP 请求，决定请求是否路由到该路由               │");
        System.out.println("│  支持多种匹配条件的组合（与、或关系）                          │");
        System.out.println("│                                                                 │");
        System.out.println("│  Java 8 函数式接口：                                            │");
        System.out.println("│  public interface Predicate<T> {                                │");
        System.out.println("│      boolean test(T t);                                         │");
        System.out.println("│  }                                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│  Gateway 中使用：Predicate<ServerWebExchange>                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【Filter（过滤器）】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  两种类型：                                                     │");
        System.out.println("│                                                                 │");
        System.out.println("│  1. GatewayFilter（路由过滤器）                                 │");
        System.out.println("│     - 作用于特定路由                                            │");
        System.out.println("│     - 通过 GatewayFilterFactory 创建                            │");
        System.out.println("│     - 配置在路由的 filters 中                                   │");
        System.out.println("│                                                                 │");
        System.out.println("│  2. GlobalFilter（全局过滤器）                                  │");
        System.out.println("│     - 作用于所有路由                                            │");
        System.out.println("│     - 实现 GlobalFilter 接口                                    │");
        System.out.println("│     - 常用于认证、日志、限流等全局逻辑                          │");
        System.out.println("│                                                                 │");
        System.out.println("│  过滤器执行顺序由 Ordered 接口的 getOrder() 决定               │");
        System.out.println("│  数值越小，优先级越高                                           │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第三部分：路由断言 ====================

    private static void demonstratePredicates() {
        System.out.println("========== 3. 内置断言（Predicate）==========\n");

        System.out.println("【常用断言工厂】");
        System.out.println("┌───────────────────────┬─────────────────────────────────────────┐");
        System.out.println("│  断言工厂             │  说明                                   │");
        System.out.println("├───────────────────────┼─────────────────────────────────────────┤");
        System.out.println("│  Path                 │  匹配请求路径                           │");
        System.out.println("│  Method               │  匹配 HTTP 方法                         │");
        System.out.println("│  Header               │  匹配请求头                             │");
        System.out.println("│  Query                │  匹配查询参数                           │");
        System.out.println("│  Cookie               │  匹配 Cookie                            │");
        System.out.println("│  Host                 │  匹配主机名                             │");
        System.out.println("│  RemoteAddr           │  匹配客户端 IP                          │");
        System.out.println("│  Before/After/Between │  匹配时间范围                           │");
        System.out.println("│  Weight               │  按权重路由（灰度发布）                 │");
        System.out.println("└───────────────────────┴─────────────────────────────────────────┘\n");

        System.out.println("【断言配置示例】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  predicates:                                                    │");
        System.out.println("│    # 路径匹配                                                   │");
        System.out.println("│    - Path=/api/user/**                                          │");
        System.out.println("│                                                                 │");
        System.out.println("│    # HTTP 方法匹配                                              │");
        System.out.println("│    - Method=GET,POST                                            │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 请求头匹配（支持正则）                                     │");
        System.out.println("│    - Header=X-Request-Id, \\d+                                   │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 查询参数匹配                                               │");
        System.out.println("│    - Query=name, zhang.*                                        │");
        System.out.println("│                                                                 │");
        System.out.println("│    # Cookie 匹配                                                │");
        System.out.println("│    - Cookie=sessionId, .*                                       │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 主机名匹配                                                 │");
        System.out.println("│    - Host=**.example.com                                        │");
        System.out.println("│                                                                 │");
        System.out.println("│    # IP 匹配（支持 CIDR）                                       │");
        System.out.println("│    - RemoteAddr=192.168.1.0/24                                  │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 时间匹配（ZonedDateTime 格式）                             │");
        System.out.println("│    - After=2024-01-01T00:00:00+08:00[Asia/Shanghai]             │");
        System.out.println("│    - Before=2025-12-31T23:59:59+08:00[Asia/Shanghai]            │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 权重路由（灰度发布）                                       │");
        System.out.println("│    - Weight=group1, 8  # 80% 流量                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第四部分：内置过滤器 ====================

    private static void demonstrateBuiltInFilters() {
        System.out.println("========== 4. 内置过滤器（GatewayFilter）==========\n");

        System.out.println("【常用过滤器工厂】");
        System.out.println("┌────────────────────────────┬────────────────────────────────────┐");
        System.out.println("│  过滤器工厂               │  说明                               │");
        System.out.println("├────────────────────────────┼────────────────────────────────────┤");
        System.out.println("│  AddRequestHeader          │  添加请求头                         │");
        System.out.println("│  AddRequestParameter       │  添加请求参数                       │");
        System.out.println("│  AddResponseHeader         │  添加响应头                         │");
        System.out.println("│  RemoveRequestHeader       │  移除请求头                         │");
        System.out.println("│  RemoveResponseHeader      │  移除响应头                         │");
        System.out.println("│  SetRequestHeader          │  设置请求头                         │");
        System.out.println("│  SetResponseHeader         │  设置响应头                         │");
        System.out.println("├────────────────────────────┼────────────────────────────────────┤");
        System.out.println("│  PrefixPath                │  添加路径前缀                       │");
        System.out.println("│  StripPrefix               │  去掉路径前缀                       │");
        System.out.println("│  RewritePath               │  重写路径                           │");
        System.out.println("│  SetPath                   │  设置路径                           │");
        System.out.println("├────────────────────────────┼────────────────────────────────────┤");
        System.out.println("│  Retry                     │  重试                               │");
        System.out.println("│  CircuitBreaker            │  熔断器                             │");
        System.out.println("│  RequestRateLimiter        │  限流                               │");
        System.out.println("│  RedirectTo                │  重定向                             │");
        System.out.println("│  SetStatus                 │  设置响应状态码                     │");
        System.out.println("│  SaveSession               │  保存 WebSession                    │");
        System.out.println("│  SecureHeaders             │  添加安全响应头                     │");
        System.out.println("│  DedupeResponseHeader      │  去重响应头                         │");
        System.out.println("└────────────────────────────┴────────────────────────────────────┘\n");

        System.out.println("【过滤器配置示例】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  filters:                                                       │");
        System.out.println("│    # 去掉路径前缀（/api/user/list → /list）                    │");
        System.out.println("│    - StripPrefix=1                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 添加请求头                                                 │");
        System.out.println("│    - AddRequestHeader=X-Request-Source, gateway                 │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 添加响应头                                                 │");
        System.out.println("│    - AddResponseHeader=X-Response-From, gateway                 │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 路径重写（支持正则）                                       │");
        System.out.println("│    - RewritePath=/api/(?<segment>.*), /$\\{segment}              │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 重试配置                                                   │");
        System.out.println("│    - name: Retry                                                │");
        System.out.println("│      args:                                                      │");
        System.out.println("│        retries: 3                                               │");
        System.out.println("│        statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE                │");
        System.out.println("│        methods: GET,POST                                        │");
        System.out.println("│        backoff:                                                 │");
        System.out.println("│          firstBackoff: 100ms                                    │");
        System.out.println("│          maxBackoff: 500ms                                      │");
        System.out.println("│          factor: 2                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 限流配置（需要 Redis）                                     │");
        System.out.println("│    - name: RequestRateLimiter                                   │");
        System.out.println("│      args:                                                      │");
        System.out.println("│        redis-rate-limiter.replenishRate: 10  # 每秒10个请求    │");
        System.out.println("│        redis-rate-limiter.burstCapacity: 20  # 令牌桶容量      │");
        System.out.println("│        key-resolver: \"#{@ipKeyResolver}\"   # 限流维度          │");
        System.out.println("│                                                                 │");
        System.out.println("│    # 熔断配置                                                   │");
        System.out.println("│    - name: CircuitBreaker                                       │");
        System.out.println("│      args:                                                      │");
        System.out.println("│        name: myCircuitBreaker                                   │");
        System.out.println("│        fallbackUri: forward:/fallback                           │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第五部分：过滤器执行流程 ====================

    private static void demonstrateFilterFlow() {
        System.out.println("========== 5. 过滤器执行流程 ==========\n");

        System.out.println("【过滤器执行顺序】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                 │");
        System.out.println("│   Request (请求)                                                │");
        System.out.println("│        │                                                        │");
        System.out.println("│        ↓                                                        │");
        System.out.println("│   ┌─────────────────────────────────────────┐                  │");
        System.out.println("│   │  GlobalFilter (order=-1) 日志过滤器    │  Pre阶段          │");
        System.out.println("│   └─────────────────────────────────────────┘                  │");
        System.out.println("│        │                                                        │");
        System.out.println("│        ↓                                                        │");
        System.out.println("│   ┌─────────────────────────────────────────┐                  │");
        System.out.println("│   │  GlobalFilter (order=0) 认证过滤器     │  Pre阶段          │");
        System.out.println("│   └─────────────────────────────────────────┘                  │");
        System.out.println("│        │                                                        │");
        System.out.println("│        ↓                                                        │");
        System.out.println("│   ┌─────────────────────────────────────────┐                  │");
        System.out.println("│   │  GatewayFilter (StripPrefix)           │  Pre阶段          │");
        System.out.println("│   └─────────────────────────────────────────┘                  │");
        System.out.println("│        │                                                        │");
        System.out.println("│        ↓                                                        │");
        System.out.println("│   ┌─────────────────────────────────────────┐                  │");
        System.out.println("│   │  NettyRoutingFilter (代理请求)         │  代理到下游       │");
        System.out.println("│   └─────────────────────────────────────────┘                  │");
        System.out.println("│        │                                                        │");
        System.out.println("│        ↓                                                        │");
        System.out.println("│   ┌─────────────────────────────────────────┐                  │");
        System.out.println("│   │  Downstream Service (下游服务)         │                   │");
        System.out.println("│   └─────────────────────────────────────────┘                  │");
        System.out.println("│        │                                                        │");
        System.out.println("│        ↓                                                        │");
        System.out.println("│   ┌─────────────────────────────────────────┐                  │");
        System.out.println("│   │  GatewayFilter (Post处理)              │  Post阶段         │");
        System.out.println("│   └─────────────────────────────────────────┘                  │");
        System.out.println("│        │                                                        │");
        System.out.println("│        ↓                                                        │");
        System.out.println("│   ┌─────────────────────────────────────────┐                  │");
        System.out.println("│   │  GlobalFilter (Post处理)               │  Post阶段         │");
        System.out.println("│   └─────────────────────────────────────────┘                  │");
        System.out.println("│        │                                                        │");
        System.out.println("│        ↓                                                        │");
        System.out.println("│   Response (响应)                                               │");
        System.out.println("│                                                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【GlobalFilter 实现示例】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Component                                                     │");
        System.out.println("│  public class AuthGlobalFilter implements GlobalFilter, Ordered{│");
        System.out.println("│                                                                 │");
        System.out.println("│      @Override                                                  │");
        System.out.println("│      public Mono<Void> filter(ServerWebExchange exchange,       │");
        System.out.println("│                               GatewayFilterChain chain) {       │");
        System.out.println("│          // Pre 阶段：请求前处理                                │");
        System.out.println("│          ServerHttpRequest request = exchange.getRequest();     │");
        System.out.println("│          String token = request.getHeaders()                    │");
        System.out.println("│                               .getFirst(\"Authorization\");       │");
        System.out.println("│                                                                 │");
        System.out.println("│          if (!StringUtils.hasText(token)) {                     │");
        System.out.println("│              exchange.getResponse().setStatusCode(UNAUTHORIZED);│");
        System.out.println("│              return exchange.getResponse().setComplete();       │");
        System.out.println("│          }                                                      │");
        System.out.println("│                                                                 │");
        System.out.println("│          // 继续执行过滤器链                                    │");
        System.out.println("│          return chain.filter(exchange)                          │");
        System.out.println("│              .then(Mono.fromRunnable(() -> {                    │");
        System.out.println("│                  // Post 阶段：响应后处理                       │");
        System.out.println("│                  log.info(\"请求完成\");                          │");
        System.out.println("│              }));                                               │");
        System.out.println("│      }                                                          │");
        System.out.println("│                                                                 │");
        System.out.println("│      @Override                                                  │");
        System.out.println("│      public int getOrder() {                                    │");
        System.out.println("│          return 0;  // 数值越小，优先级越高                     │");
        System.out.println("│      }                                                          │");
        System.out.println("│  }                                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第六部分：动态路由 ====================

    private static void demonstrateDynamicRouting() {
        System.out.println("========== 6. 动态路由 ==========\n");

        System.out.println("【服务发现动态路由】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  spring:                                                        │");
        System.out.println("│    cloud:                                                       │");
        System.out.println("│      gateway:                                                   │");
        System.out.println("│        discovery:                                               │");
        System.out.println("│          locator:                                               │");
        System.out.println("│            enabled: true              # 启用服务发现路由       │");
        System.out.println("│            lower-case-service-id: true # 服务名小写            │");
        System.out.println("│                                                                 │");
        System.out.println("│  启用后，自动为注册中心的每个服务创建路由：                     │");
        System.out.println("│  /SERVICE-NAME/** → lb://service-name                          │");
        System.out.println("│                                                                 │");
        System.out.println("│  例如：http://gateway/user-service/api/user/1                  │");
        System.out.println("│  会路由到：http://user-service/api/user/1                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【LoadBalancer 负载均衡】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  uri: lb://user-service                                         │");
        System.out.println("│                                                                 │");
        System.out.println("│  lb:// 前缀表示使用 LoadBalancer 进行负载均衡                   │");
        System.out.println("│  Gateway 会从注册中心获取服务实例列表，选择一个实例转发请求    │");
        System.out.println("│                                                                 │");
        System.out.println("│  负载均衡策略（Spring Cloud LoadBalancer）：                    │");
        System.out.println("│  - RoundRobinLoadBalancer（轮询，默认）                         │");
        System.out.println("│  - RandomLoadBalancer（随机）                                   │");
        System.out.println("│  - 自定义策略                                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【基于 Nacos 的动态路由】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  可以将路由配置存储在 Nacos 配置中心                            │");
        System.out.println("│  实现路由的动态更新，无需重启 Gateway                           │");
        System.out.println("│                                                                 │");
        System.out.println("│  实现方式：                                                     │");
        System.out.println("│  1. 监听 Nacos 配置变化                                         │");
        System.out.println("│  2. 解析配置获取路由定义                                        │");
        System.out.println("│  3. 调用 RouteDefinitionWriter 更新路由                         │");
        System.out.println("│  4. 发布 RefreshRoutesEvent 刷新路由                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第七部分：高可用架构 ====================

    private static void demonstrateHighAvailability() {
        System.out.println("========== 7. 高可用架构 ==========\n");

        System.out.println("【Gateway 高可用部署】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                 │");
        System.out.println("│                     ┌─────────────┐                             │");
        System.out.println("│                     │   Nginx     │                             │");
        System.out.println("│                     │  (负载均衡) │                             │");
        System.out.println("│                     └──────┬──────┘                             │");
        System.out.println("│                            │                                    │");
        System.out.println("│           ┌────────────────┼────────────────┐                  │");
        System.out.println("│           │                │                │                  │");
        System.out.println("│           ↓                ↓                ↓                  │");
        System.out.println("│    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │");
        System.out.println("│    │  Gateway-1   │ │  Gateway-2   │ │  Gateway-3   │         │");
        System.out.println("│    └──────┬───────┘ └──────┬───────┘ └──────┬───────┘         │");
        System.out.println("│           │                │                │                  │");
        System.out.println("│           └────────────────┼────────────────┘                  │");
        System.out.println("│                            │                                    │");
        System.out.println("│                            ↓                                    │");
        System.out.println("│                   ┌─────────────────┐                          │");
        System.out.println("│                   │   注册中心       │                          │");
        System.out.println("│                   │  (Nacos/Eureka) │                          │");
        System.out.println("│                   └────────┬────────┘                          │");
        System.out.println("│                            │                                    │");
        System.out.println("│           ┌────────────────┼────────────────┐                  │");
        System.out.println("│           ↓                ↓                ↓                  │");
        System.out.println("│    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │");
        System.out.println("│    │  Service-A   │ │  Service-B   │ │  Service-C   │         │");
        System.out.println("│    └──────────────┘ └──────────────┘ └──────────────┘         │");
        System.out.println("│                                                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【限流与熔断】");
        System.out.println("  1. RequestRateLimiter：基于 Redis 令牌桶限流");
        System.out.println("  2. CircuitBreaker：集成 Resilience4j/Sentinel 熔断");
        System.out.println("  3. Retry：失败重试机制");
        System.out.println("  4. FallbackUri：熔断降级路径\n");
    }

    // ==================== 第八部分：高频面试题 ====================

    private static void interviewQuestions() {
        System.out.println("==================== 高频面试题 ====================\n");

        // Q1
        System.out.println("【面试题1】Spring Cloud Gateway 的核心组件有哪些？\n");
        System.out.println("  三大核心组件：");
        System.out.println("  1. Route（路由）：由 ID、URI、断言、过滤器组成");
        System.out.println("  2. Predicate（断言）：匹配 HTTP 请求的条件");
        System.out.println("  3. Filter（过滤器）：处理请求和响应");
        System.out.println("     - GatewayFilter：路由级过滤器");
        System.out.println("     - GlobalFilter：全局过滤器\n");

        // Q2
        System.out.println("【面试题2】Gateway 和 Zuul 的区别？\n");
        System.out.println("  ┌───────────────┬──────────────────┬──────────────────┐");
        System.out.println("  │   特性        │  Zuul 1.x        │  Gateway         │");
        System.out.println("  ├───────────────┼──────────────────┼──────────────────┤");
        System.out.println("  │  IO 模型      │  同步阻塞        │  异步非阻塞      │");
        System.out.println("  │  底层框架     │  Servlet         │  WebFlux/Netty   │");
        System.out.println("  │  性能         │  较低            │  高              │");
        System.out.println("  │  WebSocket    │  不支持          │  支持            │");
        System.out.println("  │  Spring 集成  │  兼容            │  原生支持        │");
        System.out.println("  └───────────────┴──────────────────┴──────────────────┘\n");

        // Q3
        System.out.println("【面试题3】Gateway 的过滤器执行顺序是怎样的？\n");
        System.out.println("  1. 过滤器实现 Ordered 接口，通过 getOrder() 返回优先级");
        System.out.println("  2. 数值越小，优先级越高");
        System.out.println("  3. Pre 阶段：按 order 从小到大执行");
        System.out.println("  4. Post 阶段：按 order 从大到小执行（逆序）");
        System.out.println("  5. GlobalFilter 和 GatewayFilter 统一排序后执行\n");

        // Q4
        System.out.println("【面试题4】GlobalFilter 和 GatewayFilter 的区别？\n");
        System.out.println("  GlobalFilter（全局过滤器）：");
        System.out.println("  - 作用于所有路由");
        System.out.println("  - 实现 GlobalFilter 接口");
        System.out.println("  - 常用于认证、日志、限流等全局逻辑");
        System.out.println("  ");
        System.out.println("  GatewayFilter（路由过滤器）：");
        System.out.println("  - 作用于特定路由");
        System.out.println("  - 通过 GatewayFilterFactory 创建");
        System.out.println("  - 在 YAML 的 filters 中配置\n");

        // Q5
        System.out.println("【面试题5】Gateway 如何实现限流？\n");
        System.out.println("  使用 RequestRateLimiter 过滤器 + Redis 实现：");
        System.out.println("  ");
        System.out.println("  - name: RequestRateLimiter");
        System.out.println("    args:");
        System.out.println("      redis-rate-limiter.replenishRate: 10  # 每秒补充10个令牌");
        System.out.println("      redis-rate-limiter.burstCapacity: 20  # 令牌桶容量20");
        System.out.println("      key-resolver: \"#{@ipKeyResolver}\"    # 按IP限流");
        System.out.println("  ");
        System.out.println("  原理：基于 Redis 的令牌桶算法");
        System.out.println("  - 令牌按 replenishRate 速率补充");
        System.out.println("  - 请求需要获取令牌才能通过");
        System.out.println("  - 令牌不足时拒绝请求（返回 429）\n");

        // Q6
        System.out.println("【面试题6】Gateway 如何实现熔断降级？\n");
        System.out.println("  集成 Resilience4j 或 Sentinel：");
        System.out.println("  ");
        System.out.println("  - name: CircuitBreaker");
        System.out.println("    args:");
        System.out.println("      name: myCircuitBreaker");
        System.out.println("      fallbackUri: forward:/fallback");
        System.out.println("  ");
        System.out.println("  熔断状态：");
        System.out.println("  - CLOSED：正常状态，请求正常转发");
        System.out.println("  - OPEN：熔断状态，请求直接走降级");
        System.out.println("  - HALF_OPEN：半开状态，放行部分请求探测\n");

        // Q7
        System.out.println("【面试题7】Gateway 如何实现动态路由？\n");
        System.out.println("  方式一：服务发现自动路由");
        System.out.println("  spring.cloud.gateway.discovery.locator.enabled=true");
        System.out.println("  ");
        System.out.println("  方式二：基于配置中心（Nacos/Apollo）");
        System.out.println("  1. 监听配置变化");
        System.out.println("  2. 调用 RouteDefinitionWriter.save() 更新路由");
        System.out.println("  3. 发布 RefreshRoutesEvent 刷新");
        System.out.println("  ");
        System.out.println("  方式三：Actuator 端点");
        System.out.println("  POST /actuator/gateway/routes/{id}\n");

        // Q8
        System.out.println("【面试题8】Gateway 为什么基于 WebFlux 而不是 Servlet？\n");
        System.out.println("  WebFlux 优势：");
        System.out.println("  1. 异步非阻塞：基于 Reactor/Netty，少量线程处理大量请求");
        System.out.println("  2. 高吞吐量：适合网关这种 IO 密集型场景");
        System.out.println("  3. 背压支持：响应式流控制");
        System.out.println("  4. 函数式编程：更简洁的代码");
        System.out.println("  ");
        System.out.println("  Servlet 劣势：");
        System.out.println("  1. 同步阻塞：一个请求占用一个线程");
        System.out.println("  2. 线程资源有限：高并发下性能下降\n");

        // Q9
        System.out.println("【面试题9】Gateway 如何传递用户信息到下游服务？\n");
        System.out.println("  在 GlobalFilter 中修改请求头：");
        System.out.println("  ");
        System.out.println("  ServerHttpRequest request = exchange.getRequest().mutate()");
        System.out.println("      .header(\"X-User-Id\", userId)");
        System.out.println("      .header(\"X-User-Name\", userName)");
        System.out.println("      .build();");
        System.out.println("  ");
        System.out.println("  ServerWebExchange newExchange = exchange.mutate()");
        System.out.println("      .request(request)");
        System.out.println("      .build();");
        System.out.println("  ");
        System.out.println("  return chain.filter(newExchange);");
        System.out.println("  ");
        System.out.println("  下游服务通过请求头获取用户信息\n");

        // Q10
        System.out.println("【面试题10】Gateway 如何处理跨域（CORS）？\n");
        System.out.println("  配置方式：");
        System.out.println("  spring:");
        System.out.println("    cloud:");
        System.out.println("      gateway:");
        System.out.println("        globalcors:");
        System.out.println("          cors-configurations:");
        System.out.println("            '[/**]':");
        System.out.println("              allowedOriginPatterns: \"*\"");
        System.out.println("              allowedMethods: \"*\"");
        System.out.println("              allowedHeaders: \"*\"");
        System.out.println("              allowCredentials: true");
        System.out.println("              maxAge: 3600");
        System.out.println("  ");
        System.out.println("  注意：Gateway 处理了跨域后，下游服务不要重复处理");
        System.out.println("  可以用 DedupeResponseHeader 过滤器去重响应头\n");
    }
}
