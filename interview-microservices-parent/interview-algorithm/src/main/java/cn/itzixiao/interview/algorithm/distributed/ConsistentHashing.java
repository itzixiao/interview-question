package cn.itzixiao.interview.algorithm.distributed;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 一致性哈希（Consistent Hashing）详解与实现
 *
 * <p>一致性哈希由David Karger等人在1997年提出，主要用于解决分布式系统中
 * 数据分布和负载均衡问题，特别是在节点动态增减时最小化数据迁移。
 *
 * <p>核心思想：
 * 1. 将节点和数据都映射到一个哈希环上（0 ~ 2³²-1）
 * 2. 数据存储在顺时针方向的第一个节点上
 * 3. 节点增减时只影响相邻节点的数据
 *
 * <p>解决的问题：
 * - 传统取模哈希：节点变化导致大量数据重新映射
 * - 一致性哈希：节点变化只影响相邻节点，数据迁移量小
 *
 * <p>虚拟节点：
 * - 解决数据分布不均匀问题
 * - 每个物理节点对应多个虚拟节点
 * - 提高负载均衡性
 *
 * <p>应用场景：
 * - 分布式缓存（Redis Cluster、Memcached）
 * - 分布式存储（Cassandra、DynamoDB）
 * - 负载均衡（Nginx、HAProxy）
 * - 分布式消息队列（Kafka分区分配）
 *
 * @author itzixiao
 * @since 2024-01-01
 */
public class ConsistentHashing {

    /**
     * 哈希环节点
     */
    static class Node {
        String name;       // 节点名称
        String physicalNode; // 所属物理节点（虚拟节点用）
        boolean isVirtual; // 是否为虚拟节点

        Node(String name) {
            this.name = name;
            this.physicalNode = name;
            this.isVirtual = false;
        }

        Node(String name, String physicalNode) {
            this.name = name;
            this.physicalNode = physicalNode;
            this.isVirtual = true;
        }

        @Override
        public String toString() {
            return isVirtual ? name + "(" + physicalNode + ")" : name;
        }
    }

    /**
     * 1. 基础一致性哈希实现
     *
     * <p>数据结构：
     * - TreeMap：有序存储哈希环，支持快速查找
     * - 顺时针查找：使用ceilingEntry或firstEntry
     */
    static class ConsistentHashRing {
        private final TreeMap<Long, Node> ring;  // 哈希环
        private final int virtualNodes;          // 每个物理节点的虚拟节点数

        public ConsistentHashRing() {
            this(0); // 无虚拟节点
        }

        public ConsistentHashRing(int virtualNodes) {
            this.ring = new TreeMap<>();
            this.virtualNodes = virtualNodes;
        }

        /**
         * 添加节点
         */
        public void addNode(String nodeName) {
            if (virtualNodes > 0) {
                // 添加虚拟节点
                for (int i = 0; i < virtualNodes; i++) {
                    String virtualName = nodeName + "#" + i;
                    long hash = hash(virtualName);
                    ring.put(hash, new Node(virtualName, nodeName));
                }
            } else {
                // 添加物理节点
                long hash = hash(nodeName);
                ring.put(hash, new Node(nodeName));
            }
        }

        /**
         * 移除节点
         */
        public void removeNode(String nodeName) {
            if (virtualNodes > 0) {
                // 移除虚拟节点
                for (int i = 0; i < virtualNodes; i++) {
                    String virtualName = nodeName + "#" + i;
                    ring.remove(hash(virtualName));
                }
            } else {
                // 移除物理节点
                ring.remove(hash(nodeName));
            }
        }

        /**
         * 获取数据对应的节点
         */
        public String getNode(String key) {
            if (ring.isEmpty()) {
                return null;
            }

            long hash = hash(key);
            Map.Entry<Long, Node> entry = ring.ceilingEntry(hash);

            // 如果没有找到，返回环的第一个节点（顺时针绕回）
            if (entry == null) {
                entry = ring.firstEntry();
            }

            return entry.getValue().physicalNode;
        }

        /**
         * 获取节点分布统计
         */
        public Map<String, Integer> getNodeDistribution() {
            Map<String, Integer> distribution = new HashMap<>();
            for (Node node : ring.values()) {
                distribution.merge(node.physicalNode, 1, Integer::sum);
            }
            return distribution;
        }

        /**
         * 打印哈希环
         */
        public void printRing() {
            System.out.println("哈希环分布:");
            for (Map.Entry<Long, Node> entry : ring.entrySet()) {
                System.out.printf("  Hash: %10d -> %s%n", entry.getKey(), entry.getValue());
            }
        }

        /**
         * 获取环大小
         */
        public int size() {
            return ring.size();
        }
    }

    /**
     * 2. 带权重的一致性哈希
     *
     * <p>不同节点可以配置不同的权重，权重高的节点分配更多虚拟节点
     */
    static class WeightedConsistentHash {
        private final TreeMap<Long, String> ring;
        private final Map<String, Integer> nodeWeights;
        private final int defaultVirtualNodes;

        public WeightedConsistentHash(int defaultVirtualNodes) {
            this.ring = new TreeMap<>();
            this.nodeWeights = new HashMap<>();
            this.defaultVirtualNodes = defaultVirtualNodes;
        }

        public void addNode(String nodeName, int weight) {
            nodeWeights.put(nodeName, weight);
            int virtualNodes = defaultVirtualNodes * weight;

            for (int i = 0; i < virtualNodes; i++) {
                String virtualName = nodeName + "#" + i;
                ring.put(hash(virtualName), nodeName);
            }
        }

        public void removeNode(String nodeName) {
            int weight = nodeWeights.getOrDefault(nodeName, 1);
            int virtualNodes = defaultVirtualNodes * weight;

            for (int i = 0; i < virtualNodes; i++) {
                ring.remove(hash(nodeName + "#" + i));
            }

            nodeWeights.remove(nodeName);
        }

        public String getNode(String key) {
            if (ring.isEmpty()) return null;

            long hash = hash(key);
            Map.Entry<Long, String> entry = ring.ceilingEntry(hash);

            if (entry == null) {
                entry = ring.firstEntry();
            }

            return entry.getValue();
        }
    }

    /**
     * 3. 带数据迁移统计的一致性哈希
     *
     * <p>用于分析节点变化时的数据迁移量
     */
    static class ConsistentHashWithMigration {
        private final ConsistentHashRing ring;
        private final Set<String> dataKeys;

        public ConsistentHashWithMigration(int virtualNodes) {
            this.ring = new ConsistentHashRing(virtualNodes);
            this.dataKeys = new HashSet<>();
        }

        public void addNode(String nodeName) {
            // 记录添加节点前的数据分布
            Map<String, List<String>> beforeDistribution = getDataDistribution();

            ring.addNode(nodeName);

            // 计算数据迁移
            Map<String, List<String>> afterDistribution = getDataDistribution();
            int migrationCount = calculateMigration(beforeDistribution, afterDistribution);

            System.out.println("添加节点 " + nodeName + "，数据迁移量: " + migrationCount);
        }

        public void removeNode(String nodeName) {
            Map<String, List<String>> beforeDistribution = getDataDistribution();

            ring.removeNode(nodeName);

            Map<String, List<String>> afterDistribution = getDataDistribution();
            int migrationCount = calculateMigration(beforeDistribution, afterDistribution);

            System.out.println("移除节点 " + nodeName + "，数据迁移量: " + migrationCount);
        }

        public void addData(String key) {
            dataKeys.add(key);
        }

        public String getNode(String key) {
            return ring.getNode(key);
        }

        private Map<String, List<String>> getDataDistribution() {
            Map<String, List<String>> distribution = new HashMap<>();
            for (String key : dataKeys) {
                String node = ring.getNode(key);
                distribution.computeIfAbsent(node, k -> new ArrayList<>()).add(key);
            }
            return distribution;
        }

        private int calculateMigration(Map<String, List<String>> before,
                                       Map<String, List<String>> after) {
            int migration = 0;
            for (String key : dataKeys) {
                String beforeNode = null;
                String afterNode = null;

                for (Map.Entry<String, List<String>> entry : before.entrySet()) {
                    if (entry.getValue().contains(key)) {
                        beforeNode = entry.getKey();
                        break;
                    }
                }

                for (Map.Entry<String, List<String>> entry : after.entrySet()) {
                    if (entry.getValue().contains(key)) {
                        afterNode = entry.getKey();
                        break;
                    }
                }

                if (!Objects.equals(beforeNode, afterNode)) {
                    migration++;
                }
            }
            return migration;
        }
    }

    /**
     * 4. 模拟分布式缓存场景
     *
     * <p>模拟Redis Cluster的数据分布
     */
    static class DistributedCacheSimulator {
        private final ConsistentHashRing ring;
        private final Map<String, Map<String, String>> nodeData;  // 每个节点的数据

        public DistributedCacheSimulator(int virtualNodes) {
            this.ring = new ConsistentHashRing(virtualNodes);
            this.nodeData = new HashMap<>();
        }

        public void addServer(String serverName) {
            ring.addNode(serverName);
            nodeData.put(serverName, new HashMap<>());

            // 重新分配数据（实际场景会逐步迁移）
            rebalanceData();
        }

        public void removeServer(String serverName) {
            ring.removeNode(serverName);

            // 迁移数据到其他节点
            Map<String, String> dataToMigrate = nodeData.remove(serverName);
            if (dataToMigrate != null) {
                for (Map.Entry<String, String> entry : dataToMigrate.entrySet()) {
                    String newServer = ring.getNode(entry.getKey());
                    if (newServer != null) {
                        nodeData.get(newServer).put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        public void put(String key, String value) {
            String server = ring.getNode(key);
            if (server != null) {
                nodeData.get(server).put(key, value);
            }
        }

        public String get(String key) {
            String server = ring.getNode(key);
            if (server != null) {
                return nodeData.get(server).get(key);
            }
            return null;
        }

        public void printDistribution() {
            System.out.println("数据分布情况:");
            for (Map.Entry<String, Map<String, String>> entry : nodeData.entrySet()) {
                System.out.printf("  %s: %d 条数据%n", entry.getKey(), entry.getValue().size());
            }
        }

        private void rebalanceData() {
            // 简化的重平衡逻辑：重新分配所有数据
            Map<String, String> allData = new HashMap<>();
            for (Map<String, String> data : nodeData.values()) {
                allData.putAll(data);
            }

            for (Map<String, String> data : nodeData.values()) {
                data.clear();
            }

            for (Map.Entry<String, String> entry : allData.entrySet()) {
                String server = ring.getNode(entry.getKey());
                if (server != null) {
                    nodeData.get(server).put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * 5. 一致性哈希 vs 传统取模哈希对比
     */
    static class HashComparison {

        /**
         * 传统取模哈希
         */
        public static String modHash(String key, List<String> nodes) {
            if (nodes.isEmpty()) return null;
            int hash = key.hashCode();
            int index = Math.abs(hash) % nodes.size();
            return nodes.get(index);
        }

        /**
         * 对比节点变化时的数据迁移
         */
        public static void compareMigration(List<String> keys, int initialNodes, int finalNodes) {
            System.out.println("\n=== 传统取模哈希 vs 一致性哈希 ===");

            List<String> initialNodeList = new ArrayList<>();
            for (int i = 0; i < initialNodes; i++) {
                initialNodeList.add("Node" + i);
            }

            // 传统取模哈希
            Map<String, String> modBefore = new HashMap<>();
            for (String key : keys) {
                modBefore.put(key, modHash(key, initialNodeList));
            }

            List<String> finalNodeList = new ArrayList<>();
            for (int i = 0; i < finalNodes; i++) {
                finalNodeList.add("Node" + i);
            }

            Map<String, String> modAfter = new HashMap<>();
            for (String key : keys) {
                modAfter.put(key, modHash(key, finalNodeList));
            }

            int modMigration = 0;
            for (String key : keys) {
                if (!modBefore.get(key).equals(modAfter.get(key))) {
                    modMigration++;
                }
            }

            System.out.println("传统取模哈希数据迁移: " + modMigration + "/" + keys.size() +
                    " (" + String.format("%.2f", 100.0 * modMigration / keys.size()) + "%)");

            // 一致性哈希
            ConsistentHashRing consistentRing = new ConsistentHashRing(150);
            for (int i = 0; i < initialNodes; i++) {
                consistentRing.addNode("Node" + i);
            }

            Map<String, String> consistentBefore = new HashMap<>();
            for (String key : keys) {
                consistentBefore.put(key, consistentRing.getNode(key));
            }

            if (finalNodes > initialNodes) {
                // 添加节点
                for (int i = initialNodes; i < finalNodes; i++) {
                    consistentRing.addNode("Node" + i);
                }
            } else {
                // 移除节点
                for (int i = finalNodes; i < initialNodes; i++) {
                    consistentRing.removeNode("Node" + i);
                }
            }

            Map<String, String> consistentAfter = new HashMap<>();
            for (String key : keys) {
                consistentAfter.put(key, consistentRing.getNode(key));
            }

            int consistentMigration = 0;
            for (String key : keys) {
                if (!consistentBefore.get(key).equals(consistentAfter.get(key))) {
                    consistentMigration++;
                }
            }

            System.out.println("一致性哈希数据迁移: " + consistentMigration + "/" + keys.size() +
                    " (" + String.format("%.2f", 100.0 * consistentMigration / keys.size()) + "%)");
        }
    }

    /**
     * 6. 负载均衡性测试
     *
     * <p>测试不同虚拟节点数量对数据分布均匀性的影响
     */
    public static void testLoadBalance(int nodeCount, int dataCount, int[] virtualNodeOptions) {
        System.out.println("\n=== 负载均衡性测试 ===");
        System.out.println("节点数: " + nodeCount + ", 数据量: " + dataCount);

        // 生成测试数据
        List<String> testData = new ArrayList<>();
        Random random = new Random(42);
        for (int i = 0; i < dataCount; i++) {
            testData.add("data_" + random.nextInt(1000000));
        }

        for (int vNodes : virtualNodeOptions) {
            ConsistentHashRing ring = new ConsistentHashRing(vNodes);

            // 添加节点
            for (int i = 0; i < nodeCount; i++) {
                ring.addNode("Node" + i);
            }

            // 统计分布
            Map<String, Integer> distribution = new HashMap<>();
            for (String key : testData) {
                String node = ring.getNode(key);
                distribution.merge(node, 1, Integer::sum);
            }

            // 计算标准差
            double mean = dataCount / (double) nodeCount;
            double variance = 0;
            for (int count : distribution.values()) {
                variance += Math.pow(count - mean, 2);
            }
            variance /= nodeCount;
            double stdDev = Math.sqrt(variance);
            double cv = stdDev / mean;  // 变异系数

            System.out.printf("虚拟节点=%d: 标准差=%.2f, 变异系数=%.4f%n",
                    vNodes, stdDev, cv);
        }
    }

    // ==================== 哈希函数 ====================

    /**
     * 使用MD5计算哈希值
     */
    public static long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            // 取前8字节作为long
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            // 降级到简单哈希
            return key.hashCode() & 0xFFFFFFFFL;
        }
    }

    // ==================== 演示与测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 一致性哈希算法演示 ==========\n");

        // 1. 基础一致性哈希演示
        System.out.println("1. 基础一致性哈希（无虚拟节点）:");
        ConsistentHashRing basicRing = new ConsistentHashRing(0);
        basicRing.addNode("NodeA");
        basicRing.addNode("NodeB");
        basicRing.addNode("NodeC");

        System.out.println("   添加节点: NodeA, NodeB, NodeC");
        String[] testKeys = {"key1", "key2", "key3", "key4", "key5"};
        System.out.println("   数据分布:");
        for (String key : testKeys) {
            System.out.printf("     %s -> %s%n", key, basicRing.getNode(key));
        }

        // 2. 带虚拟节点的一致性哈希
        System.out.println("\n2. 带虚拟节点的一致性哈希（150个虚拟节点/物理节点）:");
        ConsistentHashRing virtualRing = new ConsistentHashRing(150);
        virtualRing.addNode("NodeA");
        virtualRing.addNode("NodeB");
        virtualRing.addNode("NodeC");

        System.out.println("   添加节点: NodeA, NodeB, NodeC");
        System.out.println("   虚拟节点总数: " + virtualRing.size());
        System.out.println("   数据分布:");
        for (String key : testKeys) {
            System.out.printf("     %s -> %s%n", key, virtualRing.getNode(key));
        }

        // 3. 节点变化测试
        System.out.println("\n3. 节点变化测试（添加NodeD）:");
        Map<String, String> before = new HashMap<>();
        for (String key : testKeys) {
            before.put(key, virtualRing.getNode(key));
        }

        virtualRing.addNode("NodeD");
        System.out.println("   添加NodeD后的数据分布:");
        int changed = 0;
        for (String key : testKeys) {
            String after = virtualRing.getNode(key);
            String beforeNode = before.get(key);
            boolean isChanged = !beforeNode.equals(after);
            if (isChanged) changed++;
            System.out.printf("     %s: %s -> %s %s%n",
                    key, beforeNode, after, isChanged ? "(迁移)" : "");
        }
        System.out.println("   数据迁移比例: " + changed + "/" + testKeys.length);

        // 4. 带权重的哈希环
        System.out.println("\n4. 带权重的一致性哈希:");
        WeightedConsistentHash weightedRing = new WeightedConsistentHash(50);
        weightedRing.addNode("Server1", 1);  // 权重1
        weightedRing.addNode("Server2", 2);  // 权重2（2倍虚拟节点）
        weightedRing.addNode("Server3", 3);  // 权重3（3倍虚拟节点）

        System.out.println("   Server1(权重1), Server2(权重2), Server3(权重3)");
        Map<String, Integer> weightDist = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String node = weightedRing.getNode("key" + i);
            weightDist.merge(node, 1, Integer::sum);
        }
        System.out.println("   1000个key的分布:");
        for (Map.Entry<String, Integer> entry : weightDist.entrySet()) {
            System.out.printf("     %s: %d (%.1f%%)%n",
                    entry.getKey(), entry.getValue(), entry.getValue() / 10.0);
        }

        // 5. 分布式缓存模拟
        System.out.println("\n5. 分布式缓存模拟:");
        DistributedCacheSimulator cache = new DistributedCacheSimulator(150);
        cache.addServer("Redis1");
        cache.addServer("Redis2");
        cache.addServer("Redis3");

        // 写入数据
        System.out.println("   写入100条数据...");
        for (int i = 0; i < 100; i++) {
            cache.put("user:" + i, "data" + i);
        }
        cache.printDistribution();

        // 添加新服务器
        System.out.println("   添加Redis4...");
        cache.addServer("Redis4");
        cache.printDistribution();

        // 6. 对比测试
        System.out.println("\n6. 传统哈希 vs 一致性哈希对比:");
        List<String> testKeys2 = new ArrayList<>();
        Random rand = new Random(42);
        for (int i = 0; i < 10000; i++) {
            testKeys2.add("key_" + rand.nextInt(1000000));
        }

        // 3节点 -> 4节点
        HashComparison.compareMigration(testKeys2, 3, 4);
        // 4节点 -> 3节点
        HashComparison.compareMigration(testKeys2, 4, 3);

        // 7. 负载均衡性测试
        testLoadBalance(5, 10000, new int[]{0, 50, 150, 300});

        // 8. 实际应用场景
        System.out.println("\n8. 实际应用场景 - 数据库分片:");
        ConsistentHashRing dbRing = new ConsistentHashRing(200);
        dbRing.addNode("DB-Shard-01");
        dbRing.addNode("DB-Shard-02");
        dbRing.addNode("DB-Shard-03");
        dbRing.addNode("DB-Shard-04");

        String[] userIds = {"user_1001", "user_2048", "user_3392", "user_4096", "user_5678"};
        System.out.println("   用户ID分片:");
        for (String userId : userIds) {
            System.out.printf("     %s -> %s%n", userId, dbRing.getNode(userId));
        }

        System.out.println("   添加新分片DB-Shard-05...");
        dbRing.addNode("DB-Shard-05");
        System.out.println("   重新分片后:");
        for (String userId : userIds) {
            System.out.printf("     %s -> %s%n", userId, dbRing.getNode(userId));
        }

        System.out.println("\n========== 演示结束 ==========");
    }
}