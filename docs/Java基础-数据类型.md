# Java 基础数据类型

## 基本数据类型（8种）

| 类型 | 位数 | 范围 | 默认值 | 包装类 |
|------|------|------|--------|--------|
| byte | 8 | -128 ~ 127 | 0 | Byte |
| short | 16 | -32768 ~ 32767 | 0 | Short |
| int | 32 | -2³¹ ~ 2³¹-1 | 0 | Integer |
| long | 64 | -2⁶³ ~ 2⁶³-1 | 0L | Long |
| float | 32 | IEEE 754 | 0.0f | Float |
| double | 64 | IEEE 754 | 0.0d | Double |
| char | 16 | 0 ~ 65535 | '\u0000' | Character |
| boolean | 1 | true/false | false | Boolean |

## 类型转换

### 自动类型转换（小 → 大）

```java
byte → short → int → long → float → double
char → int
```

```java
byte b = 10;
int i = b;  // 自动转换
```

### 强制类型转换（大 → 小）

```java
double d = 3.14;
int i = (int) d;  // 3，截断小数

int big = 130;
byte small = (byte) big;  // -126，溢出
```

### 表达式类型提升

```java
byte a = 10;
byte b = 20;
// byte sum = a + b;  // 编译错误！a + b 结果是 int
int sum = a + b;       // 正确
```

## 包装类

### 对应关系

| 基本类型 | 包装类 |
|----------|--------|
| byte | Byte |
| short | Short |
| int | Integer |
| long | Long |
| float | Float |
| double | Double |
| char | Character |
| boolean | Boolean |

### 自动装箱与拆箱

```java
// 自动装箱
Integer i = 100;  // Integer.valueOf(100)

// 自动拆箱
int n = i;        // i.intValue()
```

### 缓存机制

```java
Integer a = 100;
Integer b = 100;
a == b;  // true（-128 ~ 127 缓存）

Integer c = 200;
Integer d = 200;
c == d;  // false（超出缓存范围）
```

## 引用数据类型

### 类（Class）

```java
String str = new String("Hello");
Object obj = new Object();
```

### 接口（Interface）

```java
List<String> list = new ArrayList<>();
Runnable runnable = () -> {};
```

### 数组（Array）

```java
int[] arr = new int[10];
String[] strs = {"a", "b", "c"};
```

## 基本类型 vs 引用类型

| 特性 | 基本类型 | 引用类型 |
|------|----------|----------|
| 存储内容 | 值本身 | 对象引用（地址）|
| 存储位置 | 栈 | 堆（对象）+ 栈（引用）|
| 默认值 | 有（0, false等）| null |
| 比较 | == 比较值 | == 比较地址，equals 比较内容 |
| 泛型支持 | 不支持 | 支持 |

## 使用建议

1. **优先使用基本类型**
   - 性能更好（无装箱拆箱）
   - 内存占用小

2. **必须使用包装类的场景**
   - 集合泛型（`List<Integer>`）
   - 需要 null 值
   - 需要使用方法（`Integer.parseInt()`）

3. **注意空指针**
   ```java
   Integer i = null;
   int n = i;  // NullPointerException！
   ```

4. **比较使用 equals**
   ```java
   Integer a = 200;
   Integer b = 200;
   a.equals(b);  // true
   a == b;       // false
   ```
