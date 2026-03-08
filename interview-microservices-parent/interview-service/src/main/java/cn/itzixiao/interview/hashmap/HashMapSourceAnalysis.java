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
     */
    private static void demonstrateBasicStructure() throws Exception {
        System.out.println("【1. 基本结构与扩容演示】");
        
        // 创建指定初始容量的HashMap
        HashMap<Integer, String> map = new HashMap<>(16, 0.75f);
        
        System.out.println("初始容量: 16, 负载因子: 0.75, 扩容阈值: 12");
        printMapInternalStructure(map);
        
        // 添加元素，观察扩容
        System.out.println("\n添加元素过程：");
        for (int i = 0; i < 15; i++) {
            map.put(i, "value" + i);
            if (i == 11 || i == 12 || i == 14) {
                System.out.println("添加第 " + (i + 1) + " 个元素后：");
                printMapInternalStructure(map);
            }
        }
        System.out.println();
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
     * 当链表长度>=8且数组长度>=64时，链表转为红黑树
     */
    private static void demonstrateTreeify() throws Exception {
        System.out.println("【3. 链表转红黑树演示】");
        System.out.println("树化条件：链表长度 >= 8 且 数组长度 >= 64");
        
        HashMap<CollisionKey, String> map = new HashMap<>();
        
        // 添加8个hashCode相同的key，触发链表转红黑树
        System.out.println("\n添加8个hashCode相同的元素：");
        for (int i = 0; i < 8; i++) {
            CollisionKey key = new CollisionKey("key" + i, 200);
            map.put(key, "value" + i);
            System.out.println("添加第 " + (i + 1) + " 个元素");
        }
        
        System.out.println("\n注意：JDK8中，当链表长度>=8时，如果数组长度<64，会先扩容而不是树化");
        System.out.println("这是为了避免在较小数组上过早树化，因为扩容可以分散哈希冲突\n");
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
     * 通过反射打印HashMap内部结构
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
        System.out.println("  当前容量: " + capacity + 
                          ", 元素个数: " + size + 
                          ", 扩容阈值: " + threshold);
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
