package cn.itzixiao.interview.cache.memcached.hash;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 一致性哈希算法实现
 * 
 * <h3>核心思想：</h3>
 * <ol>
 *   <li>将服务器节点映射到哈希环上</li>
 *   <li>将数据 key 也映射到哈希环上</li>
 *   <li>数据存储在顺时针方向第一个节点上</li>
 * </ol>
 * 
 * <h3>虚拟节点：</h3>
 * <ul>
 *   <li>解决数据倾斜问题</li>
 *   <li>每个物理节点映射多个虚拟节点</li>
 *   <li>虚拟节点均匀分布在哈希环上</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 创建一致性哈希，每个物理节点 150 个虚拟节点
 * ConsistentHash<String> hash = new ConsistentHash<>(150);
 * 
 * // 添加节点
 * hash.addNode("server1:11211");
 * hash.addNode("server2:11211");
 * hash.addNode("server3:11211");
 * 
 * // 根据 key 获取节点
 * String node = hash.getNode("user:1001");
 * 
 * // 移除节点（只影响相邻节点的数据）
 * hash.removeNode("server1:11211");
 * }
 * </pre>
 * 
 * @param <T> 节点类型
 * @author itzixiao
 * @since 2026-03-21
 */
@Slf4j
public class ConsistentHash<T> {

    /**
     * 哈希环，使用 TreeMap 实现
     * 可以快速找到顺时针方向的下一个节点
     * Key: 虚拟节点的哈希值
     * Value: 物理节点
     */
    private final TreeMap<Long, T> ring = new TreeMap<>();
    
    /**
     * 每个物理节点的虚拟节点数量
     * 建议值：150-200
     * 虚拟节点越多，数据分布越均匀，但内存占用也越大
     */
    private final int virtualNodes;
    
    /**
     * 物理节点集合
     */
    private final Set<T> nodes = new HashSet<>();
    
    /**
     * MD5 消息摘要实例
     * 使用 ThreadLocal 保证线程安全
     */
    private final ThreadLocal<MessageDigest> md5Holder = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // MD5 算法一定存在，不会抛出此异常
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    });

    /**
     * 构造函数
     * 
     * @param virtualNodes 每个物理节点的虚拟节点数量，建议 150-200
     */
    public ConsistentHash(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }

    /**
     * 默认构造函数，使用 150 个虚拟节点
     */
    public ConsistentHash() {
        this(150);
    }

    // ==================== 节点管理 ====================

    /**
     * 添加节点
     * 
     * <p>将物理节点和其虚拟节点添加到哈希环上</p>
     * 
     * @param node 物理节点
     */
    public synchronized void addNode(T node) {
        if (nodes.contains(node)) {
            log.warn("Node already exists: {}", node);
            return;
        }
        
        nodes.add(node);
        
        // 为每个物理节点创建 virtualNodes 个虚拟节点
        for (int i = 0; i < virtualNodes; i++) {
            // 虚拟节点的 key 格式：node##index
            // 使用 ## 分隔符避免与真实 key 冲突
            String virtualKey = node.toString() + "##" + i;
            
            // 计算虚拟节点的哈希值
            long hash = hash(virtualKey);
            
            // 将虚拟节点放入哈希环
            // 如果哈希冲突，后添加的节点会覆盖
            ring.put(hash, node);
        }
        
        log.info("Added node: {}, virtual nodes: {}", node, virtualNodes);
    }

    /**
     * 移除节点
     * 
     * <p>移除物理节点及其所有虚拟节点</p>
     * <p>移除后，原本映射到该节点的数据会自动映射到下一个节点</p>
     * 
     * @param node 物理节点
     */
    public synchronized void removeNode(T node) {
        if (!nodes.contains(node)) {
            log.warn("Node does not exist: {}", node);
            return;
        }
        
        nodes.remove(node);
        
        // 移除所有虚拟节点
        for (int i = 0; i < virtualNodes; i++) {
            String virtualKey = node.toString() + "##" + i;
            long hash = hash(virtualKey);
            ring.remove(hash);
        }
        
        log.info("Removed node: {}", node);
    }

    /**
     * 获取所有节点
     * 
     * @return 节点集合的副本
     */
    public Set<T> getNodes() {
        return new HashSet<>(nodes);
    }

    /**
     * 获取节点数量
     */
    public int getNodeCount() {
        return nodes.size();
    }

    // ==================== 数据路由 ====================

    /**
     * 根据 key 获取对应的节点
     * 
     * <p>算法流程：</p>
     * <ol>
     *   <li>计算 key 的哈希值</li>
     *   <li>在哈希环上顺时针查找第一个节点</li>
     * </ol>
     * 
     * @param key 数据 key
     * @return 对应的物理节点，环为空时返回 null
     */
    public T getNode(String key) {
        if (ring.isEmpty()) {
            log.warn("Hash ring is empty");
            return null;
        }
        
        // 计算 key 的哈希值
        long hash = hash(key);
        
        // 在哈希环上顺时针查找第一个大于等于该哈希值的节点
        // tailMap 返回大于等于给定 key 的子 Map
        Map.Entry<Long, T> entry = ring.ceilingEntry(hash);
        
        if (entry == null) {
            // 如果没有找到，说明哈希值超过了环上最大的节点
            // 返回环上的第一个节点（环形结构）
            entry = ring.firstEntry();
        }
        
        return entry.getValue();
    }

    /**
     * 获取 key 映射的节点及其哈希值
     * 
     * @param key 数据 key
     * @return 包含哈希值和节点的 Map Entry
     */
    public Map.Entry<Long, T> getNodeWithHash(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        
        long hash = hash(key);
        Map.Entry<Long, T> entry = ring.ceilingEntry(hash);
        
        if (entry == null) {
            entry = ring.firstEntry();
        }
        
        return new AbstractMap.SimpleEntry<>(hash, entry.getValue());
    }

    // ==================== 哈希计算 ====================

    /**
     * 计算 MD5 哈希值
     * 
     * <p>使用 MD5 算法计算哈希值，分布更均匀</p>
     * <p>相比简单的取模哈希，MD5 哈希的碰撞率更低</p>
     * 
     * @param key 要计算哈希的字符串
     * @return 哈希值（正数）
     */
    public long hash(String key) {
        MessageDigest md5 = md5Holder.get();
        md5.reset();
        
        // 计算 MD5 哈希
        byte[] digest = md5.digest(key.getBytes(StandardCharsets.UTF_8));
        
        // 取 MD5 的前 8 字节作为哈希值
        // MD5 输出 16 字节，我们取前 8 字节构造 64 位整数
        long hash = 0;
        for (int i = 0; i < 8; i++) {
            // 每字节左移 8 位，然后与当前字节进行或运算
            hash = (hash << 8) | (digest[i] & 0xFF);
        }
        
        // 确保返回正数
        // Long.MIN_VALUE 会导致 ceilingEntry 计算错误
        return hash == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(hash);
    }

    // ==================== 统计分析 ====================

    /**
     * 获取节点分布统计
     * 
     * <p>模拟大量 key 的分布情况，用于验证负载均衡效果</p>
     * 
     * @param keys 要统计的 key 列表
     * @return 节点到 key 数量的映射
     */
    public Map<T, Integer> getDistribution(List<String> keys) {
        Map<T, Integer> distribution = new HashMap<>();
        
        // 初始化所有节点的计数为 0
        for (T node : nodes) {
            distribution.put(node, 0);
        }
        
        // 统计每个 key 映射到的节点
        for (String key : keys) {
            T node = getNode(key);
            if (node != null) {
                distribution.merge(node, 1, Integer::sum);
            }
        }
        
        return distribution;
    }

    /**
     * 计算分布的标准差
     * 
     * <p>标准差越小，说明分布越均匀</p>
     * 
     * @param distribution 节点分布统计
     * @return 标准差
     */
    public double calculateStdDev(Map<T, Integer> distribution) {
        if (distribution.isEmpty()) {
            return 0;
        }
        
        // 计算平均值
        double mean = distribution.values().stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0);
        
        // 计算方差
        double variance = distribution.values().stream()
            .mapToDouble(count -> Math.pow(count - mean, 2))
            .average()
            .orElse(0);
        
        // 返回标准差
        return Math.sqrt(variance);
    }

    /**
     * 打印分布统计信息
     * 
     * @param keys 要统计的 key 列表
     */
    public void printDistributionStats(List<String> keys) {
        Map<T, Integer> distribution = getDistribution(keys);
        double stdDev = calculateStdDev(distribution);
        int totalKeys = keys.size();
        int nodeCount = nodes.size();
        double expectedPerNode = (double) totalKeys / nodeCount;
        
        log.info("=== Consistent Hash Distribution Stats ===");
        log.info("Total keys: {}", totalKeys);
        log.info("Node count: {}", nodeCount);
        log.info("Expected keys per node: %.2f", expectedPerNode);
        log.info("Standard deviation: %.2f", stdDev);
        log.info("Distribution:");
        
        distribution.entrySet().stream()
            .sorted(Map.Entry.<T, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                double percentage = (double) entry.getValue() / totalKeys * 100;
                log.info("  {}: {} keys ({.2f}%)", 
                    entry.getKey(), entry.getValue(), percentage);
            });
    }

    /**
     * 分析节点变更影响
     * 
     * <p>计算添加或移除节点后，有多少比例的 key 需要迁移</p>
     * 
     * @param keys      要分析的 key 列表
     * @param oldRing   变更前的哈希环
     * @return 需要迁移的 key 比例（0-1）
     */
    public double calculateMigrationRatio(List<String> keys, ConsistentHash<T> oldRing) {
        if (keys.isEmpty()) {
            return 0;
        }
        
        int migratedCount = 0;
        for (String key : keys) {
            T newNode = this.getNode(key);
            T oldNode = oldRing.getNode(key);
            
            if (!Objects.equals(newNode, oldNode)) {
                migratedCount++;
            }
        }
        
        return (double) migratedCount / keys.size();
    }
}
