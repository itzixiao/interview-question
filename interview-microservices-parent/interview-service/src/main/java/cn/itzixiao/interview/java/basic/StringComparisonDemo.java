package cn.itzixiao.interview.java.basic;

/**
 * String、StringBuffer、StringBuilder 对比详解
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  String                                                      │
 * │  - 不可变（final char[] / byte[]）                            │
 * │  - 线程安全（不可变即安全）                                    │
 * │  - 适用：字符串常量、少量操作                                  │
 * ├─────────────────────────────────────────────────────────────┤
 * │  StringBuffer                                                │
 * │  - 可变（char[] / byte[]）                                    │
 * │  - 线程安全（synchronized）                                   │
 * │  - 适用：多线程环境下大量字符串操作                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │  StringBuilder                                               │
 * │  - 可变（char[] / byte[]）                                    │
 * │  - 非线程安全                                                │
 * │  - 适用：单线程环境下大量字符串操作                            │
 * └─────────────────────────────────────────────────────────────┘
 */
public class StringComparisonDemo {

    public static void main(String[] args) {
        System.out.println("========== String、StringBuffer、StringBuilder 对比 ==========\n");

        demonstrateStringImmutability();
        demonstrateStringConcatenation();
        demonstrateStringBufferAndBuilder();
        demonstratePerformanceComparison();
        demonstrateThreadSafety();
        demonstrateStringPool();
    }

    /**
     * 1. String 的不可变性
     */
    public static void demonstrateStringImmutability() {
        System.out.println("【1. String 的不可变性】\n");

        String s = "Hello";
        System.out.println("原始字符串: " + s);
        System.out.println("原始 hashCode: " + s.hashCode());

        // 字符串拼接，创建新对象
        s = s + " World";
        System.out.println("\n拼接后: " + s);
        System.out.println("新 hashCode: " + s.hashCode());

        // replace 也创建新对象
        String s2 = s.replace("World", "Java");
        System.out.println("\nreplace 后: " + s2);
        System.out.println("原字符串: " + s);  // 不变！

        System.out.println("\n【结论】String 的每次修改都创建新对象\n");
    }

    /**
     * 2. 字符串拼接的问题
     */
    public static void demonstrateStringConcatenation() {
        System.out.println("【2. 字符串拼接的问题】\n");

        // 方式1：String 拼接（创建大量临时对象）
        System.out.println("方式1：String 拼接");
        long start1 = System.currentTimeMillis();
        String str = "";
        for (int i = 0; i < 10000; i++) {
            str += i;  // 每次创建新 String 对象
        }
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("耗时: " + time1 + " ms");
        System.out.println("产生大量临时对象，效率低，内存开销大\n");

        // 方式2：StringBuilder 拼接
        System.out.println("方式2：StringBuilder 拼接");
        long start2 = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append(i);  // 直接修改内部数组
        }
        String result = sb.toString();
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("耗时: " + time2 + " ms");
        System.out.println("效率高，无临时对象\n");

        System.out.println("性能提升约 " + (time1 / time2) + " 倍\n");
    }

    /**
     * 3. StringBuffer 和 StringBuilder 的使用
     */
    public static void demonstrateStringBufferAndBuilder() {
        System.out.println("【3. StringBuffer 和 StringBuilder 的使用】\n");

        // StringBuilder（非线程安全，单线程推荐）
        StringBuilder sb = new StringBuilder();
        sb.append("Hello");
        sb.append(" ");
        sb.append("World");
        System.out.println("StringBuilder: " + sb.toString());

        // 链式调用
        String result = new StringBuilder()
                .append("Java")
                .append(" ")
                .append("Programming")
                .toString();
        System.out.println("链式调用: " + result);

        // StringBuffer（线程安全，多线程使用）
        StringBuffer sbf = new StringBuffer();
        sbf.append("Thread");
        sbf.append("-");
        sbf.append("Safe");
        System.out.println("StringBuffer: " + sbf.toString());

        // 其他常用方法
        System.out.println("\n【常用方法】");
        StringBuilder demo = new StringBuilder("Hello World");
        System.out.println("原始: " + demo);
        System.out.println("长度: " + demo.length());
        System.out.println("容量: " + demo.capacity());  // 默认 16 + 长度

        demo.insert(5, " Java");
        System.out.println("insert: " + demo);

        demo.delete(5, 10);
        System.out.println("delete: " + demo);

        demo.reverse();
        System.out.println("reverse: " + demo);

        demo.setLength(5);
        System.out.println("setLength(5): " + demo);

        System.out.println();
    }

    /**
     * 4. 性能对比
     */
    public static void demonstratePerformanceComparison() {
        System.out.println("【4. 性能对比】\n");

        int count = 100000;

        // String（极慢）
        System.out.println("拼接 " + count + " 次:");
        long start = System.currentTimeMillis();
        String s = "";
        for (int i = 0; i < count; i++) {
            s += "a";
        }
        System.out.println("String: " + (System.currentTimeMillis() - start) + " ms");

        // StringBuffer
        start = System.currentTimeMillis();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < count; i++) {
            sb.append("a");
        }
        System.out.println("StringBuffer: " + (System.currentTimeMillis() - start) + " ms");

        // StringBuilder
        start = System.currentTimeMillis();
        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sbd.append("a");
        }
        System.out.println("StringBuilder: " + (System.currentTimeMillis() - start) + " ms");

        System.out.println("\n【结论】");
        System.out.println("  单线程：StringBuilder > StringBuffer >> String");
        System.out.println("  多线程：StringBuffer（线程安全）\n");
    }

    /**
     * 5. 线程安全性测试
     */
    public static void demonstrateThreadSafety() {
        System.out.println("【5. 线程安全性测试】\n");

        // StringBuilder（非线程安全，可能出现错误结果）
        System.out.println("StringBuilder（非线程安全）:");
        testThreadSafety(new StringBuilder());

        // StringBuffer（线程安全）
        System.out.println("\nStringBuffer（线程安全）:");
        testThreadSafety(new StringBuffer());

        System.out.println();
    }

    private static void testThreadSafety(Appendable appendable) {
        int threadCount = 10;
        int appendCount = 1000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < appendCount; j++) {
                    try {
                        appendable.append("a");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int length = appendable.toString().length();
        int expected = threadCount * appendCount;
        System.out.println("  实际长度: " + length);
        System.out.println("  期望长度: " + expected);
        System.out.println("  结果: " + (length == expected ? "正确" : "错误（数据竞争）"));
    }

    /**
     * 6. 字符串常量池
     */
    public static void demonstrateStringPool() {
        System.out.println("【6. 字符串常量池】\n");

        // 直接赋值（从常量池获取）
        String s1 = "Java";
        String s2 = "Java";
        System.out.println("String s1 = \"Java\"");
        System.out.println("String s2 = \"Java\"");
        System.out.println("s1 == s2: " + (s1 == s2));  // true

        // new 创建（堆中新建对象）
        String s3 = new String("Java");
        System.out.println("\nString s3 = new String(\"Java\")");
        System.out.println("s1 == s3: " + (s1 == s3));  // false
        System.out.println("s1.equals(s3): " + s1.equals(s3));  // true

        // intern() 入池
        String s4 = new String("Java").intern();
        System.out.println("\nString s4 = new String(\"Java\").intern()");
        System.out.println("s1 == s4: " + (s1 == s4));  // true

        // 编译期优化
        String s5 = "Hel" + "lo";  // 编译期合并为 "Hello"
        String s6 = "Hello";
        System.out.println("\nString s5 = \"Hel\" + \"lo\"");
        System.out.println("s5 == s6: " + (s5 == s6));  // true

        // 运行时拼接（不会入池）
        String s7 = "Hel";
        String s8 = s7 + "lo";  // 运行时创建新对象
        System.out.println("\nString s7 = \"Hel\"");
        System.out.println("String s8 = s7 + \"lo\"");
        System.out.println("s6 == s8: " + (s6 == s8));  // false
        System.out.println("s6.equals(s8): " + s6.equals(s8));  // true

        System.out.println("\n【intern() 使用建议】");
        System.out.println("  - 大量重复字符串时，使用 intern() 节省内存");
        System.out.println("  - 注意：JDK7+ 字符串常量池在堆中，不会导致 PermGen OOM\n");
    }
}
