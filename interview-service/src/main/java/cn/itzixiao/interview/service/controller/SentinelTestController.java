package cn.itzixiao.interview.service.controller;

import cn.itzixiao.interview.common.result.Result;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Sentinel 限流测试接口
 */
@Slf4j
@Api(tags = "Sentinel限流测试")
@RestController
@RequestMapping("/sentinel")
public class SentinelTestController {

    /**
     * 简单限流测试 - 可通过控制台配置 QPS 限流
     */
    @ApiOperation("简单限流测试")
    @GetMapping("/hello")
    @SentinelResource(value = "sentinelHello", blockHandler = "helloBlockHandler")
    public Result<String> hello() {
        return Result.success("Hello Sentinel! 当前时间: " + System.currentTimeMillis());
    }

    public Result<String> helloBlockHandler(BlockException ex) {
        return Result.error(429, "【限流】请求太频繁了，请稍后再试！");
    }

    /**
     * 热点参数限流测试 - 针对特定参数值限流
     */
    @ApiOperation("热点参数限流测试")
    @GetMapping("/hot")
    @SentinelResource(value = "sentinelHot", blockHandler = "hotBlockHandler")
    public Result<String> hotParam(
            @RequestParam("userId") String userId,
            @RequestParam(value = "type", defaultValue = "1") Integer type) {
        return Result.success("用户 " + userId + " 访问成功，类型: " + type);
    }

    public Result<String> hotBlockHandler(String userId, Integer type, BlockException ex) {
        return Result.error(429, "【热点限流】用户 " + userId + " 访问过于频繁！");
    }

    /**
     * 慢调用降级测试 - 模拟慢接口
     */
    @ApiOperation("慢调用降级测试")
    @GetMapping("/slow")
    @SentinelResource(value = "sentinelSlow", blockHandler = "slowBlockHandler")
    public Result<String> slowRequest() {
        try {
            // 模拟耗时操作
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Result.success("慢调用完成");
    }

    public Result<String> slowBlockHandler(BlockException ex) {
        return Result.error(503, "【降级】服务响应慢，已降级处理");
    }

    /**
     * 异常比例降级测试 - 模拟随机异常
     */
    @ApiOperation("异常比例降级测试")
    @GetMapping("/error")
    @SentinelResource(value = "sentinelError", blockHandler = "errorBlockHandler", fallback = "errorFallback")
    public Result<String> errorRequest(@RequestParam(value = "fail", defaultValue = "false") Boolean fail) {
        if (fail) {
            throw new RuntimeException("模拟业务异常");
        }
        return Result.success("正常响应");
    }

    public Result<String> errorBlockHandler(Boolean fail, BlockException ex) {
        return Result.error(429, "【限流】请求被拦截");
    }

    public Result<String> errorFallback(Boolean fail, Throwable ex) {
        return Result.error(500, "【降级】业务异常，已降级: " + ex.getMessage());
    }

    /**
     * 获取 Sentinel 监控信息
     */
    @ApiOperation("获取 Sentinel 规则信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("appName", "interview-service");
        info.put("dashboard", "http://localhost:8858");
        info.put("rules", "可通过控制台动态配置");
        info.put("testEndpoints", new String[]{
                "/sentinel/hello - 简单限流",
                "/sentinel/hot?userId=123 - 热点限流",
                "/sentinel/slow - 慢调用降级",
                "/sentinel/error?fail=true - 异常降级"
        });
        return Result.success(info);
    }
}
