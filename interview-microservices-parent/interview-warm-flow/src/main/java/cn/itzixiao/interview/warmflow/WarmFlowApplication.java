package cn.itzixiao.interview.warmflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Warm-Flow 工作流引擎启动类
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
@SpringBootApplication
@MapperScan("cn.itzixiao.interview.warmflow.mapper")
public class WarmFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarmFlowApplication.class, args);
        System.out.println("========================================");
        System.out.println("Warm-Flow 工作流引擎启动成功！");
        System.out.println("API文档地址: http://localhost:8095/doc.html");
        System.out.println("========================================");
    }
}
