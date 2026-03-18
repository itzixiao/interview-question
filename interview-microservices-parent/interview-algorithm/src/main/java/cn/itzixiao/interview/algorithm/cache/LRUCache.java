package cn.itzixiao.interview.algorithm.cache;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * LRU缓存淘汰策略详解与实现
 *
 * <p>LRU（Least Recently Used，最近最少使用）是一种常用的缓存淘汰策略。
 * 核心思想：当缓存满时，淘汰最久未被访问的数据。
 *
 * <p>设计原理：
 * - 基于时间局部性原理：最近被访问的数据很可能再次被访问
 * - 基于空间局部性原理：相邻数据可能被一起访问
 *
 * <p>实现方式：
 * 1. 哈希表 + 双向链表（O(1)时间复杂度）
 * 2. 使用Java LinkedHashMap（简洁实现）
 * 3. 使用Java LinkedHashMap（访问顺序模式）
 *
 * <p>时间复杂度：
 * - get操作：O(1)
 * - put操作：O(1)
 *
 * <p>空间复杂度：O(capacity)
 *
 * <p>应用场景：
 * - 数据库连接池
 * - 图片缓存
 * - 页面缓存
 * - CPU缓存
 *
 * @author itzixiao
 * @since 2024-01-01
 */
public class LRUCache {

    /**
     * 1. 手动实现LRU缓存（哈希表 + 双向链表）
     *
     * <p>数据结构：
     * - 哈希表：存储key到节点的映射，实现O(1)查找
     * - 双向链表：按访问时间排序，头部为最近访问，尾部为最久未访问
     *
     * <p>操作逻辑：
     * - get：将节点移到链表头部
     * - put：新节点插入头部；若已存在则更新并移到头部；若满则淘汰尾部
     */
    static class LRUCacheManual<K, V> {
        // 双向链表节点
        class DLinkedNode {
            K key;
            V value;
            DLinkedNode prev;
            DLinkedNode next;

            DLinkedNode() {
            }  // 哨兵节点用

            DLinkedNode(K key, V value) {
                this.key = key;
                this.value = value;
            }
        }

        private final int capacity;
        private final Map<K, DLinkedNode> cache;
        private final DLinkedNode head;  // 哨兵头节点
        private final DLinkedNode tail;  // 哨兵尾节点
        private int size;

        public LRUCacheManual(int capacity) {
            this.capacity = capacity;
            this.cache = new HashMap<>();
            this.size = 0;

            // 初始化哨兵节点
            head = new DLinkedNode();
            tail = new DLinkedNode();
            head.next = tail;
            tail.prev = head;
        }

        /**
         * 获取值，并将节点移到头部
         */
        public V get(K key) {
            DLinkedNode node = cache.get(key);
            if (node == null) {
                return null;
            }
            // 移动到头部（表示最近使用）
            moveToHead(node);
            return node.value;
        }

        /**
         * 插入或更新值
         */
        public void put(K key, V value) {
            DLinkedNode node = cache.get(key);

            if (node == null) {
                // 创建新节点
                DLinkedNode newNode = new DLinkedNode(key, value);
                cache.put(key, newNode);
                addToHead(newNode);
                size++;

                // 超出容量，淘汰尾部节点
                if (size > capacity) {
                    DLinkedNode tailNode = removeTail();
                    cache.remove(tailNode.key);
                    size--;
                }
            } else {
                // 更新现有节点
                node.value = value;
                moveToHead(node);
            }
        }

        /**
         * 删除节点
         */
        public void remove(K key) {
            DLinkedNode node = cache.get(key);
            if (node != null) {
                removeNode(node);
                cache.remove(key);
                size--;
            }
        }

        /**
         * 获取当前大小
         */
        public int size() {
            return size;
        }

        /**
         * 检查是否包含key
         */
        public boolean containsKey(K key) {
            return cache.containsKey(key);
        }

        /**
         * 打印当前缓存状态（调试用）
         */
        public void printCache() {
            System.out.print("Cache [head -> tail]: ");
            DLinkedNode curr = head.next;
            while (curr != tail) {
                System.out.print("(" + curr.key + "=" + curr.value + ") ");
                curr = curr.next;
            }
            System.out.println();
        }

        // 双向链表操作
        private void addToHead(DLinkedNode node) {
            node.prev = head;
            node.next = head.next;
            head.next.prev = node;
            head.next = node;
        }

        private void removeNode(DLinkedNode node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        private void moveToHead(DLinkedNode node) {
            removeNode(node);
            addToHead(node);
        }

        private DLinkedNode removeTail() {
            DLinkedNode node = tail.prev;
            removeNode(node);
            return node;
        }
    }

    /**
     * 2. 使用LinkedHashMap实现LRU（简洁版）
     *
     * <p>原理：LinkedHashMap维护插入顺序，可以重写removeEldestEntry实现LRU
     */
    static class LRUCacheLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public LRUCacheLinkedHashMap(int capacity) {
            // accessOrder = false，按插入顺序
            super(capacity, 0.75f, false);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    /**
     * 3. 使用LinkedHashMap实现LRU（访问顺序版）
     *
     * <p>原理：accessOrder = true，按访问顺序排序
     * 每次访问元素，该元素会被移到链表尾部
     */
    static class LRUCacheAccessOrder<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public LRUCacheAccessOrder(int capacity) {
            // accessOrder = true，按访问顺序
            super(capacity, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    /**
     * 4. 线程安全的LRU缓存（使用Synchronized）
     */
    static class ThreadSafeLRUCache<K, V> {
        private final LRUCacheManual<K, V> cache;

        public ThreadSafeLRUCache(int capacity) {
            this.cache = new LRUCacheManual<>(capacity);
        }

        public synchronized V get(K key) {
            return cache.get(key);
        }

        public synchronized void put(K key, V value) {
            cache.put(key, value);
        }

        public synchronized void remove(K key) {
            cache.remove(key);
        }
    }

    /**
     * 5. 带过期时间的LRU缓存
     *
     * <p>扩展：为每个缓存项添加过期时间
     */
    static class ExpirableLRUCache<K, V> {
        class CacheEntry {
            V value;
            long expireTime;

            CacheEntry(V value, long ttlMillis) {
                this.value = value;
                this.expireTime = System.currentTimeMillis() + ttlMillis;
            }

            boolean isExpired() {
                return System.currentTimeMillis() > expireTime;
            }
        }

        private final LRUCacheManual<K, CacheEntry> cache;

        public ExpirableLRUCache(int capacity) {
            this.cache = new LRUCacheManual<>(capacity);
        }

        public V get(K key) {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            return entry.value;
        }

        public void put(K key, V value, long ttlMillis) {
            cache.put(key, new CacheEntry(value, ttlMillis));
        }
    }

    /**
     * 6. LFU缓存（Least Frequently Used）
     *
     * <p>与LRU的区别：按访问频率淘汰，而非时间
     */
    static class LFUCache<K, V> {
        class Node {
            K key;
            V value;
            int freq;

            Node(K key, V value) {
                this.key = key;
                this.value = value;
                this.freq = 1;
            }
        }

        private final int capacity;
        private final Map<K, Node> cache;
        private final Map<Integer, LinkedHashSet<K>> freqMap;
        private int minFreq;

        public LFUCache(int capacity) {
            this.capacity = capacity;
            this.cache = new HashMap<>();
            this.freqMap = new HashMap<>();
            this.minFreq = 0;
        }

        public V get(K key) {
            Node node = cache.get(key);
            if (node == null) {
                return null;
            }
            increaseFreq(node);
            return node.value;
        }

        public void put(K key, V value) {
            if (capacity == 0) return;

            Node node = cache.get(key);
            if (node != null) {
                node.value = value;
                increaseFreq(node);
                return;
            }

            if (cache.size() >= capacity) {
                removeMinFreq();
            }

            Node newNode = new Node(key, value);
            cache.put(key, newNode);
            freqMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
            minFreq = 1;
        }

        private void increaseFreq(Node node) {
            int freq = node.freq;
            freqMap.get(freq).remove(node.key);

            if (freqMap.get(freq).isEmpty() && freq == minFreq) {
                minFreq++;
            }

            node.freq++;
            freqMap.computeIfAbsent(node.freq, k -> new LinkedHashSet<>()).add(node.key);
        }

        private void removeMinFreq() {
            LinkedHashSet<K> keys = freqMap.get(minFreq);
            K keyToRemove = keys.iterator().next();
            keys.remove(keyToRemove);
            cache.remove(keyToRemove);
        }
    }

    /**
     * 7. 二级缓存（LRU + LFU组合）
     *
     * <p>第一级：热点数据（LRU，小容量，快速访问）
     * 第二级：温数据（LFU，大容量）
     */
    static class TwoLevelCache<K, V> {
        private final LRUCacheManual<K, V> hotCache;   // 热点缓存
        private final LFUCache<K, V> warmCache;        // 温缓存

        public TwoLevelCache(int hotCapacity, int warmCapacity) {
            this.hotCache = new LRUCacheManual<>(hotCapacity);
            this.warmCache = new LFUCache<>(warmCapacity);
        }

        public V get(K key) {
            // 先查热点缓存
            V value = hotCache.get(key);
            if (value != null) {
                return value;
            }

            // 再查温缓存
            value = warmCache.get(key);
            if (value != null) {
                // 提升到热点缓存
                hotCache.put(key, value);
                return value;
            }

            return null;
        }

        public void put(K key, V value) {
            hotCache.put(key, value);
        }
    }

    // ==================== 演示与测试 ====================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== LRU缓存淘汰策略演示 ==========\n");

        // 1. 手动实现LRU演示
        System.out.println("1. 手动实现LRU缓存（容量=3）:");
        LRUCacheManual<Integer, String> cache1 = new LRUCacheManual<>(3);

        System.out.println("   插入 1=A, 2=B, 3=C:");
        cache1.put(1, "A");
        cache1.put(2, "B");
        cache1.put(3, "C");
        cache1.printCache();

        System.out.println("   访问 1 (get 1): " + cache1.get(1));
        cache1.printCache();

        System.out.println("   插入 4=D (触发淘汰):");
        cache1.put(4, "D");
        cache1.printCache();

        System.out.println("   访问 2 (已淘汰): " + cache1.get(2));

        // 2. LinkedHashMap实现
        System.out.println("\n2. LinkedHashMap实现LRU（容量=3）:");
        LRUCacheAccessOrder<Integer, String> cache2 = new LRUCacheAccessOrder<>(3);
        cache2.put(1, "A");
        cache2.put(2, "B");
        cache2.put(3, "C");
        System.out.println("   插入后: " + cache2);

        cache2.get(1);  // 访问1
        System.out.println("   访问1后: " + cache2);

        cache2.put(4, "D");
        System.out.println("   插入4后: " + cache2);

        // 3. 带过期时间的缓存
        System.out.println("\n3. 带过期时间的LRU缓存:");
        ExpirableLRUCache<String, String> cache3 = new ExpirableLRUCache<>(3);
        cache3.put("key1", "value1", 1000);  // 1秒过期
        cache3.put("key2", "value2", 5000);  // 5秒过期

        System.out.println("   插入后 get key1: " + cache3.get("key1"));
        System.out.println("   等待2秒...");
        Thread.sleep(2000);
        System.out.println("   2秒后 get key1 (已过期): " + cache3.get("key1"));
        System.out.println("   2秒后 get key2 (未过期): " + cache3.get("key2"));

        // 4. LFU缓存演示
        System.out.println("\n4. LFU缓存（容量=3）:");
        LFUCache<Integer, String> lfuCache = new LFUCache<>(3);
        lfuCache.put(1, "A");
        lfuCache.put(2, "B");
        lfuCache.put(3, "C");

        // 增加访问频率
        lfuCache.get(1);
        lfuCache.get(1);
        lfuCache.get(2);

        System.out.println("   访问频率: 1(2次), 2(1次), 3(0次)");
        System.out.println("   插入 4=D (淘汰访问频率最低的3):");
        lfuCache.put(4, "D");
        System.out.println("   get 3 (已被淘汰): " + lfuCache.get(3));

        // 5. 二级缓存演示
        System.out.println("\n5. 二级缓存（热点=2，温存=3）:");
        TwoLevelCache<Integer, String> twoLevel = new TwoLevelCache<>(2, 3);
        twoLevel.put(1, "A");
        twoLevel.put(2, "B");
        twoLevel.put(3, "C");

        System.out.println("   插入 1,2,3后 get 1: " + twoLevel.get(1));
        System.out.println("   get 3 (从温存提升到热点): " + twoLevel.get(3));
        System.out.println("   get 4 (不存在): " + twoLevel.get(4));

        // 6. 性能对比
        System.out.println("\n6. 性能测试（10万次操作）:");
        int operations = 100000;

        // 手动实现
        LRUCacheManual<Integer, Integer> manualCache = new LRUCacheManual<>(1000);
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < operations; i++) {
            manualCache.put(i % 2000, i);
            manualCache.get(i % 2000);
        }
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("   手动实现: " + time1 + "ms");

        // LinkedHashMap实现
        LRUCacheAccessOrder<Integer, Integer> linkedCache = new LRUCacheAccessOrder<>(1000);
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < operations; i++) {
            linkedCache.put(i % 2000, i);
            linkedCache.get(i % 2000);
        }
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("   LinkedHashMap: " + time2 + "ms");

        // 7. 实际应用场景
        System.out.println("\n7. 实际应用场景 - 数据库查询缓存:");
        LRUCacheManual<String, Object> dbCache = new LRUCacheManual<>(5);

        // 模拟数据库查询
        String[] queries = {
                "SELECT * FROM users WHERE id=1",
                "SELECT * FROM users WHERE id=2",
                "SELECT * FROM orders WHERE user_id=1",
                "SELECT * FROM users WHERE id=1",  // 重复查询，从缓存获取
                "SELECT * FROM products WHERE id=1",
                "SELECT * FROM categories",
                "SELECT * FROM users WHERE id=3"
        };

        for (String query : queries) {
            Object result = dbCache.get(query);
            if (result == null) {
                // 模拟数据库查询
                result = "Result of: " + query;
                dbCache.put(query, result);
                System.out.println("   [DB查询] " + query.substring(0, Math.min(40, query.length())));
            } else {
                System.out.println("   [缓存命中] " + query.substring(0, Math.min(40, query.length())));
            }
        }

        System.out.println("\n   最终缓存内容:");
        dbCache.printCache();

        System.out.println("\n========== 演示结束 ==========");
    }
}
