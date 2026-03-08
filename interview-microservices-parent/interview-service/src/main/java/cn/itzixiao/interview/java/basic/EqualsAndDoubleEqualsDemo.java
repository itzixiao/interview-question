package cn.itzixiao.interview.java.basic;

import java.util.Objects;

/**
 * == 与 equals 的区别详解
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  == 运算符                                                    │
 * │  - 基本数据类型：比较值是否相等                                │
 * │  - 引用数据类型：比较内存地址是否相同（是否是同一个对象）       │
 * ├─────────────────────────────────────────────────────────────┤
 * │  equals 方法                                                 │
 * │  - Object 默认实现：使用 == 比较（比较内存地址）               │
 * │  - 通常需要重写：比较对象的内容是否相等                        │
 * └─────────────────────────────────────────────────────────────┘
 */
public class EqualsAndDoubleEqualsDemo {

    public static void main(String[] args) {
        System.out.println("========== == 与 equals 的区别 ==========\n");

        demonstrateBasicTypes();
        demonstrateReferenceTypes();
        demonstrateStringPool();
        demonstrateIntegerCache();
        demonstrateCustomClass();
        demonstrateNullSafety();
    }

    /**
     * 1. 基本数据类型的比较
     */
    public static void demonstrateBasicTypes() {
        System.out.println("【1. 基本数据类型的比较】\n");

        int a = 10;
        int b = 10;

        System.out.println("int a = 10, int b = 10");
        System.out.println("a == b: " + (a == b));  // true，比较值

        double d1 = 3.14;
        double d2 = 3.14;
        System.out.println("\ndouble d1 = 3.14, double d2 = 3.14");
        System.out.println("d1 == d2: " + (d1 == d2));  // true

        char c1 = 'A';
        char c2 = 'A';
        System.out.println("\nchar c1 = 'A', char c2 = 'A'");
        System.out.println("c1 == c2: " + (c1 == c2));  // true

        System.out.println("\n基本数据类型只能用 == 比较，不能用 equals\n");
    }

    /**
     * 2. 引用数据类型的比较
     */
    public static void demonstrateReferenceTypes() {
        System.out.println("【2. 引用数据类型的比较】\n");

        // 创建两个内容相同但不同对象
        Person person1 = new Person("张三", 20);
        Person person2 = new Person("张三", 20);

        System.out.println("Person person1 = new Person(\"张三\", 20)");
        System.out.println("Person person2 = new Person(\"张三\", 20)");
        System.out.println("\nperson1 == person2: " + (person1 == person2));  // false，不同对象
        System.out.println("person1.equals(person2): " + person1.equals(person2));  // 取决于是否重写

        // 指向同一个对象
        Person person3 = person1;
        System.out.println("\nPerson person3 = person1");
        System.out.println("person1 == person3: " + (person1 == person3));  // true，同一对象
        System.out.println("person1.equals(person3): " + person1.equals(person3));  // true

        System.out.println();
    }

    /**
     * 3. String 的特殊性（字符串常量池）
     */
    public static void demonstrateStringPool() {
        System.out.println("【3. String 的特殊性 - 字符串常量池】\n");

        // 直接赋值（从常量池获取）
        String s1 = "hello";
        String s2 = "hello";

        System.out.println("String s1 = \"hello\"");
        System.out.println("String s2 = \"hello\"");
        System.out.println("s1 == s2: " + (s1 == s2));  // true，同一常量池对象
        System.out.println("s1.equals(s2): " + s1.equals(s2));  // true

        // new 创建（堆中新对象）
        String s3 = new String("hello");
        String s4 = new String("hello");

        System.out.println("\nString s3 = new String(\"hello\")");
        System.out.println("String s4 = new String(\"hello\")");
        System.out.println("s3 == s4: " + (s3 == s4));  // false，不同对象
        System.out.println("s3.equals(s4): " + s3.equals(s4));  // true，内容相同

        System.out.println("\ns1 == s3: " + (s1 == s3));  // false
        System.out.println("s1.equals(s3): " + s1.equals(s3));  // true

        // intern() 方法
        System.out.println("\ns3.intern() == s1: " + (s3.intern() == s1));  // true

        System.out.println("\n【String 比较建议】");
        System.out.println("  - 比较内容：使用 equals()");
        System.out.println("  - 比较引用：使用 ==（通常不推荐）\n");
    }

    /**
     * 4. Integer 缓存（-128 ~ 127）
     */
    public static void demonstrateIntegerCache() {
        System.out.println("【4. Integer 缓存（-128 ~ 127）】\n");

        // 缓存范围内的整数
        Integer i1 = 100;
        Integer i2 = 100;
        System.out.println("Integer i1 = 100, Integer i2 = 100");
        System.out.println("i1 == i2: " + (i1 == i2));  // true，缓存同一对象
        System.out.println("i1.equals(i2): " + i1.equals(i2));  // true

        // 超出缓存范围
        Integer i3 = 200;
        Integer i4 = 200;
        System.out.println("\nInteger i3 = 200, Integer i4 = 200");
        System.out.println("i3 == i4: " + (i3 == i4));  // false，不同对象
        System.out.println("i3.equals(i4): " + i3.equals(i4));  // true
        System.out.println("i3.intValue() == i4.intValue(): " + (i3.intValue() == i4.intValue()));  // true

        // 显式 new（Java 9+ 已废弃）
        Integer i5 = new Integer(100);
        System.out.println("\nInteger i5 = new Integer(100)");
        System.out.println("i1 == i5: " + (i1 == i5));  // false

        System.out.println("\n【包装类比较建议】");
        System.out.println("  - 比较值：使用 equals()");
        System.out.println("  - 或拆箱后用 ==：intValue()\n");
    }

    /**
     * 5. 自定义类的 equals 重写
     */
    public static void demonstrateCustomClass() {
        System.out.println("【5. 自定义类的 equals 重写】\n");

        Student student1 = new Student("李四", 1001);
        Student student2 = new Student("李四", 1001);

        System.out.println("Student 重写了 equals 和 hashCode");
        System.out.println("student1 == student2: " + (student1 == student2));  // false
        System.out.println("student1.equals(student2): " + student1.equals(student2));  // true

        System.out.println("\n【equals 重写规范】");
        System.out.println("  1. 自反性：x.equals(x) == true");
        System.out.println("  2. 对称性：x.equals(y) == y.equals(x)");
        System.out.println("  3. 传递性：x.equals(y) && y.equals(z) => x.equals(z)");
        System.out.println("  4. 一致性：多次调用结果相同");
        System.out.println("  5. 非空性：x.equals(null) == false\n");
    }

    /**
     * 6. null 安全性
     */
    public static void demonstrateNullSafety() {
        System.out.println("【6. null 安全性】\n");

        String s1 = null;
        String s2 = "hello";


        // 安全的比较方式
        System.out.println("Objects.equals(s1, s2): " + Objects.equals(s1, s2));  // false
        System.out.println("Objects.equals(s2, s1): " + Objects.equals(s2, s1));  // false

        // 常量放前面（不推荐，易出错）
        System.out.println("\"hello\".equals(s1): " + "hello".equals(s1));  // false
        System.out.println("s2.equals(s1): " + s2.equals(s1));  // false

        System.out.println("\n【建议】使用 Objects.equals() 进行 null 安全的比较\n");
        // System.out.println(s1.equals(s2));  // 空指针异常！
    }

    /**
     * 未重写 equals 的类
     */
    static class Person {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /**
     * 重写了 equals 和 hashCode 的类
     */
    static class Student {
        String name;
        int id;

        Student(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Student student = (Student) o;
            return id == student.id &&
                    Objects.equals(name, student.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, id);
        }
    }
}
