package cn.itzixiao.interview.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 服务提供者启动类
 * <p>
 * 用于演示 OpenFeign 微服务调用的服务端
 */
@SpringBootApplication(scanBasePackages = {"cn.itzixiao.interview"})
@EnableDiscoveryClient
public class ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
