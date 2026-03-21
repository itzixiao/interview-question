package cn.itzixiao.interview.cache.memcached.controller;

import cn.itzixiao.interview.cache.memcached.hash.ConsistentHash;
import cn.itzixiao.interview.cache.memcached.lock.MemcachedDistributedLock;
import cn.itzixiao.interview.cache.memcached.service.MemcachedService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Memcached 演示控制器
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Slf4j
@RestController
@RequestMapping("/api/memcached")
@Api(tags = "Memcached 演示接口")
public class MemcachedDemoController {

    private final MemcachedService memcachedService;
    private final MemcachedDistributedLock distributedLock;

    public MemcachedDemoController(MemcachedService memcachedService,
            MemcachedDistributedLock distributedLock) {
        this.memcachedService = memcachedService;
        this.distributedLock = distributedLock;
    }

    @PostMapping("/set")
    @ApiOperation("存储数据到 Memcached")
    public Map<String, Object> set(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(defaultValue = "3600") int expire) {
        boolean success = memcachedService.set(key, value, expire);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("key", key);
        result.put("value", value);
        result.put("expire", expire);
        result.put("message", success ? "存储成功" : "存储失败");
        return result;
    }

    @GetMapping("/get")
    @ApiOperation("从 Memcached 获取数据")
    public Map<String, Object> get(@RequestParam String key) {
        String value = memcachedService.get(key);
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value != null ? value : "null");
        result.put("exists", value != null);
        return result;
    }

    @DeleteMapping("/delete")
    @ApiOperation("从 Memcached 删除数据")
    public Map<String, Object> delete(@RequestParam String key) {
        boolean success = memcachedService.delete(key);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("key", key);
        result.put("message", success ? "删除成功" : "删除失败或 key 不存在");
        return result;
    }

    @PostMapping("/incr")
    @ApiOperation("原子自增操作")
    public Map<String, Object> incr(
            @RequestParam String key,
            @RequestParam(defaultValue = "1") long delta) {
        long result = memcachedService.incr(key, delta);
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("delta", delta);
        response.put("result", result);
        response.put("success", result >= 0);
        return response;
    }

    @PostMapping("/lock/demo")
    @ApiOperation("分布式锁演示")
    public Map<String, Object> lockDemo(
            @RequestParam String resourceId,
            @RequestParam(defaultValue = "30") int expireSeconds,
            @RequestParam(defaultValue = "5") long waitSeconds) {
        
        log.info("尝试获取锁: resourceId={}", resourceId);
        
        String lockValue = distributedLock.lock(
            "lock:" + resourceId, 
            expireSeconds, 
            waitSeconds, 
            TimeUnit.SECONDS
        );
        
        Map<String, Object> result = new HashMap<>();
        if (lockValue == null) {
            result.put("success", false);
            result.put("message", "获取锁超时");
            result.put("resourceId", resourceId);
            return result;
        }
        
        try {
            log.info("获取锁成功，执行业务逻辑: lockValue={}", lockValue);
            Thread.sleep(1000);
            
            result.put("success", true);
            result.put("message", "业务执行成功");
            result.put("resourceId", resourceId);
            result.put("lockValue", lockValue);
            result.put("expireSeconds", expireSeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("success", false);
            result.put("message", "业务执行被中断");
            result.put("resourceId", resourceId);
        } finally {
            boolean released = distributedLock.unlock("lock:" + resourceId, lockValue);
            log.info("释放锁: success={}", released);
        }
        return result;
    }

    @GetMapping("/hash/demo")
    @ApiOperation("一致性哈希演示")
    public Map<String, Object> hashDemo(
            @RequestParam(defaultValue = "3") int nodeCount,
            @RequestParam(defaultValue = "150") int virtualNodes,
            @RequestParam(defaultValue = "1000") int keyCount) {
        
        ConsistentHash<String> hash = new ConsistentHash<>(virtualNodes);
        
        for (int i = 0; i < nodeCount; i++) {
            hash.addNode("server" + (i + 1) + ":11211");
        }
        
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < keyCount; i++) {
            keys.add("key:" + UUID.randomUUID().toString().substring(0, 8));
        }
        
        Map<String, Integer> distribution = hash.getDistribution(keys);
        double stdDev = hash.calculateStdDev(distribution);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeCount", nodeCount);
        result.put("virtualNodes", virtualNodes);
        result.put("keyCount", keyCount);
        result.put("standardDeviation", String.format("%.2f", stdDev));
        result.put("distribution", distribution);
        return result;
    }

    @GetMapping("/stats")
    @ApiOperation("获取 Memcached 统计信息")
    public Map<String, Object> getStats() {
        Map<String, String> stats = memcachedService.getStats();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rawStats", stats);
        
        Map<String, Object> keyMetrics = new LinkedHashMap<>();
        if (stats.containsKey("curr_items")) {
            keyMetrics.put("当前 item 数量", Long.parseLong(stats.get("curr_items")));
        }
        if (stats.containsKey("bytes")) {
            long bytes = Long.parseLong(stats.get("bytes"));
            keyMetrics.put("当前内存使用", formatBytes(bytes));
        }
        if (stats.containsKey("limit_maxbytes")) {
            long limit = Long.parseLong(stats.get("limit_maxbytes"));
            keyMetrics.put("最大内存限制", formatBytes(limit));
            if (stats.containsKey("bytes")) {
                long used = Long.parseLong(stats.get("bytes"));
                keyMetrics.put("内存使用率", String.format("%.2f%%", (double) used / limit * 100));
            }
        }
        if (stats.containsKey("curr_connections")) {
            keyMetrics.put("当前连接数", Long.parseLong(stats.get("curr_connections")));
        }
        result.put("keyMetrics", keyMetrics);
        return result;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
