package cn.itzixiao.interview.transaction.circular;

import org.springframework.stereotype.Component;

/**
 * Service A - 演示循环依赖
 * 
 * @author itzixiao
 * @since 2026-03-13
 */
@Component
public class ServiceA {
    
    private final ServiceB serviceB;
    
    /**
     * 构造器注入 ServiceB
     * Spring 会尝试创建 ServiceB，但 ServiceB 又依赖 ServiceA，形成循环依赖
     */
    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
        System.out.println("===== ServiceA 构造器注入完成 =====");
    }
    
    /**
     * A 调用 B 的方法
     */
    public void doSomething() {
        System.out.println("[ServiceA] 执行业务逻辑");
        serviceB.doSomethingElse();
    }
    
    /**
     * A 的其他方法
     */
    public void methodA() {
        System.out.println("[ServiceA] 执行 methodA");
    }
}
