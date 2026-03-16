package cn.itzixiao.interview.algorithm.linkedlist;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU 缓存（Least Recently Used - 最近最少使用）
 * 
 * LeetCode 第 146 题：https://leetcode.cn/problems/lru-cache/
 * 
 * 题目描述：
 * 设计一个满足最近最少使用（LRU）缓存约束的数据结构。
 * 
 * LRU 原理：
 * - 当缓存满时，优先淘汰最长时间未被访问的元素
 * - 访问（get）或更新（put）现有元素会刷新其使用顺序
 * 
 * 实现方案：
 * 1. LinkedHashMap 实现（推荐）：利用 JDK 内置数据结构，代码简洁
 * 2. HashMap + 双向链表：手写实现，展示底层原理
 * 
 * @author itzixiao
 * @date 2026-03-16
 */
class LRUCache {
    
    /**
     * 使用 LinkedHashMap 实现 LRU 缓存
     * 
     * LinkedHashMap 特性：
     * - 继承自 HashMap，具备哈希表的高效查找性能
     * - 维护元素的插入顺序或访问顺序
     * - accessOrder=true 时，按访问顺序排序（LRU 的关键）
     */
    private Map<Integer, Integer> cache;

    /**
     * 构造方法：初始化 LRU 缓存
     * 
     * @param capacity 缓存容量（正整数）
     */
    public LRUCache(int capacity) {
        // 创建 LinkedHashMap，关键参数说明：
        // - initialCapacity: 初始容量为传入的 capacity
        // - loadFactor: 负载因子 0.75f（默认值，超过此比例会扩容）
        // - accessOrder: true 表示按访问顺序排序（LRU 的核心）
        //   - false: 按插入顺序排序
        //   - true: 按访问顺序排序（最近访问的放在最后）
        this.cache = new LinkedHashMap<Integer, Integer>(capacity, 0.75f, true) {
            /**
             * 重写 removeEldestEntry 方法，实现自动淘汰机制
             * 
             * @param eldest 最老的元素（最先被插入或最久未访问的）
             * @return true 表示需要移除该元素，false 表示不移除
             * 
             * 触发时机：每次调用 put() 方法后都会检查
             */
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                // 当大小超过容量时，移除最老的元素
                // 返回 true 表示移除 eldest，false 表示不移除
                return size() > capacity;
            }
        };
    }

    /**
     * 获取缓存中的值
     * 
     * 时间复杂度：O(1)
     * 空间复杂度：O(1)
     * 
     * @param key 要获取的键
     * @return 如果关键字 key 存在于缓存中，则返回关键字的值，否则返回 -1
     * 
     * 核心逻辑：
     * - getOrDefault(key, -1): 存在返回对应值，不存在返回 -1
     * - 由于 accessOrder=true，get 操作会自动将该键值对移到最后（最近使用位置）
     */
    public int get(int key) {
        // get 方法会自动更新访问顺序
        return cache.getOrDefault(key, -1);
    }

    /**
     * 向缓存中添加或更新键值对
     * 
     * 时间复杂度：O(1)
     * 空间复杂度：O(1)
     * 
     * @param key 键
     * @param value 值
     * 
     * 核心逻辑：
     * - 如果 key 已存在：更新 value，并刷新访问顺序（移到最后）
     * - 如果 key 不存在：插入新的键值对到末尾
     * - 插入后自动触发 removeEldestEntry 检查，如果超出容量则淘汰最老元素
     */
    public void put(int key, int value) {
        cache.put(key, value);
    }

    public static void main(String[] args) {
        System.out.println("========== LRU Cache 演示 ==========");
        int capacity = 2;
        LRUCache lRUCache = new LRUCache(capacity);
        System.out.println("初始化：创建容量为 " + capacity + " 的 LRU 缓存\n");
        
        System.out.println("1. put(1, 1) - 插入键值对 (1, 1)");
        lRUCache.put(1, 1);
        System.out.println("   当前缓存：{1=1}\n");
        
        System.out.println("2. put(2, 2) - 插入键值对 (2, 2)");
        lRUCache.put(2, 2);
        System.out.println("   当前缓存：{1=1, 2=2}\n");
        
        System.out.println("3. get(1) - 访问键 1（应返回 1）");
        int result1 = lRUCache.get(1);
        System.out.println("   返回：" + result1);
        System.out.println("   说明：访问 key=1 后，它成为最近使用的元素\n");
        
        System.out.println("4. put(3, 3) - 插入键值对 (3, 3)");
        lRUCache.put(3, 3);
        System.out.println("   当前缓存：{1=1, 3=3}");
        System.out.println("   说明：缓存已满，淘汰最久未使用的 key=2\n");
        
        System.out.println("5. get(2) - 访问键 2（应返回 -1）");
        int result2 = lRUCache.get(2);
        System.out.println("   返回：" + result2);
        System.out.println("   说明：key=2 已被淘汰，找不到\n");
        
        System.out.println("6. put(4, 4) - 插入键值对 (4, 4)");
        lRUCache.put(4, 4);
        System.out.println("   当前缓存：{3=3, 4=4}");
        System.out.println("   说明：缓存已满，淘汰最久未使用的 key=1\n");
        
        System.out.println("7. get(1) - 访问键 1（应返回 -1）");
        int result3 = lRUCache.get(1);
        System.out.println("   返回：" + result3);
        System.out.println("   说明：key=1 已被淘汰\n");
        
        System.out.println("8. get(3) - 访问键 3（应返回 3）");
        int result4 = lRUCache.get(3);
        System.out.println("   返回：" + result4);
        System.out.println("   说明：key=3 存在，访问后成为最近使用的元素\n");
        
        System.out.println("9. get(4) - 访问键 4（应返回 4）");
        int result5 = lRUCache.get(4);
        System.out.println("   返回：" + result5);
        System.out.println("   说明：key=4 存在，访问后成为最近使用的元素\n");
        
        System.out.println("========== 演示结束 ==========");
        System.out.println("最终缓存状态：{3=3, 4=4}");
        System.out.println("\nLRU 核心原理：");
        System.out.println("- 缓存满时，淘汰【最久未使用】的元素");
        System.out.println("- 访问（get）或更新（put）现有元素会刷新其使用顺序");
        System.out.println("- LinkedHashMap 通过 accessOrder=true 实现 LRU 语义");
    }
}