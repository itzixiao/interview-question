package cn.itzixiao.interview.containerization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 容器化示例应用主启动类
 * 
 * @author itzixiao
 * @since 2026-03-15
 */
@SpringBootApplication
@RestController
public class ContainerizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContainerizationApplication.class, args);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public String health() {
        return "Containerization application is running!";
    }

    /**
     * 欢迎接口
     */
    @GetMapping("/")
    public String welcome() {
        return "Welcome to Containerization & Cloud Native Demo!";
    }
}
