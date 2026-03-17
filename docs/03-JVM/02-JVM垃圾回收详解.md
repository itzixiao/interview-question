# JVM垃圾回收详解

## 一、垃圾回收基础

### 1.1 为什么需要垃圾回收？

**手动内存管理的问题**：

- 忘记释放导致内存泄漏
- 重复释放导致程序崩溃
- 悬垂指针（指向已释放内存）

**Java 的解决方案**：

- 自动垃圾回收（Garbage Collection）
- JVM 统一管理内存
- 开发者无需关心释放细节

---

### 1.2 判断对象存活

#### 1. 引用计数法（Reference Counting）

```java
public class ReferenceCountingDemo {
    private Object instance = null;

    public static void main(String[] args) {
        ReferenceCountingDemo a = new ReferenceCountingDemo();
        ReferenceCountingDemo b = new ReferenceCountingDemo();

        // 互相引用
        a.instance = b;
        b.instance = a;

        // 断开外部引用
        a = null;
        b = null;

        // 引用计数为 0，可以被回收
        // 但循环引用问题无法解决
    }
}
```

**缺点**：无法解决循环引用问题

#### 2. 可达性分析（Reachability Analysis）✅

```
GC Roots：
  ├─ 虚拟机栈中引用的对象
  │   └─ 局部变量表中的对象引用
  │
  ├─ 方法区中类静态属性引用的对象
  │   └─ static 字段引用的对象（如 private static Object obj）
  │
  ├─ 方法区中运行时常量池引用的对象
  │   ├─ 字符串常量池（如 "constant"）
  │   └─ Class 常量池字面量引用
  │
  ├─ 本地方法栈中 JNI 引用的对象
  │   └─ Native 方法持有的 Java 对象
  │
  ├─ 所有被同步锁持有的对象
  │   └─ synchronized 锁住的对象
  │
  └─ JVM 内部引用
      ├─ 类加载器
      ├─ 异常对象（如 OutOfMemoryError）
      └─ 系统类（如 java.lang.Object）
```

**示例**：

```java
public class ReachabilityDemo {
    private static Object staticObj = new Object(); // GC Root（静态属性）
    
    public void method() {
        Object localObj = new Object(); // GC Root（栈帧局部变量）
        String str = "constant";        // GC Root（字符串常量池）
        
        synchronized (this) {           // GC Root（同步锁）
            // ...
        }
    }
}
```

---

## 二、垃圾回收算法详解

### 2.1 标记 - 清除（Mark-Sweep）

```
┌─────────────────────────────────────────┐
│  阶段 1: 标记                            │
│  ┌───┬───┬───┬───┬───┬───┬───┐          │
│  │ ✓ │ ✗ │ ✓ │ ✗ │ ✗ │ ✓ │ ✗ │          │  ✓=存活 ✗=垃圾
│  └───┴───┴───┴───┴───┴───┴───┘          │
├─────────────────────────────────────────┤
│  阶段 2: 清除                            │
│  ┌───┬─────┬───┬───────┬───┬─────┐     │
│  │ ✓ │ 空隙│ ✓ │  空隙  │ ✓ │空隙 │      │  产生内存碎片
│  └───┴─────┴───┴───────┴───┴─────┘     │
└─────────────────────────────────────────┘
```

**工作原理**：

1. **标记阶段**：从 GC Roots 遍历，标记所有存活对象
2. **清除阶段**：清除未标记的对象，释放空间

**优点**：

- 算法简单，易于实现
- 不需要移动对象，开销较小

**缺点**：

- **内存碎片**：清除后产生不连续空间，大对象无法分配
- **效率问题**：标记和清除都需要遍历整个堆

**适用场景**：老年代（CMS 收集器）

**代码模拟**：

```java
public class MarkSweepDemo {
    // 伪代码展示原理
    public void markSweep() {
        // 1. 标记
        for (Object obj : heap) {
            if (isReachable(obj)) {
                obj.mark(); // 标记为存活
            }
        }
        
        // 2. 清除
        for (Object obj : heap) {
            if (!obj.isMarked()) {
                free(obj); // 释放内存
            } else {
                obj.unmark(); // 清除标记
            }
        }
    }
}
```

---

### 2.2 复制（Copying）

```
┌─────────────────────────────────────────────┐
│          新生代（Young Generation）           │
│  ┌───────────────┬───────────────┐          │
│  │   Eden 区     │  Survivor 区   │          │
│  │   (80%)       │   (20%)       │          │
│  │               │  S0 + S1      │          │
│  └───────────────┴───────────────┘          │
└─────────────────────────────────────────────┘

Minor GC 过程：
  Before:                      After:
  ┌────────┬─────┬─────┐      ┌────────┬─────┬─────┐
  │ Eden   │ S0  │ S1  │      │ Eden   │ S0  │ S1  │
  │ ████░░ │ ██░ │ ░░░ │  →   │ ░░░░░░ │ ░░░ │ ███ │
  └────────┴─────┴─────┘      └────────┴─────┴─────┘
  对象分散在 Eden 和 S0          存活对象复制到 S1
```

**工作原理**：

1. 将内存分为两块相等的区域（From/To）
2. 只使用其中一块，另一块空闲
3. GC 时将存活对象复制到空闲区域
4. 清空已使用的区域

**优点**：

- **无内存碎片**：连续分配，利用率高
- **高效**：只需处理存活对象

**缺点**：

- **内存利用率低**：只有 50% 可用（实际新生代用 8:1:1 比例）
- **复制开销**：大量对象存活时效率低

**优化方案**（HotSpot JVM）：

- 新生代分为 Eden + 两个 Survivor（S0/S1），比例为 8:1:1；
- 每次只使用 Eden + 一个 Survivor（如 Eden+S0），另一个 Survivor 空闲；
- 对象年龄 +1 后切换到空闲 Survivor，年龄达到阈值（默认 15）晋升老年代；

**适用场景**：新生代（Serial、ParNew、Parallel Scavenge）

**代码模拟**：

```java
public class CopyingGCDemo {
    private MemoryRegion fromSpace;
    private MemoryRegion to_space;
    
    public void gc() {
        // 1. 标记 GC Roots 直接引用的对象
        List<Object> roots = getGCRoots();
        
        // 2. 扫描 From Space，复制存活对象到 To Space
        for (Object obj : from_space) {
            if (isAlive(obj)) {
                Object newAddr = copy(obj, to_space);
                updateReference(obj, newAddr);
            }
        }
        
        // 3. 交换 From 和 To
        swap(from_space, to_space);
        
        // 4. 清空原 From Space
        from_space.clear();
    }
}
```

---

### 2.3 标记 - 整理（Mark-Compact）

```
┌─────────────────────────────────────────┐
│  阶段 1: 标记（同 Mark-Sweep）            │
│  ┌───┬───┬───┬───┬───┬───┬───┐          │
│  │ ✓ │ ✗ │ ✓ │ ✗ │ ✗ │ ✓ │ ✗ │          │
│  └───┴───┴───┴───┴───┴───┴───┘          │
├─────────────────────────────────────────┤
│  阶段 2: 整理                             │
│  ┌───┬───┬───┬─────────────────┐         │
│  │ ✓ │ ✓ │ ✓ │    空闲空间       │        │  对象向一端移动
│  └───┴───┴───┴─────────────────┘        │
└─────────────────────────────────────────┘
```

**工作原理**：

1. **标记阶段**：标记所有存活对象
2. **整理阶段**：将存活对象向一端移动，然后清理边界外的垃圾

**优点**：

- **无内存碎片**：整理后空间连续
- **适合老年代**：对象存活率高，避免复制

**缺点**：

- **移动成本高**：需要更新对象引用地址
- **STW 时间长**：移动过程中不能访问对象

**适用场景**：老年代（Serial Old、Parallel Old）

**代码模拟**：

```java
public class MarkCompactDemo {
    public void gc() {
        // 1. 标记
        List<Object> liveObjects = new ArrayList<>();
        for (Object obj : heap) {
            if (isReachable(obj)) {
                liveObjects.add(obj);
            }
        }
        
        // 2. 整理 - 对象向一端移动
        int compactedAddress = 0;
        for (Object obj : liveObjects) {
            int oldAddr = obj.getAddress();
            move(obj, compactedAddress);
            updateReferences(oldAddr, compactedAddress);
            compactedAddress += obj.getSize();
        }
        
        // 3. 清理边界外垃圾
        clearAfter(compactedAddress);
    }
}
```

---

### 2.4 分代收集（Generational Collection）

```
┌──────────────────────────────────────────────────────┐
│                     整堆（Heap）                       │
│  ┌────────────────────────────────────────────────┐  │
│  │              新生代（Young）                    │  │
│  │  ┌──────────┬────────┬────────┐                │  │
│  │  │   Eden   │ S0     │ S1     │  复制算法       │  │
│  │  │  (80%)   │ (10%)  │ (10%)  │                │  │
│  │  └──────────┴────────┴────────┘                │  │
│  └────────────────────────────────────────────────┘  │
│                        ↓ 晋升                        │
│  ┌────────────────────────────────────────────────┐  │
│  │              老年代（Old）                      │  │
│  │             标记 - 整理算法                     │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘

对象生命周期：
新对象 → Eden → Minor GC → Survivor(S0↔S1) → 年龄阈值 → 老年代 → Full GC
```

**核心思想**：根据对象的生命周期特点，采用不同的回收策略

**年轻代特点**：

- 对象"朝生夕死"，存活率低
- 适合**复制算法**，快速回收
- Minor GC 频繁（毫秒级）

**老年代特点**：

- 对象长期存活，存活率高
- 适合**标记 - 整理**，避免复制
- Full GC 较少（秒级）

**优点**：

- **高效**：针对不同年代采用最优算法
- **低停顿**：Minor GC 快速，Full GC 较少

**缺点**：

- **复杂**：需要维护年代划分和对象晋升机制；
- **跨代引用**：新生代 GC 时，需扫描老年代中引用新生代对象的指针（通过 Remembered Set 记录），避免全量扫描老年代；

**适用场景**：现代 JVM 默认方案（G1、ZGC 等）

---

## 三、垃圾回收器详解

### 3.1 Serial 收集器（单线程）

```
┌─────────────────────────────────┐
│  Serial 收集器                   │
│  - JDK 1.3 引入                  │
│  - 单线程串行执行                │
│  - STW（Stop The World）         │
└─────────────────────────────────┘

新生代：复制算法
老年代：标记 - 整理算法

优点：简单高效，无交互开销
缺点：停顿时间长
适用：客户端应用，小内存（<100M）
```

**配置参数**：

```bash
-XX:+UseSerialGC      # 启用 Serial 收集器
```

---

### 3.2 ParNew 收集器（Serial 的多线程版）

```
┌─────────────────────────────────┐
│  ParNew 收集器                   │
│  - JDK 1.5 引入                  │
│  - 多线程并行执行                │
│  - CMS 的默认新生代收集器        │
└─────────────────────────────────┘

新生代：复制算法（多线程）

优点：多线程，减少 STW
缺点：CPU 敏感，线程切换开销
适用：多核 CPU，CMS 搭配
```

**配置参数**：

```bash
-XX:+UseConcMarkSweepGC  # 启用 CMS（自动搭配 ParNew）
-XX:ParallelGCThreads=4  # 设置并行线程数
```

---

### 3.3 Parallel Scavenge / Parallel Old（吞吐量优先）

```
┌─────────────────────────────────┐
│  Parallel 收集器                 │
│  - JDK 1.6 引入                  │
│  - 吞吐量优先                    │
│  - 自适应调节策略                │
└─────────────────────────────────┘

Parallel Scavenge（新生代）：复制算法
Parallel Old（老年代）：标记 - 整理算法

关键参数：
  -XX:MaxGCPauseMillis=<ms>    # 最大停顿时间
  -XX:GCTimeRatio=<N>          # GC 时间占比 (1/(1+N))

优点：高吞吐量，自适应
缺点：停顿时间不可控
适用：后台计算任务，科学计算
```

**配置参数**：

```bash
-XX:+UseParallelGC         # 启用 Parallel Scavenge
-XX:+UseParallelOldGC      # 启用 Parallel Old
-XX:MaxGCPauseMillis=100   # 最大停顿时间目标
-XX:GCTimeRatio=99         # GC 时间占比 < 1%
```

---

### 3.4 CMS（Concurrent Mark Sweep）⭐

```
┌─────────────────────────────────────────────┐
│  CMS 收集器（低停顿）                        │
│  - JDK 1.5 引入                             │
│  - 并发标记，并发清除                        │
│  - 最短回收停顿时间                          │
└─────────────────────────────────────────────┘

执行阶段：
  ┌──────────────┬──────────┬──────────────┬──────────┬──────────┐
  │   初始标记    │  并发标记 │  并发预清理  │  重新标记  │  并发清除  │
  │   (STW)      │  (并发)   │   (并发)     │  (STW)    │  (并发)   │
  │  标记 GCRoots│  遍历图   │  处理跨代引用 │  修正变动  │  清垃圾    │
  │  很短        │  很长     │  较长        │  较短      │  很长     │
  └──────────────┴──────────┴──────────────┴──────────┴──────────┘

优点：低停顿，并发执行
缺点：
  - CPU 敏感（占用线程资源）
  - 浮动垃圾（并发期间产生的垃圾）
  - 内存碎片（标记 - 清除算法）
适用：Web 应用，B/S 架构，注重用户体验

**CMS 失败情况**：
1. **Concurrent Mode Failure**：并发清理期间老年代空间不足，触发 Full GC；
2. **Promotion Failed**：新生代 Minor GC 时，Survivor 区空间不足，对象需晋升老年代，但老年代也无连续空间，触发 Full GC。
```

**配置参数**：

```bash
-XX:+UseConcMarkSweepGC           # 启用 CMS
-XX:CMSInitiatingOccupancyFraction=75  # 触发 CMS 的老年代阈值
-XX:+UseCMSCompactAtFullCollection     # Full GC 时压缩
-XX:CMSFullGCsBeforeCompaction=0       # 每次 Full GC 都压缩
```

**CMS 失败情况**：

1. **Concurrent Mode Failure**：并发清理期间老年代空间不足
2. **Promotion Failed**：新生代晋升失败

**解决方案**：

```bash
# 增加老年代空间
-Xmx4g -Xms4g

# 降低触发阈值
-XX:CMSInitiatingOccupancyFraction=60

# 增加 Survivor 区
-XX:SurvivorRatio=8
```

---

### 3.5 G1（Garbage First）⭐⭐⭐

```
┌─────────────────────────────────────────────────────┐
│  G1 收集器（区域化分代）                             │
│  - JDK 7u4 引入                                      │
│  - JDK 9+ 默认收集器                                 │
│  - 可预测的停顿时间模型                              │
└─────────────────────────────────────────────────────┘

堆布局：
  ┌───┬───┬───┬───┬───┬───┬───┬───┐
  │ E │ E │ O │ H │ E │ O │ H │ E │  E=Eden, O=Old, H=Humongous
  └───┴───┴───┴───┴───┴───┴───┴───┘
  每个 Region 独立，可动态调整角色

执行阶段：
  1. 初始标记（STW）：标记 GC Roots 直接关联
  2. 并发标记：遍历对象图，记录可达性
  3. 最终标记（STW）：处理 SATB 记录
  4. 筛选回收（STW）：选择价值最大的 Region 回收

优点：
  - 可预测停顿（用户设定目标时间）
  - 无内存碎片（Region 整理）
  - 大堆友好（TB 级别）
缺点：
  - 内存占用较高（Remembered Set）
  - CPU 负担重（并发标记）
适用：大内存（>4GB），服务端应用
```

**Region 类型**：

| 类型            | 说明                    | 特点                       |
|---------------|-----------------------|--------------------------|
| **Eden**      | 新对象分配区域               | 占大多数                     |
| **Survivor**  | 存放 Minor GC 后存活对象     | 数量较少                     |
| **Old**       | 存放长期存活对象              | 对象晋升目标                   |
| **Humongous** | 存放超大对象（Region 容量 50%） | 占用连续多个 Region，直接分配在老年代区域 |

**配置参数**：

```bash
-XX:+UseG1GC                        # 启用 G1
-XX:MaxGCPauseMillis=200            # 最大停顿时间目标
-XX:InitiatingHeapOccupancyPercent=45  # 并发标记触发阈值
-XX:G1HeapRegionSize=16m            # Region 大小（1MB-32MB）
-XX:G1ReservePercent=10             # 预留堆比例
-XX:G1NewSizePercent=5              # 新生代最小比例
-XX:G1MaxNewSizePercent=60          # 新生代最大比例
```

---

### 3.6 ZGC（超低延迟）⭐⭐⭐⭐

```
┌─────────────────────────────────────────────┐
│  ZGC 收集器（Z Garbage Collector）           │
│  - JDK 11 引入（实验性）                     │
│  - JDK 15+ 正式支持                          │
│  - 停顿时间 < 10ms                           │
└─────────────────────────────────────────────┘

核心技术：
  1. 染色指针（Colored Pointers）
     ┌───────────────────────────────────────────────────┐
     │ 64 位指针：46 位地址（支持 64TB 内存）+ 4 位标记 + 14 位保留 │
     │ 标记位直接存在指针中，无需额外空间                    │
     └───────────────────────────────────────────────────┘
  
  2. 读屏障（Load Barriers）
     ┌─────────────────────────────────┐
     │ 对象访问时检查指针颜色            │
     │ 如需转发，自动重定向到新地址       │
     └─────────────────────────────────┘
  
  3. 并发整理
     - 并发标记
     - 并发预备整理
     - 并发重映射

优点：
  - 超低延迟（<10ms，与堆大小无关）
  - 扩展性强（支持 TB 级堆）
  - 无碎片
缺点：
  - CPU 开销（约 15%-30%）
  - JDK 版本要求高
适用：低延迟要求，金融交易，实时系统
```

**配置参数**：

```bash
-XX:+UseZGC              # 启用 ZGC
-Xms8g -Xmx8g            # 堆内存建议 > 4GB
-XX:ZAllocationSpikeTolerance=2.0  # 分配尖峰容忍度
-XX:ZCollectionInterval=0          # 自动间隔（0=自动）
-XX:ZFragmentationLimit=25         # 碎片化限制
```

---

### 3.7 Shenandoah（红帽开发的超低延迟收集器）

```
┌─────────────────────────────────────────────┐
│  Shenandoah 收集器                           │
│  - 红帽开发，JDK 12 引入                      │
│  - 与 ZGC 类似，但实现不同                    │
│  - 停顿时间 < 10ms                           │
└─────────────────────────────────────────────┘

核心技术：
  1. 转发指针（Forwarding Pointers）
     ┌─────────────────────────────────┐
     │ 原对象头存储转发地址              │
     │ 新对象分配到新区域                │
     └─────────────────────────────────┘
  
  2. Brooks Pointer
     每个对象额外指针指向新位置
  
  3. 并发整理
     - 并发标记
     - 并发转移
     - 并发更新引用

优点：
  - 超低延迟（<10ms）
  - 开源免费（红帽支持）
  - 兼容性好（支持压缩类指针）
缺点：
  - 吞吐量略低（约 10%-20%）
  - 内存开销（Brooks Pointer）
适用：低延迟，开源偏好场景
```

**配置参数**：

```bash
-XX:+UseShenandoahGC     # 启用 Shenandoah
-Xms8g -Xmx8g            # 堆内存建议
-XX:ShenandoahGCHeuristics=compact  # 启发式策略
```

---

## 四、各版本垃圾回收器对比总结

### 4.1 性能对比表

| 收集器            | JDK 版本  | 停顿时间            |  吞吐量  | 内存开销 | CPU 开销 | 适用场景          |
|----------------|---------|-----------------|:-----:|:----:|:------:|---------------|
| **Serial**     | 1.3+    | 高（100-500ms）    |   中   |  低   |   低    | 客户端，小内存       |
| **ParNew**     | 1.5+    | 中（50-200ms）     |  中高   |  中   |   中    | 多核 CPU + CMS  |
| **Parallel**   | 1.6+    | 中高（80-300ms）    | **高** |  中   |   中    | 批处理，科学计算      |
| **CMS**        | 1.5-14  | **低**（20-100ms） |   中   |  中高  |   高    | Web 应用，B/S 架构 |
| **G1**         | 7u4+    | 中低（30-150ms）    |  中高   |  高   |   中高   | **大内存，服务端**   |
| **ZGC**        | 11+/15+ | **超低**（<10ms）   |   中   |  中   |   中高   | 低延迟，金融交易      |
| **Shenandoah** | 12+     | **超低**（<10ms）   |  中低   |  中高  |   高    | 低延迟，开源场景      |

---

### 4.2 演进路线图

```
JDK 1.3 ──→ Serial GC（唯一选择）
             新生代 Serial + 老年代 Serial Old
             单线程 STW，简单高效
   │
JDK 1.4.2 ─→ ① Parallel Scavenge（新生代并行回收，专注吞吐量）
             ② ParNew（新生代并行版 Serial，为 CMS 适配）
             Serial 仍作为兜底收集器
   │
JDK 1.5 ──→ CMS（Concurrent Mark Sweep）正式发布
             老年代并发回收，主打低停顿
             需搭配 ParNew 使用（CMS 不处理新生代）
   │
JDK 1.6 ──→ Parallel Old（老年代并行整理算法）
             完善 Parallel GC 体系
             新生代 Parallel Scavenge + 老年代 Parallel Old
   │
JDK 7u4 ──→ G1 GC 发布（实验特性）
             基于 Region 的分代回收
             支持可预测停顿时间目标
   │
JDK 8   ──→ ① G1 持续优化
             ② 移除永久代（PermGen），替换为元空间（Metaspace）
             ③ 默认收集器：Parallel GC（Parallel Scavenge + Parallel Old）
             元空间使用本地内存，减少 GC 压力
   │
JDK 9   ──→ ① G1 GC 成为默认收集器
             ② CMS 标记为"废弃候选"
   │
JDK 11  ──→ ① ZGC 发布（实验性，需 -XX:+UnlockExperimentalVMOptions -XX:+UseZGC）
             ② 仅支持 Linux 平台
             ③ TB 级内存支持，STW < 10ms
             ④ G1 进一步优化（自适应 Region 大小等）
   │
JDK 12  ──→ Shenandoah GC 发布（实验性）
             红帽主导的低延迟收集器
             无分代设计
   │
JDK 14  ──→ ① CMS 正式废弃
             ② ZGC 支持 macOS/Windows
             ③ Shenandoah 从实验性转产品级
   │
JDK 15  ──→ ZGC 从实验性转产品级（正式支持）
   │
JDK 17+ ──→ ① ZGC/Shenandoah 持续优化（如 ZGC 压缩指针）
(LTS)        ② G1 引入分代回收优化
             ③ G1 仍是生产主流选择
             ④ Epsilon GC（无操作 GC，用于性能测试）
   │
JDK 21  ──→ ① ZGC 成为推荐低延迟收集器
(LTS)        ② Shenandoah 进一步降低停顿
             ③ G1 持续优化
             ④ ZGC 支持分代回收（JDK 21+ 重要特性）
```

---

### 4.3 选择建议

**按应用场景选择**：

| 场景               | 推荐收集器          | 理由       |
|------------------|----------------|----------|
| **桌面应用/小程序**     | Serial         | 简单高效，无交互 |
| **Web 应用（中等规模）** | G1             | 平衡吞吐和延迟  |
| **大内存服务端（>4GB）** | G1/ZGC         | 可扩展性好    |
| **低延迟交易系统**      | ZGC/Shenandoah | <10ms 停顿 |
| **批处理/科学计算**     | Parallel       | 吞吐量优先    |
| **遗留系统（JDK8）**   | CMS/G1         | 稳定成熟     |

**按 JDK 版本选择**：

| JDK 版本  | 推荐收集器                   |
|---------|-------------------------|
| JDK 8   | G1（默认 Parallel，可手动切换）   |
| JDK 11  | G1/ZGC（实验性）             |
| JDK 15+ | G1（默认）/ZGC（生产级）         |
| JDK 17+ | G1/ZGC/Shenandoah（根据需求） |

---

## 五、垃圾回收调优

### 5.1 常用参数

```bash
# 堆内存设置
-Xms4g -Xmx4g           # 初始/最大堆内存
-Xmn2g                  # 新生代大小
-XX:MetaspaceSize=256m  # 元空间初始大小
-XX:MaxMetaspaceSize=256m

# 垃圾回收器选择
-XX:+UseG1GC            # 使用 G1
-XX:+UseZGC             # 使用 ZGC
-XX:+UseConcMarkSweepGC # 使用 CMS

# G1 调优
-XX:MaxGCPauseMillis=200    # 最大停顿时间目标（默认 200ms）
-XX:G1HeapRegionSize=16m    # Region 大小（1MB-32MB）
-XX:InitiatingHeapOccupancyPercent=45  # 触发并发标记的堆占用率（默认 45%）
-XX:G1NewSizePercent=5      # 新生代最小比例（默认 5%）
-XX:G1MaxNewSizePercent=60  # 新生代最大比例（默认 60%）
```

### 5.2 调优目标选择

| 场景  | 推荐收集器             | 关注点   |
|-----|-------------------|-------|
| 低延迟 | ZGC/Shenandoah/G1 | 停顿时间  |
| 高吞吐 | Parallel/G1       | 吞吐量   |
| 小内存 | Serial/Parallel   | 简单高效  |
| 大内存 | G1/ZGC            | 可预测停顿 |

### 5.3 G1 调优实战

**步骤 1：监控 baseline**

```bash
java -Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10M -jar app.jar
```

**步骤 2：分析 GC 日志**

```bash
# 查看停顿时间
grep "Pause Time" gc.log

# 查看 Mixed GC 频率
grep "Mixed GC" gc.log

# 查看 Evacuation Failure
grep "Evacuation Failure" gc.log
```

**步骤 3：调整参数**

```bash
# 停顿过长 → 降低 MaxGCPauseMillis
-XX:MaxGCPauseMillis=100

# GC 过频 → 提高 IHOP
-XX:InitiatingHeapOccupancyPercent=50

# 晋升失败 → 增加 G1ReservePercent
-XX:G1ReservePercent=15
```

**步骤 4：迭代验证**

```bash
# 压测环境验证
jmeter -n -t test.jmx -l result.jtl

# 对比性能指标
# - 吞吐量（TPS）
# - 响应时间（RT）
# - GC 频率和停顿
```

---

## 六、高频面试题

### 6.1 四种垃圾回收算法的区别和适用场景？

**参考答案**：

| 算法          | 原理           | 优点        | 缺点          | 适用场景                    |
|-------------|--------------|-----------|-------------|-------------------------|
| **标记 - 清除** | 标记存活对象，清除未标记 | 简单，无需移动对象 | 产生内存碎片，效率低  | CMS 老年代                 |
| **复制**      | 存活对象复制到空闲区域  | 无碎片，高效    | 内存利用率低（50%） | 新生代（Serial、ParNew）      |
| **标记 - 整理** | 存活对象向一端移动    | 无碎片，适合老年代 | 移动开销大，STW 长 | Serial Old、Parallel Old |
| **分代收集**    | 不同年代用不同算法    | 针对性优化，高效  | 复杂，需维护记忆集   | G1、ZGC 等现代收集器           |

**详细对比**：

1. **标记 - 清除**：
    - 第一步：从 GC Roots 遍历标记存活对象
    - 第二步：清除未标记对象
    - 问题：产生大量不连续空间，大对象无法分配

2. **复制**：
    - HotSpot 优化：Eden:Survivor = 8:1:1
    - 每次只用 Eden + 一个 Survivor
    - Minor GC：存活对象复制到另一个 Survivor，年龄 +1

3. **标记 - 整理**：
    - 类似标记 - 清除的标记阶段
    - 整理阶段：对象向一端移动，更新引用地址
    - 清理边界外垃圾

4. **分代收集**：
    - 年轻代：复制算法（对象朝生夕死）
    - 老年代：标记 - 整理（对象长期存活）
    - 需要 Remembered Set 处理跨代引用

---

### 6.2 CMS 和 G1 的区别？如何选择？

**参考答案**：

| 对比项        | CMS             | G1                |
|------------|-----------------|-------------------|
| **引入版本**   | JDK 1.5         | JDK 7u4           |
| **算法**     | 标记 - 清除         | 标记 - 整理 + 复制      |
| **内存结构**   | 物理分代（新生代 + 老年代） | 逻辑分代（Region）      |
| **执行方式**   | 并发标记 + 并发清除     | 并发标记 + STW 筛选     |
| **停顿时间**   | 低（20-100ms）     | 中低（30-150ms），可预测  |
| **内存碎片**   | 有（标记 - 清除）      | 无（Region 整理）      |
| **CPU 敏感** | 高（并发占用线程）       | 中高                |
| **内存占用**   | 中               | 高（Remembered Set） |
| **适用场景**   | Web 应用，B/S 架构   | 大内存（>4GB），服务端     |

**选择建议**：

- **使用 CMS**：
    - JDK 8 及以下版本
    - 应用对延迟敏感
    - 堆内存较小（<4GB）
    - 追求稳定成熟方案

- **使用 G1**：
    - JDK 9+（默认收集器）
    - 大内存场景（>4GB）
    - 需要可预测的停顿时间
    - 不想手动调优参数

**迁移示例**（CMS → G1）：

```bash
# CMS 配置
-XX:+UseConcMarkSweepGC
-XX:+UseParNewGC
-XX:CMSInitiatingOccupancyFraction=75

# 替换为 G1
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45
```

---

### 6.3 ZGC 如何实现超低延迟（<10ms）？

**参考答案**：

ZGC 的核心技术：

1. **染色指针（Colored Pointers）**：
    - 64 位指针：42 位地址 + 4 位标记 + 18 位保留
    - 标记位直接存在指针中，无需额外空间
    - 访问对象时检查指针颜色判断状态

2. **读屏障（Load Barriers）**：
   ```java
   // 伪代码示例
   Object loadObject(ObjectRef ref) {
       Object obj = ref.load();  // 加载对象
       if (isForwardingPointer(obj)) {  // 读屏障
           obj = forwardToNewLocation(obj);  // 重定向
       }
       return obj;
   }
   ```
    - 每次对象访问时检查指针颜色
    - 如需转发，自动重定向到新地址
    - 应用无感知，透明完成

3. **并发整理**：
    - **并发标记**：与应用线程同时执行
    - **并发预备整理**：选择要整理的 Region
    - **并发重映射**：更新引用，释放旧空间

4. **Region 布局**：
    - 小 Region（2MB-32MB）
    - 大 Region（Humongous，连续多个小 Region）
    - 避免大对象分配导致的停顿

**性能特点**：

- 停顿时间 < 10ms（与堆大小无关）
- 支持 TB 级堆内存
- CPU 开销约 15%-30%
- 吞吐量略低于 G1（约 10%-15%）

---

### 6.4 G1 的 Region 有哪些类型？

**参考答案**：

G1 将堆划分为多个固定大小的 Region（默认 2MB-32MB）：

| Region 类型     | 说明                     | 特点               |
|---------------|------------------------|------------------|
| **Eden**      | 新对象分配区域                | 占大多数，快速分配        |
| **Survivor**  | 存放 Minor GC 后存活对象      | 数量较少，对象年龄 +1     |
| **Old**       | 存放长期存活对象               | 对象晋升目标区域         |
| **Humongous** | 存放超大对象（>Region 容量 50%） | 连续多个 Region，避免复制 |

**Region 特点**：

- **动态调整**：Region 角色可在 GC 过程中改变
- **逻辑分代**：物理上不连续，逻辑上属于某一代
- **价值优先**：优先回收垃圾最多的 Region（Garbage First）

**Humongous 对象处理**：

```bash
# 对象大小 > Region 容量 * 50%，视为 Humongous 对象
-XX:G1HeapRegionSize=16m  # Region 大小 16MB
# 则 > 8MB 的对象直接分配到 Humongous Region
```

---

### 6.5 如何选择合适的垃圾回收器？

**参考答案**：

**按应用场景选择**：

| 场景特征         | 推荐收集器          | 理由         |
|--------------|----------------|------------|
| 桌面应用/小程序     | Serial         | 简单高效，无交互开销 |
| Web 应用（中等规模） | G1             | 平衡吞吐和延迟    |
| 大内存服务端（>4GB） | G1/ZGC         | 可扩展性好      |
| 低延迟交易系统      | ZGC/Shenandoah | <10ms 停顿   |
| 批处理/科学计算     | Parallel       | 吞吐量优先      |
| 遗留系统（JDK8）   | CMS/G1         | 稳定成熟       |

**按 JDK 版本选择**：

| JDK 版本  | 默认收集器                            | 推荐方案                  |
|---------|----------------------------------|-----------------------|
| JDK 8   | Parallel Scavenge + Parallel Old | G1（手动切换）              |
| JDK 11  | G1                               | G1 / ZGC（实验性）         |
| JDK 15+ | G1                               | G1 / ZGC（生产级）         |
| JDK 17+ | G1                               | G1 / ZGC / Shenandoah |

**决策流程**：

```
1. 确定 JDK 版本
   ↓
2. 评估堆内存大小
   ↓
3. 分析业务场景（延迟敏感 or 吞吐敏感）
   ↓
4. 选择候选收集器
   ↓
5. 压测对比性能指标
   ↓
6. 确定最终方案
```

---

### 6.6 什么是 Remembered Set？作用是什么？

**参考答案**：

**Remembered Set（记忆集）**：

- 用于记录跨 Region 引用的数据结构；
- 每个 Region 维护一个 RSet，记录"当前 Region 中的对象引用了哪些外部 Region 的对象"；
- 作用：GC 回收某 Region 时，仅扫描其 RSet 即可找到所有跨代/跨 Region 引用，避免全堆扫描，加速 GC 过程。

**示例**：

```
Region A (Old) ──────→ Region B (Eden)
   ↑                        ↑
   └──── RSet 记录 ─────────┘

RSet 内容：
  Region A 中的对象引用了以下 Region 的对象：
    - Region B, offset 0x1234
    - Region C, offset 0x5678
```

**实现方式**：

- **Card Table**：G1 使用，将堆划分为 512 字节卡片
- **Hash Table**：存储引用关系
- **写屏障**：对象引用变化时更新 RSet

---

### 6.7 ZGC 和 Shenandoah 的区别？

**参考答案**：

| 对比项        | ZGC                                     | Shenandoah            |
|------------|-----------------------------------------|-----------------------|
| **开发者**    | Oracle                                  | Red Hat               |
| **引入版本**   | JDK 11（实验）/ JDK 15（正式）                  | JDK 12                |
| **核心技术**   | 染色指针                                    | 转发指针 + Brooks Pointer |
| **指针开销**   | 4 位标记（嵌入 64 位指针）                        | 每个对象额外指针              |
| **读屏障**    | 有                                       | 有                     |
| **兼容性**    | JDK 17+ 支持压缩类指针（-XX:+UseCompressedOops） | 天然支持压缩类指针             |
| **CPU 开销** | 15%-30%                                 | 20%-35%               |
| **吞吐量**    | 比 G1 低 10%-15%                          | 比 G1 低 15%-20%        |
| **停顿时间**   | <10ms                                   | <10ms                 |
| **社区支持**   | Oracle 官方                               | 红帽社区                  |

**选择建议**：

- **ZGC**：Oracle 官方支持，性能略优
- **Shenandoah**：开源免费，兼容性好

---

## 七、实战技巧

### 7.1 GC 日志分析

**G1 GC 日志示例**：

```
[GC pause (G1 Evacuation Pause) (young), 0.0052345 secs]
   [Parallel Time: 4.2 ms, GC Workers: 8]
   [Code Root Fixup: 0.1 ms]
   [Clear CT: 0.2 ms]
   [Other: 0.7 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.3 ms]
      [Weak Ref Processing: 0.1 ms]
   [Eden: 512.0M(512.0M)->0.0B(480.0M) Survivors: 64.0M->96.0M 
    Heap: 1024.0M(4096.0M)->256.5M(4096.0M)]
```

**解读**：

- 停顿时间：5.2ms
- 并行处理：4.2ms（8 个线程）
- Eden 区：回收前总容量 512M、已用 512M（满） → 回收后总容量调整为 480M、已用 0B（全部回收）
- Survivor 区：总容量从 64M 扩容到 96M（G1 动态调整年代大小）
- 堆使用：1024MB → 256.5MB（回收 75%）

### 7.2 GC 问题排查

**问题 1：频繁 Full GC**

```bash
# 查看 GC 日志
jstat -gcutil <pid> 1000

# 如果 FGC 列快速增长
FGC     FGCT
100     50.23   ← Full GC 次数和时间

# 原因分析
# 1. 老年代空间不足
# 2. Metaspace 不足
# 3. System.gc() 调用
```

**解决方案**：

```bash
# 1. 增加堆内存
-Xmx8g -Xms8g

# 2. 调整 Metaspace
-XX:MaxMetaspaceSize=512m

# 3. 禁用显式 GC
-XX:+DisableExplicitGC
```

**问题 2：GC 停顿时间过长**

```bash
# 查看 GC 日志中的停顿时间
grep "Pause Time" gc.log

# 如果停顿超过预期
Pause Time: 500ms  ← 期望 200ms

# 解决方案
# 1. 切换到 G1 或 ZGC
-XX:+UseG1GC

# 2. 调整停顿目标
-XX:MaxGCPauseMillis=100

# 3. 减少堆内存
-Xmx4g
```

---

## 八、下一步学习

- ⬅️ **[JVM内存模型](./01-JVM内存模型详解.md)** - 运行时数据区
- 📚 **[JVM 调优实战](../05-SpringBoot与自动装配/README.md)** - 性能优化
- 🔗 **[内存泄漏排查](../14-JavaIO模型/README.md)** - 实战技巧

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
