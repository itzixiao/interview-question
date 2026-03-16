package cn.itzixiao.interview.transaction.circular;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Service F - 演示 @Autowired + @PostConstruct 解决循环依赖
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Component
public class ServiceF {

    @Autowired
    private ServiceE serviceE;

    /**
     * PostConstruct 方法
     * Spring 先创建 ServiceF 实例，然后创建 ServiceE，最后执行 init 方法
     * 此时 serviceE 已经被注入，可以安全使用
     */
    @PostConstruct
    public void init() {
        System.out.println("===== ServiceF @PostConstruct 初始化完成 =====");
        serviceE.methodE();
    }

    /**
     * F 执行业务逻辑
     */
    public void doSomethingElse() {
        System.out.println("[ServiceF] 执行业务逻辑");
        if (serviceE != null) {
            serviceE.methodE();
        }
    }

    /**
     * F 的其他方法
     */
    public void methodF() {
        System.out.println("[ServiceF] 执行 methodF");
    }
}
