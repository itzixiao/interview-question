package cn.itzixiao.interview.servicemesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Service Mesh 服务网格示例应用
 * 包含：Istio 架构、Sidecar 模式、流量管理、策略执行、可观测性增强
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@SpringBootApplication
public class ServiceMeshApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ServiceMeshApplication.class, args);
    }
}
