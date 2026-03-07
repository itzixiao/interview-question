# == 与 equals 的区别

## 概述

| 比较方式 | 作用 | 适用场景 |
|----------|------|----------|
| `==` | 比较内存地址（引用类型）或值（基本类型）| 基本类型比较 |
| `equals()` | 比较对象内容（需重写）| 引用类型内容比较 |

## == 运算符

### 基本数据类型

比较**值**是否相等：

```java
int a = 10;
int b = 10;
a == b;  // true，比较值

double d1 = 3.14;
double d2 = 3.14;
d1 == d2;  // true
```

### 引用数据类型

比较**内存地址**是否相同（是否是同一个对象）：

```java
Person p1 = new Person("张三");
Person p2 = new Person("张三");
p1 == p2;  // false，不同对象

Person p3 = p1;
p1 == p3;  // true，同一对象
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
s1 == s2;  // true

String s3 = new String("hello");  // 堆中新对象
s1 == s3;  // false
s1.equals(s3);  // true
```

### intern() 方法

将字符串放入常量池：

```java
String s = new String("hello").intern();
s == "hello";  // true
```

## 包装类缓存

### Integer 缓存（-128 ~ 127）

```java
Integer a = 100;
Integer b = 100;
a == b;  // true，缓存同一对象

Integer c = 200;
Integer d = 200;
c == d;  // false，超出缓存范围
```

### 缓存范围

| 类型 | 缓存范围 |
|------|----------|
| Byte | -128 ~ 127 |
| Short | -128 ~ 127 |
| Integer | -128 ~ 127 |
| Long | -128 ~ 127 |
| Character | 0 ~ 127 |
| Boolean | true, false |

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
   a == b;  // 正确
   ```
