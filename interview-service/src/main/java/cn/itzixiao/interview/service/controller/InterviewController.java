package cn.itzixiao.interview.service.controller;

import cn.itzixiao.interview.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 面试题接口
 */
@RestController
@RequestMapping("/interview")
@RefreshScope  // 支持配置动态刷新
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${interview.welcome:欢迎使用面试题服务}")
    private String welcomeMessage;

    /**
     * 服务信息
     */
    @GetMapping("/info")
    public Result<Map<String, String>> info() {
        Map<String, String> info = new HashMap<>();
        info.put("serviceName", serviceName);
        info.put("serverPort", serverPort);
        info.put("welcome", welcomeMessage);
        return Result.success(info);
    }

    /**
     * 获取面试题列表
     */
    @GetMapping("/questions")
    public Result<String> getQuestions(@RequestParam(required = false) String category) {
        log.info("获取面试题列表, category: {}", category);
        return Result.success("面试题列表 - 来自端口: " + serverPort);
    }

    /**
     * 获取面试题详情
     */
    @GetMapping("/question/{id}")
    public Result<String> getQuestion(@PathVariable Long id) {
        log.info("获取面试题详情, id: {}", id);
        return Result.success("面试题详情 " + id + " - 来自端口: " + serverPort);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("UP");
    }
}
