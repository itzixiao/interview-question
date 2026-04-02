# CountDownLatch 线程顺序执行详解

## 概述

CountDownLatch 是 Java 并发包中的一个同步辅助类，它允许一个或多个线程等待，直到在其他线程中执行的一组操作完成。

## 核心原理

```
┌─────────────────────────────────────────────────────────────┐
│                      CountDownLatch 原理                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   初始化: new CountDownLatch(N)                             │
│          设置计数器为 N                                     │
│                                                             │
│   等待:  await()                                            │
│          计数器 > 0 时阻塞                                  │
│                                                             │
│   计数:  countDown()                                        │
│          计数器减 1                                         │
│                                                             │
│   唤醒:  计数器 = 0 时                                      │
│          唤醒所有等待线程                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 线程顺序执行实现

### ABC 顺序执行方案

使用两个 CountDownLatch 实现 A → B → C 顺序：

```
CountDownLatch latchAB = new CountDownLatch(1);  // A -> B
CountDownLatch latchBC = new CountDownLatch(1);  // B -> C

线程A: 执行操作 -> latchAB.countDown() 
                        ↓
线程B: latchAB.await() -> 执行操作 -> latchBC.countDown()
                                          ↓
线程C: latchBC.await() -> 执行操作
```

### 代码实现

```java
public class SequentialExecution {
    public static void main(String[] args) {
        CountDownLatch latchAB = new CountDownLatch(1);
        CountDownLatch latchBC = new CountDownLatch(1);

        // 线程 A
        new Thread(() -> {
            System.out.println("A 执行");
            latchAB.countDown();  // 唤醒 B
        }).start();

        // 线程 B
        new Thread(() -> {
            latchAB.await();      // 等待 A
            System.out.println("B 执行");
            latchBC.countDown();  // 唤醒 C
        }).start();

        // 线程 C
        new Thread(() -> {
            latchBC.await();      // 等待 B
            System.out.println("C 执行");
        }).start();
    }
}
```

## 核心 API

| 方法                                   | 说明             |
|--------------------------------------|----------------|
| `CountDownLatch(int count)`          | 构造函数，指定计数器初始值  |
| `await()`                            | 等待计数器归零（无限期阻塞） |
| `await(long timeout, TimeUnit unit)` | 等待指定时间         |
| `countDown()`                        | 计数器减 1         |
| `getCount()`                         | 获取当前计数器值       |

## 典型应用场景

### 1. 系统启动顺序

```java
public class SystemStartup {
    public static void main(String[] args) {
        CountDownLatch dbReady = new CountDownLatch(1);
        CountDownLatch cacheReady = new CountDownLatch(1);

        // 1. 初始化数据库
        new Thread(() -> {
            initDatabase();
            dbReady.countDown();
        }).start();

        // 2. 初始化缓存（依赖数据库）
        new Thread(() -> {
            dbReady.await();
            initCache();
            cacheReady.countDown();
        }).start();

        // 3. 加载业务数据（依赖缓存）
        new Thread(() -> {
            cacheReady.await();
            loadBusinessData();
        }).start();
    }
}
```

### 2. 多阶段任务

```java
public class MultiStageTask {
    public static void main(String[] args) {
        // 阶段1：3个并行任务
        CountDownLatch stage1 = new CountDownLatch(3);
        // 阶段2：2个并行任务（依赖阶段1）
        CountDownLatch stage2 = new CountDownLatch(2);

        // 阶段1任务
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                doTask();
                stage1.countDown();
            }).start();
        }

        // 阶段2任务
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                stage1.await();  // 等待阶段1完成
                doTask();
                stage2.countDown();
            }).start();
        }
    }
}
```

### 3. 主线程等待子线程

```java
public class MainWaitForWorkers {
    public static void main(String[] args) {
        int workerCount = 5;
        CountDownLatch latch = new CountDownLatch(workerCount);

        for (int i = 0; i < workerCount; i++) {
            new Thread(() -> {
                doWork();
                latch.countDown();
            }).start();
        }

        latch.await();  // 主线程等待所有工作线程完成
        System.out.println("所有任务完成");
    }
}
```

## 注意事项

### 1. 计数器不能重置

```java
// CountDownLatch 是一次性的
CountDownLatch latch = new CountDownLatch(1);
latch.countDown();
latch.await();  // 立即通过

// 不能重置，再次使用需要创建新的
// latch = new CountDownLatch(1);  // 需要重新创建
```

### 2. 避免死锁

```java
// 错误：countDown() 在 await() 之后，且在同一线程
CountDownLatch latch = new CountDownLatch(1);

new Thread(() ->{
        latch.await();      // 永远等待
        latch.countDown();  // 永远不会执行
}).

start();
```

### 3. 异常处理

```java
CountDownLatch latch = new CountDownLatch(1);

try{
        latch.await();
}catch(
InterruptedException e){
        // 恢复中断状态
        Thread.currentThread().interrupt();
// 或者处理中断
}
```

## 与其他同步工具对比

| 工具                 | 特点             | 适用场景       |
|--------------------|----------------|------------|
| **CountDownLatch** | 一次性，计数器归零后不能重置 | 任务分组、多阶段任务 |
| **CyclicBarrier**  | 可循环使用，线程互相等待   | 分阶段计算、并行迭代 |
| **Semaphore**      | 控制同时访问的线程数量    | 资源池、限流     |
| **Phaser**         | 更灵活的分阶段控制      | 复杂多阶段任务    |

## 面试常见问题

**问题 1:CountDownLatch 和 CyclicBarrier 的区别？**

**A**:

- **CountDownLatch**：一次性，一个或多个线程等待其他线程完成
- **CyclicBarrier**：可循环，多个线程互相等待，同时到达屏障

**问题 2：为什么 CountDownLatch 不能重置？**

**A**: CountDownLatch 设计为一次性使用，计数器归零后状态不可改变。如果需要重复使用，应该使用 CyclicBarrier 或重新创建
CountDownLatch 实例。

**问题 3：如何实现多个线程的顺序执行？**

**A**: 使用多个 CountDownLatch 串联：

```java
CountDownLatch[] latches = new CountDownLatch[n - 1];
for(int i = 0;i<n-1;i++){
latches[i]=new CountDownLatch(1);
}

// 线程 i 等待 latches[i-1]，完成后调用 latches[i].countDown()
```

**问题 4:CountDownLatch 的计数器为 0 时调用 await() 会怎样？**

**A**: 立即返回，不会阻塞。

**问题 5：如果某个线程没有调用 countDown() 会怎样？**

**A**: 等待的线程会一直阻塞，可能导致死锁。应该确保 countDown() 在 finally 块中调用，或使用 try-with-resources 模式。

## 最佳实践

### 1. 确保 countDown() 被执行

```java
CountDownLatch latch = new CountDownLatch(1);

try{
doWork();
}finally{
        latch.countDown();  // 确保一定会执行
}
```

### 2. 使用超时等待

```java
// 避免永久阻塞
boolean completed = latch.await(10, TimeUnit.SECONDS);
if(!completed){
        // 处理超时
        }
```

### 3. 合理设置计数器

```java
// 根据实际任务数量设置
int taskCount = tasks.size();
CountDownLatch latch = new CountDownLatch(taskCount);
```

### 4. 结合线程池使用

```java
ExecutorService executor = Executors.newFixedThreadPool(4);
CountDownLatch latch = new CountDownLatch(tasks.size());

for(Task task :tasks){
        executor.submit(() ->{
        try{
            task.execute();
        }finally{
            latch.countDown();
        }
        });
        }
    latch.await();
    executor.shutdown();
```

---

## 实战案例：多线程并行下载

### 场景描述

需要并发下载大量文件，要求：
- 使用线程池控制并发数
- 等待所有下载任务完成
- 实时统计成功/失败数量
- 支持超时控制

### 完整实现

```java
protected void parallelDownloadFiles(List<DownloadTask> tasks, int timeoutMinutes) {
    // 1. 创建 CountDownLatch，计数器 = 任务数量
    CountDownLatch latch = new CountDownLatch(tasks.size());
    
    // 2. 使用原子类统计结果
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);
    
    // 3. 使用线程安全列表收集结果
    List<String> results = Collections.synchronizedList(new ArrayList<>());

    // 4. 提交所有下载任务到线程池
    for (DownloadTask downloadTask : tasks) {
        downloadExecutor.submit(() -> {
            try {
                String downloaded = downloadFile(downloadTask.url, ...);
                if (downloaded != null) {
                    results.add(downloaded);
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                // 5. 关键：必须在 finally 中 countDown()
                latch.countDown();
            }
        });
    }

    // 6. 等待所有下载完成（带超时）
    try {
        latch.await(timeoutMinutes, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

### 关键技术点

| 技术 | 作用 | 注意事项 |
|-----|------|---------|
| `CountDownLatch` | 等待所有线程完成 | 必须在 finally 中 countDown() |
| `AtomicInteger` | 线程安全计数 | 比 volatile 更安全 |
| `synchronizedList` | 线程安全列表 | 迭代时仍需同步 |
| `await(timeout, unit)` | 超时等待 | 防止永久阻塞 |

### 常见问题

**Q: 为什么 countDown() 要放在 finally 中？**

A: 防止任务执行异常时无法减少计数器，导致主线程永远等待。

```java
try {
    doWork();  // 可能抛出异常
} finally {
    latch.countDown();  // 确保一定会执行
}
```

**Q: 为什么使用 AtomicInteger 而不是 volatile int？**

A: `count++` 不是原子操作（读取→修改→写入），多线程下会丢失更新。AtomicInteger 使用 CAS 保证原子性。

**Q: 为什么用 synchronized 包裹两个 AtomicInteger 的读取？**

A: 虽然单个读取是原子的，但**连续两个读取**需要保证一致性，防止读到中间状态。
