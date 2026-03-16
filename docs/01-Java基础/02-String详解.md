# String、StringBuffer、StringBuilder 对比

## 概述

| 特性   | String  | StringBuffer     | StringBuilder |
|------|---------|------------------|---------------|
| 可变性  | 不可变     | 可变               | 可变            |
| 线程安全 | 安全（不可变） | 安全（synchronized） | 不安全           |
| 性能   | 低       | 中                | 高             |
| 适用场景 | 常量、少量操作 | 多线程大量操作          | 单线程大量操作       |

## String（不可变）

### 不可变性

```java
String s = "Hello";
s =s +" World";  // 创建新对象，原字符串不变
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

s1 ==s2;  // true
s1 ==s3;  // false
```

## StringBuffer（线程安全）

### 特点

- 可变字符序列
- 线程安全（方法加 synchronized）
- 多线程环境下使用

```java
StringBuffer sb = new StringBuffer();
sb.

append("Hello");
sb.

append(" World");  // 直接修改内部数组
```

## StringBuilder（非线程安全）

### 特点

- 可变字符序列
- 非线程安全
- 单线程环境下性能最高

```java
StringBuilder sb = new StringBuilder();
sb.

append("Hello");
sb.

append(" World");

String result = sb.toString();
```

## 性能对比

### 拼接 100000 次测试

| 方式            | 耗时      | 说明         |
|---------------|---------|------------|
| String        | ~5000ms | 创建大量临时对象   |
| StringBuffer  | ~5ms    | 线程安全，有同步开销 |
| StringBuilder | ~3ms    | 最快，无线程安全开销 |

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

| 方法         | 说明        |
|------------|-----------|
| append()   | 追加字符串     |
| insert()   | 插入字符串     |
| delete()   | 删除字符      |
| reverse()  | 反转        |
| toString() | 转为 String |
| length()   | 长度        |
| capacity() | 容量        |

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

---

## 💡 高频面试题

**问题 1：String、StringBuffer、StringBuilder 的区别？**

答案：
三者的主要区别在于可变性、线程安全性和性能：

| 特性   | String       | StringBuffer       | StringBuilder |
|------|--------------|--------------------|---------------|
| 可变性  | 不可变（final 类） | 可变                 | 可变            |
| 线程安全 | ✅ 安全（不可变）    | ✅ 安全（synchronized） | ❌ 不安全         |
| 性能   | 低（频繁创建对象）    | 中（有同步开销）           | 高（无锁）         |
| 使用场景 | 常量、少量操作      | 多线程大量操作            | 单线程大量操作       |

**源码对比：**

```java
// String - 不可变
public final class String {
    private final char value[];  // final 数组
}

// StringBuffer - 线程安全
public final class StringBuffer {
    char value[];  // 非 final，可修改

    public synchronized StringBuffer append(String str) {  // synchronized
        super.append(str);
        return this;
    }
}

// StringBuilder - 非线程安全
public final class StringBuilder {
    char value[];

    public StringBuilder append(String str) {  // 无锁
        super.append(str);
        return this;
    }
}
```

**性能测试（拼接 10 万次）：**

```java
// String - 约 5000ms
String s = "";
for(
int i = 0;
i< 100000;i++){
s +=i;  // 每次创建新对象
}

// StringBuffer - 约 5ms
StringBuffer sb = new StringBuffer();
for(
int i = 0;
i< 100000;i++){
        sb.

append(i);
}

// StringBuilder - 约 3ms
StringBuilder sb2 = new StringBuilder();
for(
int i = 0;
i< 100000;i++){
        sb2.

append(i);
}
```

**使用建议：**

- ✅ 字符串常量 → `String`
- ✅ 单线程拼接 → `StringBuilder`（最快）
- ✅ 多线程拼接 → `StringBuffer`（线程安全）

**问题 2：为什么 String 是不可变的？**

答案：
**String 的不可变性**是指 String 对象一旦创建，其内部的字符序列就不能被修改。

**实现原理：**

```java
public final class String {
    private final char value[];  // final 修饰的字符数组

    // 构造方法中复制数组内容
    public String(char[] value) {
        this.value = Arrays.copyOf(value, value.length);  // 防御性拷贝
    }

    // 没有提供任何修改 value 的方法
    public char charAt(int index) {
        return value[index];  // 只能读取
    }
}
```

**不可变的原因：**

1. **线程安全**
    - 不可变对象天然线程安全
    - 可以在多线程间共享，无需同步

2. **支持字符串常量池**
   ```java
   String s1 = "Java";  // 常量池
   String s2 = "Java";  // 同一对象
   // 如果可变，s2 修改会影响 s1
   ```

3. **作为 HashMap/HashSet 的 key**
   ```java
   Map<String, Integer> map = new HashMap<>();
   map.put("key", 123);  // String 作为 key
   // hashCode 不会改变，保证查找正确
   ```

4. **安全性**
   ```java
   // 数据库连接 URL、文件路径等作为参数
   void connectToDatabase(String url) {
       // 如果 url 可变，会有安全风险
   }
   ```

**String 真的完全不可变吗？**

```java
// 通过反射可以修改（但不推荐）
Field field = String.class.getDeclaredField("value");
field.

setAccessible(true);

char[] value = (char[]) field.get(s);
value[0]='H';  // 可以修改，但会破坏常量池
```

**问题 3：String s = new String("xyz") 创建了几个对象？**

答案：
**创建了 1 个或 2 个对象**，取决于常量池中是否已有 "xyz"。

**情况分析：**

```java
// 情况 1：常量池中没有 "xyz"
String s = new String("xyz");
// 1. 在常量池创建 "xyz"
// 2. 在堆中创建新对象
// 共 2 个对象

// 情况 2：常量池中已有 "xyz"
String s1 = "xyz";  // 先执行这行
String s2 = new String("xyz");
// 1. 只在堆中创建新对象
// 共 1 个对象
```

**内存分析：**

```
常量池：     "xyz" ←─────────────┐
                        │
堆内存：   [String 对象] ─┘
           value → "xyz"
```

**验证代码：**

```java
String s1 = "xyz";           // 常量池
String s2 = new String("xyz"); // 堆中新对象

System.out.

println(s1 ==s2);      // false，不同对象
System.out.

println(s1.equals(s2)); // true，内容相同
```

**intern() 方法的作用：**

```java
String s = new String("xyz").intern();
s =="xyz";  // true，指向常量池
```

**最佳实践：**

- ✅ 推荐：`String s = "xyz";`（直接字面量）
- ❌ 不推荐：`String s = new String("xyz");`（浪费内存）

**问题 4：字符串拼接的几种方式及性能对比？**

答案：

**方式一：+ 运算符（编译器优化）**

```java
String s = "Hello" + "World";  // 编译期优化为 "HelloWorld"
```

**方式二：concat() 方法**

```java
String s1 = "Hello";
String s2 = s1.concat(" World");
```

**方式三：StringBuilder.append()**

```java
StringBuilder sb = new StringBuilder();
sb.

append("Hello").

append(" World");

String s = sb.toString();
```

**方式四：String.join()（Java 8+）**

```java
String s = String.join(",", "A", "B", "C");
// 结果："A,B,C"
```

**方式五：String.format()**

```java
String s = String.format("%s %s", "Hello", "World");
```

**性能对比（循环拼接 10 万次）：**

```java
// ❌ String + - 约 5000ms
String s = "";
for(
int i = 0;
i< 100000;i++){
s +=i;
}

// ⚠️ concat() - 约 4000ms
String s = "";
for(
int i = 0;
i< 100000;i++){
s =s.

concat(String.valueOf(i));
        }

// ✅ StringBuilder - 约 3ms
StringBuilder sb = new StringBuilder();
for(
int i = 0;
i< 100000;i++){
        sb.

append(i);
}

// ✅ StringJoiner - 约 4ms
StringJoiner sj = new StringJoiner(",");
for(
int i = 0;
i< 100000;i++){
        sj.

add(String.valueOf(i));
        }
```

**编译器优化示例：**

```java
// 源代码
String s = "a" + "b" + "c";

// 编译后
String s = new StringBuilder().append("a").append("b").append("c").toString();
```

**循环中的陷阱：**

```java
// ❌ 错误 - 每次都创建 StringBuilder
for(int i = 0;
i< 1000;i++){
String s = "" + i;  // 相当于 new StringBuilder().append(i).toString()
}

// ✅ 正确 - 复用 StringBuilder
StringBuilder sb = new StringBuilder();
for(
int i = 0;
i< 1000;i++){
        sb.

append(i);
}
```

**问题 5：String 的常用方法有哪些？**

答案：

**查询类：**

```java
String s = "Hello World";
s.

length();           // 11
s.

charAt(0);          // 'H'
s.

indexOf("World");   // 6
s.

lastIndexOf("o");   // 7
s.

contains("World");  // true
s.

startsWith("Hello");// true
s.

endsWith("d");      // true
```

**转换类：**

```java
s.toLowerCase();      // "hello world"
s.

toUpperCase();      // "HELLO WORLD"
s.

trim();             // "Hello World"（去首尾空格）
s.

getBytes();         // byte[]
s.

toCharArray();      // char[]
```

**分割和替换：**

```java
s.split(" ");         // ["Hello", "World"]
s.

replace("l","L");  // "HeLLo WorLd"
s.

replaceAll("\\w+","*"); // "* *"
s.

substring(6);       // "World"
s.

substring(0,5);    // "Hello"
```

**比较类：**

```java
s.equals("Hello World");     // true
s.

equalsIgnoreCase("hello"); // false
s.

compareTo("abc");          // 按字典序比较
```

**格式化：**

```java
String.format("%d %s",123,"abc"); // "123 abc"
"%d %s".

formatted(123,"abc");      // Java 15+
```

**问题 6：== 和 equals() 在 String 中的区别？**

答案：

**== 比较内存地址，equals() 比较内容。**

**示例：**

```java
String s1 = "hello";
String s2 = "hello";
String s3 = new String("hello");

// == 比较
s1 ==s2;  // true，同一常量池对象
s1 ==s3;  // false，不同对象

// equals() 比较
s1.

equals(s2);  // true，内容相同
s1.

equals(s3);  // true，内容相同
```

**内存图：**

```
常量池：    "hello" ←── s1, s2
                    │
堆内存：  [String] ←─ s3
         value → "hello"
```

**最佳实践：**

```java
// ✅ 正确 - 比较内容用 equals
if(str.equals("expected")){}

// ❌ 错误 - == 比较的是地址
        if(str =="expected"){}  // 可能失败

// ✅ 防止 NPE
        if("expected".

equals(str)){}  // 常量在前
```

**问题 7：如何判断字符串为空？**

答案：

**三种空的状态：**

```java
String s1 = null;        // null，未初始化
String s2 = "";          // 空字符串
String s3 = "   ";       // 空白字符串
```

**判断方法：**

```java
// 1. 判断是否为 null
if(s ==null){}

// 2. 判断是否为空字符串
        if(s.

equals("")){}
        if(s.

isEmpty()){}  // Java 6+

// 3. 判断是否为 null 或空
        if(s ==null||s.

isEmpty()){}
        if(StringUtils.

isEmpty(s)){}  // Apache Commons

// 4. 判断是否为 null、空或空白
        if(s ==null||s.

trim().

isEmpty()){}
        if(StringUtils.

isBlank(s)){}  // Apache Commons
```

**推荐工具类：**

```java
// Spring StringUtils
StringUtils.hasText("abc");  // true，非 null 且有内容
StringUtils.

hasLength("");   // false，长度为 0

// Apache Commons
StringUtils.

isNotEmpty("abc"); // true
StringUtils.

isBlank("   ");     // true，空白也算空
StringUtils.

isNotBlank("abc"); // true
```

**最佳实践：**

```java
// ✅ 推荐 - 使用工具类
if(StringUtils.isNotBlank(str)){
        // 处理业务
        }

// ✅ 手动判断
        if(str !=null&&!str.

trim().

isEmpty()){
        // 处理业务
        }
```

**问题 8：String 为什么被设计为 final？**

答案：

**final 的含义：**

- 类不能被继承
- 方法不能被重写
- 保证不可变性

**设计为 final 的原因：**

1. **保证安全性**
   ```java
   // 如果可以继承，子类可以篡改行为
   class EvilString extends String {
       // 可以重写方法，破坏安全性
   }
   ```

2. **支持字符串常量池**
   ```java
   String s1 = "Java";
   String s2 = "Java";
   // 如果可变且可继承，s2 修改会影响 s1
   ```

3. **线程安全**
    - final 类 + 不可变 = 天然线程安全
    - 可以在多线程间安全共享

4. **作为 HashMap 的 key**
   ```java
   Map<String, Integer> map = new HashMap<>();
   map.put("key", value);
   // hashCode 不会改变，保证查找正确
   ```

5. **性能优化**
    - JVM 可以对 final 类进行优化
    - 如内联、缓存 hashCode 等

**类似的 final 类：**

```java
public final class String {
}

public final class Integer {
}

public final class Math {
}
// 都是包装类或工具类，保证不可变性
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
