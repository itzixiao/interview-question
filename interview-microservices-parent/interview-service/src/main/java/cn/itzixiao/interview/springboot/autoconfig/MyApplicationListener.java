package cn.itzixiao.interview.springboot.autoconfig;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * 自定义应用事件监听器
 * <p>
 * 监听 Spring Boot 应用生命周期事件
 */
public class MyApplicationListener implements ApplicationListener<ApplicationEvent> {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationStartedEvent) {
            System.out.println("【ApplicationListener】应用已启动");
        } else if (event instanceof ContextRefreshedEvent) {
            System.out.println("【ApplicationListener】上下文已刷新");
        } else if (event instanceof ApplicationReadyEvent) {
            System.out.println("【ApplicationListener】应用已就绪");
        }
    }
}
