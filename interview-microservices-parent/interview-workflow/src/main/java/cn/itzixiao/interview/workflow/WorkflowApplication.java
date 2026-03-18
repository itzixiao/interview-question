package cn.itzixiao.interview.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Flowable 工作流审批系统启动类
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@SpringBootApplication
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
        System.out.println("========================================");
        System.out.println("Flowable 工作流审批系统启动成功！");
        System.out.println("API 文档: http://localhost:8088/swagger-ui.html");
        System.out.println("========================================");
    }
}
