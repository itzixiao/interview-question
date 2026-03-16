package cn.itzixiao.interview.transaction.circular;

import org.springframework.stereotype.Component;

/**
 * Service D - 演示 setter 注入解决循环依赖
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Component
public class ServiceD {

    private ServiceC serviceC;

    /**
     * Setter 注入 ServiceC
     * Spring 先创建 ServiceD 实例，然后创建 ServiceC，最后通过 setter 注入
     * 这样可以解决循环依赖问题
     */
    public void setServiceC(ServiceC serviceC) {
        this.serviceC = serviceC;
        System.out.println("===== ServiceD Setter 注入完成 =====");
    }

    /**
     * D 调用 C 的方法
     */
    public void doSomethingElse() {
        System.out.println("[ServiceD] 执行业务逻辑");
        if (serviceC != null) {
            serviceC.methodC();
        }
    }

    /**
     * D 的其他方法
     */
    public void methodD() {
        System.out.println("[ServiceD] 执行 methodD");
    }
}
