# Java 并发编程核心知识点详解

## 一、线程基础

### 1.1 创建线程的四种方式

#### 方式一：继承 Thread 类
```java
Thread thread = new Thread() {
    @Override
    public void run() {
        System.out.println("子线程执行");
    }
};
thread.start();
```

#### 方式二：实现 Runnable 接口
```java
Thread thread = new Thread(() -> 
    System.out.println("子线程执行")
);
thread.start();
```

#### 方式三：实现 Callable 接口（有返回值）
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<String> future = executor.submit(() -> {
    return "Callable 返回结果";
});
String result = future.get();
executor.shutdown();
```

#### 方式四：使用线程池（推荐）
```java
ExecutorService threadPool = Executors.newFixedThreadPool(2);
threadPool.submit(() -> 
    System.out.println("线程池执行")
);
threadPool.shutdown();
```

### 1.2 线程状态

| 状态 | 说明 |
|------|------|
| NEW | 新建状态，未启动 |
| RUNNABLE | 可运行状态（包括就绪和运行中） |
| BLOCKED | 阻塞状态，等待获取锁 |
| WAITING | 无限期等待，等待其他线程唤醒 |
| TIMED_WAITING | 限期等待，指定等待时间 |
| TERMINATED | 终止状态，线程执行完成 |

### 1.3 常用方法

```java
start()          // 启动线程
run()            // 线程体（直接调用只是普通方法）
sleep(ms)        // 线程休眠，不释放锁
wait()           // 对象等待，释放锁
notify()         // 唤醒一个等待线程
notifyAll()      // 唤醒所有等待线程
join()           // 等待线程终止
yield()          // 让出 CPU 时间片
interrupt()      // 中断线程
isAlive()        // 判断线程是否存活
```

---

## 二、JMM 与 volatile 关键字

### 2.1 JMM 三大特性

#### 1. 原子性（Atomicity）
- 操作不可分割，要么全部成功，要么全部失败
- synchronized、Lock 保证原子性

#### 2. 可见性（Visibility）
- 一个线程修改共享变量，其他线程立即可见
- volatile、synchronized、Lock 保证可见性

#### 3. 有序性（Ordering）
- 程序执行顺序按照代码先后顺序
- volatile 禁止指令重排序，happens-before 原则

### 2.2 volatile 关键字

#### 作用
1. **保证可见性**：一个线程修改，其他线程立即可见
2. **禁止指令重排序**：happens-before 原则
3. **不保证原子性**：i++ 操作仍需要同步

#### 应用场景
- 状态标记量（如：`volatile boolean flag`）
- 单例模式双重检查锁定（DCL）
- 多任务完成标志

#### 示例
```java
private volatile int value = 0;

// 线程 1 修改
writerThread: value = 100;

// 线程 2 立即看到
readerThread: while (value == 0) { }
System.out.println(value); // 100
```

---

## 三、死锁

### 3.1 死锁产生的四个必要条件

1. **互斥条件**：资源一次只能被一个线程占用
2. **请求与保持**：已持有资源，又申请新资源
3. **不剥夺**：已获得的资源不能被强制剥夺
4. **循环等待**：多个线程形成头尾相接的循环链

### 3.2 死锁示例

```java
Object lock1 = new Object();
Object lock2 = new Object();

// T1: 先拿 lock1，再拿 lock2
Thread t1 = new Thread(() -> {
    synchronized (lock1) {
        synchronized (lock2) {
            // 业务逻辑
        }
    }
});

// T2: 先拿 lock2，再拿 lock1
Thread t2 = new Thread(() -> {
    synchronized (lock2) {
        synchronized (lock1) {
            // 业务逻辑
        }
    }
});
```

### 3.3 死锁预防策略

1. **破坏请求与保持**：一次性申请所有资源
2. **破坏不剥夺**：申请不到时主动释放已持有资源
3. **破坏循环等待**：按顺序申请资源（如：都先申请 lock1）
4. **使用定时锁**：tryLock(timeout) 超时放弃

### 3.4 查看死锁方法

1. `jps -l` 查看进程 ID
2. `jstack <pid>` 查看线程堆栈
3. 寻找 `Found one Java-level deadlock` 提示

---

## 四、synchronized 关键字

### 4.1 synchronized 三种用法

#### 用法一：修饰实例方法（锁当前对象实例）
```java
public synchronized void method() {
    // 业务逻辑
}
```

#### 用法二：修饰静态方法（锁当前类的 Class 对象）
```java
public static synchronized void staticMethod() {
    // 业务逻辑
}
```

#### 用法三：修饰代码块（锁指定对象）
```java
public void method() {
    synchronized (this) {
        // 业务逻辑
    }
}
```

### 4.2 synchronized 原理

JVM 基于进入和退出 Monitor 对象来实现：

- **方法同步**：ACC_SYNCHRONIZED 标志位
- **代码块同步**：monitorenter/monitorexit 指令

### 4.3 synchronized 升级过程（JDK 1.6+）

```
无锁 → 偏向锁 → 轻量级锁 → 重量级锁
```

#### 偏向锁
- 只有一个线程访问，无竞争
- 偏向第一个获取它的线程

#### 轻量级锁（自旋锁）
- 有竞争但时间短
- CAS 自旋，不立即阻塞

#### 重量级锁
- 竞争激烈，自旋失败
- 依赖底层 mutex lock 实现，线程阻塞

---

## 五、ReentrantLock

### 5.1 ReentrantLock 基本使用

```java
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock(); // 必须在 finally 中释放
}
```

### 5.2 ReentrantLock vs synchronized

#### ReentrantLock 特性
1. **可中断**：lockInterruptibly() 响应中断
2. **可超时**：tryLock(timeout) 超时放弃
3. **公平锁**：可设置公平策略（默认非公平）
4. **多条件变量**：newCondition() 支持多个 Condition
5. **手动加锁释放**：lock()/unlock() 必须在 finally 中释放

### 5.3 AQS 原理

AbstractQueuedSynchronizer（AQS）是 Lock 的核心实现框架：

#### 核心组件
- **state**：同步状态（volatile int）
- **CLH 队列**：双向链表，存储等待线程

#### 工作流程
1. 尝试获取锁（CAS 修改 state）
2. 失败则加入队列尾部
3. 被唤醒后再次尝试获取

---

## 六、ReentrantReadWriteLock

### 6.1 读写锁特性

- **读锁（共享锁）**：多个线程可同时读
- **写锁（排他锁）**：写时独占，其他读写都阻塞
- **适用场景**：读多写少

### 6.2 使用示例

```java
ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

// 读
lock.readLock().lock();
try {
    return data;
} finally {
    lock.readLock().unlock();
}

// 写
lock.writeLock().lock();
try {
    this.data = newData;
} finally {
    lock.writeLock().unlock();
}
```

### 6.3 锁降级

ReentrantReadWriteLock 支持锁降级（写锁→读锁），不支持锁升级

---

## 七、乐观锁与悲观锁

### 7.1 概念对比

#### 悲观锁（Pessimistic Lock）
- 假设最坏情况，每次操作都认为会被修改
- 实现：synchronized、ReentrantLock
- 适用：写多读少，竞争激烈

#### 乐观锁（Optimistic Lock）
- 假设最好情况，每次操作都认为不会被修改
- 实现：CAS（Compare And Swap）
- 适用：读多写少，竞争不激烈

### 7.2 CAS 原理

CAS(V, A, B)：
- V: 内存中的值
- A: 预期原值
- B: 新值

流程：
1. 比较 V 和 A 是否相等
2. 相等则将 V 更新为 B
3. 不相等则重试（自旋）

### 7.3 AtomicInteger 示例

```java
AtomicInteger atomicInt = new AtomicInteger(0);
atomicInt.incrementAndGet(); // 原子自增
```

### 7.4 CAS 问题

#### 1. ABA 问题
值从 A→B→A，CAS 认为没变
解决：AtomicStampedReference（添加版本号）

#### 2. 自旋时间长
竞争激烈时长时间自旋浪费 CPU
解决：自旋次数限制

#### 3. 只能保证单个变量原子性
解决：锁

---

## 八、ThreadLocal

### 8.1 ThreadLocal 原理

每个 Thread 内部维护 ThreadLocalMap：
- Map<ThreadLocal, Object> 存储线程私有数据
- get() 流程：
  1. 获取当前线程
  2. 获取线程内部的 ThreadLocalMap
  3. 以 ThreadLocal 为 key 获取 value

### 8.2 使用示例

```java
ThreadLocal<String> threadLocal = ThreadLocal.withInitial(() -> "default");

threadLocal.set("Thread-1 的值");
String value = threadLocal.get();
threadLocal.remove(); // 防止内存泄漏
```

### 8.3 应用场景

1. 数据库连接管理（每个线程独立 Connection）
2. Session 管理（Web 请求链路传递用户信息）
3. SimpleDateFormat 线程安全封装
4. MDC 日志追踪（TraceID 传递）

### 8.4 内存泄漏问题

ThreadLocalMap 的 key 是弱引用，value 是强引用
如果 ThreadLocal 被回收，key 变为 null，但 value 仍存在

**解决方案**：使用完必须调用 remove() 清理！

---

## 九、线程池

### 9.1 ThreadPoolExecutor 七大参数

1. **corePoolSize**：核心线程数（最小线程数）
2. **maximumPoolSize**：最大线程数
3. **keepAliveTime**：空闲线程存活时间
4. **unit**：时间单位
5. **workQueue**：任务队列（BlockingQueue）
6. **threadFactory**：线程工厂（自定义线程名）
7. **handler**：拒绝策略

### 9.2 工作流程

1. 提交任务 → 核心线程未满 → 创建核心线程执行
2. 核心线程已满 → 加入任务队列等待
3. 队列已满 → 创建非核心线程（不超过 max）
4. 线程数达上限 → 触发拒绝策略

### 9.3 四种拒绝策略

| 策略 | 说明 |
|------|------|
| AbortPolicy（默认） | 抛出异常 |
| CallerRunsPolicy | 调用者线程执行 |
| DiscardPolicy | 直接丢弃 |
| DiscardOldestPolicy | 丢弃最老任务 |

### 9.4 正确使用线程池

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                              // 核心线程数
    5,                              // 最大线程数
    60L,                            // 空闲存活时间
    TimeUnit.SECONDS,               // 时间单位
    new ArrayBlockingQueue<>(10),   // 有界队列
    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
);
```

### 9.5 不推荐使用 Executors 创建

原因：
1. FixedThreadPool/SingleThreadPool: 队列长度 Integer.MAX_VALUE，可能 OOM
2. CachedThreadPool: 允许线程数 Integer.MAX_VALUE，可能 OOM

**建议**：使用 ThreadPoolExecutor 构造函数显式指定参数

---

## 十、Future 与 CompletableFuture

### 10.1 Future 局限性

- get() 阻塞，无法异步回调
- 无法组合多个任务

### 10.2 CompletableFuture 强大功能

#### 异步执行
```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "异步结果";
});
```

#### 链式调用
```java
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenAccept(System.out::println)
    .thenRun(() -> System.out.println("执行完成"));
```

#### 组合多个任务
```java
CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> 10);
CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> 20);

// 组合
CompletableFuture<Integer> combined = future1.thenCombine(future2, (a, b) -> a + b);

// 任意一个完成
CompletableFuture<Object> anyOf = CompletableFuture.anyOf(future1, future2);

// 所有都完成
CompletableFuture<Void> allOf = CompletableFuture.allOf(future1, future2);
```

### 10.3 常用方法

| 方法 | 说明 |
|------|------|
| supplyAsync | 异步执行有返回值 |
| runAsync | 异步执行无返回值 |
| thenApply | 转换结果 |
| thenAccept | 消费结果 |
| thenRun | 执行动作 |
| thenCombine | 组合两个 Future |
| thenCompose | 扁平化组合 |
| allOf/anyOf | 所有/任意一个完成 |
| exceptionally | 异常处理 |
| whenComplete | 完成回调（无论成功失败） |

---

## 十一、虚拟线程（Project Loom）

### 11.1 虚拟线程 vs 平台线程

#### 平台线程（Platform Thread）
- 一对一映射到操作系统内核线程
- 重量级，栈内存 1MB，创建成本高
- 数量有限（几千个）

#### 虚拟线程（Virtual Thread）
- JVM 调度的轻量级线程，多对一映射到平台线程
- 轻量级，栈内存动态扩展，创建成本极低
- 数量巨大（百万级）

### 11.2 创建虚拟线程（JDK 21+）

#### 方式一：直接启动
```java
Thread virtualThread = Thread.ofVirtual().start(() -> {
    System.out.println("虚拟线程");
});
```

#### 方式二：工厂创建
```java
ThreadFactory factory = Thread.ofVirtual().factory();
Thread thread = factory.newThread(() -> {
    System.out.println("工厂创建的虚拟线程");
});
```

#### 方式三：ExecutorService
```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        System.out.println("线程池中的虚拟线程");
    });
}
```

### 11.3 适用场景

✅ 适合：IO 密集型任务（高并发网络服务）
❌ 不适合：CPU 密集型任务（计算密集）

### 11.4 优势

1. 简化并发编程模型（一个请求一个线程）
2. 提高吞吐量（减少上下文切换）
3. 降低资源消耗（栈内存小）
4. 便于调试（保留堆栈信息）

---

## 十二、高频面试题汇总

### 线程基础面试题

#### 【问题 1】创建线程的方式有哪些？
答：
1. 继承 Thread 类
2. 实现 Runnable 接口
3. 实现 Callable 接口（有返回值）
4. 使用线程池（推荐）

#### 【问题 2】sleep() 和 wait() 的区别？
答：
- 归属不同：sleep() 属于 Thread，wait() 属于 Object
- 锁释放：sleep() 不释放锁，wait() 释放锁
- 使用范围：sleep() 任何地方，wait() 必须在同步块中
- 唤醒方式：sleep() 时间到自动醒，wait() 需要 notify()

#### 【问题 3】run() 和 start() 的区别？
答：
- start(): 启动新线程，JVM 调用 run() 方法
- run(): 普通方法调用，在当前线程执行

#### 【问题 4】线程的生命周期（状态）？
答：NEW → RUNNABLE → BLOCKED → WAITING → TIMED_WAITING → TERMINATED

---

### JMM 与 volatile 面试题

#### 【问题 5】Java 内存模型（JMM）是什么？
答：
JMM 定义了线程和主内存之间的抽象关系：
- 主内存：所有变量存储在主内存
- 工作内存：每个线程有自己的工作内存
- 线程不能直接访问主内存，需通过工作内存

#### 【问题 6】volatile 关键字的作用？
答：
1. 保证可见性：一个线程修改，其他线程立即可见
2. 禁止指令重排序：happens-before 原则
3. 不保证原子性：i++ 操作仍需要同步

应用场景：状态标记、单例 DCL、多任务完成标志

#### 【问题 7】什么是原子性、可见性、有序性？
答：
- 原子性：操作不可分割，要么全成功要么全失败
- 可见性：一个线程修改，其他线程立即可见
- 有序性：程序按代码顺序执行，禁止重排序

---

### synchronized 面试题

#### 【问题 8】synchronized 的原理？
答：
- 基于 Monitor 对象（对象头中的锁记录）
- 方法同步：ACC_SYNCHRONIZED 标志位
- 代码块同步：monitorenter/monitorexit 指令

#### 【问题 9】synchronized 锁升级过程？
答：无锁 → 偏向锁 → 轻量级锁（自旋） → 重量级锁（阻塞）

#### 【问题 10】synchronized 和 ReentrantLock 的区别？
答：

相同点：都可重入、互斥、保证可见性

不同点：
- synchronized：JVM 层面，自动加锁释放，不支持公平锁
- ReentrantLock：API 层面，手动加锁释放，支持公平锁、可中断、多条件变量

---

### 锁机制面试题

#### 【问题 11】乐观锁和悲观锁的区别？
答：
- 悲观锁：假设最坏情况，每次操作都认为会被修改（synchronized、ReentrantLock）
- 乐观锁：假设最好情况，每次操作都认为不会被修改（CAS）
- 适用场景：悲观锁写多读少，乐观锁读多写少

#### 【问题 12】CAS 是什么？有什么问题？
答：
CAS(Compare And Swap)：比较并交换，包含三个操作数 V(内存值)、A(预期值)、B(新值)

问题：
1. ABA 问题：添加版本号解决（AtomicStampedReference）
2. 自旋时间长：竞争激烈时浪费 CPU
3. 只能保证单个变量原子性

#### 【问题 13】ReentrantReadWriteLock 的应用场景？
答：读多写少的场景，如缓存系统

---

### ThreadLocal 面试题

#### 【问题 14】ThreadLocal 的原理？
答：
- 每个 Thread 维护 ThreadLocalMap
- 以 ThreadLocal 为 key，存储线程私有数据
- get() 时从当前线程的 Map 中获取

#### 【问题 15】ThreadLocal 内存泄漏问题？
答：
- ThreadLocalMap 的 key 是弱引用，value 是强引用
- ThreadLocal 被回收后，key 为 null，value 仍存在
- 解决：使用完必须调用 remove()

---

### 线程池面试题

#### 【问题 16】线程池的七个参数？
答：
1. corePoolSize：核心线程数
2. maximumPoolSize：最大线程数
3. keepAliveTime：空闲存活时间
4. unit：时间单位
5. workQueue：任务队列
6. threadFactory：线程工厂
7. handler：拒绝策略

#### 【问题 17】线程池的工作流程？
答：
1. 核心线程未满 → 创建核心线程
2. 核心已满 → 加入队列
3. 队列已满 → 创建非核心线程
4. 线程达上限 → 触发拒绝策略

#### 【问题 18】有哪些拒绝策略？
答：
1. AbortPolicy：抛异常（默认）
2. CallerRunsPolicy：调用者执行
3. DiscardPolicy：直接丢弃
4. DiscardOldestPolicy：丢弃最老任务

#### 【问题 19】为什么不推荐使用 Executors 创建线程池？
答：
1. FixedThreadPool：队列长度 Integer.MAX_VALUE，可能 OOM
2. CachedThreadPool：允许线程数 Integer.MAX_VALUE，可能 OOM
建议：使用 ThreadPoolExecutor 显式指定参数

---

### Future 与虚拟线程面试题

#### 【问题 20】CompletableFuture 的优势？
答：
1. 异步非阻塞
2. 链式调用
3. 组合多个任务
4. 异常处理

#### 【问题 21】虚拟线程和平台线程的区别？
答：
- 平台线程：一对一映射内核线程，重量级，栈内存 1MB
- 虚拟线程：JVM 调度轻量级线程，多对一映射，栈内存动态，百万级并发

---

## 总结

本文详细介绍了 Java 并发编程的核心知识点：

1. **线程基础**：创建方式、生命周期、常用方法
2. **JMM**：原子性、可见性、有序性
3. **volatile**：可见性、禁止重排序
4. **死锁**：产生条件、预防策略
5. **synchronized**：三种用法、锁升级
6. **ReentrantLock**：AQS 原理、vs synchronized
7. **ReentrantReadWriteLock**：读写分离
8. **乐观锁/悲观锁**：CAS 原理
9. **ThreadLocal**：线程私有数据
10. **线程池**：七大参数、工作流程
11. **Future**：异步回调、组合任务
12. **虚拟线程**：轻量级并发

每个部分都配有高频面试题及参考答案，帮助理解和应对面试。
