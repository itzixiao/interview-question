package cn.itzixiao.interview.rpc;

// 导入 IO 异常类，处理网络/流操作中可能出现的 IO 错误
import java.io.IOException;
// 导入对象输入流，用于从网络字节流中反序列化 Java 对象（读操作）
import java.io.ObjectInputStream;
// 导入对象输出流，用于将 Java 对象序列化为字节流写入网络（写操作）
import java.io.ObjectOutputStream;
// 导入序列化接口，实现该接口的类才能被 ObjectOutputStream/ObjectInputStream 序列化传输
import java.io.Serializable;
// 导入动态代理的调用处理器接口，RPC 客户端代理的核心接口，所有方法调用都会被 invoke() 拦截
import java.lang.reflect.InvocationHandler;
// 导入反射 Method 类，用于在 invoke() 中获取被调用方法的名称、参数类型等元信息
import java.lang.reflect.Method;
// 导入动态代理工厂类，通过 Proxy.newProxyInstance() 在运行时动态生成接口的代理实现
import java.lang.reflect.Proxy;
// 导入服务端套接字，在指定端口监听客户端连接请求（服务器端使用）
import java.net.ServerSocket;
// 导入客户端套接字，用于建立 TCP 连接，进行双向通信（客户端和服务端都会用到）
import java.net.Socket;
// 导入线程池服务接口，用于管理和复用线程，避免每次请求都创建新线程
import java.util.concurrent.ExecutorService;
// 导入线程池工厂类，提供 newFixedThreadPool 等便捷方法创建线程池
import java.util.concurrent.Executors;

/**
 * RPC 核心原理演示
 * <p>
 * RPC (Remote Procedure Call) 远程过程调用
 * 核心目标：让本地程序像调用本地方法一样调用远程服务
 * <p>
 * RPC 架构核心组件：
 * 1. Client - 服务调用方
 * 2. Server - 服务提供方
 * 3. Registry - 服务注册中心（可选，用于服务发现）
 * 4. Protocol - 通信协议（HTTP、TCP 自定义协议等）
 * 5. Serialization - 序列化方式（JSON、Protobuf、Hessian 等）
 * <p>
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

    // 程序入口，throws Exception 声明允许抛出受检异常（Socket、反射、序列化等都会抛出受检异常）
    public static void main(String[] args) throws Exception {
        System.out.println("========== RPC 核心原理演示 ==========\n");

        // 第一步：在后台线程启动 RPC 服务器，开始监听客户端连接
        startRpcServer();

        // 第二步：主线程休眠 2 秒，等待服务器完成端口绑定和初始化，避免客户端提前连接导致 "Connection refused"
        Thread.sleep(2000);

        // 第三步：通过 RpcClient 的工厂方法，利用 JDK 动态代理创建 UserService 的透明代理对象
        // 调用 userService 的任何方法，实际上都会被拦截并通过网络发送到服务器执行
        UserService userService = RpcClient.createProxy(UserService.class, "localhost", 8888);

        // 第四步：像调用本地方法一样调用远程方法，RPC 的透明性在此体现
        System.out.println("\n【客户端调用】开始调用远程服务...");

        // 调用远程 getUserById 方法，传入用户 ID=1L，实际执行在服务器端
        User user = userService.getUserById(1L);
        // 打印从服务器返回并反序列化后的用户对象
        System.out.println("【客户端收到】用户信息：" + user);

        // 调用远程 sayHello 方法，传入姓名参数
        String result = userService.sayHello("张三");
        // 打印从服务器返回的问候字符串结果
        System.out.println("【客户端收到】问候结果：" + result);
    }

    /**
     * 启动 RPC 服务器
     * 在独立的守护线程中运行，不阻塞主线程继续执行客户端逻辑
     */
    private static void startRpcServer() {
        // 创建新线程运行服务器逻辑，使用 Lambda 表达式代替 Runnable 匿名类
        new Thread(() -> {
            try {
                System.out.println("【服务器】正在启动 RPC 服务器，监听端口 8888...");
                // 在 8888 端口创建 ServerSocket，操作系统完成端口绑定，开始接收 TCP 连接请求
                ServerSocket serverSocket = new ServerSocket(8888);
                // 创建固定大小为 10 的线程池，用于并发处理多个客户端请求，避免线程无限增长
                ExecutorService executor = Executors.newFixedThreadPool(10);

                // 服务器主循环：持续监听并接受客户端连接请求
                while (true) {
                    // accept() 是阻塞调用，挂起当前线程直到有客户端连接进来，返回与该客户端通信的 Socket
                    Socket clientSocket = serverSocket.accept();
                    // 将请求处理任务提交给线程池，不在主循环线程中处理，保证服务器可以立即接受下一个连接
                    executor.submit(() -> handleRequest(clientSocket));
                }
            } catch (Exception e) {
                // 打印异常堆栈，便于定位端口占用、权限不足等启动问题
                e.printStackTrace();
            }
        }).start(); // 启动线程，立即返回，不阻塞调用方
    }

    /**
     * 处理单个客户端请求（服务端 Skeleton 的核心逻辑）
     * 完整实现了：反序列化请求 → 反射调用 → 序列化响应 的服务端处理链
     *
     * @param socket 与客户端建立的 TCP 连接套接字
     */
    private static void handleRequest(Socket socket) {
        // try-with-resources：自动关闭 ObjectInputStream 和 ObjectOutputStream，防止资源泄漏
        // 注意：ObjectOutputStream 必须先于 ObjectInputStream 创建（否则可能死锁），
        // 但这里利用 try-with-resources 同时声明，Java 会按声明顺序依次初始化
        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {

            // 从输入流中读取对象并强转为 RpcRequest，这就是反序列化过程（字节流 → Java 对象）
            RpcRequest request = (RpcRequest) input.readObject();
            // 打印接收到的请求信息，便于调试和追踪调用链路
            System.out.println("\n【服务器收到】请求方法：" + request.getMethodName()
                    + ", 参数类型：" + request.getParameterTypes()[0]
                    + ", 参数值：" + request.getParameters()[0]);

            // 在服务端实例化真正的业务实现类（生产环境中通常由 IoC 容器管理，这里简化为手动 new）
            UserServiceImpl userService = new UserServiceImpl();
            // 用于保存方法调用结果，初始化为 null
            Object result = null;

            // 根据请求中的方法名，路由到对应的实际方法调用（简化版服务端分发器）
            // 生产级 RPC 框架会用反射 + 方法注册表实现通用分发，避免硬编码 if-else
            if ("getUserById".equals(request.getMethodName())) {
                // 取出第一个参数并强转为 Long 类型，调用 getUserById 方法
                result = userService.getUserById((Long) request.getParameters()[0]);
            } else if ("sayHello".equals(request.getMethodName())) {
                // 取出第一个参数并强转为 String 类型，调用 sayHello 方法
                result = userService.sayHello((String) request.getParameters()[0]);
            }

            // 将业务方法的返回值封装到 RpcResponse 响应对象中
            RpcResponse response = new RpcResponse();
            // 设置调用结果（如果方法抛出异常，应设置 error 字段而非 result）
            response.setResult(result);
            // 将 RpcResponse 对象序列化写入输出流，通过 TCP 发回给客户端
            output.writeObject(response);

            System.out.println("【服务器返回】结果：" + result);

        } catch (Exception e) {
            // 捕获反序列化、反射调用、网络 IO 等各类异常
            e.printStackTrace();
        } finally {
            // finally 块确保 socket 一定被关闭，释放文件描述符等系统资源
            // 注意：try-with-resources 只关闭了流，socket 本身需要手动关闭
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
 * RPC 请求对象 - 封装客户端调用的所有元信息
 * 该对象会被序列化后通过网络传输到服务端，因此必须实现 Serializable 接口
 */
class RpcRequest implements Serializable {
    // 序列化版本号，用于反序列化时校验类版本一致性，防止因类结构变更导致反序列化失败
    private static final long serialVersionUID = 1L;

    // 目标服务接口的全限定类名，服务端用来查找对应的服务实现（如 cn.itzixiao.interview.rpc.UserService）
    private String className;
    // 被调用的方法名称（如 "getUserById"），服务端据此路由到对应方法
    private String methodName;
    // 方法参数类型数组，与 parameters 配合使用，用于方法重载时精确匹配（Class 数组本身也可序列化）
    private Class<?>[] parameterTypes;
    // 方法实际参数值数组，与 parameterTypes 一一对应，传递真实的调用入参
    private Object[] parameters;

    // ---- 以下为标准 getter/setter，供序列化框架和业务代码读写字段 ----

    // 获取目标类的全限定名
    public String getClassName() {
        return className;
    }

    // 设置目标类的全限定名
    public void setClassName(String className) {
        this.className = className;
    }

    // 获取被调用的方法名
    public String getMethodName() {
        return methodName;
    }

    // 设置被调用的方法名
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    // 获取参数类型数组（用于方法签名匹配）
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    // 设置参数类型数组
    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    // 获取实际参数值数组
    public Object[] getParameters() {
        return parameters;
    }

    // 设置实际参数值数组
    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
}

/**
 * RPC 响应对象 - 封装服务端方法的执行结果或异常信息
 * 服务端处理完请求后，将结果封装到该对象并序列化返回给客户端
 */
class RpcResponse implements Serializable {
    // 序列化版本号，保证客户端和服务端使用同一版本的响应结构进行反序列化
    private static final long serialVersionUID = 1L;

    // 方法调用成功时的返回值，类型为 Object 以支持任意返回类型
    private Object result;
    // 方法调用抛出异常时的错误信息，正常情况下为 null；客户端收到后需判断是否重新抛出
    private Throwable error;

    // 获取方法调用成功的返回值
    public Object getResult() {
        return result;
    }

    // 设置方法调用的返回值（服务端在业务方法执行后调用）
    public void setResult(Object result) {
        this.result = result;
    }

    // 获取调用过程中发生的异常（客户端可根据此判断是否调用失败）
    public Throwable getError() {
        return error;
    }

    // 设置调用过程中捕获的异常（服务端在 catch 块中调用）
    public void setError(Throwable error) {
        this.error = error;
    }
}

// ==================== 服务接口与实现 ====================

/**
 * 用户服务接口 - RPC 的服务契约（Service Contract）
 * 客户端和服务端都依赖此接口，客户端持有其代理对象，服务端提供真实实现
 * 在实际项目中，该接口通常放在独立的 API 模块中，供双方共同依赖
 */
interface UserService {
    // 根据用户 ID 查询用户信息，参数为 Long 类型的用户主键
    User getUserById(Long id);

    // 向指定姓名的用户发送问候，返回问候字符串
    String sayHello(String name);
}

/**
 * 用户服务实现类（服务端真实业务逻辑）
 * 实现 UserService 接口，是 RPC 调用链中真正执行业务代码的一端
 * 生产环境中通常标注 @Service 由 Spring 容器管理，此处简化为普通类
 */
class UserServiceImpl implements UserService {

    // 重写 getUserById，模拟从数据库查询用户，根据 ID 构造一个 User 对象返回
    @Override
    public User getUserById(Long id) {
        // 打印日志，表明该方法在服务端被真实执行，而非客户端本地调用
        System.out.println("【服务端执行】getUserById, id=" + id);
        // 模拟数据库查询：根据 ID 构造并返回一个 User 实体（真实场景中应查询 DB）
        return new User(id, "用户" + id, "user" + id + "@example.com");
    }

    // 重写 sayHello，返回一个包含用户姓名的问候字符串
    @Override
    public String sayHello(String name) {
        // 打印日志，确认方法在服务端执行
        System.out.println("【服务端执行】sayHello, name=" + name);
        // 拼接并返回问候语，客户端将收到这个字符串
        return "Hello, " + name + "! 欢迎使用 RPC 服务";
    }
}

/**
 * 用户实体类 - 需要跨网络传输的领域对象
 * 实现 Serializable 接口，才能被 Java 原生序列化机制转换为字节流在网络中传输
 */
class User implements Serializable {
    // 序列化版本号，客户端和服务端的 User 类版本必须一致，否则反序列化会抛出 InvalidClassException
    private static final long serialVersionUID = 1L;

    // 用户唯一标识，使用包装类 Long（而非基本类型 long）以支持 null 值
    private Long id;
    // 用户姓名
    private String name;
    // 用户邮箱地址
    private String email;

    // 无参构造器，Java 序列化机制在反序列化时需要通过无参构造器创建对象实例
    public User() {
    }

    // 全参构造器，方便在服务端快速创建完整的 User 对象
    public User(Long id, String name, String email) {
        this.id = id;       // 设置用户 ID
        this.name = name;   // 设置用户姓名
        this.email = email; // 设置用户邮箱
    }

    // 重写 toString，方便打印输出时直观展示用户信息，而非默认的内存地址格式
    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}

// ==================== RPC 客户端代理 ====================

/**
 * RPC 客户端 - 动态代理实现（客户端 Stub）
 * <p>
 * 核心原理：
 * 1. 使用 JDK 动态代理创建接口代理对象
 * 2. 拦截所有方法调用，封装成 RPC 请求
 * 3. 通过网络发送到服务端
 * 4. 接收响应并返回结果
 */
class RpcClient {

    /**
     * 创建 RPC 代理对象（工厂方法）
     * 调用方拿到代理对象后，可以像使用本地接口实现一样调用远程服务
     *
     * @param interfaceClass 服务接口的 Class 对象（如 UserService.class）
     * @param host           RPC 服务器的 IP 地址或域名
     * @param port           RPC 服务器监听的端口号
     * @return 实现了 interfaceClass 接口的动态代理对象，类型为 T
     */
    // 抑制"unchecked cast"编译警告，因为 Proxy.newProxyInstance 返回的是 Object，需要强转为 T
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(final Class<T> interfaceClass,
                                    final String host,
                                    final int port) {
        // Proxy.newProxyInstance 在运行时动态生成一个实现了 interfaceClass 的代理类并实例化
        return (T) Proxy.newProxyInstance(
                // 参数1：类加载器，用于加载动态生成的代理类，通常与被代理接口使用同一个类加载器
                interfaceClass.getClassLoader(),
                // 参数2：代理对象需要实现的接口列表，这里只实现 interfaceClass 一个接口
                new Class<?>[]{interfaceClass},
                // 参数3：InvocationHandler 实现，每次调用代理对象的方法都会触发 invoke()
                new InvocationHandler() {
                    @Override
                    // proxy: 代理对象本身（很少直接使用）
                    // method: 当前被调用的方法对象，包含方法名、参数类型等反射元信息
                    // args: 调用方传入的实际参数数组
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        // ---------- Step 1：将方法调用封装为可序列化的 RPC 请求对象 ----------
                        RpcRequest request = new RpcRequest();
                        // 设置目标服务接口的全限定类名，服务端据此找到对应的服务实现
                        request.setClassName(interfaceClass.getName());
                        // 从反射 Method 对象中提取方法名（如 "getUserById"）
                        request.setMethodName(method.getName());
                        // 从反射 Method 对象中提取参数类型数组，用于服务端方法签名匹配
                        request.setParameterTypes(method.getParameterTypes());
                        // 设置调用方传入的实际参数值
                        request.setParameters(args);

                        // ---------- Step 2：建立 TCP 连接，将序列化后的请求发送到服务器 ----------
                        // 向指定 host:port 发起 TCP 连接（三次握手在此完成）
                        Socket socket = new Socket(host, port);
                        // 包装 socket 输出流为对象输出流，支持直接写入 Java 对象（内部完成序列化）
                        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                        // 将 RpcRequest 对象序列化写入网络字节流，发送到服务器
                        output.writeObject(request);

                        // ---------- Step 3：等待并读取服务器的响应 ----------
                        // 包装 socket 输入流为对象输入流，支持从字节流中反序列化 Java 对象
                        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                        // 阻塞等待服务器返回响应，并将字节流反序列化为 RpcResponse 对象
                        RpcResponse response = (RpcResponse) input.readObject();

                        // ---------- Step 4：关闭 TCP 连接，释放系统资源 ----------
                        // 生产级 RPC 框架会使用连接池复用连接，而非每次请求都新建/关闭连接
                        socket.close();

                        // ---------- Step 5：将响应结果返回给调用方 ----------
                        // 生产级实现还应检查 response.getError()，如不为空则重新抛出异常
                        return response.getResult();
                    }
                }
        );
    }
}

/**
 * ========================================
 * RPC 高频面试题目
 * ========================================
 * <p>
 * 【问题 1】什么是 RPC？它的核心优势是什么？
 * <p>
 * 【答案】
 * RPC（Remote Procedure Call）远程过程调用，是一种通过网络从远程计算机程序上请求服务的软件协议。
 * <p>
 * 核心优势：
 * 1. 透明性：对调用者屏蔽了底层网络通信细节，像调用本地方法一样调用远程服务
 * 2. 高效性：相比 RESTful API，RPC 通常使用二进制协议，性能更高
 * 3. 强类型：接口定义明确，编译时就能检查类型错误
 * 4. 支持复杂数据结构：可以直接传递对象、集合等复杂结构
 * 5. 双向通信：支持同步/异步调用、流式传输等多种模式
 * <p>
 * 常见 RPC 框架：Dubbo、gRPC、Thrift、Spring Cloud OpenFeign 等
 * <p>
 * <p>
 * 【问题 2】RPC 的完整工作流程是什么？
 * <p>
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
 * <p>
 * <p>
 * 【问题 3】RPC 框架需要解决哪些核心问题？
 * <p>
 * 【答案】
 * 1. 服务寻址（Addressing）
 * - 如何找到服务提供者？通过注册中心（Nacos、Zookeeper、Eureka）
 * - 负载均衡策略：随机、轮询、权重、一致性哈希等
 * <p>
 * 2. 通信协议（Protocol）
 * - HTTP/1.1、HTTP/2、TCP 自定义协议
 * - 协议设计：魔数、版本号、序列化方式、请求 ID、数据长度、数据内容
 * <p>
 * 3. 序列化方式（Serialization）
 * - JSON：人类可读，跨语言，但性能一般
 * - Protobuf：Google 出品，高性能，强类型
 * - Hessian：Java 友好，二进制协议
 * - Kryo：高性能 Java 序列化
 * - Avro：Hadoop 生态常用
 * <p>
 * 4. 容错机制（Fault Tolerance）
 * - Failover 失败自动切换：重试其他节点
 * - Failfast 快速失败：立即报错
 * - Failsafe 失败安全：记录日志，不重试
 * - Failback 失败自动恢复：定时重发
 * <p>
 * 5. 服务治理（Service Governance）
 * - 服务注册与发现
 * - 健康检查
 * - 限流熔断降级
 * - 监控告警
 * - 链路追踪
 * <p>
 * <p>
 * 【问题 4】JDK 动态代理在 RPC 中是如何应用的？
 * <p>
 * 【答案】
 * JDK 动态代理是 RPC 客户端实现透明调用的核心技术。
 * <p>
 * 实现步骤：
 * 1. 实现 InvocationHandler 接口
 * 2. 在 invoke() 方法中：
 * - 获取方法名、参数类型、参数值
 * - 封装成 RPC 请求对象
 * - 通过网络发送到服务端
 * - 接收响应并返回结果
 * 3. 使用 Proxy.newProxyInstance() 创建代理对象
 * <p>
 * 代码示例：
 * ```java
 * public static <T> T createProxy(Class<T> interfaceClass) {
 * return (T) Proxy.newProxyInstance(
 * interfaceClass.getClassLoader(),
 * new Class<?>[]{interfaceClass},
 * (proxy, method, args) -> {
 * // 1. 封装请求
 * RpcRequest request = new RpcRequest();
 * request.setMethodName(method.getName());
 * request.setParameterTypes(method.getParameterTypes());
 * request.setParameters(args);
 * <p>
 * // 2. 发送请求并接收响应
 * RpcResponse response = sendRequest(request);
 * <p>
 * // 3. 返回结果
 * return response.getResult();
 * }
 * );
 * }
 * ```
 * <p>
 * CGLIB 代理 vs JDK 动态代理：
 * - JDK 代理：只能代理接口，基于反射实现
 * - CGLIB：可以代理类，基于字节码生成子类
 * - Dubbo 默认使用 Javassist 或 CGLIB 生成代理
 * <p>
 * <p>
 * 【问题 5】RPC 中的序列化协议有哪些？如何选择？
 * <p>
 * 【答案】
 * <p>
 * 常见序列化协议对比：
 * <p>
 * 1. JSON
 * 优点：人类可读、跨语言、调试方便
 * 缺点：性能一般、没有类型约束
 * 适用场景：对性能要求不高、需要跨语言的场景
 * 框架：Jackson、Gson、Fastjson
 * <p>
 * 2. Protobuf（Protocol Buffers）
 * 优点：高性能、小体积、强类型、支持多语言
 * 缺点：需要预定义.proto 文件、调试不便
 * 适用场景：高性能要求的微服务架构
 * 框架：gRPC 默认序列化方式
 * <p>
 * 3. Hessian
 * 优点：Java 友好、支持复杂对象、二进制协议性能较好
 * 缺点：主要用在 Java 生态
 * 适用场景：Dubbo 早期版本默认
 * <p>
 * 4. Kryo
 * 优点：高性能、Java 专用
 * 缺点：不支持跨语言、安全性需要注意
 * 适用场景：Java 内部系统高性能场景
 * <p>
 * 5. Avro
 * 优点：支持 Schema 演化、Hadoop 生态友好
 * 缺点：需要 Schema 定义
 * 适用场景：大数据场景
 * <p>
 * 选择建议：
 * - 追求性能：Protobuf > Kryo > Hessian > JSON
 * - 跨语言：Protobuf > JSON > Hessian
 * - 调试便利：JSON > Hessian > Protobuf
 * - Dubbo 生态：Hessian2 / Kryo / Protobuf
 * - gRPC 生态：Protobuf
 * <p>
 * <p>
 * 【问题 6】RPC 中的负载均衡策略有哪些？
 * <p>
 * 【答案】
 * <p>
 * 1. Random（随机）
 * - 从服务提供者列表中随机选择一个
 * - 适合集群性能差异不大的场景
 * - Dubbo 默认策略
 * <p>
 * 2. RoundRobin（轮询）
 * - 按顺序依次选择服务提供者
 * - 请求分布均匀，但忽略机器性能差异
 * - 加权轮询可根据机器配置分配权重
 * <p>
 * 3. LeastActive（最少活跃调用）
 * - 选择当前活跃请求数最少的服务提供者
 * - 自动适配机器性能，快的机器处理更多请求
 * - 适合集群性能差异较大的场景
 * <p>
 * 4. ConsistentHash（一致性哈希）
 * - 相同参数的请求总是路由到同一个服务提供者
 * - 适合有状态服务、缓存场景
 * - 需要处理节点增减时的数据迁移
 * <p>
 * 5. Weighted Response Time（加权响应时间）
 * - 根据响应时间动态调整权重
 * - 响应越快的服务获得更多流量
 * - 需要持续收集响应时间指标
 * <p>
 * 6. 自定义策略
 * - 根据业务需求定制，如按机房、地域、版本等
 * <p>
 * 实现示例（Dubbo SPI）：
 * ```java
 *
 * @Activate("myLoadBalance") public class MyLoadBalance implements LoadBalance {
 * @Override public <T> Invoker<T> select(List<Invoker<T>> invokers,
 * URL url,
 * Invocation invocation) {
 * // 自定义选择逻辑
 * return invokers.get(0);
 * }
 * }
 * ```
 * <p>
 * <p>
 * 【问题 7】RPC 中的服务注册与发现是如何实现的？
 * <p>
 * 【答案】
 * <p>
 * 服务注册与发现是 RPC 的核心机制，主要有两种模式：
 * <p>
 * 1. 客户端发现模式（Client-side Discovery）
 * 流程：
 * - 服务启动时向注册中心注册自己
 * - 客户端调用时从注册中心拉取服务列表
 * - 客户端在本地进行负载均衡选择
 * - 直接调用选中的服务提供者
 * <p>
 * 代表框架：Spring Cloud Netflix（Eureka + Ribbon）
 * 优点：架构简单
 * 缺点：客户端需要维护服务列表，增加复杂度
 * <p>
 * 2. 服务端发现模式（Server-side Discovery）
 * 流程：
 * - 客户端通过负载均衡器发起请求
 * - 负载均衡器查询注册中心
 * - 负载均衡器转发请求到服务提供者
 * <p>
 * 代表框架：Kubernetes Service、Istio
 * 优点：客户端无感知
 * 缺点：需要额外的基础设施
 * <p>
 * 常见注册中心对比：
 * <p>
 * | 注册中心   | CAP 模型 | 一致性 | 可用性 | 适用场景         |
 * |-----------|---------|--------|--------|------------------|
 * | Zookeeper | CP      | 强一致 | 宕机不可用 | 对一致性要求高   |
 * | Nacos     | AP/CP   | 可切换 | 高可用 | 云原生微服务     |
 * | Eureka    | AP      | 最终一致 | 高可用 | 已停更，不推荐   |
 * | Consul    | CP      | 强一致 | 高可用 | 多数据中心场景   |
 * | Etcd      | CP      | 强一致 | 高可用 | K8s 生态         |
 * <p>
 * Nacos 服务注册流程：
 * 1. 服务提供者启动后，周期性发送心跳到 Nacos
 * 2. Nacos 维护服务实例的健康状态
 * 3. 订阅者监听服务变化，实时推送服务列表
 * 4. 超过阈值未心跳的实例被标记为不健康并剔除
 * <p>
 * <p>
 * 【问题 8】RPC 中的超时与重试机制如何处理？
 * <p>
 * 【答案】
 * <p>
 * 1. 超时控制
 * 目的：防止调用方长时间等待，快速释放资源
 * <p>
 * 超时类型：
 * - 连接超时：建立连接的超时时间
 * - 读取超时：等待响应的超时时间
 * - 总超时：整个调用过程的超时时间
 * <p>
 * 配置示例（Dubbo）：
 * ```xml
 * <dubbo:consumer timeout="3000" />
 * <dubbo:service timeout="5000" retries="2" />
 * ```
 * <p>
 * 2. 重试机制
 * 目的：应对临时性故障，提高成功率
 * <p>
 * 重试策略：
 * - 固定间隔重试：每次间隔固定时间
 * - 指数退避重试：间隔时间指数增长（1s, 2s, 4s, 8s...）
 * - 带抖动的退避：避免多个客户端同时重试造成雪崩
 * <p>
 * 注意事项：
 * - 幂等性：只有幂等操作才能重试（查询、新增防重）
 * - 非幂等操作：支付、扣款等不能盲目重试
 * - 重试次数：通常 2-3 次，过多会加剧服务端压力
 * <p>
 * 代码示例（Spring Retry）：
 * ```java
 * @Retryable( value = {RemoteException.class},
 * maxAttempts = 3,
 * backoff = @Backoff(delay = 2000, multiplier = 2)
 * )
 * public Result callRemoteService() {
 * // 调用远程服务
 * }
 * @Recover public Result recover(RemoteException e) {
 * // 重试失败后的降级处理
 * return Result.fail("服务调用失败，已重试 3 次");
 * }
 * ```
 * <p>
 * 3. 最佳实践
 * - 设置合理的超时时间（P99 响应时间的 2-3 倍）
 * - 区分读操作和写操作的超时策略
 * - 重试前判断操作是否幂等
 * - 结合熔断器使用，避免雪崩效应
 * - 记录重试日志，便于问题排查
 * <p>
 * <p>
 * 【问题 9】RPC 中的熔断与降级是如何实现的？
 * <p>
 * 【答案】
 * <p>
 * 1. 熔断器模式（Circuit Breaker）
 * 三种状态：
 * - Closed（关闭）：正常状态，允许请求通过
 * - Open（打开）：熔断状态，直接拒绝请求
 * - Half-Open（半开）：尝试恢复，允许少量请求测试
 * <p>
 * 状态转换：
 * - Closed → Open：失败率/慢调用比例超过阈值
 * - Open → Half-Open：等待恢复时间后自动转换
 * - Half-Open → Closed：测试请求成功
 * - Half-Open → Open：测试请求失败
 * <p>
 * 实现框架：Hystrix（已停更）、Resilience4j、Sentinel
 * <p>
 * 2. 降级策略（Fallback）
 * 目的：熔断后提供备选方案，保证基本功能可用
 * <p>
 * 降级方式：
 * - 返回默认值：返回空对象或缓存数据
 * - 返回托底数据：返回静态配置的提示语
 * - 调用备用服务：切换到备用实现
 * - 异步补偿：记录日志，后续补偿
 * <p>
 * 代码示例（Resilience4j）：
 * ```java
 * CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("rpcService");
 * <p>
 * Supplier<Result> supplier = CircuitBreaker
 * .decorateSupplier(circuitBreaker, () -> rpcService.call());
 * <p>
 * Result result = Try.ofSupplier(supplier)
 * .recover(throwable -> {
 * // 降级处理
 * return Result.defaultResult();
 * })
 * .get();
 * ```
 * <p>
 * Sentinel 注解方式：
 * ```java
 * @SentinelResource( value = "rpcCall",
 * fallback = "fallbackHandler",
 * blockHandler = "blockHandler"
 * )
 * public Result callRpc() {
 * return rpcService.call();
 * }
 * <p>
 * public Result fallbackHandler(Throwable ex) {
 * // 异常降级
 * return Result.fail("服务异常：" + ex.getMessage());
 * }
 * <p>
 * public Result blockHandler(BlockException ex) {
 * // 限流降级
 * return Result.fail("请求过多，请稍后再试");
 * }
 * ```
 * <p>
 * 3. 熔断规则配置
 * - 失败率阈值：默认 50%
 * - 慢调用比例：响应时间超过阈值的请求比例
 * - 最小请求数：达到该数量才开始统计
 * - 滑动窗口大小：统计的时间窗口
 * - 等待时长：熔断后多久进入半开状态
 * <p>
 * <p>
 * 【问题 10】如何设计一个高性能的 RPC 框架？
 * <p>
 * 【答案】
 * <p>
 * 设计高性能 RPC 框架需要考虑以下方面：
 * <p>
 * 1. 通信层优化
 * - 使用 NIO 异步非阻塞 IO（Netty）
 * - 连接池复用，避免频繁建立连接
 * - 批量发送，减少网络往返次数
 * - 使用 TCP 长连接，避免三次握手开销
 * <p>
 * 2. 序列化层优化
 * - 选择高性能序列化协议（Protobuf、Kryo）
 * - 避免使用 Java 原生序列化（性能差、不安全）
 * - 压缩大数据量（GZIP、Snappy、LZ4）
 * <p>
 * 3. 线程模型优化
 * - Reactor 多线程模式
 * - 业务线程池与 IO 线程池分离
 * - 无锁化设计（CAS、RingBuffer）
 * - 线程局部存储（ThreadLocal）减少竞争
 * <p>
 * 4. 零拷贝技术
 * - FileChannel.transferTo() 文件传输
 * - Netty 的 CompositeByteBuf 合并缓冲区
 * - 堆外内存（DirectMemory）减少拷贝
 * <p>
 * 5. 编解码优化
 * - 自定义二进制协议，减少协议头开销
 * - 字段编码使用 varint 等压缩格式
 * - 字符串使用 ASCII 编码而非 UTF-8
 * <p>
 * 6. 服务治理优化
 * - 本地缓存服务列表，减少注册中心压力
 * - 异步化调用，提高吞吐量
 * - 预热机制，避免冷启动性能差
 * - 优雅停机，正在处理的请求处理完再关闭
 * <p>
 * 7. 监控与调优
 * - 全链路监控（SkyWalking、Zipkin）
 * - 指标收集（QPS、RT、成功率）
 * - 动态配置调整参数
 * - 压测找出性能瓶颈
 * <p>
 * 性能指标参考：
 * - QPS：单机 10 万+（简单场景）
 * - RT：P99 < 10ms（内网）
 * - 吞吐量：100MB/s+
 * - 连接数：单机 10 万+ 并发连接
 * <p>
 * <p>
 * 【问题 11】RPC 与 RESTful API 有什么区别？如何选择？
 * <p>
 * 【答案】
 * <p>
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
 * <p>
 * 选择建议：
 * <p>
 * 1. 选择 RPC 的场景：
 * - 内部微服务之间的高性能调用
 * - 对延迟敏感的场景（金融交易、实时计算）
 * - 需要强类型约束的系统
 * - 复杂的分布式事务场景
 * - 双向流式通信需求（gRPC Streaming）
 * <p>
 * 2. 选择 RESTful 的场景：
 * - 对外的开放平台 API
 * - 移动端、Web 前端调用
 * - 需要良好可读性和调试性
 * - 快速原型开发
 * - 跨组织、跨公司的系统集成
 * <p>
 * 3. 混合使用：
 * - 内部服务间用 RPC（Dubbo、gRPC）
 * - 对外暴露用 RESTful（Spring MVC）
 * - 通过 Gateway 进行协议转换
 * <p>
 * 实际案例：
 * - 阿里巴巴：内部 Dubbo，对外 RESTful/HSF
 * - 腾讯：内部 TARS，对外 RESTful
 * - 字节跳动：内部 Kitex（gRPC），对外 RESTful
 * <p>
 * <p>
 * 【问题 12】什么是 gRPC？它有什么特点？
 * <p>
 * 【答案】
 * <p>
 * gRPC 是 Google 开源的高性能 RPC 框架，基于 HTTP/2 和 Protobuf。
 * <p>
 * 核心特点：
 * <p>
 * 1. 基于 HTTP/2
 * - 多路复用，单个 TCP 连接并发多个请求
 * - 头部压缩，减少带宽消耗
 * - 服务器推送，主动推送数据给客户端
 * - 双向流式通信
 * <p>
 * 2. 使用 Protobuf 序列化
 * - 二进制协议，高性能
 * - 强类型，自动生成代码
 * - 支持多语言（Java、Go、Python、C++ 等）
 * <p>
 * 3. 四种服务模式
 * - Unary RPC：简单请求响应
 * - Server streaming RPC：服务端流式
 * - Client streaming RPC：客户端流式
 * - Bidirectional streaming RPC：双向流式
 * <p>
 * 4. 内置支持
 * - 认证：SSL/TLS、Token
 * - 负载均衡：Round Robin、Pick First
 * - 重试、超时、取消
 * - 健康检查
 * - 反射（用于调试工具）
 * <p>
 * 服务定义示例（.proto 文件）：
 * ```protobuf
 * syntax = "proto3";
 * <p>
 * service UserService {
 * // 简单 RPC
 * rpc GetUser(UserRequest) returns (UserResponse);
 * <p>
 * // 服务端流式
 * rpc ListUsers(ListRequest) returns (stream UserResponse);
 * <p>
 * // 客户端流式
 * rpc CreateUser(stream UserResponse) returns (CreateResponse);
 * <p>
 * // 双向流式
 * rpc Chat(stream ChatMessage) returns (stream ChatMessage);
 * }
 * <p>
 * message UserRequest {
 * int64 user_id = 1;
 * }
 * <p>
 * message UserResponse {
 * string name = 1;
 * string email = 2;
 * }
 * ```
 * <p>
 * Java 客户端调用：
 * ```java
 * ManagedChannel channel = ManagedChannelBuilder
 * .forAddress("localhost", 50051)
 * .usePlaintext()
 * .build();
 * <p>
 * UserServiceGrpc.UserServiceBlockingStub stub =
 * UserServiceGrpc.newBlockingStub(channel);
 * <p>
 * UserResponse response = stub.getUser(
 * UserRequest.newBuilder().setUserId(1).build()
 * );
 * ```
 * <p>
 * 适用场景：
 * - 多语言微服务架构
 * - 高性能要求的内部服务调用
 * - 流式数据处理（实时推荐、监控）
 * - 移动端与后端通信（减少流量）
 * <p>
 * 局限性：
 * - 浏览器支持需要 grpc-web 代理
 * - 对人类可读性要求高的场景不适合
 * - 学习曲线比 RESTful 陡峭
 * <p>
 * <p>
 * ========================================
 */
