package cn.itzixiao.interview.transaction;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 事务传播机制演示应用启动类
 * 
 * @author itzixiao
 * @since 2026-03-13
 */
@SpringBootApplication
@EnableDiscoveryClient(autoRegister = false) // 禁用自动注册到 Nacos（本地开发不需要）
@MapperScan("cn.itzixiao.interview.transaction.mapper")
@EnableTransactionManagement // 启用事务管理
public class TransactionDemoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TransactionDemoApplication.class, args);
        System.out.println("===========================================");
        System.out.println("    事务传播机制演示服务启动成功！");
        System.out.println("    访问地址：http://localhost:8084");
        System.out.println("    API 文档：http://localhost:8084/api/transaction/*");
        System.out.println("===========================================");
    }
}
