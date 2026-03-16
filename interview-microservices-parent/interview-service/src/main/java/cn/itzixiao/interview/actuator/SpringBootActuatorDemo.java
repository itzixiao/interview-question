package cn.itzixiao.interview.actuator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring Boot Actuator 使用示例
 * <p>
 * 核心知识点：
 * 1. Actuator 是什么？
 * - Spring Boot 提供的监控和管理功能
 * - 内置多个端点（health, info, metrics 等）
 * - 支持自定义端点
 * <p>
 * 2. 常用端点：
 * - /actuator/health - 健康检查
 * - /actuator/info - 应用信息
 * - /actuator/metrics - 性能指标
 * - /actuator/env - 环境变量
 * - /actuator/beans - Spring Bean 列表
 * - /actuator/threaddump - 线程快照
 * <p>
 * 3. 配置说明：
 * - management.endpoints.web.exposure.include: 暴露的端点
 * - management.endpoint.health.show-details: 健康详情显示级别
 * - management.info.env.enabled: 环境信息启用
 */
@SpringBootApplication
@RestController
public class SpringBootActuatorDemo {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootActuatorDemo.class, args);
    }

    /**
     * 测试接口
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello! Visit /actuator endpoints for monitoring.";
    }
}
