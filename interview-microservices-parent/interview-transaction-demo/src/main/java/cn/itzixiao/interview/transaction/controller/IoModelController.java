package cn.itzixiao.interview.transaction.controller;

import cn.itzixiao.interview.transaction.io.AioServerDemo;
import cn.itzixiao.interview.transaction.io.BioServerDemo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Java IO 模型对比控制器
 * 
 * Java 三种 IO 模型：
 * 1. BIO（Blocking IO）- 同步阻塞 IO
 * 2. NIO（Non-blocking IO）- 同步非阻塞 IO
 * 3. AIO（Asynchronous IO）- 异步非阻塞 IO
 * 
 * @author itzixiao
 * @since 2026-03-14
 */
@Slf4j
@RestController
@RequestMapping("/api/io")
@Api(tags = "Java IO 模型对比（BIO/NIO/AIO）")
public class IoModelController {
    
    @GetMapping("/bio-summary")
    @ApiOperation("1. BIO 模式详解")
    public String bioSummary() {
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("=== BIO（Blocking IO - 同步阻塞 IO）详解 ===\n\n");
        
        explanation.append("1. 什么是 BIO？\n");
        explanation.append("   BIO 是传统的同步阻塞 IO 模型\n");
        explanation.append("   所有的 IO 操作（连接、读取、写入）都会阻塞当前线程\n");
        explanation.append("   直到操作完成或发生错误\n\n");
        
        explanation.append("2. 核心特点：\n");
        explanation.append("   (1) 同步阻塞：\n");
        explanation.append("       - accept() 阻塞等待客户端连接\n");
        explanation.append("       - read() 阻塞等待数据到达\n");
        explanation.append("       - write() 阻塞等待数据发送完成\n\n");
        explanation.append("   (2) 一对一模型：\n");
        explanation.append("       - 一个连接需要一个独立线程处理\n");
        explanation.append("       - 线程数 = 连接数\n\n");
        explanation.append("   (3) 简单易懂：\n");
        explanation.append("       - 编程模型最简单\n");
        explanation.append("       - 代码直观易读\n\n");
        
        explanation.append("3. 工作流程：\n");
        explanation.append("   Step1: 服务器启动，创建 ServerSocket，绑定端口\n");
        explanation.append("   Step2: 调用 accept() 阻塞等待客户端连接\n");
        explanation.append("   Step3: 客户端连接成功，创建新线程处理\n");
        explanation.append("   Step4: 在线程中调用 read() 阻塞读取数据\n");
        explanation.append("   Step5: 处理数据，调用 write() 阻塞响应\n");
        explanation.append("   Step6: 通信结束，关闭连接和线程\n\n");
        
        explanation.append("4. 优缺点分析：\n");
        explanation.append("   优点：\n");
        explanation.append("     ✓ 编程简单，易于理解和维护\n");
        explanation.append("     ✓ 适合连接数少且固定的场景\n");
        explanation.append("     ✓ 不需要复杂的 IO 多路复用知识\n\n");
        explanation.append("   缺点：\n");
        explanation.append("     ✗ 线程资源消耗大（每个连接一个线程）\n");
        explanation.append("     ✗ 高并发下性能差（线程切换开销大）\n");
        explanation.append("     ✗ 无法处理百万级并发（受限于最大线程数）\n");
        explanation.append("     ✗ 资源利用率低（大量线程等待 IO）\n\n");
        
        explanation.append("5. 应用场景：\n");
        explanation.append("   ✓ 连接数较少的内部系统（<100 个连接）\n");
        explanation.append("   ✓ 对性能要求不高的应用\n");
        explanation.append("   ✓ 学习和理解 IO 的基础模型\n");
        explanation.append("   ✓ 快速原型开发\n\n");
        
        explanation.append("6. 性能瓶颈：\n");
        explanation.append("   - 最大线程数限制（通常 1000-2000）\n");
        explanation.append("   - 线程上下文切换开销\n");
        explanation.append("   - 内存占用（每个线程需要栈空间）\n");
        explanation.append("   - CPU 时间片轮转开销\n\n");
        
        explanation.append("7. 示例代码结构：\n");
        explanation.append("   BioServerDemo.java:\n");
        explanation.append("     - startServer(): 启动服务器\n");
        explanation.append("     - handleClient(): 处理客户端请求\n");
        explanation.append("     - bioClientExample(): 客户端示例\n\n");
        
        explanation.append("8. 测试命令：\n");
        explanation.append("   // 查看 BIO 总结\n");
        explanation.append("   GET /api/io/bio-summary\n");
        
        return explanation.toString();
    }
    
    @GetMapping("/nio-summary")
    @ApiOperation("2. NIO 模式详解")
    public String nioSummary() {
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("=== NIO（Non-blocking IO - 同步非阻塞 IO）详解 ===\n\n");
        
        explanation.append("1. 什么是 NIO？\n");
        explanation.append("   NIO 是同步非阻塞 IO 模型\n");
        explanation.append("   基于 IO 多路复用技术（Selector）\n");
        explanation.append("   一个线程可以管理多个连接\n\n");
        
        explanation.append("2. 核心组件：\n");
        explanation.append("   (1) Channel（通道）：\n");
        explanation.append("       - 双向的，可以读写\n");
        explanation.append("       - 类似管道，但更高效\n\n");
        explanation.append("   (2) Buffer（缓冲区）：\n");
        explanation.append("       - 所有数据都通过 Buffer 读写\n");
        explanation.append("       - 常用：ByteBuffer、CharBuffer\n\n");
        explanation.append("   (3) Selector（选择器）：\n");
        explanation.append("       - 核心组件，实现 IO 多路复用\n");
        explanation.append("       - 监听多个 Channel 的事件\n");
        explanation.append("       - 事件类型：OP_ACCEPT、OP_READ、OP_WRITE\n\n");
        
        explanation.append("3. Reactor 模式：\n");
        explanation.append("   (1) 单线程 Reactor：\n");
        explanation.append("       一个线程处理所有事件（简单但性能受限）\n\n");
        explanation.append("   (2) 多线程 Reactor：\n");
        explanation.append("       Reactor 负责事件分发\n");
        explanation.append("       Worker 线程池处理业务逻辑\n");
        explanation.append("       Netty 采用此模式\n\n");
        explanation.append("   (3) 主从 Reactor：\n");
        explanation.append("       Main Reactor：只处理连接事件\n");
        explanation.append("       Sub Reactor：处理 IO 事件\n");
        explanation.append("       适合超高并发场景\n\n");
        
        explanation.append("4. 工作流程：\n");
        explanation.append("   Step1: 创建 Selector（事件分发器）\n");
        explanation.append("   Step2: Channel 配置为非阻塞并注册到 Selector\n");
        explanation.append("   Step3: Selector 轮询就绪的事件\n");
        explanation.append("   Step4: 分发到对应的 Handler 处理\n");
        explanation.append("   Step5: Handler 执行实际的 IO 操作\n\n");
        
        explanation.append("5. 优缺点分析：\n");
        explanation.append("   优点：\n");
        explanation.append("     ✓ 高并发性能好（一个线程管理多个连接）\n");
        explanation.append("     ✓ 资源利用率高（减少线程数量）\n");
        explanation.append("     ✓ 可扩展性强（支持百万级并发）\n");
        explanation.append("     ✓ 无阻塞开销（非阻塞 IO）\n\n");
        explanation.append("   缺点：\n");
        explanation.append("     ✗ 编程模型复杂\n");
        explanation.append("     ✗ 学习成本高\n");
        explanation.append("     ✗ 代码可读性差\n");
        explanation.append("     ✗ 需要理解 IO 多路复用原理\n\n");
        
        explanation.append("6. 应用场景：\n");
        explanation.append("   ✓ 高并发网络服务器\n");
        explanation.append("   ✓ IM 即时通讯系统\n");
        explanation.append("   ✓ 游戏服务器\n");
        explanation.append("   ✓ RPC 框架（Dubbo、gRPC）\n");
        explanation.append("   ✓ Web 服务器（Tomcat NIO Connector）\n\n");
        
        explanation.append("7. 示例代码结构：\n");
        explanation.append("   NioReactorDemo.java:\n");
        explanation.append("     - selectorExample(): Selector 示例\n");
        explanation.append("     - handleAccept(): 处理接受连接\n");
        explanation.append("     - handleRead(): 处理读事件\n\n");
        
        explanation.append("8. 测试命令：\n");
        explanation.append("   // 查看 NIO Reactor 模式说明\n");
        explanation.append("   GET /api/reactor/nio-reactor-demo\n");
        
        return explanation.toString();
    }
    
    @GetMapping("/aio-summary")
    @ApiOperation("3. AIO 模式详解")
    public String aioSummary() {
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("=== AIO（Asynchronous IO - 异步非阻塞 IO）详解 ===\n\n");
        
        explanation.append("1. 什么是 AIO？\n");
        explanation.append("   AIO 是异步非阻塞 IO 模型\n");
        explanation.append("   真正的异步，IO 操作立即返回\n");
        explanation.append("   操作完成后通过回调通知应用程序\n\n");
        
        explanation.append("2. 核心特点：\n");
        explanation.append("   (1) 异步非阻塞：\n");
        explanation.append("       - IO 操作立即返回，不阻塞线程\n");
        explanation.append("       - 操作系统内核主动通知完成\n\n");
        explanation.append("   (2) 回调机制：\n");
        explanation.append("       - 提供 CompletionHandler 接口\n");
        explanation.append("       - completed(): 操作成功时回调\n");
        explanation.append("       - failed(): 操作失败时回调\n\n");
        explanation.append("   (3) 事件驱动：\n");
        explanation.append("       - 基于操作系统异步 IO\n");
        explanation.append("       - Windows: IOCP（IO Completion Port）\n");
        explanation.append("       - Linux: AIO（支持不完善）\n\n");
        
        explanation.append("3. 工作流程：\n");
        explanation.append("   Step1: 创建 AsynchronousServerSocketChannel\n");
        explanation.append("   Step2: 注册 CompletionHandler\n");
        explanation.append("   Step3: 客户端连接时触发 completed 回调\n");
        explanation.append("   Step4: 异步读写，完成后回调通知\n");
        explanation.append("   Step5: 在回调中处理业务逻辑\n\n");
        
        explanation.append("4. 与 NIO 的区别：\n");
        explanation.append("   NIO（同步非阻塞）：\n");
        explanation.append("     - 应用程序主动轮询 Selector\n");
        explanation.append("     - 需要不断检查是否有事件发生\n");
        explanation.append("     - \"询问\"模式\n\n");
        explanation.append("   AIO（异步非阻塞）：\n");
        explanation.append("     - 操作系统主动通知\n");
        explanation.append("     - 不需要轮询\n");
        explanation.append("     - \"回调\"模式\n\n");
        
        explanation.append("5. 优缺点分析：\n");
        explanation.append("   优点：\n");
        explanation.append("     ✓ 真正的异步，不阻塞线程\n");
        explanation.append("     ✓ 高并发性能优秀\n");
        explanation.append("     ✓ 线程资源消耗少\n");
        explanation.append("     ✓ 编程模型比 NIO 简单（无需轮询）\n\n");
        explanation.append("   缺点：\n");
        explanation.append("     ✗ 依赖操作系统支持\n");
        explanation.append("     ✗ Linux 支持不完善\n");
        explanation.append("     ✗ 生态不如 NIO 成熟\n");
        explanation.append("     ✗ 调试相对复杂\n\n");
        
        explanation.append("6. 应用场景：\n");
        explanation.append("   ✓ 高并发网络服务器\n");
        explanation.append("   ✓ 文件传输服务\n");
        explanation.append("   ✓ 实时数据处理\n");
        explanation.append("   ✓ Windows 平台高性能应用\n\n");
        
        explanation.append("7. 示例代码结构：\n");
        explanation.append("   AioServerDemo.java:\n");
        explanation.append("     - startServer(): 启动 AIO 服务器\n");
        explanation.append("     - aioClientExample(): AIO 客户端示例\n");
        explanation.append("     - printSummary(): 总结说明\n\n");
        
        explanation.append("8. 测试命令：\n");
        explanation.append("   // 查看 AIO 总结\n");
        explanation.append("   GET /api/io/aio-summary\n");
        
        return explanation.toString();
    }
    
    @GetMapping("/compare-all")
    @ApiOperation("4. 三种 IO 模型对比总结")
    public String compareAll() {
        StringBuilder comparison = new StringBuilder();
        
        comparison.append("=== 三种 IO 模型全面对比 ===\n\n");
        
        comparison.append("【对比表格】\n\n");
        comparison.append("┌─────────┬──────────┬──────────┬──────────┐\n");
        comparison.append("│ 特性    │   BIO    │   NIO    │   AIO    │\n");
        comparison.append("├─────────┼──────────┼──────────┼──────────┤\n");
        comparison.append("│ IO 模型   │ 同步阻塞 │ 同步非阻塞│ 异步非阻塞│\n");
        comparison.append("│ 阻塞方式 │ 阻塞     │ 非阻塞   │ 非阻塞   │\n");
        comparison.append("│ 线程模型 │ 一对一   │ 多对一   │ 多对一   │\n");
        comparison.append("│ 实现难度 │ 简单     │ 复杂     │ 中等     │\n");
        comparison.append("│ 并发能力 │ 低       │ 高       │ 高       │\n");
        comparison.append("│ 适用场景 │ 少连接   │ 高并发   │ 高并发   │\n");
        comparison.append("└─────────┴──────────┴──────────┴──────────┘\n\n");
        
        comparison.append("【详细对比】\n\n");
        
        comparison.append("1. 阻塞 vs 非阻塞：\n");
        comparison.append("   BIO: 所有 IO 操作都阻塞线程\n");
        comparison.append("   NIO: IO 操作不阻塞，需要轮询\n");
        comparison.append("   AIO: IO 操作不阻塞，回调通知\n\n");
        
        comparison.append("2. 线程模型：\n");
        comparison.append("   BIO: 一个连接一个线程（1:1）\n");
        comparison.append("   NIO: 多个连接共享一个线程（N:1）\n");
        comparison.append("   AIO: 多个连接共享一个线程（N:1）\n\n");
        
        comparison.append("3. 实现复杂度：\n");
        comparison.append("   BIO: ★☆☆☆☆ 最简单\n");
        comparison.append("   NIO: ★★★☆☆ 最复杂\n");
        comparison.append("   AIO: ★★☆☆☆ 中等\n\n");
        
        comparison.append("4. 并发性能：\n");
        comparison.append("   BIO: ★☆☆☆☆ <1000 连接\n");
        comparison.append("   NIO: ★★★★☆ 10 万 + 连接\n");
        comparison.append("   AIO: ★★★★☆ 10 万 + 连接\n\n");
        
        comparison.append("5. 操作系统支持：\n");
        comparison.append("   BIO: 所有系统完美支持\n");
        comparison.append("   NIO: 所有系统完美支持\n");
        comparison.append("   AIO: Windows(IOCP) > Linux(AIO)\n\n");
        
        comparison.append("【技术选型建议】\n\n");
        
        comparison.append("选择 BIO 的场景：\n");
        comparison.append("✓ 连接数少（<100）\n");
        comparison.append("✓ 快速原型开发\n");
        comparison.append("✓ 团队技术能力有限\n");
        comparison.append("✓ 对性能要求不高\n\n");
        
        comparison.append("选择 NIO 的场景：\n");
        comparison.append("✓ 高并发需求（>1000 连接）\n");
        comparison.append("✓ 需要跨平台支持\n");
        comparison.append("✓ 使用成熟框架（Netty）\n");
        comparison.append("✓ 技术团队实力强\n\n");
        
        comparison.append("选择 AIO 的场景：\n");
        comparison.append("✓ Windows 平台高性能应用\n");
        comparison.append("✓ 文件 IO 密集型应用\n");
        comparison.append("✓ 追求极致性能\n");
        comparison.append("✓ 愿意尝试新技术\n\n");
        
        comparison.append("【主流框架】\n\n");
        comparison.append("BIO 框架：\n");
        comparison.append("- Tomcat BIO Connector（已淘汰）\n");
        comparison.append("- 传统 Socket 编程\n\n");
        
        comparison.append("NIO 框架：\n");
        comparison.append("- Netty（最流行）\n");
        comparison.append("- Tomcat NIO Connector\n");
        comparison.append("- Jetty NIO Connector\n");
        comparison.append("- Grizzly\n\n");
        
        comparison.append("AIO 框架：\n");
        comparison.append("- Netty AIO Transport\n");
        comparison.append("- Apache MINA\n");
        comparison.append("- Java NIO.2 (AsynchronousFileChannel)\n\n");
        
        comparison.append("【面试高频问题】\n\n");
        comparison.append("1. BIO、NIO、AIO 的区别？\n");
        comparison.append("2. NIO 的核心组件？\n");
        comparison.append("3. Select、Poll、Epoll 的区别？\n");
        comparison.append("4. Reactor 模式的三种变体？\n");
        comparison.append("5. Netty 为什么性能高？\n");
        comparison.append("6. 零拷贝原理？\n");
        comparison.append("7. 直接内存 vs 堆内存？\n");
        
        return comparison.toString();
    }
}
