package cn.itzixiao.interview.transaction.circular;

import org.springframework.stereotype.Component;

/**
 * Service C - 演示 setter 注入解决循环依赖
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Component
public class ServiceC {

    private ServiceD serviceD;

    /**
     * Setter 注入 ServiceD
     * Spring 先创建 ServiceC 实例，然后创建 ServiceD，最后通过 setter 注入
     * 这样可以解决循环依赖问题
     */
    public void setServiceD(ServiceD serviceD) {
        this.serviceD = serviceD;
        System.out.println("===== ServiceC Setter 注入完成 =====");
    }

    /**
     * C 调用 D 的方法
     */
    public void doSomething() {
        System.out.println("[ServiceC] 执行业务逻辑");
        if (serviceD != null) {
            serviceD.doSomethingElse();
        }
    }

    /**
     * C 的其他方法
     */
    public void methodC() {
        System.out.println("[ServiceC] 执行 methodC");
    }
}
