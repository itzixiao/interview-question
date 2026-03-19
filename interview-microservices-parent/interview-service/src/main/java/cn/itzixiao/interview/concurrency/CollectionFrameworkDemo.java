package cn.itzixiao.interview.concurrency;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * =====================================================================================
 * Java 集合框架详解 —— List / Set / Map 实战演示
 * =====================================================================================
 * <p>
 * 对应文档：docs/02-Java并发编程/01-Java集合框架.md
 * <p>
 * 演示内容：
 * 1. ArrayList —— 扩容机制、随机访问、序列化
 * 2. LinkedList —— 双向链表、头尾操作、作为队列/栈
 * 3. CopyOnWriteArrayList —— 写时复制、读无锁、迭代安全
 * 4. ArrayList 线程安全方案对比
 * 5. HashSet / LinkedHashSet / TreeSet / ConcurrentSkipListSet
 * 6. HashMap / LinkedHashMap / TreeMap
 * 7. fail-fast 机制与安全迭代
 * 8. Collections 工具类
 * 9. 高频面试题答案演示
 */
public class CollectionFrameworkDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== Java 集合框架详解 ==========\n");

        // 1. ArrayList 扩容机制演示
        demonstrateArrayList();

        // 2. LinkedList 双向链表演示
        demonstrateLinkedList();

        // 3. CopyOnWriteArrayList 写时复制演示
        demonstrateCopyOnWriteArrayList();

        // 4. ArrayList 线程安全方案对比
        demonstrateListThreadSafety();

        // 5. Set 体系演示
        demonstrateSetFamily();

        // 6. Map 体系演示
        demonstrateMapFamily();

        // 7. fail-fast 机制与安全迭代
        demonstrateFailFast();

        // 8. Collections 工具类
        demonstrateCollectionsUtils();

        // 9. 高频面试题
        showInterviewQuestions();
    }

    // =========================================================================
    // 1. ArrayList 扩容机制
    // =========================================================================
    private static void demonstrateArrayList() {
        System.out.println("【一、ArrayList 扩容机制】\n");

        // 默认构造：初始容量 0，第一次 add 扩容至 10
        List<Integer> list = new ArrayList<>();

        System.out.println("ArrayList 扩容规则：");
        System.out.println("  初始容量：0（懒初始化，第一次 add 才分配 10）");
        System.out.println("  扩容公式：newCapacity = oldCapacity + (oldCapacity >> 1)  即 1.5 倍");
        System.out.println("  0→10→15→22→33→49...（按 1.5 倍增长）\n");

        // 演示随机访问 O(1)
        List<String> accessList = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        System.out.println("随机访问 O(1)：get(3) = " + accessList.get(3));

        // 演示中间插入 O(n)（需移位）
        accessList.add(2, "X");
        System.out.println("中间插入后：" + accessList);

        // 演示预设容量避免扩容（生产最佳实践）
        List<Integer> preAllocated = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            preAllocated.add(i);
        }
        System.out.println("预分配容量 1000，实际元素数：" + preAllocated.size() + "（零扩容开销）");

        // subList 视图（修改原 list 会影响 subList）
        List<String> sub = accessList.subList(1, 4);
        System.out.println("subList(1,4)：" + sub + "（视图，非拷贝）\n");
    }

    // =========================================================================
    // 2. LinkedList 双向链表
    // =========================================================================
    private static void demonstrateLinkedList() {
        System.out.println("【二、LinkedList 双向链表】\n");

        LinkedList<String> linkedList = new LinkedList<>();

        // 头尾操作 O(1)
        linkedList.addFirst("B");
        linkedList.addFirst("A");   // 头插
        linkedList.addLast("C");    // 尾插
        linkedList.addLast("D");
        System.out.println("头插 A、B，尾插 C、D：" + linkedList);

        System.out.println("peekFirst()：" + linkedList.peekFirst() + "（查看不移除）");
        System.out.println("pollFirst()：" + linkedList.pollFirst() + "（查看并移除）");
        System.out.println("移除后：" + linkedList);

        // 作为栈使用（LIFO）
        System.out.println("\n--- 作为栈使用（LIFO）---");
        Deque<String> stack = new LinkedList<>();
        stack.push("第1层");
        stack.push("第2层");
        stack.push("第3层");
        System.out.println("入栈后：" + stack);
        System.out.println("pop()：" + stack.pop() + "（后进先出）");

        // 作为队列使用（FIFO）
        System.out.println("\n--- 作为队列使用（FIFO）---");
        Queue<String> queue = new LinkedList<>();
        queue.offer("请求1");
        queue.offer("请求2");
        queue.offer("请求3");
        System.out.println("入队后：" + queue);
        System.out.println("poll()：" + queue.poll() + "（先进先出）");
        System.out.println();
    }

    // =========================================================================
    // 3. CopyOnWriteArrayList 写时复制
    // =========================================================================
    private static void demonstrateCopyOnWriteArrayList() throws InterruptedException {
        System.out.println("【三、CopyOnWriteArrayList 写时复制】\n");

        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>(
                Arrays.asList("白名单-A", "白名单-B", "白名单-C")
        );

        System.out.println("初始白名单：" + cowList);

        // 读线程：遍历期间不受写操作影响（迭代器是快照）
        Thread reader = new Thread(() -> {
            System.out.println("[读线程] 开始迭代（快照视图，不受写影响）：");
            for (String item : cowList) {
                System.out.println("  读取：" + item);
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            System.out.println("[读线程] 迭代完成");
        });

        // 写线程：add 时复制新数组
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(80);  // 在读线程迭代中途写入
                cowList.add("白名单-D（新增）");
                System.out.println("[写线程] 新增白名单-D，当前列表：" + cowList);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        reader.start();
        writer.start();
        reader.join();
        writer.join();

        System.out.println("\nCOW 特点：");
        System.out.println("  ✅ 读操作无锁，性能极高（直接读数组引用）");
        System.out.println("  ✅ 写操作加锁 + Arrays.copyOf 复制新数组");
        System.out.println("  ✅ 迭代器是快照，不会抛 ConcurrentModificationException");
        System.out.println("  ❌ 写操作内存开销大（每次复制整个数组）");
        System.out.println("  ❌ 读可能读到旧数据（弱一致性）");
        System.out.println("  最佳场景：读多写少（监听器、白名单、路由规则配置）\n");
    }

    // =========================================================================
    // 4. ArrayList 线程安全方案对比
    // =========================================================================
    private static void demonstrateListThreadSafety() throws InterruptedException {
        System.out.println("【四、ArrayList 线程安全方案对比】\n");

        // 方案1：CopyOnWriteArrayList（读多写少）
        List<Integer> cowList = new CopyOnWriteArrayList<>();

        // 方案2：Collections.synchronizedList（通用）
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());

        // 方案3：同时写入，验证线程安全性
        int threadCount = 5;
        int writeCount = 100;

        // 验证 CopyOnWriteArrayList 线程安全
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < writeCount; j++) {
                    cowList.add(threadId * writeCount + j);
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        System.out.println("CopyOnWriteArrayList 并发写入 " + threadCount + " 线程 × " + writeCount
                + " 次，最终大小：" + cowList.size() + "（期望：" + (threadCount * writeCount) + "）");

        // 验证 synchronizedList 线程安全
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < writeCount; j++) {
                    syncList.add(threadId * writeCount + j);
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        System.out.println("Collections.synchronizedList 并发写入 " + threadCount + " 线程 × " + writeCount
                + " 次，最终大小：" + syncList.size() + "（期望：" + (threadCount * writeCount) + "）");

        System.out.println("\n方案对比：");
        System.out.println("┌──────────────────────────────┬────────┬──────┬────────────┐");
        System.out.println("│ 方案                         │ 性能   │ 灵活 │ 适用场景   │");
        System.out.println("├──────────────────────────────┼────────┼──────┼────────────┤");
        System.out.println("│ CopyOnWriteArrayList         │ 读极高 │ 中   │ 读多写少   │");
        System.out.println("│ Collections.synchronizedList │ 中     │ 高   │ 通用场景   │");
        System.out.println("│ Vector（不推荐）              │ 低     │ 低   │ 已过时     │");
        System.out.println("│ ReentrantLock+ArrayList       │ 高     │ 极高 │ 精细控制   │");
        System.out.println("│ ThreadLocal                  │ 极高   │ 中   │ 线程隔离   │");
        System.out.println("└──────────────────────────────┴────────┴──────┴────────────┘\n");
    }

    // =========================================================================
    // 5. Set 体系
    // =========================================================================
    private static void demonstrateSetFamily() {
        System.out.println("【五、Set 体系演示】\n");

        // --- HashSet：无序去重 ---
        System.out.println("--- HashSet：无序去重 ---");
        Set<String> hashSet = new HashSet<>();
        hashSet.add("banana");
        hashSet.add("apple");
        hashSet.add("cherry");
        hashSet.add("apple");   // 重复，不入集
        System.out.println("HashSet（无序）：" + hashSet);
        System.out.println("add 重复元素返回：" + hashSet.add("apple") + "（false 表示已存在）");

        // 自定义对象必须重写 hashCode + equals
        Set<Point> pointSet = new HashSet<>();
        pointSet.add(new Point(1, 2));
        pointSet.add(new Point(1, 2));  // 同坐标，应视为重复
        pointSet.add(new Point(3, 4));
        System.out.println("Point HashSet（重写了 hashCode+equals）：" + pointSet + "\n");

        // --- LinkedHashSet：保持插入顺序 ---
        System.out.println("--- LinkedHashSet：保持插入顺序 ---");
        Set<String> linkedSet = new LinkedHashSet<>();
        linkedSet.add("banana");
        linkedSet.add("apple");
        linkedSet.add("cherry");
        linkedSet.add("apple");   // 重复，不改变顺序
        System.out.println("LinkedHashSet（插入顺序）：" + linkedSet + "\n");

        // --- TreeSet：有序去重（自然排序） ---
        System.out.println("--- TreeSet：有序去重 ---");
        Set<Integer> treeSet = new TreeSet<>();
        treeSet.add(5);
        treeSet.add(1);
        treeSet.add(3);
        treeSet.add(2);
        treeSet.add(4);
        System.out.println("TreeSet（自然升序）：" + treeSet);

        // 自定义比较器（降序）
        TreeSet<Integer> descSet = new TreeSet<>(Comparator.reverseOrder());
        descSet.addAll(Arrays.asList(5, 1, 3, 2, 4));
        System.out.println("TreeSet（自定义降序）：" + descSet);

        // TreeSet 特有的范围操作
        TreeSet<Integer> rangeSet = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        System.out.println("subSet(3, 7)：" + rangeSet.subSet(3, 7) + "  [3, 7)");
        System.out.println("headSet(5)：" + rangeSet.headSet(5) + "  小于 5");
        System.out.println("tailSet(7)：" + rangeSet.tailSet(7) + "  >= 7\n");

        // --- ConcurrentSkipListSet：线程安全有序 ---
        System.out.println("--- ConcurrentSkipListSet：线程安全有序 Set ---");
        ConcurrentSkipListSet<Integer> skipSet = new ConcurrentSkipListSet<>();
        // 多线程并发写入
        Thread t1 = new Thread(() -> { for (int i = 0; i < 50; i++) skipSet.add(i * 2); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 50; i++) skipSet.add(i * 2 + 1); });
        try {
            t1.start(); t2.start(); t1.join(); t2.join();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("ConcurrentSkipListSet 并发写入 100 个元素，大小：" + skipSet.size());
        System.out.println("前 10 个（有序）：" + new ArrayList<>(skipSet).subList(0, 10) + "\n");
    }

    // =========================================================================
    // 6. Map 体系
    // =========================================================================
    private static void demonstrateMapFamily() {
        System.out.println("【六、Map 体系演示】\n");

        // --- HashMap：无序 O(1) ---
        System.out.println("--- HashMap：无序，O(1) ---");
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("语文", 90);
        hashMap.put("数学", 95);
        hashMap.put("英语", 88);
        hashMap.put("语文", 92);  // 覆盖旧值
        System.out.println("HashMap：" + hashMap);
        // 推荐遍历方式：entrySet（比 keySet 少一次 get 查询）
        System.out.print("entrySet 遍历：");
        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            System.out.print(entry.getKey() + "=" + entry.getValue() + " ");
        }

        // getOrDefault / putIfAbsent / computeIfAbsent
        System.out.println("\ngetOrDefault(\"物理\", 0)：" + hashMap.getOrDefault("物理", 0));
        hashMap.putIfAbsent("物理", 85);  // 不存在才写入
        System.out.println("putIfAbsent 物理=85 后：" + hashMap.get("物理"));

        // merge 统计词频
        String[] words = {"apple", "banana", "apple", "cherry", "banana", "apple"};
        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) {
            freq.merge(w, 1, Integer::sum);
        }
        System.out.println("词频统计 merge：" + freq + "\n");

        // --- LinkedHashMap：保持插入顺序 / 实现 LRU ---
        System.out.println("--- LinkedHashMap：LRU 缓存实现 ---");
        // accessOrder=true：每次 get/put 会将元素移到链表尾部
        int capacity = 4;
        Map<Integer, String> lruCache = new LinkedHashMap<Integer, String>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                // 超过容量时自动淘汰最久未使用的元素
                return size() > capacity;
            }
        };
        lruCache.put(1, "Page-1");
        lruCache.put(2, "Page-2");
        lruCache.put(3, "Page-3");
        lruCache.put(4, "Page-4");
        System.out.println("LRU 缓存（容量4）：" + lruCache.keySet());
        lruCache.get(1);    // 访问 key=1，移动到尾部（最近使用）
        System.out.println("访问 key=1 后：" + lruCache.keySet());
        lruCache.put(5, "Page-5");  // 超容量，淘汰 key=2（最久未使用）
        System.out.println("新增 key=5 后（淘汰最久未用）：" + lruCache.keySet() + "\n");

        // --- TreeMap：按 Key 有序 ---
        System.out.println("--- TreeMap：按 Key 有序 ---");
        TreeMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("banana", 2);
        treeMap.put("apple", 1);
        treeMap.put("cherry", 3);
        treeMap.put("date", 4);
        System.out.println("TreeMap（Key 字典序）：" + treeMap);
        System.out.println("firstKey：" + treeMap.firstKey() + "，lastKey：" + treeMap.lastKey());
        System.out.println("floorKey(\"cherry\")：" + treeMap.floorKey("cherry") + "（≤cherry 最大 key）");
        System.out.println("ceilingKey(\"b\")：" + treeMap.ceilingKey("b") + "（≥b 最小 key）\n");
    }

    // =========================================================================
    // 7. fail-fast 机制与安全迭代
    // =========================================================================
    private static void demonstrateFailFast() {
        System.out.println("【七、fail-fast 机制与安全迭代】\n");

        List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));

        // 触发 fail-fast
        System.out.println("❌ 触发 fail-fast（增强 for 中直接 remove）：");
        try {
            for (String s : list) {
                if ("C".equals(s)) {
                    list.remove(s);  // 修改 modCount，迭代器检测到抛异常
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("  捕获 ConcurrentModificationException：" + e.getClass().getSimpleName());
        }

        // 方案1：Iterator.remove()（推荐）
        System.out.println("\n✅ 方案1：Iterator.remove()（最通用）：");
        List<String> list1 = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        Iterator<String> it = list1.iterator();
        while (it.hasNext()) {
            if ("C".equals(it.next())) {
                it.remove();    // 同步更新 modCount，安全
            }
        }
        System.out.println("  删除 C 后：" + list1);

        // 方案2：removeIf（Java 8+，最简洁）
        System.out.println("\n✅ 方案2：removeIf（Java 8+）：");
        List<String> list2 = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        list2.removeIf(s -> "C".equals(s) || "D".equals(s));
        System.out.println("  删除 C、D 后：" + list2);

        // 方案3：普通 for 倒序（避免索引错位）
        System.out.println("\n✅ 方案3：倒序 for 循环：");
        List<String> list3 = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        for (int i = list3.size() - 1; i >= 0; i--) {
            if ("B".equals(list3.get(i))) {
                list3.remove(i);
            }
        }
        System.out.println("  删除 B 后：" + list3);

        // 方案4：CopyOnWriteArrayList（并发安全）
        System.out.println("\n✅ 方案4：CopyOnWriteArrayList（迭代快照）：");
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>(
                Arrays.asList("A", "B", "C", "D", "E")
        );
        for (String s : cowList) {       // 迭代的是快照数组
            if ("C".equals(s)) {
                cowList.remove(s);       // 写操作产生新数组，不影响迭代
            }
        }
        System.out.println("  删除 C 后：" + cowList + "\n");
    }

    // =========================================================================
    // 8. Collections 工具类
    // =========================================================================
    private static void demonstrateCollectionsUtils() {
        System.out.println("【八、Collections 工具类】\n");

        List<Integer> nums = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6));
        System.out.println("原始列表：" + nums);

        // 排序 / 反转 / 打乱
        Collections.sort(nums);
        System.out.println("sort()   ：" + nums);
        Collections.reverse(nums);
        System.out.println("reverse()：" + nums);

        // 二分查找（要求有序）
        Collections.sort(nums);
        int idx = Collections.binarySearch(nums, 5);
        System.out.println("binarySearch(5) 索引：" + idx + "，值：" + nums.get(idx));

        // 最值
        System.out.println("max：" + Collections.max(nums) + "，min：" + Collections.min(nums));

        // 不可修改视图
        List<String> immutable = Collections.unmodifiableList(Arrays.asList("A", "B", "C"));
        System.out.print("unmodifiableList 修改尝试：");
        try {
            immutable.add("D");
        } catch (UnsupportedOperationException e) {
            System.out.println("UnsupportedOperationException（正确！）");
        }

        // frequency / disjoint
        List<String> words = Arrays.asList("apple", "banana", "apple", "cherry");
        System.out.println("frequency(apple)：" + Collections.frequency(words, "apple"));

        // nCopies
        List<String> copies = Collections.nCopies(5, "hello");
        System.out.println("nCopies(5, \"hello\")：" + copies + "\n");
    }

    // =========================================================================
    // 9. 高频面试题答案演示
    // =========================================================================
    private static void showInterviewQuestions() {
        System.out.println("【九、高频面试题】\n");

        System.out.println("Q1：ArrayList 和 LinkedList 的区别？");
        System.out.println("┌──────────┬─────────────┬─────────────────┐");
        System.out.println("│ 特性     │ ArrayList   │ LinkedList      │");
        System.out.println("├──────────┼─────────────┼─────────────────┤");
        System.out.println("│ 底层结构 │ 动态数组    │ 双向链表        │");
        System.out.println("│ 随机访问 │ O(1)        │ O(n)            │");
        System.out.println("│ 首尾插删 │ O(n)        │ O(1)            │");
        System.out.println("│ 内存占用 │ 较少        │ 多（节点指针）  │");
        System.out.println("│ 实现接口 │ List        │ List + Deque    │");
        System.out.println("└──────────┴─────────────┴─────────────────┘");
        System.out.println("结论：90% 场景首选 ArrayList；需要队列/栈语义时用 LinkedList 或 ArrayDeque\n");

        System.out.println("Q2：HashSet 如何保证元素不重复？");
        System.out.println("A2：底层基于 HashMap，put(e, PRESENT)；");
        System.out.println("    1. 计算 hashCode() 找到桶位置");
        System.out.println("    2. 桶非空时，用 equals() 逐一比较");
        System.out.println("    3. equals() 返回 true → 已存在，不入集");
        System.out.println("    ⚠️  必须同时重写 hashCode 和 equals，否则去重失效\n");

        System.out.println("Q3：HashMap 和 Hashtable 的区别？");
        System.out.println("A3：HashMap 非线程安全、允许 null、性能高；");
        System.out.println("    Hashtable 全 synchronized、不允许 null、性能低、已过时；");
        System.out.println("    并发场景请用 ConcurrentHashMap\n");

        System.out.println("Q4：Collection 和 Collections 的区别？");
        System.out.println("A4：Collection 是集合根接口（List/Set/Queue 的父接口）；");
        System.out.println("    Collections 是操作集合的静态工具类（sort/reverse/unmodifiable...）\n");
    }

    // =========================================================================
    // 辅助类
    // =========================================================================

    /**
     * 演示自定义对象在 HashSet 中的去重（必须重写 hashCode + equals）
     */
    static class Point {
        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int hashCode() {
            // 用 Objects.hash 生成综合哈希值
            return Objects.hash(x, y);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }
}
