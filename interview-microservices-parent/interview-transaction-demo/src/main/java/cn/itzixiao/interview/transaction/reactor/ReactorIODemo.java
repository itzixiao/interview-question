package cn.itzixiao.interview.transaction.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

/**
 * Reactor IO 示例：非阻塞 IO 操作
 * 
 * 反应式 IO 的核心特点：
 * 1. 非阻塞：IO 操作不会阻塞线程
 * 2. 事件驱动：基于事件循环（EventLoop）处理 IO
 * 3. 背压支持：自动处理流速不匹配
 * 4. 资源高效：少量线程处理大量并发连接
 * 
 * @author itzixiao
 * @since 2026-03-14
 */
public class ReactorIODemo {
    
    /**
     * HTTP 客户端示例：非阻塞 HTTP 请求
     */
    public static void httpClientExample() throws InterruptedException {
        System.out.println("========== Reactor HTTP 客户端示例 ==========");
        
        // 创建非阻塞 HTTP 客户端
        HttpClient client = HttpClient.create();
        
        // 发起 GET 请求
        Mono<String> response = client.get()
            .uri("https://httpbin.org/get")
            .responseContent()
            .aggregate()
            .asString();
        
        // 订阅并消费结果
        response.subscribe(
            body -> System.out.println("响应内容：" + body.substring(0, Math.min(200, body.length()))),
            error -> System.err.println("请求失败：" + error.getMessage())
        );
        
        // 等待响应（实际开发中不需要这样等待）
        Thread.sleep(3000);
    }
    
    /**
     * 文件读取示例：非阻塞文件 IO
     */
    public static void fileIOExample() {
        System.out.println("\n========== Reactor 文件 IO 示例 ==========");
        System.out.println("注意：实际文件 IO 需要配置路径，这里展示概念");
        
        // 使用 Files.readAllLines 的reactive包装
        // 实际项目中通常使用：Files.readAllLines(path)
        //   .map(line -> process(line))
        //   .subscribeOn(Schedulers.boundedElastic())
        
        Flux<String> fileLines = Flux.just(
            "第一行内容",
            "第二行内容",
            "第三行内容"
        )
        .publishOn(Schedulers.boundedElastic());  // 在专用线程池执行 IO
        
        fileLines.subscribe(
            line -> System.out.println("读取行：" + line),
            error -> error.printStackTrace()
        );
    }
    
    /**
     * 数据库操作示例：非阻塞数据库访问
     */
    public static void databaseIOExample() {
        System.out.println("\n========== Reactor 数据库 IO 示例 ==========");
        System.out.println("R2DBC 示例（Reactive Relational Database Connectivity）");
        
        // 使用 R2DBC 进行非阻塞数据库操作
        // 伪代码示例：
        // ConnectionFactory factory = ...
        // DatabaseClient client = DatabaseClient.create(factory);
        // 
        // Flux<User> users = client.execute()
        //     .sql("SELECT * FROM users")
        //     .map((row, metadata) -> new User(...))
        //     .all();
        // 
        // users.subscribe(user -> System.out.println(user));
        
        System.out.println("特点：非阻塞、背压支持、流式处理");
    }
}
