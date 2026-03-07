package cn.itzixiao.interview.hashmap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * =====================================================================================
 * ConcurrentHashMap 源码详解（JDK8）
 * =====================================================================================
 * 
 * 一、核心设计
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────┐
 * │  数据结构：数组 + 链表 + 红黑树（与 HashMap 相同）              │
 * │  线程安全：CAS + synchronized（锁分段细化到桶级别）            │
 * │  锁粒度：  每个桶（Node 数组元素）独立加锁                      │
 * │  并发度：  默认 16（数组长度），可扩容                          │
 * └─────────────────────────────────────────────────────────────┘
 *
 * 二、与 Hashtable/Collections.synchronizedMap 对比
 * -------------------------------------------------------------------------------------
 * ┌─────────────────┬─────────────────┬─────────────────────────┐
 * │   实现方式       │    锁机制        │       性能              │
 * ├─────────────────┼─────────────────┼─────────────────────────┤
 * │  Hashtable      │  全局锁（synchronized）│  低，单线程访问      │
 * │  synchronizedMap│  全局锁（synchronized）│  低，单线程访问      │
 * │  ConcurrentHashMap│ 桶级别锁（CAS+synchronized）│ 高，多线程并发 │
 * └─────────────────┴─────────────────┴─────────────────────────┘
 * 
 * 三、JDK7 vs JDK8 实现差异
 * -------------------------------------------------------------------------------------
 * JDK 7：分段锁（Segment）
 * - 默认 16 个 Segment，每个 Segment 是一个 ReentrantLock
 * - 锁粒度较粗，并发度受限
 * - 数据结构：Segment[] + HashEntry[][]
 * 
 * JDK 8：桶级别锁（synchronized + CAS）
 * - 锁粒度细化到每个桶（Node 数组元素）
 * - 没有冲突的桶访问不需要加锁
 * - 使用 CAS 进行无锁的插入和更新
 * - 数据结构：Node[] + 链表/红黑树
 */
public class ConcurrentHashMapDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== ConcurrentHashMap 源码详解 ==========\n");

        // 1. 核心结构
        demonstrateCoreStructure();

        // 2. 线程安全机制
        demonstrateThreadSafety();

        // 3. put 操作详解
        demonstratePutOperation();

        // 4. 扩容机制
        demonstrateTransfer();

        // 5. 计数机制
        demonstrateCounting();

        // 6. 与 HashMap 对比
        demonstrateComparison();

        // 7. 高频面试题
        showInterviewQuestions();
    }

    /**
     * 1. 核心结构
     */
    private static void demonstrateCoreStructure() {
        System.out.println("【1. 核心数据结构】\n");

        System.out.println("ConcurrentHashMap 类结构：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  ConcurrentHashMap                                          │");
        System.out.println("│  ├── Node<K,V>[] table          // 主干数组，2的幂次长度      │");
        System.out.println("│  ├── Node<K,V> nextTable        // 扩容时使用的新数组         │");
        System.out.println("│  ├── long baseCount             // 基础计数器                 │");
        System.out.println("│  ├── int sizeCtl                // 控制标识符                 │");
        System.out.println("│  │   - 负数：正在初始化或扩容                                  │");
        System.out.println("│  │   - -1：正在初始化                                         │");
        System.out.println("│  │   - -(1+n)：n个线程正在扩容                                │");
        System.out.println("│  │   - 正数：下次扩容阈值                                     │");
        System.out.println("│  ├── int transferIndex          // 扩容进度                   │");
        System.out.println("│  └── CounterCell[] counterCells // 计数单元数组（高并发优化）  │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("Node 节点类型：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  Node：普通节点，链表结构                                     │");
        System.out.println("│  ├── int hash                                                 │");
        System.out.println("│  ├── K key                                                    │");
        System.out.println("│  ├── V val                                                    │");
        System.out.println("│  └── Node<K,V> next                                           │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  TreeNode：红黑树节点（链表长度≥8时转换）                     │");
        System.out.println("│  └── TreeNode<K,V> prev  // 双向链表，便于删除               │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  TreeBin：红黑树的包装容器                                    │");
        System.out.println("│  └── 持有红黑树根节点，提供锁控制                             │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  ForwardingNode：转发节点（扩容时使用）                       │");
        System.out.println("│  └── hash = MOVED (-1)，指向新数组                            │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("哈希桶数组结构示意：");
        System.out.println("┌─────────┬─────────┬─────────┬─────────┬─────────┬─────────┐");
        System.out.println("│  table  │  table  │  table  │  table  │  table  │  table  │");
        System.out.println("│  [0]    │  [1]    │  [2]    │  [3]    │  [4]    │  [5]    │");
        System.out.println("├────┬────┼────┬────┼────┬────┼────┬────┼────┬────┼────┬────┤");
        System.out.println("│Node│next│Node│next│Tree│    │Node│next│FWD │    │Node│next│");
        System.out.println("│    │→Node│   │→null│Bin │    │   │→Node│→new│    │   │→null│");
        System.out.println("│    │→null│   │     │Root│    │   │→null│table│   │   │     │");
        System.out.println("└────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘");
        System.out.println("  链表      单节点   红黑树            转发节点    链表\n");
    }

    /**
     * 2. 线程安全机制
     */
    private static void demonstrateThreadSafety() {
        System.out.println("【2. 线程安全机制】\n");

        System.out.println("JDK8 同步策略：");
        System.out.println("1. 读操作：无锁，volatile 保证可见性");
        System.out.println("   - get()：不需要加锁，直接读取 volatile 变量");
        System.out.println();
        System.out.println("2. 写操作：");
        System.out.println("   - 无冲突：使用 CAS 直接插入（tabAt + casTabAt）");
        System.out.println("   - 有冲突：synchronized 锁定桶头节点");
        System.out.println();
        System.out.println("3. 扩容：多线程协同扩容，每个线程负责一部分");
        System.out.println();

        System.out.println("关键 CAS 操作：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  tabAt(Node[] tab, int i)                                   │");
        System.out.println("│  → 获取 tab[i]，使用 Unsafe.getObjectVolatile               │");
        System.out.println("│                                                             │");
        System.out.println("│  casTabAt(Node[] tab, int i, Node c, Node v)                │");
        System.out.println("│  → CAS 设置 tab[i] = v，期望当前值为 c                       │");
        System.out.println("│                                                             │");
        System.out.println("│  setTabAt(Node[] tab, int i, Node v)                        │");
        System.out.println("│  → 设置 tab[i] = v，仅在扩容时使用（已加锁保证安全）          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("为什么使用 synchronized 而不是 ReentrantLock？");
        System.out.println("1. JDK8 对 synchronized 优化很好（偏向锁、轻量级锁）");
        System.out.println("2. synchronized 代码更简洁，JVM 优化空间更大");
        System.out.println("3. 锁的粒度很小（单个桶），冲突概率低，synchronized 足够");
        System.out.println("4. 减少内存开销（ReentrantLock 需要额外对象）\n");
    }

    /**
     * 3. put 操作详解
     */
    private static void demonstratePutOperation() {
        System.out.println("【3. put() 操作详解】\n");

        System.out.println("putVal() 执行流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 计算 hash：spread(key.hashCode())                        │");
        System.out.println("│     → (h ^ (h >>> 16)) & HASH_BITS                          │");
        System.out.println("│     → 高位参与运算，减少冲突                                  │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  2. 检查 table 是否为空或长度为0                              │");
        System.out.println("│     → 是：初始化 table（initTable）                          │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  3. 计算索引：i = (n - 1) & hash                             │");
        System.out.println("│     → 获取桶位置 tab[i]                                      │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  4. 检查 tab[i] 是否为空                                      │");
        System.out.println("│     → 是：CAS 插入新节点（无锁）                             │");
        System.out.println("│       casTabAt(tab, i, null, new Node<K,V>(hash, key, value))│");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  5. 检查 tab[i] 是否为 ForwardingNode（扩容中）               │");
        System.out.println("│     → 是：帮助扩容（helpTransfer），然后重试                 │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  6. 桶不为空，synchronized 锁定桶头节点                       │");
        System.out.println("│     synchronized (f) {  // f = tab[i]                        │");
        System.out.println("│         // 再次确认 f 是头节点（可能被其他线程修改）           │");
        System.out.println("│         if (tabAt(tab, i) == f) {                            │");
        System.out.println("│             if (f.hash >= 0) {  // 链表                      │");
        System.out.println("│                 // 遍历链表，找到或插入                        │");
        System.out.println("│                 // 计数，检查是否需要树化                     │");
        System.out.println("│             } else if (f instanceof TreeBin) {  // 红黑树    │");
        System.out.println("│                 // 插入红黑树                                │");
        System.out.println("│             }                                                │");
        System.out.println("│         }                                                    │");
        System.out.println("│     }                                                        │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  7. 检查是否需要扩容（binCount >= 8 且数组长度 < 64）          │");
        System.out.println("│     → 是：tryPresize(n << 1) 扩容                           │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  8. 更新计数（addCount）                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 4. 扩容机制
     */
    private static void demonstrateTransfer() {
        System.out.println("【4. 扩容机制（transfer）】\n");

        System.out.println("扩容触发条件：");
        System.out.println("- 元素数量 > sizeCtl（容量 * 负载因子，默认 0.75）");
        System.out.println("- 单个桶链表长度 >= 8，但数组长度 < 64（优先扩容而非树化）\n");

        System.out.println("多线程协同扩容：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  旧数组：16个桶            新数组：32个桶                     │");
        System.out.println("│  ┌──┬──┬──┬──┐            ┌──┬──┬──┬──┬──┬──┬──┬──┐         │");
        System.out.println("│  │0 │1 │2 │3 │            │0 │1 │2 │3 │4 │5 │6 │7 │         │");
        System.out.println("│  ├──┼──┼──┼──┤            ├──┼──┼──┼──┼──┼──┼──┼──┤         │");
        System.out.println("│  │A │B │C │D │            │A │E │B │F │C │G │D │H │         │");
        System.out.println("│  └──┴──┴──┴──┘            └──┴──┴──┴──┴──┴──┴──┴──┘         │");
        System.out.println("│                                                             │");
        System.out.println("│  扩容过程：                                                  │");
        System.out.println("│  1. 创建新数组（2倍大小）                                     │");
        System.out.println("│  2. 多个线程从数组尾部开始，每个线程负责一个区间               │");
        System.out.println("│  3. 处理完的桶标记为 ForwardingNode（hash = -1）              │");
        System.out.println("│  4. 其他线程遇到 ForwardingNode 会帮助扩容或跳过               │");
        System.out.println("│  5. 所有桶处理完毕，替换旧数组引用                             │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("链表拆分原理：");
        System.out.println("- 数组扩容2倍，新索引 = 原索引 或 原索引 + 原数组长度");
        System.out.println("- 根据 hash & n（原数组长度）的结果拆分：");
        System.out.println("  - 结果为 0：保持原位（lo 链表）");
        System.out.println("  - 结果为 1：移到高位（hi 链表，索引 + n）\n");

        System.out.println("扩容期间的操作：");
        System.out.println("- get()：正常访问，遇到 ForwardingNode 去新数组查找");
        System.out.println("- put()：帮助扩容（如果正在扩容），然后在合适位置插入");
        System.out.println("- remove()：帮助扩容（如果正在扩容），然后删除\n");
    }

    /**
     * 5. 计数机制
     */
    private static void demonstrateCounting() {
        System.out.println("【5. 计数机制（size）】\n");

        System.out.println("为什么不用 AtomicInteger？");
        System.out.println("- 所有 put/remove 都要修改计数器，成为热点");
        System.out.println("- AtomicInteger 是单个变量，高并发下 CAS 冲突严重\n");

        System.out.println("CounterCell 分散计数（借鉴 LongAdder）：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  baseCount：基础计数器                                       │");
        System.out.println("│  └── 低并发时直接使用                                        │");
        System.out.println("│                                                             │");
        System.out.println("│  CounterCell[] counterCells：计数单元数组                    │");
        System.out.println("│  └── 高并发时，线程分散到不同 Cell 计数                      │");
        System.out.println("│  └── 每个线程通过 ThreadLocalRandom 选择 Cell               │");
        System.out.println("│                                                             │");
        System.out.println("│  最终 size = baseCount + sum(CounterCell[])                 │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("size() 方法：");
        System.out.println("- 累加 baseCount 和所有 CounterCell 的值");
        System.out.println("- 不保证实时精确（弱一致性），但效率高\n");
    }

    /**
     * 6. 与 HashMap 对比
     */
    private static void demonstrateComparison() {
        System.out.println("【6. ConcurrentHashMap vs HashMap vs Hashtable】\n");

        System.out.println("对比表：");
        System.out.println("┌────────────────┬─────────────┬─────────────────┬─────────────┐");
        System.out.println("│     特性       │   HashMap   │ConcurrentHashMap│  Hashtable  │");
        System.out.println("├────────────────┼─────────────┼─────────────────┼─────────────┤");
        System.out.println("│  线程安全      │    否       │      是         │     是      │");
        System.out.println("│  锁机制        │    无       │  CAS+synchronized│  synchronized│");
        System.out.println("│  锁粒度        │    -        │    桶级别       │    全局锁   │");
        System.out.println("│  允许null key  │    是       │      否         │     否      │");
        System.out.println("│  允许null value│    是       │      否         │     否      │");
        System.out.println("│  迭代器        │  fail-fast  │   弱一致性      │  fail-fast  │");
        System.out.println("│  性能          │    最高     │     高          │     低      │");
        System.out.println("└────────────────┴─────────────┴─────────────────┴─────────────┘\n");

        System.out.println("原子性复合操作：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 非原子（错误）                                           │");
        System.out.println("│  if (!map.containsKey(k)) {  // 检查                        │");
        System.out.println("│      map.put(k, v);          // 插入                        │");
        System.out.println("│  }  // 中间可能被其他线程插入                                 │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  // 原子（正确）                                             │");
        System.out.println("│  map.putIfAbsent(k, v);      // 原子操作                    │");
        System.out.println("│  // 或                                                      │");
        System.out.println("│  map.computeIfAbsent(k, key -> createValue(key));           │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  其他原子方法：                                              │");
        System.out.println("│  - replace(K key, V oldValue, V newValue)                   │");
        System.out.println("│  - remove(Object key, Object value)                         │");
        System.out.println("│  - compute / computeIfPresent / merge                       │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 7. 高频面试题
     */
    private static void showInterviewQuestions() {
        System.out.println("【7. 高频面试题】\n");

        System.out.println("Q1: ConcurrentHashMap 如何保证线程安全？");
        System.out.println("答案：");
        System.out.println("  JDK 7: 分段锁（Segment），每个 Segment 继承 ReentrantLock");
        System.out.println("  JDK 8: CAS + synchronized，锁住链表/红黑树头节点");
        System.out.println("  优势：锁粒度更细，并发度更高\n");

        System.out.println("Q2: JDK7 和 JDK8 的 ConcurrentHashMap 有什么区别？");
        System.out.println("答案：");
        System.out.println("  JDK 7:");
        System.out.println("    - 使用 Segment 分段锁");
        System.out.println("    - Segment 数组默认 16，并发度固定");
        System.out.println("    - Segment 继承 ReentrantLock");
        System.out.println("  JDK 8:");
        System.out.println("    - 取消 Segment，直接在 Node 上加锁");
        System.out.println("    - 使用 CAS + synchronized");
        System.out.println("    - 锁粒度更细，支持动态扩容\n");

        System.out.println("Q3: ConcurrentHashMap 为什么不允许 null 键/值？");
        System.out.println("答案：");
        System.out.println("  二义性问题：get(key) 返回 null 时，无法区分是「不存在」还是「值为null」");
        System.out.println("  在多线程环境下，containsKey() 和 get() 之间可能被其他线程修改");
        System.out.println("  为了避免歧义，直接禁止 null\n");

        System.out.println("Q4: ConcurrentHashMap 的迭代器有什么特点？");
        System.out.println("答案：");
        System.out.println("  弱一致性（Weakly Consistent）：");
        System.out.println("    - 迭代期间修改不会抛 ConcurrentModificationException");
        System.out.println("    - 可能反映迭代期间的修改，也可能不反映");
        System.out.println("    - 不会导致死循环或数据不一致\n");

        System.out.println("Q5: ConcurrentHashMap 如何保证扩容线程安全？");
        System.out.println("答案：");
        System.out.println("  JDK 8 多线程协同扩容：");
        System.out.println("    1. 每个线程负责一部分桶的迁移");
        System.out.println("    2. 使用 ForwardingNode 标记正在迁移的桶");
        System.out.println("    3. 其他线程遇到 ForwardingNode 会协助扩容");
        System.out.println("    4. 扩容期间读写操作正常进行\n");

        System.out.println("Q6: ConcurrentHashMap 的 size() 如何实现？");
        System.out.println("答案：");
        System.out.println("  JDK 8 使用 LongAdder 思想：");
        System.out.println("    - baseCount：基础计数");
        System.out.println("    - counterCells：竞争时的分散计数");
        System.out.println("    - 最终 size = baseCount + counterCells 总和");
        System.out.println("  特点：高并发下性能好，但结果不是强一致的\n");

        System.out.println("Q7: 为什么 JDK8 用 synchronized 替代 ReentrantLock？");
        System.out.println("答案：");
        System.out.println("  1. synchronized 在 JDK6 后优化很好（偏向锁、轻量级锁）");
        System.out.println("  2. 锁粒度已细化到桶级别，冲突概率低");
        System.out.println("  3. synchronized 不需要额外对象，内存开销小");
        System.out.println("  4. JVM 对 synchronized 优化空间更大\n");
    }
}
