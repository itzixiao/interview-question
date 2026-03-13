package cn.itzixiao.interview.transaction.circular;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 循环依赖演示配置 - 使用 @Lazy 注解解决
 * 
 * @author itzixiao
 * @since 2026-03-13
 */
@Configuration
public class CircularDependencyConfig {
    
    /**
     * 使用 @Lazy 注入 ServiceA
     * Spring 会创建 ServiceA 的代理对象，而不是真实对象
     * 当真正调用 ServiceA 的方法时，才会去创建 ServiceB
     * 这样就打破了循环依赖
     */
    @Bean
    public LazyServiceA lazyServiceA() {
        return new LazyServiceA(lazyServiceB());
    }
    
    /**
     * 使用 @Lazy 注入 ServiceB
     */
    @Bean
    public LazyServiceB lazyServiceB() {
        return new LazyServiceB(lazyServiceA());
    }
}

/**
 * Lazy Service A - 演示 @Lazy 解决循环依赖
 */
class LazyServiceA {
    
    private final LazyServiceB serviceB;
    
    public LazyServiceA(@Lazy LazyServiceB serviceB) {
        this.serviceB = serviceB;
        System.out.println("===== LazyServiceA 构造完成 =====");
    }
    
    public void doSomething() {
        System.out.println("[LazyServiceA] 执行业务逻辑");
        serviceB.doSomethingElse();
    }
    
    public void methodA() {
        System.out.println("[LazyServiceA] 执行 methodA");
    }
}

/**
 * Lazy Service B - 演示 @Lazy 解决循环依赖
 */
class LazyServiceB {
    
    private final LazyServiceA serviceA;
    
    public LazyServiceB(@Lazy LazyServiceA serviceA) {
        this.serviceA = serviceA;
        System.out.println("===== LazyServiceB 构造完成 =====");
    }
    
    public void doSomethingElse() {
        System.out.println("[LazyServiceB] 执行业务逻辑");
        serviceA.methodA();
    }
    
    public void methodB() {
        System.out.println("[LazyServiceB] 执行 methodB");
    }
}
