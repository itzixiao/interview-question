package cn.itzixiao.interview.java;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 集合框架深入理解
 *
 * 集合框架结构：
 * ┌─────────────────────────────────────────────────────────────┐
 * │                         Collection                          │
 * │                    /          |          \                  │
 * │                List         Set         Queue               │
 * │               /    \      /    \       /     \              │
 * │         ArrayList  LinkedList  HashSet  TreeSet  PriorityQueue│
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │                          Map                                │
 * │                    /          |          \                  │
 * │               HashMap    LinkedHashMap   TreeMap            │
 * │                  |                                          │
 * │            ConcurrentHashMap                                │
 * └─────────────────────────────────────────────────────────────┘
 */
public class CollectionDeepDiveDemo {

    /**
     * 1. List 实现对比
     */
    public void listComparison() {
        System.out.println("========== List 实现对比 ==========\n");

        // ArrayList：基于数组，随机访问快，插入删除慢
        List<String> arrayList = new ArrayList<>();
        System.out.println("【ArrayList】");
        System.out.println("  底层：Object[] 数组");
        System.out.println("  扩容：原容量的 1.5 倍（oldCapacity + (oldCapacity >> 1)）");
        System.out.println("  特点：随机访问 O(1)，插入删除 O(n)");
        System.out.println("  适用：查询多、插入删除少\n");

        // LinkedList：基于双向链表，插入删除快，随机访问慢
        List<String> linkedList = new LinkedList<>();
        System.out.println("【LinkedList】");
        System.out.println("  底层：双向链表（Node {prev, item, next}）");
        System.out.println("  特点：随机访问 O(n)，插入删除 O(1)");
        System.out.println("  适用：插入删除多、查询少");
        System.out.println("  额外：实现了 Deque 接口，可作栈/队列使用\n");

        // Vector：线程安全的 ArrayList（已过时）
        Vector<String> vector = new Vector<>();
        System.out.println("【Vector】");
        System.out.println("  特点：所有方法 synchronized，线程安全");
        System.out.println("  扩容：原容量的 2 倍");
        System.out.println("  替代：使用 Collections.synchronizedList 或 CopyOnWriteArrayList\n");

        // CopyOnWriteArrayList：读多写少的并发场景
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
        System.out.println("【CopyOnWriteArrayList】");
        System.out.println("  原理：写操作复制新数组，读操作不加锁");
        System.out.println("  适用：读多写少（读性能极高）");
        System.out.println("  缺点：内存占用大，数据最终一致性\n");
    }

    /**
     * 2. Set 实现对比
     */
    public void setComparison() {
        System.out.println("========== Set 实现对比 ==========\n");

        // HashSet：基于 HashMap
        Set<String> hashSet = new HashSet<>();
        System.out.println("【HashSet】");
        System.out.println("  底层：HashMap（key = 元素，value = 固定 Object PRESENT）");
        System.out.println("  特点：无序、去重、O(1) 操作");
        System.out.println("  去重：hashCode() + equals()\n");

        // LinkedHashSet：保持插入顺序
        Set<String> linkedHashSet = new LinkedHashSet<>();
        System.out.println("【LinkedHashSet】");
        System.out.println("  底层：LinkedHashMap（数组 + 链表 + 双向链表）");
        System.out.println("  特点：按插入顺序遍历");
        System.out.println("  适用：需要顺序的 Set\n");

        // TreeSet：基于红黑树
        Set<String> treeSet = new TreeSet<>();
        System.out.println("【TreeSet】");
        System.out.println("  底层：TreeMap（红黑树）");
        System.out.println("  特点：有序（自然排序或自定义 Comparator）");
        System.out.println("  操作：O(log n)");
        System.out.println("  适用：需要排序的 Set\n");

        // ConcurrentSkipListSet：线程安全的有序 Set
        Set<String> skipListSet = new ConcurrentSkipListSet<>();
        System.out.println("【ConcurrentSkipListSet】");
        System.out.println("  底层：跳表（Skip List）");
        System.out.println("  特点：线程安全、有序");
        System.out.println("  操作：O(log n)\n");
    }

    /**
     * 3. Map 实现对比
     */
    public void mapComparison() {
        System.out.println("========== Map 实现对比 ==========\n");

        // HashMap：最常用
        Map<String, String> hashMap = new HashMap<>();
        System.out.println("【HashMap】");
        System.out.println("  底层：数组 + 链表 + 红黑树（JDK8+）");
        System.out.println("  容量：默认 16，扩容 2 倍");
        System.out.println("  树化：链表长度 >= 8 且数组长度 >= 64");
        System.out.println("  退化：树节点 <= 6 退化为链表");
        System.out.println("  线程安全：否（并发使用 ConcurrentHashMap）\n");

        // LinkedHashMap：保持插入顺序
        Map<String, String> linkedHashMap = new LinkedHashMap<>();
        System.out.println("【LinkedHashMap】");
        System.out.println("  底层：HashMap + 双向链表");
        System.out.println("  特点：按插入顺序或访问顺序遍历");
        System.out.println("  应用：LRU 缓存实现\n");

        // TreeMap：基于红黑树
        Map<String, String> treeMap = new TreeMap<>();
        System.out.println("【TreeMap】");
        System.out.println("  底层：红黑树");
        System.out.println("  特点：按键排序");
        System.out.println("  操作：O(log n)\n");

        // ConcurrentHashMap：线程安全
        Map<String, String> concurrentMap = new ConcurrentHashMap<>();
        System.out.println("【ConcurrentHashMap】");
        System.out.println("  JDK7：分段锁（Segment[]，默认16段）");
        System.out.println("  JDK8：CAS + synchronized（锁单个桶）");
        System.out.println("  并发度：JDK7 固定16，JDK8 与桶数量相关");
        System.out.println("  读：无锁（volatile）");
        System.out.println("  写：细粒度锁\n");

        // Hashtable：已过时
        Hashtable<String, String> hashtable = new Hashtable<>();
        System.out.println("【Hashtable】");
        System.out.println("  特点：全表锁（synchronized），线程安全");
        System.out.println("  替代：ConcurrentHashMap\n");
    }

    /**
     * 4. Queue 实现
     */
    public void queueComparison() {
        System.out.println("========== Queue 实现对比 ==========\n");

        // LinkedList：双向链表实现
        Queue<String> linkedListQueue = new LinkedList<>();
        System.out.println("【LinkedList 作为 Queue】");
        System.out.println("  特点：无界队列，FIFO");
        System.out.println("  操作：offer/poll O(1)\n");

        // PriorityQueue：优先队列（堆实现）
        Queue<Integer> priorityQueue = new PriorityQueue<>();
        System.out.println("【PriorityQueue】");
        System.out.println("  底层：小顶堆（完全二叉树）");
        System.out.println("  特点：按优先级出队");
        System.out.println("  操作：插入 O(log n)，查看队首 O(1)\n");

        // ArrayDeque：双端队列
        Deque<String> arrayDeque = new ArrayDeque<>();
        System.out.println("【ArrayDeque】");
        System.out.println("  底层：循环数组");
        System.out.println("  特点：无界双端队列，比 Stack 快");
        System.out.println("  应用：栈（push/pop）、队列（offer/poll）\n");

        // 阻塞队列
        System.out.println("【阻塞队列】");
        BlockingQueue<String> arrayBlockingQueue = new ArrayBlockingQueue<>(100);
        System.out.println("  ArrayBlockingQueue：有界数组，一把锁");

        BlockingQueue<String> linkedBlockingQueue = new LinkedBlockingQueue<>();
        System.out.println("  LinkedBlockingQueue：可选有界链表，两把锁（读写分离）");

        BlockingQueue<String> priorityBlockingQueue = new PriorityBlockingQueue<>();
        System.out.println("  PriorityBlockingQueue：无界优先队列");

        BlockingQueue<String> synchronousQueue = new SynchronousQueue<>();
        System.out.println("  SynchronousQueue：不存储元素，直接传递");

        // DelayQueue 需要 Delayed 类型的元素，这里仅作演示
        // BlockingQueue<DelayedElement> delayQueue = new DelayQueue<>();
        System.out.println("  DelayQueue：延时队列，元素需要实现 Delayed 接口\n");
    }

    /**
     * 5. 迭代器与 fail-fast 机制
     */
    public void iteratorAndFailFast() {
        System.out.println("========== 迭代器与 fail-fast ==========\n");

        // fail-fast 示例
        List<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        System.out.println("【fail-fast 机制】");
        System.out.println("  原理：modCount（修改次数）检查");
        System.out.println("  触发：迭代过程中修改集合结构\n");

        try {
            for (String s : list) {
                if ("B".equals(s)) {
                    list.remove(s);  // 会抛出 ConcurrentModificationException
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("  异常：ConcurrentModificationException");
        }

        // 正确删除方式1：使用 Iterator.remove()
        System.out.println("\n【正确删除方式1】Iterator.remove()");
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String s = iterator.next();
            if ("B".equals(s)) {
                iterator.remove();  // 安全删除
            }
        }

        // 正确删除方式2：使用 removeIf（Java 8+）
        System.out.println("【正确删除方式2】removeIf()");
        list.removeIf(s -> "B".equals(s));

        // 正确删除方式3：使用 CopyOnWriteArrayList
        System.out.println("【正确删除方式3】CopyOnWriteArrayList");
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>(list);
        for (String s : cowList) {
            if ("B".equals(s)) {
                cowList.remove(s);  // 不会抛异常
            }
        }

        System.out.println();
    }

    /**
     * 6. 集合性能测试对比
     */
    public void performanceComparison() {
        System.out.println("========== 集合性能对比 ==========\n");

        int size = 100000;

        // ArrayList vs LinkedList
        System.out.println("【插入性能】（尾部插入 " + size + " 次）");
        long start = System.currentTimeMillis();
        List<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(i);
        }
        System.out.println("  ArrayList: " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            linkedList.add(i);
        }
        System.out.println("  LinkedList: " + (System.currentTimeMillis() - start) + "ms\n");

        // 随机访问性能
        System.out.println("【随机访问性能】（访问 " + size + " 次）");
        start = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            arrayList.get(i);
        }
        System.out.println("  ArrayList: " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            linkedList.get(i);
        }
        System.out.println("  LinkedList: " + (System.currentTimeMillis() - start) + "ms\n");

        // HashMap vs ConcurrentHashMap
        System.out.println("【并发写入性能】（10线程各写入 10000 次）");
        Map<Integer, Integer> hashMap = new HashMap<>();
        start = System.currentTimeMillis();
        concurrentWriteTest(hashMap, 10, 10000);
        System.out.println("  HashMap（加锁）: " + (System.currentTimeMillis() - start) + "ms");

        Map<Integer, Integer> concurrentHashMap = new ConcurrentHashMap<>();
        start = System.currentTimeMillis();
        concurrentWriteTest(concurrentHashMap, 10, 10000);
        System.out.println("  ConcurrentHashMap: " + (System.currentTimeMillis() - start) + "ms\n");
    }

    private void concurrentWriteTest(Map<Integer, Integer> map, int threads, int count) {
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            new Thread(() -> {
                for (int j = 0; j < count; j++) {
                    int key = threadId * count + j;
                    synchronized (map) {  // HashMap 需要加锁
                        map.put(key, key);
                    }
                }
                latch.countDown();
            }).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 7. 集合工具类使用
     */
    public void collectionsUtility() {
        System.out.println("========== Collections 工具类 ==========\n");

        // 创建空集合
        List<String> emptyList = Collections.emptyList();
        Set<String> emptySet = Collections.emptySet();
        Map<String, String> emptyMap = Collections.emptyMap();
        System.out.println("【空集合】不可修改");
        System.out.println("  emptyList: " + emptyList);
        System.out.println("  emptySet: " + emptySet);
        System.out.println("  emptyMap: " + emptyMap + "\n");

        // 创建单元素集合
        List<String> singletonList = Collections.singletonList("item");
        Set<String> singletonSet = Collections.singleton("item");
        Map<String, String> singletonMap = Collections.singletonMap("key", "value");
        System.out.println("【单元素集合】不可修改");
        System.out.println("  singletonList: " + singletonList);
        System.out.println("  singletonSet: " + singletonSet);
        System.out.println("  singletonMap: " + singletonMap + "\n");

        // 不可修改集合
        List<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");
        List<String> unmodifiableList = Collections.unmodifiableList(list);
        System.out.println("【不可修改集合】");
        System.out.println("  unmodifiableList: " + unmodifiableList);
        System.out.println("  修改会抛出 UnsupportedOperationException\n");

        // 线程安全集合
        List<String> synchronizedList = Collections.synchronizedList(new ArrayList<>());
        Map<String, String> synchronizedMap = Collections.synchronizedMap(new HashMap<>());
        System.out.println("【线程安全包装】");
        System.out.println("  synchronizedList: 所有方法加 synchronized");
        System.out.println("  synchronizedMap: 所有方法加 synchronized\n");

        // 排序
        List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        Collections.sort(numbers);
        System.out.println("【排序】");
        System.out.println("  sort: " + numbers);
        Collections.reverse(numbers);
        System.out.println("  reverse: " + numbers);
        Collections.shuffle(numbers);
        System.out.println("  shuffle: " + numbers + "\n");

        // 二分查找
        Collections.sort(numbers);
        int index = Collections.binarySearch(numbers, 4);
        System.out.println("【二分查找】");
        System.out.println("  binarySearch(4): index = " + index + "\n");
    }

    public static void main(String[] args) {
        CollectionDeepDiveDemo demo = new CollectionDeepDiveDemo();
        demo.listComparison();
        demo.setComparison();
        demo.mapComparison();
        demo.queueComparison();
        demo.iteratorAndFailFast();
        demo.performanceComparison();
        demo.collectionsUtility();
    }
}
