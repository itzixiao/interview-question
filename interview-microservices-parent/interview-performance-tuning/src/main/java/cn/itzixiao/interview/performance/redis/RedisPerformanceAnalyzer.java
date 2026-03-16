package cn.itzixiao.interview.performance.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Redis 性能分析与优化工具类
 * <p>
 * 大 Key 处理：
 * - 定义：Value 大小超过 10KB 的 Key
 * - 危害：网络阻塞、内存不均、集群迁移慢
 * - 解决：拆分、压缩、使用 Hash 结构
 * <p>
 * 热 Key 处理：
 * - 定义：访问频率 > 1000 次/秒
 * - 危害：单节点负载过高
 * - 解决：本地缓存、多副本、读写分离
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class RedisPerformanceAnalyzer {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisPerformanceAnalyzer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检测大 Key
     * <p>
     * 检测方法：
     * 1. redis-cli --bigkeys
     * 2. MEMORY USAGE key_name
     * 3. 定期扫描分析
     */
    public void detectBigKeys() {
        log.info("========== 大 Key 检测 ==========");

        // 模拟检测逻辑
        Set<String> keys = redisTemplate.keys("*");

        if (keys != null) {
            for (String key : keys) {
                try {
                    Long size = redisTemplate.opsForValue().size(key);
                    if (size != null && size > 10240) { // 大于 10KB
                        log.warn("发现大 Key: {}, 大小：{} bytes", key, size);

                        // 优化建议
                        log.info("优化建议：");
                        log.info("1. 拆分数据为多个小 Key");
                        log.info("2. 使用 Hash 结构代替 String");
                        log.info("3. 压缩数据后存储");
                    }
                } catch (Exception e) {
                    log.debug("Key {} 不是 String 类型，跳过", key);
                }
            }
        }

        log.info("==============================");
    }

    /**
     * 检测热 Key（需要配合监控工具）
     * <p>
     * 检测方法：
     * 1. Redis 4.0+ hotkeys 功能
     * 2. 监控工具：Redis Monitor、Prometheus
     * 3. 日志分析
     */
    public void detectHotKeys() {
        log.info("========== 热 Key 检测 ==========");

        // 实际应该通过监控工具获取
        Map<String, Long> hotKeys = new HashMap<>();
        hotKeys.put("product:1001", 5000L); // 5000 次/秒
        hotKeys.put("user:token:123", 3000L);

        for (Map.Entry<String, Long> entry : hotKeys.entrySet()) {
            if (entry.getValue() > 1000) {
                log.warn("发现热 Key: {}, 访问频率：{} 次/秒", entry.getKey(), entry.getValue());

                // 优化建议
                log.info("优化建议：");
                log.info("1. 使用本地缓存（Caffeine/Guava）");
                log.info("2. 增加从节点副本");
                log.info("3. 开启 Redis 集群读写分离");
            }
        }

        log.info("==============================");
    }

    /**
     * 优化方案演示 - 拆分大 Key
     */
    public void optimizeBigKey() {
        String bigKey = "user:profile:123";

        // ❌ 错误示例 - 单个大 Key
        // redisTemplate.opsForValue().set(bigKey, bigData);

        // ✅ 正确示例 - 拆分为多个小 Key
        for (int i = 0; i < 10; i++) {
            String subKey = bigKey + ":" + i;
            // redisTemplate.opsForValue().set(subKey, subData.get(i));
        }

        log.info("大 Key 已拆分为 10 个小 Key");
    }
}
