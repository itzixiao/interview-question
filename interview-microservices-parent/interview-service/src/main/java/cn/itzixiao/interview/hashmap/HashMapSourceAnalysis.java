package cn.itzixiao.interview.hashmap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * HashMap 源码理解示例
 * 
 * 核心概念：
 * 1. 底层数据结构：数组 + 链表 + 红黑树（JDK8+）
 * 2. 哈希冲突解决：链地址法
 * 3. 扩容机制：2倍扩容，重新哈希
 * 4. 树化阈值：链表长度>=8 且 数组长度>=64
 */
public class HashMapSourceAnalysis {

    public static void main(String[] args) throws Exception {
        System.out.println("========== HashMap 源码分析示例 ==========\n");
        
        // 1. 演示基本结构和扩容
        demonstrateBasicStructure();
        
        // 2. 演示哈希冲突
        demonstrateHashCollision();
        
        // 3. 演示链表转红黑树
        demonstrateTreeify();
        
        // 4. 核心源码解析注释
        explainCoreSourceCode();
    }
    
    /**
     * 1. 基本结构演示
     * 默认初始容量：16
     * 负载因子：0.75
     * 扩容阈值：16 * 0.75 = 12
     * 
     * 关键问题：先插入再扩容，还是先扩容再插入？
     * 答案：先判断是否需要扩容 -> 如果需要，先扩容 -> 然后再插入新元素
     */
    private static void demonstrateBasicStructure() throws Exception {
        System.out.println("【1. 基本结构与扩容演示】");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("核心问题：扩容是【先插入再扩容】还是【先扩容再插入】？");
        System.out.println("答案：在 put() 方法内部，先检查 size >= threshold，如果是，先调用 resize() 扩容，然后再插入元素");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            
        // 创建指定初始容量的 HashMap
        HashMap<Integer, String> map = new HashMap<>(16, 0.75f);
            
        System.out.println("【初始化状态】");
        System.out.println("  初始容量：16, 负载因子：0.75, 扩容阈值：12");
        printMapInternalStructure(map);
            
        // 添加元素，观察扩容
        System.out.println("\n【开始逐个插入元素，观察扩容时机】");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        for (int i = 0; i < 15; i++) {
            // 插入前状态
            int sizeBefore = getSize(map);
            int thresholdBefore = getThreshold(map);
                
            System.out.println("\n>>> 准备插入元素 [" + i + ", value" + i + "]");
            System.out.println("    插入前：size=" + sizeBefore + ", threshold=" + thresholdBefore);
                
            // 执行插入
            map.put(i, "value" + i);
                
            // 插入后状态
            int sizeAfter = getSize(map);
            int capacityAfter = getCapacity(map);
            int thresholdAfter = getThreshold(map);
                
            System.out.println("    插入后：size=" + sizeAfter + ", capacity=" + capacityAfter + ", threshold=" + thresholdAfter);
                
            // 关键节点详细分析
            if (i == 11) {
                System.out.println("    ⚠️  注意：size(12) == threshold(12)，但还没扩容！");
                System.out.println("    原因：HashMap 在 size > threshold 时才扩容，不是 >=");
                printMapInternalStructure(map);
            } else if (i == 12) {
                System.out.println("    🔥 关键：插入第 13 个元素时！");
                System.out.println("    流程：检测到 size(12) >= threshold(12) → 先调用 resize() 扩容到 32 → 然后插入新元素");
                System.out.println("    结论：【先扩容，再插入】");
                printMapInternalStructure(map);
                System.out.println("    ✅ 验证：容量从 16 变为 32，threshold 从 12 变为 24");
            } else if (i == 14) {
                System.out.println("    当前状态稳定，下次扩容要等 size > 24");
                printMapInternalStructure(map);
            }
        }
            
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("【扩容机制总结】");
        System.out.println("1. 扩容时机：在 put() 方法中，插入前先检查 size >= threshold");
        System.out.println("2. 扩容流程：if (size >= threshold) resize(); → 然后才执行插入逻辑");
        System.out.println("3. 扩容细节：创建新数组（容量×2）→ 重新计算哈希 → 元素迁移到新位置");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
    
    /**
     * 2. 哈希冲突演示
     * 通过自定义对象的hashCode使其产生冲突
     */
    private static void demonstrateHashCollision() throws Exception {
        System.out.println("【2. 哈希冲突演示】");
        
        HashMap<CollisionKey, String> map = new HashMap<>();
        
        // 这些key的hashCode相同，会产生冲突
        CollisionKey key1 = new CollisionKey("A", 100);
        CollisionKey key2 = new CollisionKey("B", 100);
        CollisionKey key3 = new CollisionKey("C", 100);
        
        System.out.println("key1.hashCode() = " + key1.hashCode());
        System.out.println("key2.hashCode() = " + key2.hashCode());
        System.out.println("key3.hashCode() = " + key3.hashCode());
        System.out.println("三个key的hashCode相同，会产生哈希冲突\n");
        
        map.put(key1, "Value-A");
        map.put(key2, "Value-B");
        map.put(key3, "Value-C");
        
        System.out.println("冲突解决方式：链地址法（链表存储）");
        System.out.println("map.get(key1) = " + map.get(key1));
        System.out.println("map.get(key2) = " + map.get(key2));
        System.out.println("map.get(key3) = " + map.get(key3));
        System.out.println();
    }
    
    /**
     * 3. 链表转红黑树演示
     * 当链表长度>=8 且数组长度>=64 时，链表转为红黑树
     * 
     * 关键问题：先插入再转树，还是先转树再插入？
     * 答案：在 putVal() 方法中，先插入新节点到链表尾部 → 然后检查链表长度 → 如果>=TREEIFY_THRESHOLD 且容量>=MIN_TREEIFY_CAPACITY，则调用 treeifyBin() 转树
     */
    private static void demonstrateTreeify() throws Exception {
        System.out.println("\n【3. 链表转红黑树演示】");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("核心问题：树化是【先插入再转树】还是【先转树再插入】？");
        System.out.println("答案：在 putVal() 方法中，先将新元素插入链表尾部 → 然后检查链表长度 → 如果达到阈值且容量足够，则调用 treeifyBin() 转树");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        // 场景 1：数组容量<64，即使链表长度>=8，也会先扩容而不是树化
        System.out.println("【场景 1：小容量数组 - 优先扩容而非树化】");
        System.out.println("  条件：链表长度>=8，但数组容量<64");
        System.out.println("  行为：调用 resize() 扩容，而不是 treeifyBin() 树化");
        System.out.println("  原因：通过扩容分散哈希冲突，避免过早树化浪费空间\n");
        
        HashMap<CollisionKey, String> smallMap = new HashMap<>(16);
        System.out.println("初始状态：capacity=16, threshold=12");
        
        for (int i = 0; i < 10; i++) {
            CollisionKey key = new CollisionKey("small" + i, 300); // 所有 key 的 hashCode 相同
            smallMap.put(key, "value" + i);
            
            int capacity = getCapacity(smallMap);
            int size = getSize(smallMap);
            
            System.out.println("  插入第 " + (i + 1) + " 个元素后：size=" + size + ", capacity=" + capacity);
            
            if (i == 7) {
                System.out.println("    ⚠️  链表长度达到 8，但 capacity=16<64，不会树化");
            } else if (i == 9) {
                System.out.println("    🔥 注意：此时发生了扩容（从 16→32），但仍然没有树化！");
                System.out.println("    原因：虽然链表长度>=8，但容量 32 仍然<64，继续扩容优先");
            }
        }
        
        // 场景 2：数组容量>=64，链表长度>=8 时触发树化
        System.out.println("\n【场景 2：大容量数组 - 触发树化】");
        System.out.println("  条件：链表长度>=8 且 数组容量>=64");
        System.out.println("  行为：调用 treeifyBin() 将链表转为红黑树");
        
        // 直接创建大容量 HashMap
        HashMap<CollisionKey, String> largeMap = new HashMap<>(64);
        System.out.println("\n初始状态：capacity=64 (已达到树化最小容量要求)");
        
        System.out.println("\n开始插入 hashCode 相同的元素（强制产生冲突）：");
        for (int i = 0; i < 10; i++) {
            CollisionKey key = new CollisionKey("large" + i, 500);
            largeMap.put(key, "value" + i);
            
            int size = getSize(largeMap);
            System.out.println("  插入第 " + (i + 1) + " 个元素：size=" + size);
            
            if (i == 7) {
                System.out.println("    ⚠️  链表长度达到 8，capacity=64，满足树化条件！");
                System.out.println("    流程：插入新节点 → binCount=8 → treeifyBin() → 链表转红黑树");
                System.out.println("    ✅ 结论：【先插入，再转树】");
            } else if (i == 9) {
                System.out.println("    当前桶中已经是红黑树结构，后续插入走树的平衡逻辑");
            }
        }
        
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("【树化机制总结】");
        System.out.println("1. 树化时机：在 putVal() 中，插入新节点后检查链表长度 binCount");
        System.out.println("2. 树化条件：binCount >= TREEIFY_THRESHOLD(8) && capacity >= MIN_TREEIFY_CAPACITY(64)");
        System.out.println("3. 树化流程：插入新节点 → if (binCount >= 8) treeifyBin() → 遍历链表转红黑树");
        System.out.println("4. 特殊情况：如果 capacity < 64，优先调用 resize() 扩容而非树化");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
    
    /**
     * 4. 核心源码解析
     */
    private static void explainCoreSourceCode() {
        System.out.println("【4. HashMap 核心源码解析】\n");
        
        String explanation = 
            "一、核心常量\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "DEFAULT_INITIAL_CAPACITY = 1 << 4  // 默认初始容量 16\n" +
            "MAXIMUM_CAPACITY = 1 << 30         // 最大容量 2^30\n" +
            "DEFAULT_LOAD_FACTOR = 0.75f        // 默认负载因子\n" +
            "TREEIFY_THRESHOLD = 8              // 链表转树阈值\n" +
            "UNTREEIFY_THRESHOLD = 6            // 树转链表阈值\n" +
            "MIN_TREEIFY_CAPACITY = 64          // 最小树化容量\n\n" +
            
            "二、核心方法：put() 流程\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "1. 计算哈希值：hash(key)\n" +
            "   - (h = key.hashCode()) ^ (h >>> 16)\n" +
            "   - 高16位与低16位异或，减少冲突\n\n" +
            
            "2. 计算数组下标：(n - 1) & hash\n" +
            "   - n是数组长度，必须是2的幂\n" +
            "   - 等价于 hash % n，但位运算更快\n\n" +
            
            "3. 处理冲突：\n" +
            "   - 如果位置为空：直接插入新节点\n" +
            "   - 如果位置有值：遍历链表/树，key相同则覆盖，不同则追加\n\n" +
            
            "4. 检查扩容：\n" +
            "   - size > threshold 时触发扩容\n" +
            "   - 容量变为原来的2倍\n" +
            "   - 元素重新散列到新数组\n\n" +
            
            "三、扩容机制 resize()\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "1. 创建新数组，容量为原来的2倍\n" +
            "2. 遍历旧数组，重新计算每个元素的位置\n" +
            "3. 优化：如果hash新增的高位是0，位置不变；是1，位置+旧容量\n" +
            "   - 例：原容量16，下标5的元素\n" +
            "   - hash & 16 == 0：新下标还是5\n" +
            "   - hash & 16 != 0：新下标是 5 + 16 = 21\n\n" +
            
            "四、为什么容量必须是2的幂？\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "1. 计算下标：(n-1) & hash 代替 hash % n\n" +
            "2. 位运算效率远高于取模运算\n" +
            "3. 保证元素均匀分布，减少冲突\n\n" +
            
            "五、为什么负载因子是0.75？\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "1. 空间与时间的权衡\n" +
            "2. 0.75时，链表平均长度为0.5，冲突概率较低\n" +
            "3. 过高：空间省但冲突多；过低：冲突少但空间浪费\n";
            
        System.out.println(explanation);
    }
    
    /**
     * 通过反射打印 HashMap 内部结构
     */
    private static void printMapInternalStructure(HashMap<?, ?> map) throws Exception {
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);
            
        Field sizeField = HashMap.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        int size = (int) sizeField.get(map);
            
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        int threshold = (int) thresholdField.get(map);
            
        int capacity = table == null ? 0 : table.length;
        System.out.println("  当前容量：" + capacity + 
                          ", 元素个数：" + size + 
                          ", 扩容阈值：" + threshold);
    }
        
    /**
     * 通过反射获取 HashMap 容量
     */
    private static int getCapacity(HashMap<?, ?> map) throws Exception {
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);
        return table == null ? 0 : table.length;
    }
        
    /**
     * 通过反射获取 HashMap 元素个数
     */
    private static int getSize(HashMap<?, ?> map) throws Exception {
        Field sizeField = HashMap.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        return (int) sizeField.get(map);
    }
        
    /**
     * 通过反射获取 HashMap 扩容阈值
     */
    private static int getThreshold(HashMap<?, ?> map) throws Exception {
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        return (int) thresholdField.get(map);
    }
    
    /**
     * 用于产生哈希冲突的自定义Key
     */
    static class CollisionKey {
        private final String name;
        private final int hashCode;
        
        public CollisionKey(String name, int hashCode) {
            this.name = name;
            this.hashCode = hashCode;
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CollisionKey)) return false;
            CollisionKey other = (CollisionKey) obj;
            return this.name.equals(other.name);
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}
