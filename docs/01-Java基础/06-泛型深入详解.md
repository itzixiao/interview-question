# Java 泛型深入详解

## 一、泛型概述

泛型是 JDK5 引入的特性，允许在定义类、接口、方法时使用类型参数。

### 核心作用

1. **类型安全**：编译期检查类型，避免 ClassCastException
2. **代码复用**：一套代码适用于多种类型
3. **消除强制转换**：编译器自动插入类型转换

---

## 二、类型擦除（Type Erasure）

Java 泛型是通过类型擦除实现的**伪泛型**：

| 阶段 | 泛型信息 |
|------|----------|
| 编译期 | 完整保留，进行类型检查 |
| 运行期 | 完全擦除，只保留原始类型 |

### 擦除规则

```
无界类型参数 <T>              → 擦除为 Object
有界类型参数 <T extends Number> → 擦除为边界类型 Number
多边界 <T extends Number & Comparable> → 擦除为第一个边界
```

### 验证类型擦除

```java
List<String> stringList = new ArrayList<>();
List<Integer> intList = new ArrayList<>();

// 输出：true！运行时是同一个类
System.out.println(stringList.getClass() == intList.getClass());
```

---

## 三、查看运行时实际的泛型信息

虽然类型擦除会擦除大部分泛型信息，但以下地方保留了泛型签名（Signature 属性）：

### 1. 通过父类获取泛型信息

```java
class StringList extends ArrayList<String> {}

Type type = StringList.class.getGenericSuperclass();
// 输出：java.util.ArrayList<java.lang.String>
```

### 2. 通过字段获取泛型信息

```java
private Map<String, Integer> map;

Field field = MyClass.class.getDeclaredField("map");
Type type = field.getGenericType();
// 输出：java.util.Map<java.lang.String, java.lang.Integer>
```

### 3. 局部变量无法获取

```java
List<String> localList = new ArrayList<>();
// 无法通过反射获取局部变量的泛型类型 <String>
```

---

## 四、泛型的工作时机

| 阶段 | 泛型信息 | 操作 |
|------|----------|------|
| 源码阶段 | 完整保留 | 类型检查、错误报告 |
| 编译阶段 | 部分擦除 | 插入类型转换、桥接方法 |
| 字节码阶段 | 仅保留签名 | Signature 属性记录 |
| 运行阶段 | 完全擦除 | 只知道原始类型 |

---

## 五、泛型通配符

## PECS 原则

**Producer Extends, Consumer Super**

| 场景 | 通配符 | 说明 |
|------|--------|------|
| 只读取 | `? extends T` | 生产者 |
| 只写入 | `? super T` | 消费者 |
| 都要 | 精确类型 T | 精确类型 |

#### 为什么"只读是生产者，只写是消费者"？

关键在于**视角的转换**："生产者/消费者"是从**集合/变量的视角**来看的，而不是从"你"的视角！

```
生产者（? extends T）：集合 → 数据 → 你的代码
                      （集合生产数据给你读）

消费者（? super T）：你的代码 → 数据 → 集合
                      （集合消费你写入的数据）
```

#### 上界通配符 `? extends T` = 生产者（只读）

```java
// 集合是"生产者"——它生产数据给你的代码使用
public double sum(List<? extends Number> producer) {
    // 你只能从list中"读取"数据
    Number n = producer.get(0);  // ✅ 可以读
    // producer.add(1);          // ❌ 不能写
    
    // 集合"生产"了数据给你消费
    double total = 0;
    for (Number n : producer) {
        total += n.doubleValue();
    }
    return total;
}
```

**为什么只能读？** 因为无法保证类型安全：

```java
List<Integer> integers = new ArrayList<>();
List<? extends Number> numbers = integers;

// 假设能写：
// numbers.add(3.14); // Double也是Number的子类
// 但numbers实际指向Integer列表！放Double进去就崩了
```

#### 下界通配符 `? super T` = 消费者（只写）

```java
// 集合是"消费者"——它消费你代码产生的数据
public void addNumbers(List<? super Integer> consumer) {
    // 你只能向list中"写入"数据
    consumer.add(1);              // ✅ 可以写
    // Integer i = consumer.get(0); // ❌ 读出来是Object
    
    // 集合"消费"了你生产的数据
}
```

**为什么只能写？** 因为读取类型不确定：

```java
List<Object> objects = new ArrayList<>();
List<? super Integer> integers = objects;

// 读取时：
// Integer i = integers.get(0); // 实际可能是String、Date...
// 因为底层是Object列表，什么都能放
```

#### 形象类比：餐厅

| 角色 | 行为 | 对应泛型 |
|------|------|----------|
| **厨师（生产者）** | 做菜给你吃 → 你只能**读取/消费**他的菜品 | `? extends T` |
| **顾客（消费者）** | 你做菜给他吃 → 你只能**写入/提供**菜品给他 | `? super T` |

- **`? extends T`**：集合像厨师，生产数据给你读
- **`? super T`**：集合像顾客，消费你写入的数据

#### 记忆口诀

```
PECS = Producer Extends, Consumer Super

从集合的视角看：
- 集合给你数据 = 集合是生产者 = 你只能读
- 集合接受数据 = 集合是消费者 = 你只能写
```

---

## 六、泛型与数组

### 为什么不能创建泛型数组？

数组是协变的，泛型是不变的，两者结合会产生类型安全问题。

### 解决方案

```java
// 方案1：使用 List 代替数组
List<List<String>> list = new ArrayList<>();

// 方案2：原始类型数组
@SuppressWarnings("unchecked")
List<String>[] array = (List<String>[]) new List[10];
```

---

## 七、高频面试题

### Q1: 什么是类型擦除？

编译时泛型信息存在，运行时被擦除。Java 选择类型擦除是为了向后兼容。

### Q2: List<String> 和 List<Integer> 运行时有什么区别？

完全没有区别！运行时都擦除为 List。

### Q3: 运行时还能获取泛型信息吗？

可以通过 Signature 属性获取父类、字段、方法的泛型信息。

### Q4: 为什么不能创建泛型数组？

数组协变 + 泛型不变 = 类型安全问题。

### Q5: 什么是 PECS 原则？为什么只读是生产者，只写是消费者？

**Producer Extends, Consumer Super**。

关键在于**视角的转换**：
- "生产者/消费者"是从**集合的视角**来看的
- 生产者（`? extends T`）：集合给你数据 → 你只能读
- 消费者（`? super T`）：集合接受数据 → 你只能写

类比：厨师生产菜品给你吃（只读），顾客消费你做的菜（只写）。

### Q6: 什么是桥接方法？

编译器生成的合成方法，用于保证多态的正确性。

### Q7: 泛型和 C++ 模板有什么区别？

Java 泛型是伪泛型（类型擦除），C++ 模板是真泛型。

### Q8: 泛型的工作时机？

编译期检查 + 擦除，运行期只有原始类型。

---

## 示例代码

完整示例代码请参考：[GenericDeepDiveDemo.java](../interview-service/src/main/java/cn/itzixiao/interview/java/GenericDeepDiveDemo.java)