package cn.itzixiao.interview.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 监控与可观测性示例应用
 * 包含：Prometheus、Grafana、SkyWalking、ELK 等监控链路追踪技术
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@SpringBootApplication
public class ObservabilityApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ObservabilityApplication.class, args);
    }
}
