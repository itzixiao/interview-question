# JVM 知识点详解

## 📚 文档列表

#### 1. [01-JVM内存模型详解.md](./01-JVM内存模型详解.md)

- **内容：** 运行时数据区、堆内存、元空间、虚拟机栈
- **面试题：** 5+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 2. [02-JVM垃圾回收详解.md](./02-JVM垃圾回收详解.md) ⭐新增

- **内容：** GC 算法、收集器对比、调优实战、高频面试题
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 3. [03-Mark-Word详解.md](03-Mark-Word详解.md) ⭐新增

- **内容：** Mark Word 结构、锁状态、synchronized 锁升级、对象头布局
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 4. [04-JVM调优实战与故障排查.md](./04-JVM调优实战与故障排查.md) ⭐NEW

- **内容：** JVM参数配置、GC日志分析、Arthas诊断、内存泄漏排查
- **面试题：** 4+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

---

## 📊 统计信息

- **文档数：** 4 个
- **面试题总数：** 32+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/jvm/` 目录（3 个文件，~900 行代码）

---

## 🎯 学习建议

### 第一阶段：内存模型（2-3 天）

1. **JVM内存结构**
    - 堆（Heap）：对象存储、GC 主战场
    - 栈（Stack）：方法调用、局部变量
    - 方法区（Method Area）：类信息、常量池
    - 程序计数器、本地方法栈

2. **对象内存布局**
    - 对象头（Mark Word + Class Pointer）
    - 实例数据
    - 对齐填充

3. **Mark Word 与锁机制** ⭐新增
    - Mark Word 结构详解
    - 四种锁状态（无锁、偏向锁、轻量级锁、重量级锁）
    - synchronized 锁升级过程
    - 使用 JOL 查看对象头布局

### 第二阶段：垃圾回收（3-4 天）

1. **判断对象存活**
    - 引用计数法（已淘汰）
    - 可达性分析算法（GC Roots）

2. **垃圾回收算法**
    - 标记 - 清除（Mark-Sweep）
    - 标记 - 复制（Mark-Copy）
    - 标记 - 整理（Mark-Compact）

3. **垃圾收集器**
    - Serial、ParNew、Parallel Scavenge
    - CMS（Concurrent Mark Sweep）
    - G1（Garbage First）

### 第三阶段：性能调优（2-3 天）

1. **JVM 参数配置**
    - -Xms、-Xmx（堆大小）
    - -Xmn（新生代大小）
    - -XX:MetaspaceSize（元空间）

2. **GC 日志分析**
    - Young GC vs Full GC
    - GC 停顿时间优化

---

## 🔗 跨模块关联

### 前置知识

- ✅ **[Java基础](../01-Java基础/README.md)** - 数据类型、Object 类
- ✅ **[Java并发编程](../02-Java并发编程/README.md)** - 线程模型、volatile

### 后续进阶

- 📚 **[Spring框架](../04-Spring框架/README.md)** - Bean 生命周期、循环依赖
- 📚 **[MySQL](../07-MySQL数据库/README.md)** - InnoDB 内存管理
- 📚 **[Redis](../08-Redis缓存/README.md)** - 内存淘汰策略对比

### 知识点对应

| JVM   | 应用场景            |
|-------|-----------------|
| 堆内存划分 | 大对象直接进入老年代      |
| GC 算法 | 高并发低延迟系统调优      |
| 类加载机制 | Tomcat 热部署、OSGi |
| 字节码增强 | AOP、动态代理底层      |

---

## 💡 高频面试题 Top 18

1. **JVM内存结构是怎样的？各部分作用？**
2. **堆和栈的区别？什么情况下会 StackOverflowError？**
3. **如何判断一个对象是否可以被回收？**
4. **常见的垃圾回收算法有哪些？**
5. **CMS 收集器的工作原理？优缺点？**
6. **G1 收集器相比 CMS 有什么改进？**
7. **什么是 Minor GC、Major GC、Full GC？**
8. **JVM 中什么是类加载双亲委派模型？**
9. **如何自定义类加载器？应用场景？**
10. **什么是内存泄漏？如何排查？**
11. **JVM 参数如何调优？常用参数有哪些？**
12. **什么情况会触发 Full GC？**
13. **Young GC 和 Full GC 的区别？**
14. **如何查看 JVM GC 日志？**
15. **Metaspace 和 PermSpace 的区别？**
16. **Mark Word 的作用是什么？包含哪些信息？** ⭐新增
17. **synchronized 的锁升级过程是怎样的？** ⭐新增
18. **偏向锁、轻量级锁、重量级锁的区别？** ⭐新增

---

## 🛠️ 实战技巧

### 查看 JVM 参数

```bash
# 查看所有 JVM 参数
java -XX:+PrintFlagsFinal -version

# 查看 GC 日志
java -Xloggc:gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps YourApp
```

### 常用 JVM 配置

```bash
# 生产环境推荐配置
-Xms4g -Xmx4g           # 堆内存设置
-Xmn1g                  # 新生代大小
-XX:MetaspaceSize=256m  # 元空间初始大小
-XX:+UseG1GC            # 使用 G1 收集器
-XX:MaxGCPauseMillis=200 # 最大 GC 停顿时间
```

---

## 📖 推荐学习顺序

```
JVM内存结构
   ↓
对象内存布局
   ↓
Mark Word 与锁机制 ⭐新增
   ↓
垃圾回收算法
   ↓
垃圾收集器
   ↓
类加载机制
   ↓
JVM 调优实战
```

---

## 📈 更新日志

### v2.1 - 2026-03-16

- ✅ 新增 Mark Word 详解文档
- ✅ 补充 8+ 道锁相关面试题
- ✅ 添加 Mark Word 结构图和锁升级流程
- ✅ 配套代码示例：MarkWordDemo.java

### v2.0 - 2026-03-08

- ✅ 新增跨模块关联章节
- ✅ 补充 20+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本

- ✅ 基础 JVM 文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-16  
**问题反馈：** 欢迎提 Issue 或 PR
