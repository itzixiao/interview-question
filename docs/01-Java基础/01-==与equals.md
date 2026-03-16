# == 与 equals 的区别

## 概述

| 比较方式       | 作用                   | 适用场景     |
|------------|----------------------|----------|
| `==`       | 比较内存地址（引用类型）或值（基本类型） | 基本类型比较   |
| `equals()` | 比较对象内容（需重写）          | 引用类型内容比较 |

## == 运算符

### 基本数据类型

比较**值**是否相等：

```java
int a = 10;
int b = 10;
a ==b;  // true，比较值

double d1 = 3.14;
double d2 = 3.14;
d1 ==d2;  // true
```

### 引用数据类型

比较**内存地址**是否相同（是否是同一个对象）：

```java
Person p1 = new Person("张三");
Person p2 = new Person("张三");
p1 ==p2;  // false，不同对象

Person p3 = p1;
p1 ==p3;  // true，同一对象
```

## equals() 方法

### Object 默认实现

```java
public boolean equals(Object obj) {
    return (this == obj);  // 使用 == 比较
}
```

默认与 `==` 相同，比较内存地址。

### 重写 equals

通常需要重写以比较对象内容：

```java

@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Person person = (Person) o;
    return age == person.age &&
            Objects.equals(name, person.name);
}
```

## String 的特殊性

### 字符串常量池

```java
String s1 = "hello";  // 常量池
String s2 = "hello";  // 同一常量池对象
s1 ==s2;  // true

String s3 = new String("hello");  // 堆中新对象
s1 ==s3;  // false
s1.

equals(s3);  // true
```

### intern() 方法

将字符串放入常量池：

```java
String s = new String("hello").intern();
s =="hello";  // true
```

## 包装类缓存

### Integer 缓存（-128 ~ 127）

```java
Integer a = 100;
Integer b = 100;
a ==b;  // true，缓存同一对象

Integer c = 200;
Integer d = 200;
c ==d;  // false，超出缓存范围
```

### 缓存范围

| 类型        | 缓存范围        |
|-----------|-------------|
| Byte      | -128 ~ 127  |
| Short     | -128 ~ 127  |
| Integer   | -128 ~ 127  |
| Long      | -128 ~ 127  |
| Character | 0 ~ 127     |
| Boolean   | true, false |

## 最佳实践

1. **比较内容使用 equals()**
   ```java
   // 正确
   str1.equals(str2);
   
   // 错误（可能 NPE）
   str1 == str2;
   ```

2. **防止 NPE 的写法**
   ```java
   // 常量放前面
   "expected".equals(variable);
   
   // 或使用 Objects.equals()
   Objects.equals(a, b);
   ```

3. **重写 equals 必须重写 hashCode()**
    - 保证相等对象有相同 hashCode
    - 用于 HashMap、HashSet 等

4. **基本类型直接用 ==**
   ```java
   int a = 10;
   int b = 10;
   a == b; // 正确
   ```

---

## 💡 高频面试题

**问题 1：== 和 equals 的区别？**

答案：
`==` 和 `equals()` 都用于比较是否相等，但有以下本质区别：

| 特性     | ==     | equals()       |
|--------|--------|----------------|
| 本质     | 运算符    | Object 类的方法    |
| 基本类型   | 比较值    | ❌ 不能使用         |
| 引用类型   | 比较内存地址 | 默认比较地址，可重写比较内容 |
| 是否需要重写 | 否      | 是（才能比较内容）      |

**使用示例：**

```java
// 基本类型 - 用 ==
int a = 10, b = 10;
a ==b; // true

// 引用类型 - == 比较地址
String s1 = new String("hello");
String s2 = new String("hello");
s1 ==s2; // false，不同对象

// 引用类型 - equals 比较内容
s1.

equals(s2); // true，内容相同
```

**String 的特殊情况：**

```java
String s1 = "hello";  // 常量池
String s2 = "hello";  // 同一常量池
s1 ==s2; // true，同一对象

String s3 = new String("hello");
s1 ==s3; // false，不同对象
s1.

equals(s3); // true，内容相同
```

**问题 2：为什么重写 equals 必须重写 hashCode？**

答案：
**原因：** Java 规范要求相等的对象必须有相同的 hashCode，这是为了保证在 HashMap、HashSet 等基于哈希的集合中正常工作。

**hashCode 约定：**

1. 同一对象多次调用 hashCode 应该返回相同值
2. 如果 `obj1.equals(obj2)` 为 true，则它们的 hashCode 必须相等
3. 不等的对象 hashCode 可以不同（但不同可以减少哈希冲突）

**违反的后果：**

```java
class Person {
    String name;

    @Override
    public boolean equals(Object o) {
        if (o instanceof Person) {
            return name.equals(((Person) o).name);
        }
        return false;
    }
    // ❌ 没有重写 hashCode
}

// 问题场景
Person p1 = new Person("张三");
Person p2 = new Person("张三");

System.out.

println(p1.equals(p2)); // true，内容相等
        System.out.

println(p1.hashCode()); // 123456（假设）
        System.out.

println(p2.hashCode()); // 789012（默认地址生成，不同）

// 放入 HashSet
Set<Person> set = new HashSet<>();
set.

add(p1);
set.

add(p2); // ❌ 会被当作不同元素添加

System.out.

println(set.size()); // 2，而不是 1！
```

**正确的写法：**

```java

@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Person person = (Person) o;
    return Objects.equals(name, person.name);
}

@Override
public int hashCode() {
    return Objects.hash(name); // 基于内容生成
}
```

**问题 3：Integer a = 100 和 Integer b = 100，a == b 的结果？**

答案：
**结果：true**

**原因：** Integer 使用了**整数缓存池**技术，对 -128 ~ 127 之间的整数进行缓存。

**源码分析：**

```java
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i >= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}
```

**示例：**

```java
// 缓存范围内（-128 ~ 127）
Integer a = 100;  // 自动装箱 → valueOf(100)
Integer b = 100;
a ==b; // true，同一缓存对象

// 超出缓存范围
Integer c = 200;  // new Integer(200)
Integer d = 200;
c ==d; // false，不同对象

// 注意：直接赋值会触发缓存
Integer e = 127;   // 缓存
Integer f = 128;   // 新对象
```

**其他包装类的缓存：**
| 类型 | 缓存范围 |
|------|----------|
| Byte | -128 ~ 127 |
| Short | -128 ~ 127 |
| Integer | -128 ~ 127 |
| Long | -128 ~ 127 |
| Character | 0 ~ 127 |
| Boolean | true, false |

**问题 4：String s1 = "hello" 和 String s2 = new String("hello") 的区别？**

答案：

| 方面   | "hello"  | new String("hello") |
|------|----------|---------------------|
| 创建位置 | 字符串常量池   | 堆内存                 |
| 对象数量 | 1 个（常量池） | 2 个（堆 + 常量池）        |
| 性能   | 高        | 低                   |
| 推荐度  | ✅ 推荐     | ❌ 不推荐               |

**内存分析：**

```java
String s1 = "hello";
// 1. 检查常量池是否有 "hello"
// 2. 有则直接引用，无则创建

String s2 = new String("hello");
// 1. 在堆中创建新对象
// 2. 同时也会在常量池创建 "hello"（如果不存在）
```

**对比：**

```java
String s1 = "hello";
String s2 = "hello";
s1 ==s2; // true，同一对象

String s3 = new String("hello");
String s4 = new String("hello");
s3 ==s4; // false，不同对象
s3.

equals(s4); // true，内容相同
```

**intern() 方法：**

```java
String s = new String("hello").intern();
s =="hello"; // true，指向常量池
```

**最佳实践：**

- ✅ 优先使用字面量：`String s = "hello";`
- ❌ 避免：`String s = new String("hello");`
- 比较内容用：`s1.equals(s2)`

**问题 5：如何正确比较两个对象是否相等？**

答案：

**步骤 1：定义 equals 规则**

```java

@Data
class Person {
    private String name;
    private int age;

    @Override
    public boolean equals(Object o) {
        // 1. 检查是否是同一对象
        if (this == o) return true;

        // 2. 检查 null 和类型
        if (o == null || getClass() != o.getClass()) return false;

        // 3. 类型转换并比较属性
        Person person = (Person) o;
        return age == person.age &&
                Objects.equals(name, person.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
}
```

**步骤 2：使用 equals 比较**

```java
Person p1 = new Person("张三", 18);
Person p2 = new Person("张三", 18);
p1.

equals(p2); // true
```

**防止 NPE 的技巧：**

```java
// ❌ 可能 NPE
if(str.equals("expected")){}

// ✅ 常量在前
        if("expected".

equals(str)){}

// ✅ 使用工具类
        if(Objects.

equals(a, b)){}
```

**使用 Lombok（简化）：**

```java

@Data
@EqualsAndHashCode
class Person {
    private String name;
    private int age;
}
```

**问题 6：Object 类还有哪些重要方法？**

答案：

| 方法                    | 作用          | 说明              |
|-----------------------|-------------|-----------------|
| equals()              | 判断对象是否相等    | 需重写才能比较内容       |
| hashCode()            | 返回哈希码       | 配合 equals 使用    |
| toString()            | 返回字符串表示     | 默认返回类名@哈希码      |
| clone()               | 对象克隆        | 需实现 Cloneable   |
| finalize()            | GC 前回调（已废弃） | Java 9 已废弃      |
| getClass()            | 获取运行时类      | final 方法        |
| notify/notifyAll/wait | 线程同步        | 配合 synchronized |

**常用方法示例：**

```java
// toString - 默认实现
public String toString() {
    return getClass().getName() + "@" + Integer.toHexString(hashCode());
}

// 重写后
@Override
public String toString() {
    return "Person{name='" + name + "', age=" + age + "}";
}

// clone - 浅拷贝
@Override
protected Object clone() throws CloneNotSupportedException {
    return super.clone();
}
```

**问题 7：final、finally、finalize 的区别？**

答案：

| 关键字        | 作用          | 使用场景              |
|------------|-------------|-------------------|
| final      | 修饰符，表示"最终的" | 类、方法、变量           |
| finally    | 异常处理的一部分    | try-catch-finally |
| finalize() | Object 的方法  | GC 前清理资源          |

**final 用法：**

```java
// final 类 - 不能被继承
final class FinalClass {
}

// final 方法 - 不能被重写
class Parent {
    public final void method() {
    }
}

// final 变量 - 常量
final int MAX_VALUE = 100;
```

**finally 用法：**

```java
try{
        // 可能异常的代码
        }catch(Exception e){
        // 异常处理
        }finally{

// 总是执行，用于清理资源
closeResource();
}
```

**finalize 用法：**

```java
// 已废弃，不推荐使用
@Override
protected void finalize() throws Throwable {
    // GC 前的清理逻辑
    super.finalize();
}
```

**问题 8：深拷贝和浅拷贝的区别？（关联题）**

答案：

| 特性   | 浅拷贝        | 深拷贝            |
|------|------------|----------------|
| 复制程度 | 只复制对象本身    | 复制对象 + 引用的所有对象 |
| 引用处理 | 复制引用（共享对象） | 递归复制引用对象       |
| 实现方式 | clone()    | 序列化、手动复制       |
| 内存占用 | 少          | 多              |

**浅拷贝示例：**

```java
class Address {
    String city;
}

class Person implements Cloneable {
    String name;
    Address address; // 引用类型

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone(); // 浅拷贝
    }
}

// 问题
Person p1 = new Person();
p1.address =new

Address("北京");

Person p2 = (Person) p1.clone();

p2.address.city ="上海"; // 会影响 p1.address.city
```

**深拷贝方案：**

```java
// 方案 1：手动复制
@Override
protected Object clone() throws CloneNotSupportedException {
    Person p = (Person) super.clone();
    p.address = new Address(this.address.city); // 复制引用对象
    return p;
}

// 方案 2：序列化
public Person deepCopy() throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(this);

    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bis);
    return (Person) ois.readObject();
}
```
