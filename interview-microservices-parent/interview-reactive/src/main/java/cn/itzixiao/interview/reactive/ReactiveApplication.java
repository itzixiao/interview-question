package cn.itzixiao.interview.reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 响应式编程示例应用
 * 包含：Reactor 模式、Project Reactor、Spring WebFlux、RSocket、背压机制
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@SpringBootApplication
public class ReactiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveApplication.class, args);
    }
}
