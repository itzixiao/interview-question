# String、StringBuffer、StringBuilder 对比

## 概述

| 特性 | String | StringBuffer | StringBuilder |
|------|--------|--------------|---------------|
| 可变性 | 不可变 | 可变 | 可变 |
| 线程安全 | 安全（不可变）| 安全（synchronized）| 不安全 |
| 性能 | 低 | 中 | 高 |
| 适用场景 | 常量、少量操作 | 多线程大量操作 | 单线程大量操作 |

## String（不可变）

### 不可变性

```java
String s = "Hello";
s = s + " World";  // 创建新对象，原字符串不变
```

**优点**：
- 线程安全
- 可作为 Map 的 key
- 字符串常量池复用

**缺点**：
- 频繁修改产生大量临时对象
- 性能低

### 字符串常量池

```java
String s1 = "Java";           // 常量池
String s2 = "Java";           // 同一对象
String s3 = new String("Java");  // 堆中新对象

s1 == s2;  // true
s1 == s3;  // false
```

## StringBuffer（线程安全）

### 特点

- 可变字符序列
- 线程安全（方法加 synchronized）
- 多线程环境下使用

```java
StringBuffer sb = new StringBuffer();
sb.append("Hello");
sb.append(" World");  // 直接修改内部数组
```

## StringBuilder（非线程安全）

### 特点

- 可变字符序列
- 非线程安全
- 单线程环境下性能最高

```java
StringBuilder sb = new StringBuilder();
sb.append("Hello");
sb.append(" World");
String result = sb.toString();
```

## 性能对比

### 拼接 100000 次测试

| 方式 | 耗时 | 说明 |
|------|------|------|
| String | ~5000ms | 创建大量临时对象 |
| StringBuffer | ~5ms | 线程安全，有同步开销 |
| StringBuilder | ~3ms | 最快，无线程安全开销 |

### 源码分析

**StringBuilder.append()**：
```java
public StringBuilder append(String str) {
    super.append(str);  // 直接操作数组
    return this;        // 支持链式调用
}
```

**StringBuffer.append()**：
```java
public synchronized StringBuffer append(String str) {
    super.append(str);  // 加锁
    return this;
}
```

## 常用方法

| 方法 | 说明 |
|------|------|
| append() | 追加字符串 |
| insert() | 插入字符串 |
| delete() | 删除字符 |
| reverse() | 反转 |
| toString() | 转为 String |
| length() | 长度 |
| capacity() | 容量 |

## 使用建议

1. **字符串常量**：使用 String
   ```java
   String config = "app.name";
   ```

2. **单线程字符串拼接**：使用 StringBuilder
   ```java
   StringBuilder sb = new StringBuilder();
   for (String s : list) {
       sb.append(s);
   }
   ```

3. **多线程字符串拼接**：使用 StringBuffer
   ```java
   StringBuffer sb = new StringBuffer();
   // 多线程操作 sb
   ```

4. **避免在循环中使用 String 拼接**
   ```java
   // 错误
   String result = "";
   for (int i = 0; i < 1000; i++) {
       result += i;  // 每次创建新对象
   }
   
   // 正确
   StringBuilder sb = new StringBuilder();
   for (int i = 0; i < 1000; i++) {
       sb.append(i);
   }
   ```

## 线程安全测试

```java
// StringBuilder（非线程安全）
StringBuilder sb = new StringBuilder();
// 多线程 append，结果可能错误

// StringBuffer（线程安全）
StringBuffer sbf = new StringBuffer();
// 多线程 append，结果正确
```
