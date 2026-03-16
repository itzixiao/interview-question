package cn.itzixiao.interview.designpattern.creational.prototype;

import java.io.*;

/**
 * =====================================================================================
 * 原型模式（Prototype Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 用原型实例指定创建对象的种类，通过拷贝这些原型创建新的对象。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 原型接口（Prototype）：声明克隆方法
 * 2. 具体原型（Concrete Prototype）：实现克隆方法
 * 3. 客户端（Client）：通过克隆原型创建新对象
 * <p>
 * 三、两种克隆方式
 * -------------------------------------------------------------------------------------
 * 1. 浅拷贝（Shallow Clone）
 * - 创建新对象，复制基本类型属性
 * - 引用类型属性只复制引用，不复制对象本身
 * - 实现 Cloneable 接口，重写 clone() 方法
 * <p>
 * 2. 深拷贝（Deep Clone）
 * - 创建新对象，复制所有属性
 * - 引用类型属性也创建新对象
 * - 实现方式：序列化/反序列化、手动复制
 * <p>
 * 四、浅拷贝 vs 深拷贝
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  类型       │  基本类型  │  引用类型（String）│  引用类型（对象）     │
 * │-------------│-----------│-------------------│----------------------│
 * │  浅拷贝     │  复制值    │  复制引用         │  复制引用（共享）     │
 * │  深拷贝     │  复制值    │  复制引用         │  创建新对象（独立）   │
 * └─────────────────────────────────────────────────────────────────────┘
 * <p>
 * 注意：String 类型虽然是引用类型，但由于不可变性，浅拷贝和深拷贝效果相同
 * <p>
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - 创建成本高的对象（需要查询数据库、网络请求等）
 * - 创建复杂对象（属性多、初始化复杂）
 * - 需要保留原始对象状态的场景（撤销操作）
 * - 保护原对象不被修改
 */
public class PrototypeDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 原型模式（Prototype Pattern）==========\n");

        // 1. 浅拷贝演示
        System.out.println("【1. 浅拷贝演示】\n");
        Person original = new Person("张三", 25, new Address("北京", "朝阳区"));
        System.out.println("原始对象: " + original);

        Person shallowCopy = original.shallowClone();
        System.out.println("浅拷贝对象: " + shallowCopy);

        System.out.println("\n修改原始对象的引用类型属性：");
        original.getAddress().setCity("上海");
        System.out.println("原始对象: " + original);
        System.out.println("浅拷贝对象: " + shallowCopy);
        System.out.println("结论: 浅拷贝的引用类型属性会受原对象影响（共享同一对象）");

        // 2. 深拷贝演示
        System.out.println("\n【2. 深拷贝演示】\n");
        Person deepOriginal = new Person("李四", 30, new Address("广州", "天河区"));
        System.out.println("原始对象: " + deepOriginal);

        Person deepCopy = deepOriginal.deepClone();
        System.out.println("深拷贝对象: " + deepCopy);

        System.out.println("\n修改原始对象的引用类型属性：");
        deepOriginal.getAddress().setCity("深圳");
        System.out.println("原始对象: " + deepOriginal);
        System.out.println("深拷贝对象: " + deepCopy);
        System.out.println("结论: 深拷贝的引用类型属性不受原对象影响（独立对象）");

        // 3. 引用对比
        System.out.println("\n【3. 对象引用对比】\n");
        System.out.println("原始对象 == 浅拷贝: " + (original == shallowCopy));
        System.out.println("原始对象.Address == 浅拷贝.Address: " +
                (original.getAddress() == shallowCopy.getAddress()));
        System.out.println("原始对象 == 深拷贝: " + (deepOriginal == deepCopy));
        System.out.println("原始对象.Address == 深拷贝.Address: " +
                (deepOriginal.getAddress() == deepCopy.getAddress()));

        System.out.println("\n【模式分析】：");
        System.out.println("  - 通过克隆创建对象，避免重复初始化");
        System.out.println("  - 浅拷贝：复制引用，引用对象共享");
        System.out.println("  - 深拷贝：复制对象，完全独立");
        System.out.println("  - 浅拷贝性能好，深拷贝更安全");
        System.out.println("  - 实现深拷贝推荐使用序列化方式");
    }
}

/**
 * =====================================================================================
 * 具体原型：Person
 * =====================================================================================
 * 实现 Cloneable 接口，支持克隆
 */
class Person implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int age;
    private Address address;  // 引用类型属性

    public Person(String name, int age, Address address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }

    /**
     * 浅拷贝
     * <p>
     * 实现步骤：
     * 1. 实现 Cloneable 接口
     * 2. 重写 Object.clone() 方法
     * 3. 调用 super.clone()
     */
    public Person shallowClone() {
        try {
            return (Person) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("克隆失败", e);
        }
    }

    /**
     * 深拷贝 - 序列化方式
     * <p>
     * 实现步骤：
     * 1. 实现 Serializable 接口
     * 2. 将对象序列化到字节流
     * 3. 从字节流反序列化出新对象
     * <p>
     * 注意：所有引用类型也需要实现 Serializable
     */
    public Person deepClone() {
        try {
            // 序列化
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);

            // 反序列化
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (Person) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("深拷贝失败", e);
        }
    }

    public Address getAddress() {
        return address;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return String.format("Person{name='%s', age=%d, address=%s}",
                name, age, address);
    }
}

/**
 * =====================================================================================
 * 引用类型：Address
 * =====================================================================================
 * 用于演示浅拷贝和深拷贝的区别
 */
class Address implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private String city;
    private String district;

    public Address(String city, String district) {
        this.city = city;
        this.district = district;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return String.format("Address{city='%s', district='%s'}", city, district);
    }

    @Override
    protected Address clone() throws CloneNotSupportedException {
        return (Address) super.clone();
    }
}
