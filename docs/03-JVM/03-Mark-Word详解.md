# Java Mark Word 详解

## 📌 什么是 Mark Word？

Mark Word 是 Java 对象头（Object Header）的一部分，用于存储对象的运行时状态信息。它是 JVM 实现锁优化、GC 等核心功能的关键数据结构。

---

## 🎯 核心知识点

### 1. Java 对象内存布局

在 HotSpot JVM 中，普通对象在堆内存中的布局如下：

```
┌─────────────────────────────────────┐
│  对象头 (Header) - 12 bytes         │
│  - Mark Word: 8 bytes               │
│    * 哈希码、GC 年龄、锁状态标志      │
│  - Class Pointer: 4 bytes           │
│    * 指向类元数据的指针              │
├─────────────────────────────────────┤
│  实例数据 (Instance Data)           │
│  - 字段内容，按类型对齐              │
├─────────────────────────────────────┤
│  对齐填充 (Padding)                 │
│  - 8 字节对齐                        │
└─────────────────────────────────────┘
```

**说明：**
- **64 位 JVM + 压缩指针**：Mark Word 占 8 字节，Class Pointer 占 4 字节
- **32 位 JVM**：Mark Word 占 4 字节，Class Pointer 占 4 字节

---

### 2. Mark Word 结构详解

#### 64 位 JVM 下的 Mark Word（8 字节 = 64 位）

```
┌────────────────────────────────────────────────────────────────────┐
│                    Mark Word (64 bits)                             │
├────────────────────────────────────────────────────────────────────┤
│  无锁状态：                                                         │
│  [unused:25|hash:31|unused:1|age:4|biased_lock:1|lock:01]          │
│  - biased_lock = 0, lock = 01 → 无锁状态                           │
│  - hash: 对象的 hashCode (31 位)                                    │
│  - age: 对象分代年龄（GC 时使用，4 位）                              │
├────────────────────────────────────────────────────────────────────┤
│  偏向锁状态：                                                       │
│  [thread:54|epoch:2|unused:1|age:4|biased_lock:1|lock:01]          │
│  - biased_lock = 1, lock = 01 → 偏向锁状态                         │
│  - thread: 持有偏向锁的线程 ID (54 位)                               │
│  - epoch: 时间戳，用于批量重偏向 (2 位)                              │
├────────────────────────────────────────────────────────────────────┤
│  轻量级锁状态：                                                     │
│  [ptr_to_lock_record:62|lock:00]                                   │
│  - lock = 00 → 轻量级锁状态                                        │
│  - ptr_to_lock_record: 指向栈中锁记录的指针 (62 位)                  │
├────────────────────────────────────────────────────────────────────┤
│  重量级锁状态：                                                     │
│  [ptr_to_heavyweight_monitor:62|lock:10]                           │
│  - lock = 10 → 重量级锁状态                                        │
│  - ptr_to_heavyweight_monitor: 指向堆中监视器对象的指针 (62 位)      │
├────────────────────────────────────────────────────────────────────┤
│  GC 标记状态：                                                       │
│  [unused:2|marked:2|age:4|biased_lock:1|lock:11]                   │
│  - lock = 11 → GC 标记状态                                          │
└────────────────────────────────────────────────────────────────────┘
```

---

### 3. 四种锁状态对比

| 锁状态 | biased_lock | lock | 特点 | 适用场景 |
|--------|-------------|------|------|----------|
| **无锁** | 0 | 01 | 默认状态，未锁定 | 单线程环境 |
| **偏向锁** | 1 | 01 | 偏向持有锁的线程，无需 CAS | 单线程反复获取锁 |
| **轻量级锁** | - | 00 | CAS 自旋，不阻塞 | 多线程竞争但不激烈 |
| **重量级锁** | - | 10 | 操作系统互斥量，线程阻塞 | 多线程激烈竞争 |

---

### 4. 锁升级过程

```
无锁 
  ↓ (第一个线程尝试获取)
偏向锁 (CAS 记录线程 ID)
  ↓ (另一个线程竞争)
轻量级锁 (CAS 自旋)
  ↓ (竞争激烈或自旋超时)
重量级锁 (线程阻塞)
```

**锁升级流程图：**

```
Thread-1 首次访问     Thread-1 再次访问      Thread-2 来竞争
     ↓                    ↓                     ↓
   无锁 ────→  偏向锁  ────→  轻量级锁  ────→  重量级锁
            (直接获取)        (CAS 自旋)       (阻塞等待)
```

---

## 💡 高频面试题

### Q1: Mark Word 的作用是什么？

**答：**
Mark Word 主要用于：
1. **锁管理**：记录锁状态（无锁、偏向锁、轻量级锁、重量级锁）
2. **GC 支持**：记录对象分代年龄、GC 标记信息
3. **hashCode 缓存**：存储对象的哈希码（避免重复计算）

---

### Q2: synchronized 的锁升级过程？

**答：**
synchronized 经历了四个阶段：

1. **无锁 → 偏向锁**
   - 第一个线程获取锁时，JVM 将对象头的 Mark Word 设置为偏向该线程
   - 后续该线程再次进入同步块时，无需 CAS，直接比较线程 ID

2. **偏向锁 → 轻量级锁**
   - 当另一个线程尝试获取锁时，偏向锁撤销
   - 升级为轻量级锁，使用 CAS 自旋获取锁

3. **轻量级锁 → 重量级锁**
   - 当多个线程激烈竞争或自旋超过阈值
   - 升级为重量级锁，未获取到锁的线程被阻塞

**为什么要锁升级？**
- 减少不必要的性能开销（如线程阻塞/唤醒）
- 适应不同的竞争场景（无竞争→轻微竞争→激烈竞争）

---

### Q3: 偏向锁、轻量级锁、重量级锁的区别？

| 特性 | 偏向锁 | 轻量级锁 | 重量级锁 |
|------|--------|----------|----------|
| **实现方式** | Mark Word 记录线程 ID | CAS 自旋 | 操作系统 Mutex |
| **线程是否阻塞** | 否 | 否（忙等） | 是 |
| **适用场景** | 单线程反复访问 | 短时间锁持有 | 长时间锁持有 |
| **性能开销** | 最低 | 中等 | 最高 |

---

### Q4: 如何查看对象的 Mark Word？

**答：** 使用 JOL (Java Object Layout) 工具：

```xml
<dependency>
    <groupId>org.openjdk.jol</groupId>
    <artifactId>jol-core</artifactId>
    <version>0.16</version>
</dependency>
```

```java
import org.openjdk.jol.info.ClassLayout;

public class JolDemo {
    public static void main(String[] args) {
        Object obj = new Object();
        // 打印对象头布局
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());
    }
}
```

**输出示例：**
```
OFFSET  SIZE   TYPE DESCRIPTION
     0     8        (object header: mark word)
     8     4        (object header: class pointer)
    12     4        (alignment/padding gap)
Total size: 16 bytes
```

---

## 🔧 实战技巧

### 1. 禁用偏向锁

某些场景下（如基准测试），需要禁用偏向锁以避免干扰：

```bash
java -XX:-UseBiasedLocking YourApp
```

### 2. 调整锁升级阈值

```bash
# 调整自旋次数（轻量级锁）
-XX:PreBlockSpin=10

# 调整偏向锁延迟（JVM 启动后多久启用偏向锁）
-XX:BiasedLockingStartupDelay=0
```

### 3. 使用 Arthas 在线诊断

```bash
# 安装 Arthas
wget https://arthas.aliyun.com/arthas-boot.jar

# 启动并查看对象信息
java -jar arthas-boot.jar
```

---

## 📊 性能影响分析

### 不同锁状态的性能对比

```
┌────────────────────────────────────────────┐
│  锁类型    │  吞吐量 (ops/s)  │  延迟 (ms)  │
├────────────────────────────────────────────┤
│  偏向锁    │     100,000      │    0.01    │
│  轻量级锁  │      50,000      │    0.05    │
│  重量级锁  │      10,000      │    0.5     │
└────────────────────────────────────────────┘
```

**结论：**
- 偏向锁性能最优（无竞争场景）
- 重量级锁性能最差（线程切换开销大）
- 选择合适的锁策略对性能至关重要

---

## 🎓 知识扩展

### 1. 对象头与 GC 的关系

Mark Word 中的 `age` 字段（4 位）记录对象的分代年龄：
- 每次 Minor GC，存活对象的年龄 +1
- 年龄达到阈值（默认 15），晋升到老年代
- GC 标记阶段会使用 Mark Word 记录对象是否可达

### 2. hashCode 的存储策略

- **未计算过 hashCode**：Mark Word 的 hash 区域为 0
- **已计算过 hashCode**：结果存储在 hash 区域（31 位）
- **优势**：避免重复计算，提升性能

### 3. 批量重偏向（Bulk Rebias）

当存在大量同类对象且都被同一线程持有时：
- JVM 会在类级别记录偏向模式
- 新创建的对象直接偏向该线程
- 减少单个对象的 CAS 操作

---

## 📚 参考资源

- **官方文档**：[HotSpot VM Source Code](https://github.com/openjdk/jdk)
- **推荐书籍**：《深入理解 Java 虚拟机》第 3 版
- **工具推荐**：
  - JOL (Java Object Layout)
  - JVisualVM
  - Arthas

---

## 🔗 相关文档

- [JVM内存模型详解](./01-JVM内存模型详解.md)
- [JVM垃圾回收详解](./02-JVM垃圾回收详解.md)
- [Java并发编程](../02-Java并发编程/README.md)

---

**维护者：** itzixiao  
**最后更新：** 2026-03-16  
**代码位置：** `interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/jvm/MarkWordDemo.java`
