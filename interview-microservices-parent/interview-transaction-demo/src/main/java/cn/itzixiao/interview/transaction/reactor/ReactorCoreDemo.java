package cn.itzixiao.interview.transaction.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Project Reactor 核心示例
 * <p>
 * Reactor 是响应式编程的实现库，遵循 Reactive Streams 规范
 * <p>
 * 核心概念：
 * 1. Mono：发出 0-1 个元素的异步序列
 * 2. Flux：发出 0-N 个元素的异步序列
 * 3. 背压（Backpressure）：下游控制上游的发射速率
 * 4. 非阻塞：所有操作都是异步非阻塞的
 *
 * @author itzixiao
 * @since 2026-03-14
 */
public class ReactorCoreDemo {

    /**
     * Mono 示例：处理单个元素
     */
    public static void monoExample() throws InterruptedException {
        System.out.println("========== Mono 示例 ==========");

        // 创建 Mono
        Mono<String> mono = Mono.just("Hello Reactor");

        // 订阅并消费
        CountDownLatch latch = new CountDownLatch(1);
        mono.subscribe(
                data -> System.out.println("收到数据：" + data),
                error -> error.printStackTrace(),
                () -> {
                    System.out.println("完成！");
                    latch.countDown();
                }
        );

        latch.await(1, TimeUnit.SECONDS);
    }

    /**
     * Flux 示例：处理多个元素
     */
    public static void fluxExample() throws InterruptedException {
        System.out.println("\n========== Flux 示例 ==========");

        // 创建 Flux
        Flux<Integer> flux = Flux.range(1, 5);

        // 链式操作
        CountDownLatch latch = new CountDownLatch(1);
        flux.map(i -> i * 2)           // 转换：乘以 2
                .filter(i -> i > 4)         // 过滤：大于 4
                .subscribe(
                        data -> System.out.println("处理结果：" + data),
                        error -> error.printStackTrace(),
                        () -> {
                            System.out.println("完成！");
                            latch.countDown();
                        }
                );

        latch.await(1, TimeUnit.SECONDS);
    }

    /**
     * 异步非阻塞示例
     */
    public static void asyncNonBlockingExample() throws InterruptedException {
        System.out.println("\n========== 异步非阻塞示例 ==========");

        CountDownLatch latch = new CountDownLatch(3);

        // 在弹性调度器上异步执行
        Flux.range(1, 3)
                .map(i -> {
                    System.out.println("处理任务 " + i + ", 线程：" + Thread.currentThread().getName());
                    return "结果-" + i;
                })
                .publishOn(Schedulers.boundedElastic())  // 切换到弹性调度器
                .subscribe(
                        data -> System.out.println("收到：" + data + ", 线程：" + Thread.currentThread().getName()),
                        error -> error.printStackTrace(),
                        () -> latch.countDown()
                );

        latch.await(2, TimeUnit.SECONDS);
    }

    /**
     * 背压示例：控制流速
     */
    public static void backpressureExample() throws InterruptedException {
        System.out.println("\n========== 背压示例 ==========");

        CountDownLatch latch = new CountDownLatch(1);

        // 快速发射 vs 慢速消费
        Flux.interval(Duration.ofMillis(100))  // 每 100ms 发射一个
                .take(5)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(n -> System.out.println("发射：" + n))
                .delayElements(Duration.ofMillis(500))  // 每 500ms 消费一个
                .subscribe(
                        data -> System.out.println("消费：" + data),
                        error -> error.printStackTrace(),
                        () -> {
                            System.out.println("完成！");
                            latch.countDown();
                        }
                );

        latch.await(5, TimeUnit.SECONDS);
    }
}
