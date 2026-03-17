# JVM调优实战与故障排查

## 概述

JVM调优是高级Java工程师的核心技能，涉及GC日志分析、内存泄漏排查、线上问题诊断等实战能力。

---

## 一、JVM参数配置详解

### 1.1 常用参数分类

| 参数类型      | 示例                                | 说明        |
|-----------|-----------------------------------|-----------|
| **堆内存**   | `-Xms4g -Xmx4g`                   | 初始/最大堆内存  |
| **新生代**   | `-Xmn1g`                          | 新生代大小     |
| **元空间**   | `-XX:MetaspaceSize=256m`          | 元空间初始大小   |
| **GC收集器** | `-XX:+UseG1GC`                    | 使用G1收集器   |
| **GC日志**  | `-Xloggc:gc.log`                  | GC日志文件路径  |
| **OOM处理** | `-XX:+HeapDumpOnOutOfMemoryError` | OOM时生成堆转储 |

### 1.2 生产环境推荐配置

```bash
# 8G内存服务器推荐配置
java -Xms4g -Xmx4g \
     -Xmn1g \
     -XX:MetaspaceSize=256m \
     -XX:MaxMetaspaceSize=512m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/heapdump.hprof \
     -Xloggc:/var/log/gc.log \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -jar application.jar
```

---

## 二、GC日志分析

### 2.1 GC日志解读

```
[2024-03-15T10:23:45.123+0800] GC(1234) Pause Young (Normal) (G1 Evacuation Pause)
[2024-03-15T10:23:45.124+0800] GC(1234) Using 8 workers of 8 for evacuation
[2024-03-15T10:23:45.125+0800] GC(1234) Pre Evacuate Collection Set: 0.1ms
[2024-03-15T10:23:45.126+0800] GC(1234) Evacuate Collection Set: 45.2ms
[2024-03-15T10:23:45.127+0800] GC(1234) Post Evacuate Collection Set: 0.3ms
[2024-03-15T10:23:45.128+0800] GC(1234) Other: 0.5ms
[2024-03-15T10:23:45.129+0800] GC(1234) Eden regions: 24->0(24)
[2024-03-15T10:23:45.130+0800] GC(1234) Survivor regions: 3->4(4)
[2024-03-15T10:23:45.131+0800] GC(1234) Old regions: 45->46
[2024-03-15T10:23:45.132+0800] GC(1234) Humongous regions: 0->0
[2024-03-15T10:23:45.133+0800] GC(1234) Metaspace: 125M->125M(256M)
[2024-03-15T10:23:45.134+0800] GC(1234) Pause Young (Normal) (G1 Evacuation Pause) 120M->90M(512M) 46.234ms
```

**关键指标：**

| 指标       | 说明                 | 健康范围             |
|----------|--------------------|------------------|
| **GC次数** | Young GC/Full GC频率 | Young GC < 10次/秒 |
| **GC耗时** | 单次GC停顿时间           | < 200ms          |
| **内存回收** | 回收前后内存变化           | 有效回收             |
| **GC原因** | 触发GC的原因            | 关注System.gc()    |

### 2.2 GC分析工具

| 工具            | 用途        | 命令                            |
|---------------|-----------|-------------------------------|
| **GCViewer**  | 可视化GC日志分析 | java -jar gcviewer.jar gc.log |
| **gceasy.io** | 在线GC分析    | 上传gc.log                      |
| **jstat**     | 实时监控GC    | jstat -gcutil PID 1000        |

---

## 三、Arthas诊断工具

### 3.1 Arthas安装与启动

```bash
# 下载
wget https://arthas.aliyun.com/arthas-boot.jar

# 启动
java -jar arthas-boot.jar

# 选择要诊断的Java进程
```

### 3.2 常用命令

| 命令            | 用途         | 示例                                                      |
|---------------|------------|---------------------------------------------------------|
| **dashboard** | 系统实时面板     | 显示线程、内存、GC信息                                            |
| **thread**    | 线程分析       | thread -n 10 查看CPU占用前10                                 |
| **jvm**       | JVM信息      | 显示JVM配置、类加载、GC等                                         |
| **trace**     | 方法耗时       | trace com.example.Service getOrder                      |
| **watch**     | 观察方法入参/返回值 | watch com.example.Service getOrder '{params,returnObj}' |
| **heapdump**  | 生成堆转储      | heapdump /tmp/dump.hprof                                |
| **jad**       | 反编译类       | jad com.example.Service                                 |

### 3.3 实战案例：定位CPU飙高

```bash
# 1. 查看dashboard，找到CPU高的线程
dashboard

# 2. 查看线程详情
thread PID

# 3. 查看线程堆栈
thread -n 3

# 4. 追踪可疑方法
trace com.example.OrderService processOrder '#cost>1000'
```

---

## 四、内存泄漏排查

### 4.1 内存泄漏特征

- 堆内存持续增长，GC无法回收
- Full GC频繁，但内存不下降
- 最终出现OutOfMemoryError

### 4.2 排查步骤

```
1. 生成堆转储文件
   → jmap -dump:format=b,file=heap.hprof PID
   
2. 使用MAT分析
   → 打开heap.hprof
   → 查看Dominator Tree
   → 查找大对象和引用链
   
3. 定位泄漏源
   → 查看Path to GC Roots
   → 分析代码逻辑
```

### 4.3 常见内存泄漏场景

| 场景              | 原因               | 解决方案               |
|-----------------|------------------|--------------------|
| **静态集合**        | 静态Map/List持有对象引用 | 使用WeakReference    |
| **未关闭资源**       | 数据库连接、流未关闭       | try-with-resources |
| **监听器未移除**      | 事件监听器持有对象        | 及时removeListener   |
| **ThreadLocal** | 线程池场景下未remove    | finally中remove     |
| **缓存无限制**       | Guava Cache未设置过期 | 设置expireAfterWrite |

---

## 五、线上故障排查实战

### 5.1 故障排查流程

```
┌─────────────┐
│  发现问题    │ ← 监控告警/用户反馈
└──────┬──────┘
       ↓
┌─────────────┐
│  收集信息    │ ← 日志、指标、线程dump
└──────┬──────┘
       ↓
┌─────────────┐
│  定位问题    │ ← Arthas、MAT、GC分析
└──────┬──────┘
       ↓
┌─────────────┐
│  解决问题    │ ← 代码修复、配置调整
└──────┬──────┘
       ↓
┌─────────────┐
│  验证复盘    │ ← 验证修复、总结归档
└─────────────┘
```

### 5.2 高频面试题

**问题 1：如何排查线上Full GC频繁问题？**

**答：**

排查步骤：

1. **查看GC日志**，确认Full GC频率和耗时
2. **分析GC原因**，关注`System.gc()`调用
3. **检查内存使用**，确认老年代增长趋势
4. **排查内存泄漏**，使用MAT分析堆转储
5. **优化方案**：
    - 调整堆内存大小
    - 优化对象生命周期
    - 更换GC收集器（如G1）

---

**问题 2：Arthas的trace和watch命令有什么区别？**

**答：**

| 命令        | 用途         | 输出         |
|-----------|------------|------------|
| **trace** | 追踪方法调用链路   | 方法耗时、调用次数  |
| **watch** | 观察方法入参和返回值 | 参数值、返回值、异常 |

**使用场景：**

- trace：性能分析，找耗时瓶颈
- watch：调试问题，查看数据流转

---

**问题 3：如何使用MAT定位内存泄漏？**

**答：**

1. **生成堆转储**：`jmap -dump:format=b,file=heap.hprof PID`
2. **打开MAT**，导入hprof文件
3. **运行Leak Suspects Report**，查看可疑点
4. **查看Dominator Tree**，找到大对象
5. **Path to GC Roots**，查看引用链
6. **定位代码**，修复泄漏源

---

**问题 4：JVM调优的目标和原则是什么？**

**答：**

**调优目标：**

1. **低延迟**：GC停顿时间 < 200ms
2. **高吞吐**：GC时间占比 < 5%
3. **稳定性**：避免OOM和频繁Full GC

**调优原则：**

1. **不要过早优化**：先监控，再优化
2. **一次只改一个参数**：便于对比效果
3. **压测验证**：生产环境前充分测试
4. **监控为王**：持续监控GC指标

---

## 六、相关代码示例

完整代码示例请参考：

- `interview-service/src/main/java/cn/itzixiao/interview/jvm/`
