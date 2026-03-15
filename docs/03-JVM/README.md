# JVM 知识点详解

## 📚 文档列表

#### 1. [01-JVM 内存模型与垃圾回收.md](./01-JVM%E5%86%85%E5%AD%98%E6%A8%A1%E5%9E%8B%E4%B8%8E%E5%9E%83%E5%9C%BE%E5%9B%9E%E6%94%B6.md)
- **内容：** JVM 内存结构、垃圾收集算法、GC 收集器
- **面试题：** 12+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

---

## 📊 统计信息

- **文档数：** 1 个
- **面试题总数：** 20+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/jvm/` 目录（2 个文件，~700 行代码）

---

## 🎯 学习建议

### 第一阶段：内存模型（2-3 天）
1. **JVM 内存结构**
   - 堆（Heap）：对象存储、GC 主战场
   - 栈（Stack）：方法调用、局部变量
   - 方法区（Method Area）：类信息、常量池
   - 程序计数器、本地方法栈

2. **对象内存布局**
   - 对象头（Mark Word + Class Pointer）
   - 实例数据
   - 对齐填充

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
| JVM | 应用场景 |
|-----|---------|
| 堆内存划分 | 大对象直接进入老年代 |
| GC 算法 | 高并发低延迟系统调优 |
| 类加载机制 | Tomcat 热部署、OSGi |
| 字节码增强 | AOP、动态代理底层 |

---

## 💡 高频面试题 Top 15

1. **JVM 内存结构是怎样的？各部分作用？**
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
JVM 内存结构
   ↓
对象内存布局
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

### v2.0 - 2026-03-08
- ✅ 新增跨模块关联章节
- ✅ 补充 20+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本
- ✅ 基础 JVM 文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-08  
**问题反馈：** 欢迎提 Issue 或 PR
