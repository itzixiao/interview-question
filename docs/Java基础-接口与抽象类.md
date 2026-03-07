# 接口与抽象类详解

## 一、接口（Interface）

### 定义
接口是一种抽象类型，是抽象方法的集合。接口以 `interface` 关键字声明。

### 特点
- 所有方法默认是 `public abstract`（JDK8前）
- 所有变量默认是 `public static final`
- 不能实例化，没有构造方法
- JDK8 支持 `default` 和 `static` 方法
- JDK9 支持 `private` 方法

### 示例代码
```java
interface AnimalInterface {
    // 常量：默认 public static final
    String TYPE = "动物";

    // 抽象方法：默认 public abstract
    void eat();
    void sleep();

    // JDK8 新特性：默认方法
    default void defaultMethod() {
        System.out.println("默认方法");
    }

    // JDK8 新特性：静态方法
    static void staticMethod() {
        System.out.println("静态方法");
    }
}

// 实现类
class Dog implements AnimalInterface {
    @Override
    public void eat() {
        System.out.println("狗吃骨头");
    }

    @Override
    public void sleep() {
        System.out.println("狗睡觉");
    }
}
```

---

## 二、抽象类（Abstract Class）

### 定义
抽象类是不能被实例化的类，用 `abstract` 关键字修饰，可以包含抽象方法和具体方法。

### 特点
- 用 `abstract` 修饰，不能实例化
- 可以有抽象方法和具体方法
- 可以有构造方法（供子类调用）
- 可以有各种访问修饰符的成员变量
- 子类必须实现所有抽象方法（除非子类也是抽象类）

### 示例代码
```java
abstract class Shape {
    // 成员变量：可以是各种访问修饰符
    protected String color;
    private String name;

    // 构造方法：供子类调用
    public Shape(String name) {
        this.name = name;
        this.color = "白色";
    }

    // 抽象方法：子类必须实现
    public abstract double calculateArea();

    // 具体方法：子类可以直接使用
    public void display() {
        System.out.println("名称: " + name + ", 面积: " + calculateArea());
    }
}

// 子类
class Circle extends Shape {
    private double radius;

    public Circle(double radius) {
        super("圆形");
        this.radius = radius;
    }

    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}
```

---

## 三、核心区别

| 特性 | 接口 | 抽象类 |
|------|------|--------|
| 关键字 | `interface` | `abstract class` |
| 多继承 | 一个类可实现**多个**接口 | 一个类只能继承**一个**抽象类 |
| 成员变量 | 只能是 `public static final` | 可以有各种类型的成员变量 |
| 方法 | JDK8前全是抽象方法 | 可以有抽象和具体方法 |
| 构造方法 | **不能有** | **可以有** |
| 设计理念 | 定义行为契约 | 代码复用 + 模板设计 |

---

## 四、接口多实现

Java 不支持多继承，但支持接口多实现，这解决了单继承的限制：

```java
interface Phone {
    void call();
    default void charge() { System.out.println("充电"); }
}

interface Camera {
    void takePhoto();
    default void charge() { System.out.println("相机充电"); }
}

interface MusicPlayer {
    void playMusic();
}

// 一个类实现多个接口
class SmartPhone implements Phone, Camera, MusicPlayer {
    @Override
    public void call() { System.out.println("打电话"); }

    @Override
    public void takePhoto() { System.out.println("拍照"); }

    @Override
    public void playMusic() { System.out.println("播放音乐"); }

    // 两个接口有相同的默认方法，必须重写
    @Override
    public void charge() {
        System.out.println("快充");
        // 或指定调用哪个接口的默认方法
        // Phone.super.charge();
    }
}
```

---

## 五、JDK8/9 接口新特性

### JDK 8 新特性

**1. default 方法（默认方法）**
```java
default void defaultMethod() {
    System.out.println("默认实现");
}
```
- 提供默认实现，实现类可以选择不重写
- 用于向后兼容，为现有接口添加新方法

**2. static 方法（静态方法）**
```java
static void staticMethod() {
    System.out.println("静态方法");
}
```
- 通过接口名调用：`InterfaceName.staticMethod()`
- 不能被实现类继承

### JDK 9 新特性

**private 方法（私有方法）**
```java
private void privateMethod() {
    System.out.println("私有方法");
}
```
- 用于 default 方法之间的代码复用
- JDK 8 不支持此特性

---

## 六、高频面试题

### Q1: 接口和抽象类的区别？

| 维度 | 接口 | 抽象类 |
|------|------|--------|
| 继承关系 | 类可实现多个接口 | 类只能继承一个抽象类 |
| 成员变量 | 只能是 public static final | 无限制 |
| 方法实现 | JDK8前只能有抽象方法 | 可以有具体方法 |
| 构造方法 | 没有 | 可以有 |
| 设计理念 | 行为契约 | 代码复用 |

### Q2: 什么时候用接口？什么时候用抽象类？

**用接口：**
- 需要定义行为规范，不关心具体实现
- 需要多继承场景
- 功能之间没有关联性

**用抽象类：**
- 需要代码复用，子类有公共逻辑
- 需要定义成员变量
- 子类之间有层次关系（is-a 关系）

### Q3: JDK8 接口新增的 default 方法有什么用？

1. **向后兼容**：为现有接口添加新方法而不破坏实现类
2. **代码复用**：提供默认实现，减少重复代码
3. **示例**：Collection 接口的 `forEach()`、`stream()` 等方法

### Q4: 一个类实现多个接口，接口有相同的默认方法怎么办？

实现类必须重写该方法，解决冲突。可以使用 `InterfaceName.super.method()` 指定调用哪个接口的方法。

### Q5: 抽象类可以没有抽象方法吗？

可以。抽象类可以没有抽象方法，但有抽象方法的类必须是抽象类。用途是防止直接实例化，强制通过子类使用。

### Q6: 接口可以继承接口吗？抽象类可以实现接口吗？

- 接口可以继承接口（支持多继承）
- 抽象类可以实现接口
- 抽象类可以继承具体类
- 抽象类可以继承抽象类

### Q7: 普通类和抽象类有什么区别？

| 特性 | 普通类 | 抽象类 |
|------|--------|--------|
| 实例化 | 可以 | 不可以 |
| 抽象方法 | 不能有 | 可以有 |
| 用途 | 直接使用 | 作为基类 |

### Q8: Java 为什么要设计单继承？

1. **避免钻石问题**：多个父类有相同方法时的二义性
2. **简化设计**：降低复杂度
3. **接口多实现弥补了单继承的不足**

---

## 示例代码

完整示例代码请参考：[InterfaceAndAbstractDemo.java](../interview-service/src/main/java/cn/itzixiao/interview/java/InterfaceAndAbstractDemo.java)
