package cn.itzixiao.interview.transaction.circular;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Service E - 演示 @Autowired + @PostConstruct 解决循环依赖
 * 
 * @author itzixiao
 * @since 2026-03-13
 */
@Component
public class ServiceE {
    
    @Autowired
    private ServiceF serviceF;
    
    /**
     * PostConstruct 方法
     * Spring 先创建 ServiceE 实例，然后创建 ServiceF，最后执行 init 方法
     * 此时 serviceF 已经被注入，可以安全使用
     */
    @PostConstruct
    public void init() {
        System.out.println("===== ServiceE @PostConstruct 初始化完成 =====");
        serviceF.methodF();
    }
    
    /**
     * E 执行业务逻辑
     */
    public void doSomething() {
        System.out.println("[ServiceE] 执行业务逻辑");
        serviceF.doSomethingElse();
    }
    
    /**
     * E 的其他方法
     */
    public void methodE() {
        System.out.println("[ServiceE] 执行 methodE");
    }
}
