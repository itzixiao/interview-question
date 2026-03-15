# JVM内存模型详解

## 一、JVM 运行时数据区

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      堆内存（Heap）                          │
│                                                             │
│                     新生代（Young）                         │
│  ┌─────────────┬─────────────┬─────────────┐               │
│  │   Eden 区   │  Survivor0  │  Survivor1  │               │
│  │   (8/10)    │   (1/10)    │   (1/10)    │               │
│  └─────────────┴─────────────┴─────────────┘               │
│                         ↓ 晋升                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                   老年代（Old）                      │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                    元空间（Metaspace）                       │
│  - 类元数据                                                  │
│  - 常量池                                                    │
│  - 方法信息                                                  │
│  （JDK8 之前：永久代 PermGen）                               │
└─────────────────────────────────────────────────────────────┘
┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐
│   虚拟机栈   │   │   本地方法栈 │  │       程序计数器         │
│  - 栈帧     │   │  - Native  │  │  - 当前执行字节码位置      │
│  - 局部变量  │   │            │  │  - 线程私有              │
└─────────────┘  └─────────────┘  └─────────────────────────┘
```

---

## 二、堆内存（Heap）

### 2.1 新生代（Young Generation）

| 区域 | 比例 | 说明 |
|------|------|------|
| Eden | 8/10 | 新生代内部占比，新对象分配区域 |
| Survivor0 | 1/10 | 新生代内部占比，存活对象复制区 |
| Survivor1 | 1/10 | 新生代内部占比，存活对象复制区 |

> **注**：新生代总容量约占堆内存的 1/3（可通过 `-Xmn`/`-XX:NewRatio` 调整），Eden/S0/S1 是新生代内部的 8:1:1 划分。

**对象晋升过程**：
1. 新对象在 Eden 分配
2. Minor GC 后存活对象进入 Survivor
3. 对象在 Survivor 之间复制，年龄 +1
4. 年龄达到阈值（默认 15）晋升老年代

```java
// 对象分配示例
public class HeapAllocationDemo {
    public static void main(String[] args) {
        // 大部分对象在 Eden 分配
        byte[] buffer = new byte[1024]; // 1KB
        
        // 大对象直接进入老年代（需配合 Serial/ParNew GC，G1/Parallel GC 不生效）
        // 阈值通过 -XX:PretenureSizeThreshold=1048576 设置（1MB）
        byte[] bigObject = new byte[2 * 1024 * 1024]; // 2MB，超过阈值进入老年代
    }
}
```

### 2.2 老年代（Old Generation）

- 存放长期存活对象
- 大对象直接进入（需配合 `-XX:+UseSerialGC`/`-XX:+UseParNewGC`，G1/Parallel GC 不生效）
- Full GC 时回收

**对象晋升条件**：
1. **年龄阈值**：Survivor 中年龄达到 15
2. **动态年龄判断**：Survivor 中同年龄对象总和 > 一半空间
3. **大对象直接晋升**：超过 `-XX:PretenureSizeThreshold` 阈值直接进入老年代

---

## 三、元空间（Metaspace）

### 3.1 元空间 vs 永久代

| 特性 | 永久代（JDK7 及以前） | 元空间（JDK8+） |
|------|---------------------|---------------|
| **存储位置** | JVM 堆内 | 本地内存 |
| **GC 触发阈值** | `-XX:PermSize` | `-XX:MetaspaceSize` |
| **最大容量** | `-XX:MaxPermSize` | `-XX:MaxMetaspaceSize`（可选，默认无上限） |
| **GC 效率** | 低 | 高 |

### 3.2 存储内容

```
元空间（Metaspace）
├── 类元数据（Klass）
│   ├── 类名
│   ├── 父类引用
│   ├── 接口信息
│   └── 方法代码
├── 运行时常量池
│   ├── 数值常量
│   ├── 方法/字段符号引用
│   └── （字符串常量池已在 JDK7+ 迁移到堆内存）
└── 方法信息
    ├── 字节码
    ├── 异常表
    └── 行号表
```

> **注**：JDK7+ 将字符串常量池从永久代/元空间迁移到堆内存，减少元空间压力。

### 3.3 参数配置

```bash
# 元空间初始大小
-XX:MetaspaceSize=256m

# 元空间最大大小（不设置则无限制）
-XX:MaxMetaspaceSize=256m

# 触发 GC 的元空间阈值
-XX:MinMetaspaceFreeRatio=40
-XX:MaxMetaspaceFreeRatio=70
```

---

## 四、虚拟机栈（Java Stack）

### 4.1 栈帧结构

```
┌─────────────────┐
│   局部变量表     │  基本数据类型、对象引用
├─────────────────┤
│    操作数栈      │  方法执行的工作区
├─────────────────┤
│  动态链接        │  指向运行时常量池的方法引用
├─────────────────┤
│  方法返回地址    │  方法执行完毕后的返回位置
├─────────────────┤
│  附加信息        │  调试信息等
└─────────────────┘
```

### 4.2 局部变量表

```java
public class LocalVariableTableDemo {
    private int instanceVar; // 实例变量，不在栈中
    
    public void method(int param1, String param2) {
        // 局部变量表包含：
        // 0: this (当前对象引用)
        // 1: param1 (int)
        // 2: param2 (String 引用)
        // 3: localVar (int)
        int localVar = 100;
        
        // long/double 占用 2 个槽位
        long longVar = 1000L; // 占用 2 个槽位
    }
}
```

**特点**：
- `this` 占用 slot 0
- 参数从 slot 1 开始
- `long`/`double`占用 2 个槽位
- 局部变量表大小编译期确定

### 4.3 操作数栈

```java
// 示例：a = b + c * 2
public int calculate(int a, int b, int c) {
    return b + c * 2;
}

// 字节码执行过程：
// 1. iload_2    (加载 c 到栈顶)
// 2. iconst_2   (加载 2 到栈顶)
// 3. imul       (弹出 2 和 c，计算 c*2，结果压栈)
// 4. iload_1    (加载 b 到栈顶)
// 5. iadd       (弹出 b 和 c*2，计算 b+c*2)
// 6. istore_0   (结果存储到 a)
```

---

## 五、本地方法栈（Native Method Stack）

### 5.1 作用

- 为 JVM 调用 native 方法服务
- 保存 native 方法的局部变量、操作数栈
- 与操作系统平台相关

### 5.2 示例

```java
public class NativeMethodDemo {
    // native 方法声明
    public native void nativeMethod();
    
    static {
        // 加载本地库
        System.loadLibrary("nativeLib");
    }
    
    public static void main(String[] args) {
        new NativeMethodDemo().nativeMethod();
        // 调用时切换到本地方法栈执行
    }
}
```

---

## 六、程序计数器（Program Counter Register）

### 6.1 特点

- 线程私有，每个线程有独立的程序计数器
- 记录当前执行的字节码指令地址（或 native 方法的 undefined）
- 宽度与处理器位数一致（32/64 位）
- 唯一不会发生 OOM 的区域
- 执行 native 方法时为 undefined

### 6.2 工作原理

```
线程 1: PC = 0x1000 → 执行 bytecode[0x1000]
线程 2: PC = 0x2000 → 执行 bytecode[0x2000]
线程 3: PC = 0x3000 → 执行 bytecode[0x3000]

每个线程独立维护 PC，互不干扰
```

---

## 七、内存区域对比总结

### 7.1 线程共享性

| 区域 | 是否线程共享 | 说明 |
|------|------------|------|
| **堆** | ✅ 共享 | 所有线程共享 |
| **元空间** | ✅ 共享 | 所有线程共享 |
| **虚拟机栈** | ❌ 私有 | 线程独占 |
| **本地方法栈** | ❌ 私有 | 线程独占 |
| **程序计数器** | ❌ 私有 | 线程独占 |

### 7.2 存储内容

| 区域 | 存储内容 | 生命周期 |
|------|---------|---------|
| **堆** | 对象实例、数组 | GC 管理 |
| **元空间** | 类元数据、常量池 | 类卸载时回收 |
| **虚拟机栈** | 栈帧（局部变量、操作数栈） | 方法调用结束 |
| **本地方法栈** | Native 方法状态 | Native 方法返回 |
| **程序计数器** | 当前指令地址 | 随线程执行变化 |

### 7.3 异常情况

| 区域 | 可能异常 | 触发条件 |
|------|---------|---------|
| **堆** | OutOfMemoryError: Java heap space | 堆内存不足 |
| **元空间** | OutOfMemoryError: Metaspace | 元空间不足 |
| **虚拟机栈** | StackOverflowError | 栈深度超限（如递归无终止） |
| **虚拟机栈** | OutOfMemoryError: unable to create new native thread | 线程创建过多导致栈内存不足 |
| **本地方法栈** | StackOverflowError / OutOfMemoryError | 类似虚拟机栈 |
| **程序计数器** | 无 | 不会 OOM |

---

## 八、实战技巧

### 8.1 查看内存结构

```bash
# 查看堆内存使用情况
jstat -gc <pid> 1000  # 1000 表示每隔 1 秒输出一次

# 查看元空间
jstat -gc -t <pid> 1000

# 生成堆转储文件
jmap -dump:format=b,file=heap.hprof <pid>

# 查看类加载信息
jstat -class <pid> 1000
```

### 8.2 常用参数配置

```bash
# 堆内存配置
-Xms4g                    # 初始堆大小
-Xmx4g                    # 最大堆大小
-Xmn2g                    # 新生代大小（优先级高于 NewRatio）
-XX:NewRatio=2            # 老年代：新生代 = 2:1（新生代占堆的 1/3，设置 -Xmn 后失效）

# 元空间配置
-XX:MetaspaceSize=256m    # 触发元空间 GC 的初始阈值
-XX:MaxMetaspaceSize=256m # 最大元空间大小（默认无上限）

# 栈配置
-Xss1m                    # 每个线程栈大小（JDK8 默认：Linux/macOS 1024KB，Windows 512KB）
```

### 8.3 内存泄漏排查

```bash
# 1. 监控内存使用
jconsole / jvisualvm

# 2. 生成 dump 文件（仅保留存活对象，减小文件体积）
jmap -dump:live,format=b,file=heap.hprof <pid>

# 3. 分析 dump 文件
# 轻量分析：jhat heap.hprof（访问 http://localhost:7000）
# 专业分析：MAT（Eclipse Memory Analyzer）/ YourKit（商业工具）
```

---

## 九、高频面试题

### 9.1 JVM 运行时数据区有哪些？

**参考答案**：

**线程共享**：
1. **堆（Heap）**：对象实例、数组
2. **元空间（Metaspace）**：类元数据、常量池

**线程私有**：
1. **虚拟机栈**：栈帧、局部变量、操作数栈
2. **本地方法栈**：Native 方法执行
3. **程序计数器**：当前指令地址

### 9.2 堆和栈的区别？

**参考答案**：

| 维度 | 堆 | 栈 |
|------|-----|----|
| **用途** | 存储对象实例 | 存储栈帧（局部变量、方法调用） |
| **共享性** | 线程共享 | 线程私有 |
| **大小** | 较大（-Xmx） | 较小（-Xss） |
| **GC** | 主要 GC 区域 | 自动清理 |
| **异常** | OutOfMemoryError | StackOverflowError |

### 9.3 为什么要用元空间替代永久代？

**参考答案**：

**原因**：
1. **避免 OOM**：永久代容易满，元空间使用本地内存
2. **提升性能**：元空间 GC 效率更高
3. **便于管理**：元空间大小可动态调整
4. **与 HotSpot 合并**：Oracle JRockit 使用元空间

**优势**：
- 类元数据存储在本地内存
- 默认无上限（可设置`-XX:MaxMetaspaceSize`）
- 减少 Full GC 次数

### 9.4 对象一定在堆上分配吗？

**参考答案**：

**不一定**，特殊情况：

1. **逃逸分析优化**（JDK7+ 默认开启，可通过 `-XX:+DoEscapeAnalysis` 显式开启）：
   - **栈上分配**：对象未逃逸出方法作用域，直接在栈上分配（方法结束后随栈帧销毁，无需 GC）；
   - **标量替换**：将对象分解为基本类型（标量）分配在栈上（如 `new Point(1,2)` 替换为 `int x=1, int y=2`）；
   - **同步消除**：对象未逃逸，移除 `synchronized` 锁（JIT 优化）。

2. **TLAB（Thread Local Allocation Buffer）**（JDK7+ 默认开启）：
   - 属于堆内存的 Eden 区，为每个线程分配独立的缓存区域；
   - 新对象优先在 TLAB 分配，减少多线程分配内存的锁竞争；
   - 可通过 `-XX:TLABSize` 设置大小，`-XX:+UseTLAB` 开启（默认开启）。

3. **大对象直接进入老年代**：
   ```bash
   -XX:PretenureSizeThreshold=1048576  # 1MB（需配合 Serial/ParNew GC，G1/Parallel GC 不生效）
   ```

### 9.5 如何判断对象是否可以被回收？

**参考答案**：

**可达性分析算法**：
- 从 GC Roots 向下搜索
- 不可达的对象可被回收

**GC Roots 包括**：
1. 虚拟机栈中引用的对象（局部变量表）；
2. 方法区中静态属性引用的对象；
3. 方法区中运行时常量池引用的对象（如字符串常量）；
4. 本地方法栈中 JNI（Native 方法）引用的对象；
5. 所有被同步锁（synchronized）持有的对象；
6. JVM 内部引用（如类加载器、系统类、异常对象）。

---

## 十、下一步学习

- ➡️ **[垃圾回收详解](./02-JVM垃圾回收详解.md)** - GC 算法与收集器
- 📚 **[JVM 调优实战](../05-SpringBoot与自动装配/README.md)** - 性能优化
- 🔗 **[内存泄漏排查](./01-JVM内存模型与垃圾回收.md#八 - 内存泄漏排查)** - 实战技巧

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
