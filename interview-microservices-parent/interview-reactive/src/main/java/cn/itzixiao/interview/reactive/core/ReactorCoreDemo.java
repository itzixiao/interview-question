package cn.itzixiao.interview.reactive.core;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Reactor 模式与 Project Reactor 核心 API 演示
 * 
 * Project Reactor 是响应式流（Reactive Streams）的实现
 * 核心组件：
 * - Mono：发布 0-1 个元素
 * - Flux：发布 0-N 个元素
 * 
 * 特点：
 * - 声明式组合
 * - 背压支持
 * - 非阻塞异步
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
public class ReactorCoreDemo {
    
    /**
     * Mono 基础示例
     */
    public void monoExample() throws InterruptedException {
        log.info("========== Mono 基础 ==========");
        
        // 创建 Mono
        Mono<String> justMono = Mono.just("Hello");
        Mono<String> emptyMono = Mono.empty();
        Mono<String> errorMono = Mono.error(new RuntimeException("Error"));
        
        // 订阅消费
        justMono.subscribe(
            data -> log.info("收到数据：{}", data),
            error -> log.error("发生错误：{}", error.getMessage()),
            () -> log.info("完成")
        );
        
        Thread.sleep(100);
        log.info("==============================\n");
    }
    
    /**
     * Flux 基础示例
     */
    public void fluxExample() throws InterruptedException {
        log.info("========== Flux 基础 ==========");
        
        // 创建 Flux
        Flux<Integer> numbers = Flux.range(1, 5);
        Flux<String> fromArray = Flux.fromArray(new String[]{"A", "B", "C"});
        Flux<String> fromList = Flux.fromIterable(Arrays.asList("X", "Y", "Z"));
        
        // 订阅消费
        numbers.subscribe(
            num -> log.info("数字：{}", num),
            error -> log.error("错误：{}", error.getMessage()),
            () -> log.info("Flux 完成")
        );
        
        Thread.sleep(100);
        log.info("==============================\n");
    }
    
    /**
     * 操作符示例 - map、filter、flatMap
     */
    public void operatorsExample() throws InterruptedException {
        log.info("========== 操作符 ==========");
        
        // map：转换元素
        Flux<Integer> mapped = Flux.range(1, 5)
                .map(x -> x * 2);
        
        mapped.subscribe(x -> log.info("map 结果：{}", x));
        
        // filter：过滤
        Flux<Integer> filtered = Flux.range(1, 10)
                .filter(x -> x % 2 == 0);
        
        filtered.subscribe(x -> log.info("filter 结果：{}", x));
        
        // flatMap：扁平化转换
        Flux<String> words = Flux.just("hello", "world");
        words.flatMap(word -> 
                Flux.fromArray(word.split(""))
                    .subscribeOn(Schedulers.boundedElastic())
            )
            .subscribe(letter -> log.info("字母：{}", letter));
        
        Thread.sleep(500);
        log.info("==============================\n");
    }
    
    /**
     * 背压（Backpressure）示例
     * 
     * 背压机制：下游控制上游的生产速度
     * 策略：
     * - onError：抛出异常
     * - onBackpressureBuffer：缓冲
     * - onBackpressureDrop：丢弃
     * - onBackpressureLatest：只保留最新
     */
    public void backpressureExample() throws InterruptedException {
        log.info("========== 背压机制 ==========");
        
        // 快速生产，慢速消费
        Flux<Long> fastProducer = Flux.interval(Duration.ofMillis(10))
                .take(100);
        
        // 消费者应用背压策略
        fastProducer
                .onBackpressureBuffer(10)  // 最多缓冲 10 个
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(x -> {
                    try {
                        Thread.sleep(50);  // 模拟慢消费
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                })
                .subscribe(
                        x -> log.info("消费：{}", x),
                        error -> log.error("错误：{}", error.getMessage())
                );
        
        Thread.sleep(3000);
        log.info("==============================\n");
    }
    
    /**
     * 线程调度示例
     * 
     * publishOn：改变后续操作的线程池
     * subscribeOn：改变订阅的线程池
     */
    public void schedulerExample() throws InterruptedException {
        log.info("========== 线程调度 ==========");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        Flux.range(1, 5)
                .publishOn(Schedulers.newSingle("thread-1"))
                .map(x -> {
                    log.info("map 线程：{} - {}", Thread.currentThread().getName(), x);
                    return x * 2;
                })
                .publishOn(Schedulers.newSingle("thread-2"))
                .subscribe(
                        x -> log.info("subscribe 线程：{} - {}", Thread.currentThread().getName(), x),
                        null,
                        latch::countDown
                );
        
        latch.await();
        log.info("==============================\n");
    }
}
