package cn.itzixiao.interview.rpc;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RPC 核心原理演示
 * 
 * RPC (Remote Procedure Call) 远程过程调用
 * 核心目标：让本地程序像调用本地方法一样调用远程服务
 * 
 * RPC 架构核心组件：
 * 1. Client - 服务调用方
 * 2. Server - 服务提供方
 * 3. Registry - 服务注册中心（可选，用于服务发现）
 * 4. Protocol - 通信协议（HTTP、TCP 自定义协议等）
 * 5. Serialization - 序列化方式（JSON、Protobuf、Hessian 等）
 * 
 * RPC 调用流程：
 * 1. 客户端通过代理对象发起调用
 * 2. 客户端存根（Stub）将方法名、参数等信息序列化
 * 3. 通过网络发送到服务端
 * 4. 服务端存根（Skeleton）反序列化请求
 * 5. 服务端根据方法名反射调用实际方法
 * 6. 返回结果经过序列化返回给客户端
 * 7. 客户端反序列化得到结果
 */
public class RpcPrincipleDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("========== RPC 核心原理演示 ==========\n");
        
        // 启动 RPC 服务器
        startRpcServer();
        
        // 等待服务器启动
        Thread.sleep(2000);
        
        // 创建 RPC 客户端代理并调用
        UserService userService = RpcClient.createProxy(UserService.class, "localhost", 8888);
        
        // 调用远程方法
        System.out.println("\n【客户端调用】开始调用远程服务...");
        User user = userService.getUserById(1L);
        System.out.println("【客户端收到】用户信息：" + user);
        
        String result = userService.sayHello("张三");
        System.out.println("【客户端收到】问候结果：" + result);
    }
    
    /**
     * 启动 RPC 服务器
     */
    private static void startRpcServer() {
        new Thread(() -> {
            try {
                System.out.println("【服务器】正在启动 RPC 服务器，监听端口 8888...");
                ServerSocket serverSocket = new ServerSocket(8888);
                ExecutorService executor = Executors.newFixedThreadPool(10);
                
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(() -> handleRequest(clientSocket));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * 处理客户端请求
     */
    private static void handleRequest(Socket socket) {
        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            
            // 反序列化请求
            RpcRequest request = (RpcRequest) input.readObject();
            System.out.println("\n【服务器收到】请求方法：" + request.getMethodName() 
                + ", 参数类型：" + request.getParameterTypes()[0]
                + ", 参数值：" + request.getParameters()[0]);
            
            // 反射调用实际方法
            UserServiceImpl userService = new UserServiceImpl();
            Object result = null;
            
            if ("getUserById".equals(request.getMethodName())) {
                result = userService.getUserById((Long) request.getParameters()[0]);
            } else if ("sayHello".equals(request.getMethodName())) {
                result = userService.sayHello((String) request.getParameters()[0]);
            }
            
            // 序列化响应
            RpcResponse response = new RpcResponse();
            response.setResult(result);
            output.writeObject(response);
            
            System.out.println("【服务器返回】结果：" + result);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

// ==================== RPC 请求与响应 ====================

/**
 * RPC 请求对象
 */
class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String className;          // 类名
    private String methodName;         // 方法名
    private Class<?>[] parameterTypes; // 参数类型
    private Object[] parameters;       // 参数值
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    
    public Class<?>[] getParameterTypes() { return parameterTypes; }
    public void setParameterTypes(Class<?>[] parameterTypes) { this.parameterTypes = parameterTypes; }
    
    public Object[] getParameters() { return parameters; }
    public void setParameters(Object[] parameters) { this.parameters = parameters; }
}

/**
 * RPC 响应对象
 */
class RpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Object result;  // 返回结果
    private Throwable error; // 异常信息
    
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    
    public Throwable getError() { return error; }
    public void setError(Throwable error) { this.error = error; }
}

// ==================== 服务接口与实现 ====================

/**
 * 用户服务接口
 */
interface UserService {
    User getUserById(Long id);
    String sayHello(String name);
}

/**
 * 用户服务实现类（服务端）
 */
class UserServiceImpl implements UserService {
    @Override
    public User getUserById(Long id) {
        System.out.println("【服务端执行】getUserById, id=" + id);
        return new User(id, "用户" + id, "user" + id + "@example.com");
    }
    
    @Override
    public String sayHello(String name) {
        System.out.println("【服务端执行】sayHello, name=" + name);
        return "Hello, " + name + "! 欢迎使用 RPC 服务";
    }
}

/**
 * 用户实体类
 */
class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String name;
    private String email;
    
    public User() {}
    
    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}

// ==================== RPC 客户端代理 ====================

/**
 * RPC 客户端 - 动态代理实现
 * 
 * 核心原理：
 * 1. 使用 JDK 动态代理创建接口代理对象
 * 2. 拦截所有方法调用，封装成 RPC 请求
 * 3. 通过网络发送到服务端
 * 4. 接收响应并返回结果
 */
class RpcClient {
    
    /**
     * 创建 RPC 代理对象
     * 
     * @param interfaceClass 服务接口
     * @param host 服务器地址
     * @param port 服务器端口
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(final Class<T> interfaceClass, 
                                     final String host, 
                                     final int port) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    // 1. 封装 RPC 请求
                    RpcRequest request = new RpcRequest();
                    request.setClassName(interfaceClass.getName());
                    request.setMethodName(method.getName());
                    request.setParameterTypes(method.getParameterTypes());
                    request.setParameters(args);
                    
                    // 2. 发送请求到服务器
                    Socket socket = new Socket(host, port);
                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                    output.writeObject(request);
                    
                    // 3. 接收服务器响应
                    ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                    RpcResponse response = (RpcResponse) input.readObject();
                    
                    // 4. 关闭连接
                    socket.close();
                    
                    // 5. 返回结果
                    return response.getResult();
                }
            }
        );
    }
}

/**
 * ========================================
 *           RPC 高频面试题目
 * ========================================
 * 
 * 【问题 1】什么是 RPC？它的核心优势是什么？
 * 
 * 【答案】
 * RPC（Remote Procedure Call）远程过程调用，是一种通过网络从远程计算机程序上请求服务的软件协议。
 * 
 * 核心优势：
 * 1. 透明性：对调用者屏蔽了底层网络通信细节，像调用本地方法一样调用远程服务
 * 2. 高效性：相比 RESTful API，RPC 通常使用二进制协议，性能更高
 * 3. 强类型：接口定义明确，编译时就能检查类型错误
 * 4. 支持复杂数据结构：可以直接传递对象、集合等复杂结构
 * 5. 双向通信：支持同步/异步调用、流式传输等多种模式
 * 
 * 常见 RPC 框架：Dubbo、gRPC、Thrift、Spring Cloud OpenFeign 等
 * 
 * 
 * 【问题 2】RPC 的完整工作流程是什么？
 * 
 * 【答案】
 * 1. 服务暴露：服务端实现服务接口，并将服务注册到注册中心（直连模式下可省略）
 * 2. 服务发现：客户端从注册中心获取服务提供者地址列表
 * 3. 代理调用：客户端通过动态代理发起调用
 * 4. 请求序列化：客户端 Stub 将方法名、参数类型、参数值等序列化为字节流
 * 5. 网络传输：通过 TCP/HTTP 等协议发送到服务端
 * 6. 请求反序列化：服务端 Skeleton 反序列化请求数据
 * 7. 反射调用：服务端根据方法名反射调用实际业务逻辑
 * 8. 响应序列化：服务端将结果序列化后返回
 * 9. 响应反序列化：客户端解析响应数据并返回给调用者
 * 
 * 
 * 【问题 3】RPC 框架需要解决哪些核心问题？
 * 
 * 【答案】
 * 1. 服务寻址（Addressing）
 *    - 如何找到服务提供者？通过注册中心（Nacos、Zookeeper、Eureka）
 *    - 负载均衡策略：随机、轮询、权重、一致性哈希等
 * 
 * 2. 通信协议（Protocol）
 *    - HTTP/1.1、HTTP/2、TCP 自定义协议
 *    - 协议设计：魔数、版本号、序列化方式、请求 ID、数据长度、数据内容
 * 
 * 3. 序列化方式（Serialization）
 *    - JSON：人类可读，跨语言，但性能一般
 *    - Protobuf：Google 出品，高性能，强类型
 *    - Hessian：Java 友好，二进制协议
 *    - Kryo：高性能 Java 序列化
 *    - Avro：Hadoop 生态常用
 * 
 * 4. 容错机制（Fault Tolerance）
 *    - Failover 失败自动切换：重试其他节点
 *    - Failfast 快速失败：立即报错
 *    - Failsafe 失败安全：记录日志，不重试
 *    - Failback 失败自动恢复：定时重发
 * 
 * 5. 服务治理（Service Governance）
 *    - 服务注册与发现
 *    - 健康检查
 *    - 限流熔断降级
 *    - 监控告警
 *    - 链路追踪
 * 
 * 
 * 【问题 4】JDK 动态代理在 RPC 中是如何应用的？
 * 
 * 【答案】
 * JDK 动态代理是 RPC 客户端实现透明调用的核心技术。
 * 
 * 实现步骤：
 * 1. 实现 InvocationHandler 接口
 * 2. 在 invoke() 方法中：
 *    - 获取方法名、参数类型、参数值
 *    - 封装成 RPC 请求对象
 *    - 通过网络发送到服务端
 *    - 接收响应并返回结果
 * 3. 使用 Proxy.newProxyInstance() 创建代理对象
 * 
 * 代码示例：
 * ```java
 * public static <T> T createProxy(Class<T> interfaceClass) {
 *     return (T) Proxy.newProxyInstance(
 *         interfaceClass.getClassLoader(),
 *         new Class<?>[]{interfaceClass},
 *         (proxy, method, args) -> {
 *             // 1. 封装请求
 *             RpcRequest request = new RpcRequest();
 *             request.setMethodName(method.getName());
 *             request.setParameterTypes(method.getParameterTypes());
 *             request.setParameters(args);
 *             
 *             // 2. 发送请求并接收响应
 *             RpcResponse response = sendRequest(request);
 *             
 *             // 3. 返回结果
 *             return response.getResult();
 *         }
 *     );
 * }
 * ```
 * 
 * CGLIB 代理 vs JDK 动态代理：
 * - JDK 代理：只能代理接口，基于反射实现
 * - CGLIB：可以代理类，基于字节码生成子类
 * - Dubbo 默认使用 Javassist 或 CGLIB 生成代理
 * 
 * 
 * 【问题 5】RPC 中的序列化协议有哪些？如何选择？
 * 
 * 【答案】
 * 
 * 常见序列化协议对比：
 * 
 * 1. JSON
 *    优点：人类可读、跨语言、调试方便
 *    缺点：性能一般、没有类型约束
 *    适用场景：对性能要求不高、需要跨语言的场景
 *    框架：Jackson、Gson、Fastjson
 * 
 * 2. Protobuf（Protocol Buffers）
 *    优点：高性能、小体积、强类型、支持多语言
 *    缺点：需要预定义.proto 文件、调试不便
 *    适用场景：高性能要求的微服务架构
 *    框架：gRPC 默认序列化方式
 * 
 * 3. Hessian
 *    优点：Java 友好、支持复杂对象、二进制协议性能较好
 *    缺点：主要用在 Java 生态
 *    适用场景：Dubbo 早期版本默认
 * 
 * 4. Kryo
 *    优点：高性能、Java 专用
 *    缺点：不支持跨语言、安全性需要注意
 *    适用场景：Java 内部系统高性能场景
 * 
 * 5. Avro
 *    优点：支持 Schema 演化、Hadoop 生态友好
 *    缺点：需要 Schema 定义
 *    适用场景：大数据场景
 * 
 * 选择建议：
 * - 追求性能：Protobuf > Kryo > Hessian > JSON
 * - 跨语言：Protobuf > JSON > Hessian
 * - 调试便利：JSON > Hessian > Protobuf
 * - Dubbo 生态：Hessian2 / Kryo / Protobuf
 * - gRPC 生态：Protobuf
 * 
 * 
 * 【问题 6】RPC 中的负载均衡策略有哪些？
 * 
 * 【答案】
 * 
 * 1. Random（随机）
 *    - 从服务提供者列表中随机选择一个
 *    - 适合集群性能差异不大的场景
 *    - Dubbo 默认策略
 * 
 * 2. RoundRobin（轮询）
 *    - 按顺序依次选择服务提供者
 *    - 请求分布均匀，但忽略机器性能差异
 *    - 加权轮询可根据机器配置分配权重
 * 
 * 3. LeastActive（最少活跃调用）
 *    - 选择当前活跃请求数最少的服务提供者
 *    - 自动适配机器性能，快的机器处理更多请求
 *    - 适合集群性能差异较大的场景
 * 
 * 4. ConsistentHash（一致性哈希）
 *    - 相同参数的请求总是路由到同一个服务提供者
 *    - 适合有状态服务、缓存场景
 *    - 需要处理节点增减时的数据迁移
 * 
 * 5. Weighted Response Time（加权响应时间）
 *    - 根据响应时间动态调整权重
 *    - 响应越快的服务获得更多流量
 *    - 需要持续收集响应时间指标
 * 
 * 6. 自定义策略
 *    - 根据业务需求定制，如按机房、地域、版本等
 * 
 * 实现示例（Dubbo SPI）：
 * ```java
 * @Activate("myLoadBalance")
 * public class MyLoadBalance implements LoadBalance {
 *     @Override
 *     public <T> Invoker<T> select(List<Invoker<T>> invokers, 
 *                                   URL url, 
 *                                   Invocation invocation) {
 *         // 自定义选择逻辑
 *         return invokers.get(0);
 *     }
 * }
 * ```
 * 
 * 
 * 【问题 7】RPC 中的服务注册与发现是如何实现的？
 * 
 * 【答案】
 * 
 * 服务注册与发现是 RPC 的核心机制，主要有两种模式：
 * 
 * 1. 客户端发现模式（Client-side Discovery）
 *    流程：
 *    - 服务启动时向注册中心注册自己
 *    - 客户端调用时从注册中心拉取服务列表
 *    - 客户端在本地进行负载均衡选择
 *    - 直接调用选中的服务提供者
 *    
 *    代表框架：Spring Cloud Netflix（Eureka + Ribbon）
 *    优点：架构简单
 *    缺点：客户端需要维护服务列表，增加复杂度
 * 
 * 2. 服务端发现模式（Server-side Discovery）
 *    流程：
 *    - 客户端通过负载均衡器发起请求
 *    - 负载均衡器查询注册中心
 *    - 负载均衡器转发请求到服务提供者
 *    
 *    代表框架：Kubernetes Service、Istio
 *    优点：客户端无感知
 *    缺点：需要额外的基础设施
 * 
 * 常见注册中心对比：
 * 
 * | 注册中心   | CAP 模型 | 一致性 | 可用性 | 适用场景         |
 * |-----------|---------|--------|--------|------------------|
 * | Zookeeper | CP      | 强一致 | 宕机不可用 | 对一致性要求高   |
 * | Nacos     | AP/CP   | 可切换 | 高可用 | 云原生微服务     |
 * | Eureka    | AP      | 最终一致 | 高可用 | 已停更，不推荐   |
 * | Consul    | CP      | 强一致 | 高可用 | 多数据中心场景   |
 * | Etcd      | CP      | 强一致 | 高可用 | K8s 生态         |
 * 
 * Nacos 服务注册流程：
 * 1. 服务提供者启动后，周期性发送心跳到 Nacos
 * 2. Nacos 维护服务实例的健康状态
 * 3. 订阅者监听服务变化，实时推送服务列表
 * 4. 超过阈值未心跳的实例被标记为不健康并剔除
 * 
 * 
 * 【问题 8】RPC 中的超时与重试机制如何处理？
 * 
 * 【答案】
 * 
 * 1. 超时控制
 *    目的：防止调用方长时间等待，快速释放资源
 *    
 *    超时类型：
 *    - 连接超时：建立连接的超时时间
 *    - 读取超时：等待响应的超时时间
 *    - 总超时：整个调用过程的超时时间
 *    
 *    配置示例（Dubbo）：
 *    ```xml
 *    <dubbo:consumer timeout="3000" />
 *    <dubbo:service timeout="5000" retries="2" />
 *    ```
 * 
 * 2. 重试机制
 *    目的：应对临时性故障，提高成功率
 *    
 *    重试策略：
 *    - 固定间隔重试：每次间隔固定时间
 *    - 指数退避重试：间隔时间指数增长（1s, 2s, 4s, 8s...）
 *    - 带抖动的退避：避免多个客户端同时重试造成雪崩
 *    
 *    注意事项：
 *    - 幂等性：只有幂等操作才能重试（查询、新增防重）
 *    - 非幂等操作：支付、扣款等不能盲目重试
 *    - 重试次数：通常 2-3 次，过多会加剧服务端压力
 *    
 *    代码示例（Spring Retry）：
 *    ```java
 *    @Retryable(
 *        value = {RemoteException.class},
 *        maxAttempts = 3,
 *        backoff = @Backoff(delay = 2000, multiplier = 2)
 *    )
 *    public Result callRemoteService() {
 *        // 调用远程服务
 *    }
 *    
 *    @Recover
 *    public Result recover(RemoteException e) {
 *        // 重试失败后的降级处理
 *        return Result.fail("服务调用失败，已重试 3 次");
 *    }
 *    ```
 * 
 * 3. 最佳实践
 *    - 设置合理的超时时间（P99 响应时间的 2-3 倍）
 *    - 区分读操作和写操作的超时策略
 *    - 重试前判断操作是否幂等
 *    - 结合熔断器使用，避免雪崩效应
 *    - 记录重试日志，便于问题排查
 * 
 * 
 * 【问题 9】RPC 中的熔断与降级是如何实现的？
 * 
 * 【答案】
 * 
 * 1. 熔断器模式（Circuit Breaker）
 *    三种状态：
 *    - Closed（关闭）：正常状态，允许请求通过
 *    - Open（打开）：熔断状态，直接拒绝请求
 *    - Half-Open（半开）：尝试恢复，允许少量请求测试
 *    
 *    状态转换：
 *    - Closed → Open：失败率/慢调用比例超过阈值
 *    - Open → Half-Open：等待恢复时间后自动转换
 *    - Half-Open → Closed：测试请求成功
 *    - Half-Open → Open：测试请求失败
 *    
 *    实现框架：Hystrix（已停更）、Resilience4j、Sentinel
 * 
 * 2. 降级策略（Fallback）
 *    目的：熔断后提供备选方案，保证基本功能可用
 *    
 *    降级方式：
 *    - 返回默认值：返回空对象或缓存数据
 *    - 返回托底数据：返回静态配置的提示语
 *    - 调用备用服务：切换到备用实现
 *    - 异步补偿：记录日志，后续补偿
 *    
 *    代码示例（Resilience4j）：
 *    ```java
 *    CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("rpcService");
 *    
 *    Supplier<Result> supplier = CircuitBreaker
 *        .decorateSupplier(circuitBreaker, () -> rpcService.call());
 *    
 *    Result result = Try.ofSupplier(supplier)
 *        .recover(throwable -> {
 *            // 降级处理
 *            return Result.defaultResult();
 *        })
 *        .get();
 *    ```
 *    
 *    Sentinel 注解方式：
 *    ```java
 *    @SentinelResource(
 *        value = "rpcCall",
 *        fallback = "fallbackHandler",
 *        blockHandler = "blockHandler"
 *    )
 *    public Result callRpc() {
 *        return rpcService.call();
 *    }
 *    
 *    public Result fallbackHandler(Throwable ex) {
 *        // 异常降级
 *        return Result.fail("服务异常：" + ex.getMessage());
 *    }
 *    
 *    public Result blockHandler(BlockException ex) {
 *        // 限流降级
 *        return Result.fail("请求过多，请稍后再试");
 *    }
 *    ```
 * 
 * 3. 熔断规则配置
 *    - 失败率阈值：默认 50%
 *    - 慢调用比例：响应时间超过阈值的请求比例
 *    - 最小请求数：达到该数量才开始统计
 *    - 滑动窗口大小：统计的时间窗口
 *    - 等待时长：熔断后多久进入半开状态
 * 
 * 
 * 【问题 10】如何设计一个高性能的 RPC 框架？
 * 
 * 【答案】
 * 
 * 设计高性能 RPC 框架需要考虑以下方面：
 * 
 * 1. 通信层优化
 *    - 使用 NIO 异步非阻塞 IO（Netty）
 *    - 连接池复用，避免频繁建立连接
 *    - 批量发送，减少网络往返次数
 *    - 使用 TCP 长连接，避免三次握手开销
 * 
 * 2. 序列化层优化
 *    - 选择高性能序列化协议（Protobuf、Kryo）
 *    - 避免使用 Java 原生序列化（性能差、不安全）
 *    - 压缩大数据量（GZIP、Snappy、LZ4）
 * 
 * 3. 线程模型优化
 *    - Reactor 多线程模式
 *    - 业务线程池与 IO 线程池分离
 *    - 无锁化设计（CAS、RingBuffer）
 *    - 线程局部存储（ThreadLocal）减少竞争
 * 
 * 4. 零拷贝技术
 *    - FileChannel.transferTo() 文件传输
 *    - Netty 的 CompositeByteBuf 合并缓冲区
 *    - 堆外内存（DirectMemory）减少拷贝
 * 
 * 5. 编解码优化
 *    - 自定义二进制协议，减少协议头开销
 *    - 字段编码使用 varint 等压缩格式
 *    - 字符串使用 ASCII 编码而非 UTF-8
 * 
 * 6. 服务治理优化
 *    - 本地缓存服务列表，减少注册中心压力
 *    - 异步化调用，提高吞吐量
 *    - 预热机制，避免冷启动性能差
 *    - 优雅停机，正在处理的请求处理完再关闭
 * 
 * 7. 监控与调优
 *    - 全链路监控（SkyWalking、Zipkin）
 *    - 指标收集（QPS、RT、成功率）
 *    - 动态配置调整参数
 *    - 压测找出性能瓶颈
 * 
 * 性能指标参考：
 * - QPS：单机 10 万+（简单场景）
 * - RT：P99 < 10ms（内网）
 * - 吞吐量：100MB/s+
 * - 连接数：单机 10 万+ 并发连接
 * 
 * 
 * 【问题 11】RPC 与 RESTful API 有什么区别？如何选择？
 * 
 * 【答案】
 * 
 * | 对比维度    | RPC                      | RESTful API              |
 * |------------|--------------------------|--------------------------|
 * | 设计理念    | 面向过程，强调方法调用     | 面向对象，强调资源操作     |
 * | 协议        | 自定义协议、TCP、HTTP/2  | HTTP/HTTPS               |
 * | 序列化      | Protobuf、Hessian 等     | JSON、XML                |
 * | 性能        | 高（二进制协议）          | 相对较低（文本协议）       |
 * | 可读性      | 较差                     | 好（人类可读）            |
 * | 强类型      | 是，接口明确             | 弱类型，依赖文档          |
 * | 跨语言      | 支持（需多语言 SDK）      | 天然支持                  |
 * | 浏览器兼容  | 不支持                   | 原生支持                  |
 * | 版本管理    | 接口版本控制             | URL 版本或 Header 版本    |
 * | 调试便利性  | 较难                     | 容易（Postman、curl）     |
 * | 适用场景    | 内部微服务、高性能场景    | 对外 API、移动端、Web      |
 * 
 * 选择建议：
 * 
 * 1. 选择 RPC 的场景：
 *    - 内部微服务之间的高性能调用
 *    - 对延迟敏感的场景（金融交易、实时计算）
 *    - 需要强类型约束的系统
 *    - 复杂的分布式事务场景
 *    - 双向流式通信需求（gRPC Streaming）
 * 
 * 2. 选择 RESTful 的场景：
 *    - 对外的开放平台 API
 *    - 移动端、Web 前端调用
 *    - 需要良好可读性和调试性
 *    - 快速原型开发
 *    - 跨组织、跨公司的系统集成
 * 
 * 3. 混合使用：
 *    - 内部服务间用 RPC（Dubbo、gRPC）
 *    - 对外暴露用 RESTful（Spring MVC）
 *    - 通过 Gateway 进行协议转换
 * 
 * 实际案例：
 * - 阿里巴巴：内部 Dubbo，对外 RESTful/HSF
 * - 腾讯：内部 TARS，对外 RESTful
 * - 字节跳动：内部 Kitex（gRPC），对外 RESTful
 * 
 * 
 * 【问题 12】什么是 gRPC？它有什么特点？
 * 
 * 【答案】
 * 
 * gRPC 是 Google 开源的高性能 RPC 框架，基于 HTTP/2 和 Protobuf。
 * 
 * 核心特点：
 * 
 * 1. 基于 HTTP/2
 *    - 多路复用，单个 TCP 连接并发多个请求
 *    - 头部压缩，减少带宽消耗
 *    - 服务器推送，主动推送数据给客户端
 *    - 双向流式通信
 * 
 * 2. 使用 Protobuf 序列化
 *    - 二进制协议，高性能
 *    - 强类型，自动生成代码
 *    - 支持多语言（Java、Go、Python、C++ 等）
 * 
 * 3. 四种服务模式
 *    - Unary RPC：简单请求响应
 *    - Server streaming RPC：服务端流式
 *    - Client streaming RPC：客户端流式
 *    - Bidirectional streaming RPC：双向流式
 * 
 * 4. 内置支持
 *    - 认证：SSL/TLS、Token
 *    - 负载均衡：Round Robin、Pick First
 *    - 重试、超时、取消
 *    - 健康检查
 *    - 反射（用于调试工具）
 * 
 * 服务定义示例（.proto 文件）：
 * ```protobuf
 * syntax = "proto3";
 * 
 * service UserService {
 *     // 简单 RPC
 *     rpc GetUser(UserRequest) returns (UserResponse);
 *     
 *     // 服务端流式
 *     rpc ListUsers(ListRequest) returns (stream UserResponse);
 *     
 *     // 客户端流式
 *     rpc CreateUser(stream UserResponse) returns (CreateResponse);
 *     
 *     // 双向流式
 *     rpc Chat(stream ChatMessage) returns (stream ChatMessage);
 * }
 * 
 * message UserRequest {
 *     int64 user_id = 1;
 * }
 * 
 * message UserResponse {
 *     string name = 1;
 *     string email = 2;
 * }
 * ```
 * 
 * Java 客户端调用：
 * ```java
 * ManagedChannel channel = ManagedChannelBuilder
 *     .forAddress("localhost", 50051)
 *     .usePlaintext()
 *     .build();
 * 
 * UserServiceGrpc.UserServiceBlockingStub stub = 
 *     UserServiceGrpc.newBlockingStub(channel);
 * 
 * UserResponse response = stub.getUser(
 *     UserRequest.newBuilder().setUserId(1).build()
 * );
 * ```
 * 
 * 适用场景：
 * - 多语言微服务架构
 * - 高性能要求的内部服务调用
 * - 流式数据处理（实时推荐、监控）
 * - 移动端与后端通信（减少流量）
 * 
 * 局限性：
 * - 浏览器支持需要 grpc-web 代理
 * - 对人类可读性要求高的场景不适合
 * - 学习曲线比 RESTful 陡峭
 * 
 * 
 * ========================================
 */
