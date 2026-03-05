package cn.itzixiao.interview.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 面试题服务启动类
 */
@SpringBootApplication(scanBasePackages = {"cn.itzixiao.interview"})
@EnableDiscoveryClient
@EnableFeignClients
public class InterviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewServiceApplication.class, args);
    }
}
