package cn.itzixiao.interview.transaction.controller;

import cn.itzixiao.interview.transaction.reactor.NioReactorDemo;
import cn.itzixiao.interview.transaction.reactor.ReactorCoreDemo;
import cn.itzixiao.interview.transaction.reactor.ReactorIODemo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Java Reactor 模式与反应式编程控制器
 * 
 * Reactor 模式（反应器模式）：
 * - 一种事件驱动的设计模式
 * - 核心是 IO 多路复用（Selector/Select/Poll）
 * - 适用于高并发网络服务器
 * 
 * 反应式编程（Reactive Programming）：
 * - 基于数据流和变化传播的编程范式
 * - 核心：异步、非阻塞、背压
 * - 实现：Project Reactor、RxJava
 * 
 * @author itzixiao
 * @since 2026-03-14
 */
@Slf4j
@RestController
@RequestMapping("/api/reactor")
@Api(tags = "Java Reactor 模式与反应式编程")
public class ReactorController {
    
    @GetMapping("/core-demo")
    @ApiOperation("Reactor Core 示例")
    public String reactorCoreDemo() throws InterruptedException {
        log.info("========== Reactor Core 示例开始 ==========");
        
        // Mono 示例
        ReactorCoreDemo.monoExample();
        
        // Flux 示例
        ReactorCoreDemo.fluxExample();
        
        // 异步非阻塞示例
        ReactorCoreDemo.asyncNonBlockingExample();
        
        log.info("========== Reactor Core 示例结束 ==========");
        
        return "Reactor Core 示例已执行，请查看控制台日志";
    }
    
    @GetMapping("/io-demo")
    @ApiOperation("Reactor IO 示例")
    public String reactorIoDemo() throws InterruptedException {
        log.info("========== Reactor IO 示例开始 ==========");
        
        // HTTP 客户端示例
        ReactorIODemo.httpClientExample();
        
        // 文件 IO 示例
        ReactorIODemo.fileIOExample();
        
        // 数据库 IO 示例
        ReactorIODemo.databaseIOExample();
        
        log.info("========== Reactor IO 示例结束 ==========");
        
        return "Reactor IO 示例已执行，请查看控制台日志";
    }
    
    @GetMapping("/nio-reactor-demo")
    @ApiOperation("NIO Reactor 模式示例")
    public String nioReactorDemo() {
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("=== NIO Reactor 模式详解 ===\n\n");
        
        explanation.append("1. 什么是 Reactor 模式？\n");
        explanation.append("   Reactor（反应器）是一种事件驱动的设计模式\n");
        explanation.append("   核心思想：将事件分发和处理分离\n");
        explanation.append("   适用于高并发的网络服务器\n\n");
        
        explanation.append("2. Reactor vs 反应式编程\n");
        explanation.append("   Reactor 模式：\n");
        explanation.append("     - IO 多路复用技术（Selector）\n");
        explanation.append("     - 事件循环 + 处理器\n");
        explanation.append("     - Netty、Mina 等框架的基础\n\n");
        explanation.append("   反应式编程：\n");
        explanation.append("     - 编程范式（Functional Reactive Programming）\n");
        explanation.append("     - 数据流 + 变化传播\n");
        explanation.append("     - Project Reactor、RxJava\n\n");
        
        explanation.append("3. NIO Reactor 工作流程：\n");
        explanation.append("   Step1: 创建 Selector（事件分发器）\n");
        explanation.append("   Step2: Channel 配置为非阻塞并注册到 Selector\n");
        explanation.append("   Step3: Selector 轮询就绪的事件\n");
        explanation.append("   Step4: 分发到对应的 Handler 处理\n\n");
        
        explanation.append("4. Reactor 三种变体：\n");
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
        
        explanation.append("5. 应用场景：\n");
        explanation.append("   - Netty：高性能 RPC 框架\n");
        explanation.append("   - Tomcat NIO Connector\n");
        explanation.append("   - Redis：单线程事件循环\n");
        explanation.append("   - Node.js：事件驱动架构\n\n");
        
        explanation.append("6. 启动 NIO 服务器：\n");
        explanation.append("   访问 http://localhost:8899 测试\n");
        explanation.append("   使用 telnet 或 nc 工具连接\n");
        
        return explanation.toString();
    }
    
    @GetMapping("/flux-stream")
    @ApiOperation("Flux 数据流示例")
    public String fluxStream() throws InterruptedException {
        log.info("========== Flux 数据流示例开始 ==========");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // 创建一个热流（Hot Stream）
        Flux<Long> hotFlux = Flux.interval(Duration.ofSeconds(1))
            .take(5)
            .publish()
            .autoConnect();
        
        // 多个订阅者
        hotFlux.subscribe(
            data -> log.info("订阅者 1 收到：{}", data),
            error -> error.printStackTrace(),
            () -> {
                log.info("订阅者 1 完成");
                latch.countDown();
            }
        );
        
        hotFlux.subscribe(
            data -> log.info("订阅者 2 收到：{}", data),
            error -> error.printStackTrace(),
            () -> log.info("订阅者 2 完成")
        );
        
        latch.await(6, TimeUnit.SECONDS);
        
        log.info("========== Flux 数据流示例结束 ==========");
        
        return "Flux 数据流示例已执行，请查看控制台日志";
    }
    
    @GetMapping("/backpressure-demo")
    @ApiOperation("背压机制演示")
    public String backpressureDemo() throws InterruptedException {
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("=== 背压（Backpressure）机制详解 ===\n\n");
        
        explanation.append("1. 什么是背压？\n");
        explanation.append("   背压是一种流量控制机制\n");
        explanation.append("   当下游处理速度跟不上上游时\n");
        explanation.append("   下游向上游发出信号，减缓发射速率\n\n");
        
        explanation.append("2. 为什么需要背压？\n");
        explanation.append("   - 防止内存溢出（OOM）\n");
        explanation.append("   - 避免资源耗尽\n");
        explanation.append("   - 保证系统稳定性\n\n");
        
        explanation.append("3. 背压策略：\n");
        explanation.append("   (1) onError：抛出异常\n");
        explanation.append("   (2) onBackpressureBuffer：缓存\n");
        explanation.append("   (3) onBackpressureDrop：丢弃\n");
        explanation.append("   (4) onBackpressureLatest：只保留最新\n\n");
        
        explanation.append("4. 示例代码：\n");
        explanation.append("   Flux.range(1, 1000)\n");
        explanation.append("     .onBackpressureBuffer(100)  // 缓存 100 个\n");
        explanation.append("     .subscribe(data -> process(data));\n\n");
        
        explanation.append("5. 实际应用：\n");
        explanation.append("   - 高并发数据采集\n");
        explanation.append("   - 实时数据处理管道\n");
        explanation.append("   - 消息队列消费\n");
        
        return explanation.toString();
    }
}
