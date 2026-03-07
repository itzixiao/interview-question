package cn.itzixiao.interview.java;

import java.io.*;

/**
 * =====================================================================================
 * 深拷贝与浅拷贝详解
 * =====================================================================================
 * 
 * 一、什么是拷贝？
 * -------------------------------------------------------------------------------------
 * 拷贝就是创建一个对象的副本。根据副本与原对象的关系，分为深拷贝和浅拷贝。
 * 
 * 二、浅拷贝（Shallow Copy）
 * -------------------------------------------------------------------------------------
 * - 创建一个新对象，复制基本类型属性的值
 * - 对于引用类型属性，只复制引用（地址），不复制对象本身
 * - 原对象和副本的引用类型属性指向同一个对象
 * - 实现：实现 Cloneable 接口，重写 clone() 方法
 * 
 * 三、深拷贝（Deep Copy）
 * -------------------------------------------------------------------------------------
 * - 创建一个新对象，复制所有属性
 * - 对于引用类型属性，也创建新对象（递归拷贝）
 * - 原对象和副本完全独立，互不影响
 * - 实现方式：
 *   1. 序列化/反序列化（推荐）
 *   2. 手动复制（构造方法或工具方法）
 *   3. 重写 clone() 方法递归拷贝
 * 
 * 四、内存结构对比
 * -------------------------------------------------------------------------------------
 * 浅拷贝内存结构：
 * ┌──────────────┐     ┌──────────────┐
 * │ 原对象       │     │ 副本对象     │
 * │ ──────────── │     │ ──────────── │
 * │ name: "张三" │     │ name: "张三" │ ← 基本类型：值复制
 * │ age: 25      │     │ age: 25      │
 * │ address ─────┼──┬──┼─→ address    │ ← 引用类型：引用复制
 * └──────────────┘  │  └──────────────┘
 *                   ↓
 *              ┌──────────────┐
 *              │ Address 对象 │ ← 同一个对象，共享！
 *              │ city: "北京" │
 *              └──────────────┘
 * 
 * 深拷贝内存结构：
 * ┌──────────────┐     ┌──────────────┐
 * │ 原对象       │     │ 副本对象     │
 * │ ──────────── │     │ ──────────── │
 * │ name: "张三" │     │ name: "张三" │ ← 基本类型：值复制
 * │ age: 25      │     │ age: 25      │
 * │ address ─────┼──┐  │ address ─────┼──┐
 * └──────────────┘  │  └──────────────┘  │
 *                   ↓                    ↓
 *              ┌──────────────┐    ┌──────────────┐
 *              │ Address 对象1│    │ Address 对象2│ ← 两个独立对象！
 *              │ city: "北京" │    │ city: "北京" │
 *              └──────────────┘    └──────────────┘
 * 
 * 五、Java 中的拷贝
 * -------------------------------------------------------------------------------------
 * - Object.clone()：默认是浅拷贝
 * - 数组的 clone()：一维数组是深拷贝，多维数组是浅拷贝
 * - 集合的克隆：需要手动深拷贝
 */
public class DeepCopyAndShallowCopyDemo {

    public static void main(String[] args) {
        System.out.println("========== 深拷贝与浅拷贝详解 ==========\n");

        // 1. 浅拷贝演示
        demonstrateShallowCopy();

        // 2. 深拷贝演示
        demonstrateDeepCopy();

        // 3. 数组拷贝
        demonstrateArrayCopy();

        // 4. 集合拷贝
        demonstrateCollectionCopy();

        // 5. 高频面试题
        showInterviewQuestions();
    }

    /**
     * 浅拷贝演示
     */
    private static void demonstrateShallowCopy() {
        System.out.println("【一、浅拷贝演示】\n");

        // 创建原对象
        PersonShallow original = new PersonShallow("张三", 25, 
                new AddressShallow("北京", "朝阳区"));
        System.out.println("原对象: " + original);

        // 浅拷贝
        PersonShallow copy = original.clone();
        System.out.println("副本对象: " + copy);

        // 修改原对象的引用类型属性
        System.out.println("\n修改原对象的 address.city = '上海':");
        original.getAddress().setCity("上海");

        System.out.println("原对象: " + original);
        System.out.println("副本对象: " + copy);

        System.out.println("\n【结论】浅拷贝后，修改原对象引用类型属性，副本也受影响（共享同一对象）");

        // 对比引用
        System.out.println("\n【引用对比】:");
        System.out.println("  original == copy: " + (original == copy));
        System.out.println("  original.address == copy.address: " + 
                (original.getAddress() == copy.getAddress()));
        System.out.println("  → 地址引用相同，说明是同一个对象");
    }

    /**
     * 深拷贝演示
     */
    private static void demonstrateDeepCopy() {
        System.out.println("\n【二、深拷贝演示】\n");

        // 创建原对象
        PersonDeep original = new PersonDeep("李四", 30, 
                new AddressDeep("广州", "天河区"));
        System.out.println("原对象: " + original);

        // 深拷贝（序列化方式）
        PersonDeep copy = original.deepCopy();
        System.out.println("副本对象: " + copy);

        // 修改原对象的引用类型属性
        System.out.println("\n修改原对象的 address.city = '深圳':");
        original.getAddress().setCity("深圳");

        System.out.println("原对象: " + original);
        System.out.println("副本对象: " + copy);

        System.out.println("\n【结论】深拷贝后，修改原对象引用类型属性，副本不受影响（独立对象）");

        // 对比引用
        System.out.println("\n【引用对比】:");
        System.out.println("  original == copy: " + (original == copy));
        System.out.println("  original.address == copy.address: " + 
                (original.getAddress() == copy.getAddress()));
        System.out.println("  → 地址引用不同，说明是独立对象");
    }

    /**
     * 数组拷贝演示
     */
    private static void demonstrateArrayCopy() {
        System.out.println("\n【三、数组拷贝演示】\n");

        // 一维数组（基本类型）- 深拷贝效果
        System.out.println("1. 一维数组（基本类型）:");
        int[] arr1 = {1, 2, 3};
        int[] arr1Copy = arr1.clone();
        arr1[0] = 100;
        System.out.println("  原数组: " + java.util.Arrays.toString(arr1));
        System.out.println("  副本数组: " + java.util.Arrays.toString(arr1Copy));
        System.out.println("  → 一维基本类型数组 clone() 是深拷贝");

        // 二维数组 - 浅拷贝
        System.out.println("\n2. 二维数组（引用类型）:");
        int[][] arr2 = {{1, 2}, {3, 4}};
        int[][] arr2Copy = arr2.clone();
        arr2[0][0] = 100;
        System.out.println("  原数组: " + java.util.Arrays.deepToString(arr2));
        System.out.println("  副本数组: " + java.util.Arrays.deepToString(arr2Copy));
        System.out.println("  → 多维数组 clone() 是浅拷贝（只复制第一层）");
        System.out.println("  arr2[0] == arr2Copy[0]: " + (arr2[0] == arr2Copy[0]));
    }

    /**
     * 集合拷贝演示
     */
    private static void demonstrateCollectionCopy() {
        System.out.println("\n【四、集合拷贝演示】\n");

        // 浅拷贝
        System.out.println("1. 集合浅拷贝（new ArrayList<>(list)）:");
        java.util.List<AddressShallow> list1 = new java.util.ArrayList<>();
        list1.add(new AddressShallow("北京", "朝阳"));
        java.util.List<AddressShallow> list1Copy = new java.util.ArrayList<>(list1);
        
        list1.get(0).setCity("上海");
        System.out.println("  原集合元素: " + list1.get(0));
        System.out.println("  副本集合元素: " + list1Copy.get(0));
        System.out.println("  → 集合浅拷贝，元素仍然是共享的");

        // 深拷贝
        System.out.println("\n2. 集合深拷贝（手动复制每个元素）:");
        java.util.List<AddressDeep> list2 = new java.util.ArrayList<>();
        list2.add(new AddressDeep("广州", "天河"));
        
        java.util.List<AddressDeep> list2DeepCopy = new java.util.ArrayList<>();
        for (AddressDeep addr : list2) {
            list2DeepCopy.add(new AddressDeep(addr.getCity(), addr.getDistrict()));
        }
        
        list2.get(0).setCity("深圳");
        System.out.println("  原集合元素: " + list2.get(0));
        System.out.println("  副本集合元素: " + list2DeepCopy.get(0));
        System.out.println("  → 集合深拷贝，元素是独立的");
    }

    /**
     * 高频面试题
     */
    private static void showInterviewQuestions() {
        System.out.println("\n【五、高频面试题】\n");

        System.out.println("Q1: 深拷贝和浅拷贝的区别？");
        System.out.println("答案：");
        System.out.println("  浅拷贝：基本类型复制值，引用类型复制引用（共享）");
        System.out.println("  深拷贝：所有类型都复制值，完全独立");

        System.out.println("\nQ2: 如何实现深拷贝？");
        System.out.println("答案：");
        System.out.println("  1. 序列化/反序列化（推荐）- 实现 Serializable");
        System.out.println("  2. 手动复制 - 构造方法或工具方法");
        System.out.println("  3. 重写 clone() 递归拷贝");
        System.out.println("  4. 使用第三方库 - 如 Apache Commons Lang SerializationUtils");

        System.out.println("\nQ3: Object.clone() 是深拷贝还是浅拷贝？");
        System.out.println("答案：");
        System.out.println("  默认是浅拷贝");
        System.out.println("  需要实现 Cloneable 接口（标记接口）");
        System.out.println("  重写 clone() 方法并调用 super.clone()");

        System.out.println("\nQ4: String 类型在拷贝时需要注意什么？");
        System.out.println("答案：");
        System.out.println("  String 是不可变类，浅拷贝和深拷贝效果相同");
        System.out.println("  修改 String 实际上是创建新对象，不会影响原对象");
        System.out.println("  所以 String 类型在拷贝时可以当作基本类型处理");

        System.out.println("\nQ5: 为什么推荐使用序列化实现深拷贝？");
        System.out.println("答案：");
        System.out.println("  1. 通用性强：不用针对每个类写拷贝逻辑");
        System.out.println("  2. 自动处理对象图：递归拷贝所有引用");
        System.out.println("  3. 代码简洁：只需要实现 Serializable");
        System.out.println("  缺点：性能较差，需要处理 SerialVersionUID");

        System.out.println("\nQ6: Cloneable 接口有什么作用？");
        System.out.println("答案：");
        System.out.println("  Cloneable 是标记接口（没有方法）");
        System.out.println("  如果类实现了 Cloneable，调用 clone() 不会抛异常");
        System.out.println("  如果没有实现，调用 clone() 会抛 CloneNotSupportedException");

        System.out.println("\nQ7: 实际开发中如何选择拷贝方式？");
        System.out.println("答案：");
        System.out.println("  1. 如果没有引用类型属性：浅拷贝足够");
        System.out.println("  2. 如果引用类型是不可变类（如 String）：浅拷贝足够");
        System.out.println("  3. 如果需要完全独立：使用深拷贝");
        System.out.println("  4. 简单对象：构造方法手动复制");
        System.out.println("  5. 复杂对象图：序列化方式");
    }
}

/**
 * =====================================================================================
 * 浅拷贝示例类
 * =====================================================================================
 */
class PersonShallow implements Cloneable {
    private String name;           // 基本类型（String 不可变，效果等同于基本类型）
    private int age;               // 基本类型
    private AddressShallow address; // 引用类型

    public PersonShallow(String name, int age, AddressShallow address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }

    /**
     * 浅拷贝实现
     * 只需要实现 Cloneable 接口，调用 super.clone()
     */
    @Override
    public PersonShallow clone() {
        try {
            // Object.clone() 执行的是浅拷贝
            return (PersonShallow) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("克隆失败", e);
        }
    }

    public AddressShallow getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + 
                ", address=" + address + "}";
    }
}

/**
 * 浅拷贝用的地址类
 */
class AddressShallow {
    private String city;
    private String district;

    public AddressShallow(String city, String district) {
        this.city = city;
        this.district = district;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "Address{city='" + city + "', district='" + district + "'}";
    }
}

/**
 * =====================================================================================
 * 深拷贝示例类
 * =====================================================================================
 */
class PersonDeep implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int age;
    private AddressDeep address; // 引用类型

    public PersonDeep(String name, int age, AddressDeep address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }

    /**
     * 深拷贝实现 - 序列化方式
     * 原理：将对象序列化到字节流，再反序列化出新对象
     */
    public PersonDeep deepCopy() {
        try {
            // 1. 序列化：对象 → 字节流
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.flush();

            // 2. 反序列化：字节流 → 新对象
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (PersonDeep) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("深拷贝失败", e);
        }
    }

    /**
     * 深拷贝实现 - 手动复制方式
     */
    public PersonDeep manualCopy() {
        // 手动创建新对象，复制所有属性
        AddressDeep addressCopy = new AddressDeep(
                this.address.getCity(), 
                this.address.getDistrict());
        return new PersonDeep(this.name, this.age, addressCopy);
    }

    public AddressDeep getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + 
                ", address=" + address + "}";
    }
}

/**
 * 深拷贝用的地址类（需要实现 Serializable）
 */
class AddressDeep implements Serializable {
    private static final long serialVersionUID = 1L;

    private String city;
    private String district;

    public AddressDeep(String city, String district) {
        this.city = city;
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    @Override
    public String toString() {
        return "Address{city='" + city + "', district='" + district + "'}";
    }
}
