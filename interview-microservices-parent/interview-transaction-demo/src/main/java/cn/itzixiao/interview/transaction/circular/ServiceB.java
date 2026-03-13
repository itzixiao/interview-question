package cn.itzixiao.interview.transaction.circular;

import org.springframework.stereotype.Component;

/**
 * Service B - 演示循环依赖
 * 
 * @author itzixiao
 * @since 2026-03-13
 */
@Component
public class ServiceB {
    
    private final ServiceA serviceA;
    
    /**
     * 构造器注入 ServiceA
     * Spring 会尝试创建 ServiceA，但 ServiceA 又依赖 ServiceB，形成循环依赖
     */
    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
        System.out.println("===== ServiceB 构造器注入完成 =====");
    }
    
    /**
     * B 调用 A 的方法
     */
    public void doSomethingElse() {
        System.out.println("[ServiceB] 执行业务逻辑");
        serviceA.methodA();
    }
    
    /**
     * B 的其他方法
     */
    public void methodB() {
        System.out.println("[ServiceB] 执行 methodB");
    }
}
