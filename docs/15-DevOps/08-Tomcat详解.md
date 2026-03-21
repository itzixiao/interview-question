# Tomcat 详解

## 一、Tomcat 架构概述

### 1.1 Tomcat 是什么

Tomcat 是 Apache 软件基金会的 Jakarta 项目中的一个核心项目，是一个免费的开放源代码的 Web 应用服务器，属于轻量级应用服务器，在中小型系统和并发访问用户不是很多的场合下被普遍使用。

**核心特点：**

1. **Servlet 容器** - 实现了 Java Servlet 和 JSP 规范
2. **HTTP 服务器** - 内置 HTTP 连接器，可直接处理 HTTP 请求
3. **轻量级** - 相比 WebLogic、WebSphere 更加轻量
4. **开源免费** - Apache 许可证，可免费商用

### 1.2 Tomcat 整体架构

```
+------------------------- Tomcat 架构图 -------------------------+
|                                                                 |
|  +---------------------------------------------------------+  |
|  |                         Server                          |  |
|  |   +-------------------------------------------------+   |  |
|  |   |                     Service                     |   |  |
|  |   |   +---------------+     +---------------+       |   |  |
|  |   |   |  Connector  |     |    Engine     |       |   |  |
|  |   |   |  (连接器)   |     |    (引擎)     |       |   |  |
|  |   |   |             |     |               |       |   |  |
|  |   |   | +---------+ |     | +---------+   |       |   |  |
|  |   |   | | HTTP/1.1| |     | |  Host   |   |       |   |  |
|  |   |   | |Connector| |     | |(虚拟主机)|   |       |   |  |
|  |   |   | +---------+ |     | +----┬----+   |       |   |  |
|  |   |   | +---------+ |     |      |        |       |   |  |
|  |   |   | |   AJP   | |     | +----┴----+   |       |   |  |
|  |   |   | |Connector| |     | | Context |   |       |   |  |
|  |   |   | +---------+ |     | |(Web应用)|   |       |   |  |
|  |   |   | +---------+ |     | +----┬----+   |       |   |  |
|  |   |   | | NIO/APR | |     |      |        |       |   |  |
|  |   |   | |Connector| |     | +----┴----+   |       |   |  |
|  |   |   | +---------+ |     | | Wrapper |   |       |   |  |
|  |   |   |             |     | |(Servlet)|   |       |   |  |
|  |   |   +-------------+     | +---------+   |       |   |  |
|  |   |                       +---------------+       |   |  |
|  |   +-------------------------------------------------+   |  |
|  +---------------------------------------------------------+  |
|                                                                 |
+-----------------------------------------------------------------+
```

### 1.3 核心组件说明

| 组件 | 说明 | 作用 |
|------|------|------|
| **Server** | 顶层元素 | 代表整个 Tomcat 实例 |
| **Service** | 服务组件 | 包含一个 Engine 和多个 Connector |
| **Connector** | 连接器 | 接收客户端请求，解析协议 |
| **Engine** | 引擎 | 处理所有请求的核心容器 |
| **Host** | 虚拟主机 | 代表一个虚拟主机，可配置多个域名 |
| **Context** | Web 应用 | 代表一个 Web 应用上下文 |
| **Wrapper** | Servlet 包装器 | 包装单个 Servlet |

---

## 二、Tomcat 连接器详解

### 2.1 Connector 工作原理

```
+---------------------- Connector 请求处理流程 -------------------+
|                                                                 |
|   客户端请求                                                    |
|        |                                                        |
|        v                                                        |
|   +------------+                                                |
|   |  Acceptor  |  接收 TCP 连接                                 |
|   |   线程     |  (默认 1 个线程)                               |
|   +------+-----+                                                |
|          |                                                      |
|          v                                                      |
|   +------------+                                                |
|   |   Poller   |  IO 多路复用，检测读写事件                     |
|   |   线程池   |  (默认 2 个线程)                               |
|   +------+-----+                                                |
|          |                                                      |
|          v                                                      |
|   +----------------+                                            |
|   | SocketProcessor|  读取请求数据，封装 Request/Response       |
|   |     线程池     |  (默认 200 个线程)                         |
|   +--------+-------+                                            |
|            |                                                    |
|            v                                                    |
|   +----------------+                                            |
|   |  CoyoteAdapter |  将请求转换为 ServletRequest               |
|   +--------+-------+                                            |
|            |                                                    |
|            v                                                    |
|   +------------+                                                |
|   |   Engine   |  交给容器处理                                  |
|   +------------+                                                |
|                                                                 |
+-----------------------------------------------------------------+
```

### 2.2 三种连接器模式对比

| 模式 | 说明 | 适用场景 | 性能 |
|------|------|---------|------|
| **BIO** | 阻塞 IO，每个请求一个线程 | 低并发场景 | 低 |
| **NIO** | 非阻塞 IO，使用 Java NIO | 高并发场景 | 中高 |
| **APR** | Apache Portable Runtime，使用本地库 | 生产环境 | 最高 |

**NIO 连接器配置（server.xml）：**

```xml
<!-- NIO 连接器配置 -->
<Connector 
    port="8080" 
    protocol="org.apache.coyote.http11.Http11NioProtocol"
    connectionTimeout="20000"
    redirectPort="8443"
    
    <!-- 线程池配置 -->
    maxThreads="200"
    minSpareThreads="10"
    maxSpareThreads="50"
    
    <!-- 连接配置 -->
    maxConnections="10000"
    acceptCount="100"
    
    <!-- IO 配置 -->
    pollerThreadCount="2"
    selectorTimeout="1000"
    
    <!-- 性能优化 -->
    compression="on"
    compressionMinSize="2048"
    compressableMimeType="text/html,text/xml,text/css,application/json,application/javascript"
/>
```

**APR 连接器配置：**

```xml
<!-- APR 连接器配置（需要安装 APR 本地库） -->
<Connector 
    port="8080" 
    protocol="org.apache.coyote.http11.Http11AprProtocol"
    connectionTimeout="20000"
    redirectPort="8443"
    maxThreads="500"
    maxConnections="20000"
/>
```

### 2.3 线程池配置详解

```xml
<!-- 定义共享线程池 -->
<Executor 
    name="tomcatThreadPool" 
    namePrefix="tomcat-http-"
    
    <!-- 核心线程数：始终保持的线程数量 -->
    minSpareThreads="25"
    
    <!-- 最大线程数：并发高峰时最大线程数 -->
    maxThreads="500"
    
    <!-- 最大空闲时间：超过此时间回收多余线程（毫秒） -->
    maxIdleTime="60000"
    
    <!-- 最大队列长度：等待队列大小 -->
    maxQueueSize="1000"
/>

<!-- 使用共享线程池 -->
<Connector 
    executor="tomcatThreadPool"
    port="8080" 
    protocol="HTTP/1.1"
    connectionTimeout="20000"
/>
```

**线程数计算公式：**

```
最优线程数 = CPU 核心数 * (1 + 等待时间/计算时间)

示例：
- CPU 核心数：8
- 等待时间（IO）：100ms
- 计算时间（CPU）：20ms
- 最优线程数 = 8 * (1 + 100/20) = 8 * 6 = 48

实际配置建议：
- CPU 密集型：CPU 核心数 + 1
- IO 密集型：CPU 核心数 * 2 ~ CPU 核心数 * 3
```

---

## 三、Tomcat 请求处理流程

### 3.1 完整请求处理链路

```
+-------------------- Tomcat 请求处理完整流程 --------------------+
|                                                                 |
|  1. TCP 连接建立                                                |
|     +--> Acceptor 线程接收连接，注册到 Poller                   |
|                                                                 |
|  2. HTTP 请求解析                                               |
|     +--> Poller 检测读事件，SocketProcessor 读取数据            |
|     +--> 解析 HTTP 请求行、请求头、请求体                       |
|                                                                 |
|  3. Request/Response 封装                                       |
|     +--> CoyoteAdapter 将请求转换为 ServletRequest              |
|                                                                 |
|  4. 容器处理管道                                                |
|     +-----------------------------------------------------+    |
|     |  Engine Pipeline                                     |    |
|     |    +--> AccessLogValve (访问日志)                    |    |
|     |    +--> StandardEngineValve                         |    |
|     |          |                                          |    |
|     |          v                                          |    |
|     |  Host Pipeline                                      |    |
|     |    +--> ErrorReportValve (错误报告)                 |    |
|     |    +--> StandardHostValve                          |    |
|     |          |                                          |    |
|     |          v                                          |    |
|     |  Context Pipeline                                   |    |
|     |    +--> AuthenticatorBase (认证)                    |    |
|     |    +--> StandardContextValve                       |    |
|     |          |                                          |    |
|     |          v                                          |    |
|     |  Wrapper Pipeline                                   |    |
|     |    +--> StandardWrapperValve                       |    |
|     |          |                                          |    |
|     |          v                                          |    |
|     |    +-----------------------------+                 |    |
|     |    |  ApplicationFilterChain    |                 |    |
|     |    |    +--> Filter 1           |                 |    |
|     |    |    +--> Filter 2           |                 |    |
|     |    |    +--> Filter N           |                 |    |
|     |    |    +--> Servlet.service()  |                 |    |
|     |    +-----------------------------+                 |    |
|     +-----------------------------------------------------+    |
|                                                                 |
|  5. 响应返回                                                    |
|     +--> 通过 Pipeline 逆向返回                                |
|     +--> 序列化响应数据                                         |
|     +--> 写回客户端                                             |
|                                                                 |
+-----------------------------------------------------------------+
```

### 3.2 Valve 管道机制

```java
/**
 * Valve 接口定义
 * 
 * Valve 是 Tomcat 中的拦截器，用于在请求处理过程中执行特定逻辑
 * 类似于 Servlet Filter，但作用于容器级别
 */
public interface Valve {
    
    /**
     * 获取下一个 Valve
     */
    Valve getNext();
    
    /**
     * 设置下一个 Valve
     */
    void setNext(Valve valve);
    
    /**
     * 处理请求
     * 
     * @param request 请求对象
     * @param response 响应对象
     */
    void invoke(Request request, Response response) 
        throws IOException, ServletException;
}
```

**自定义 Valve 示例：**

```java
package cn.itzixiao.interview.tomcat.valve;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import java.io.IOException;

/**
 * 自定义访问统计 Valve
 * 
 * <p>功能：统计每个 URI 的访问次数和平均耗时</p>
 * 
 * <p>配置方式（server.xml）：</p>
 * <pre>
 * &lt;Host name="localhost" appBase="webapps"&gt;
 *     &lt;Valve className="cn.itzixiao.interview.tomcat.valve.AccessStatisticsValve" /&gt;
 * &lt;/Host&gt;
 * </pre>
 */
public class AccessStatisticsValve extends ValveBase {
    
    // 访问统计 Map：URI -> [访问次数, 总耗时]
    private final java.util.concurrent.ConcurrentHashMap<String, long[]> statistics 
        = new java.util.concurrent.ConcurrentHashMap<>();
    
    @Override
    public void invoke(Request request, Response response) 
            throws IOException, ServletException {
        
        String uri = request.getRequestURI();
        long startTime = System.currentTimeMillis();
        
        try {
            // 调用下一个 Valve
            getNext().invoke(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 更新统计信息
            statistics.compute(uri, (key, value) -> {
                if (value == null) {
                    return new long[]{1, duration};
                }
                value[0]++;  // 增加访问次数
                value[1] += duration;  // 累加耗时
                return value;
            });
        }
    }
    
    /**
     * 获取统计信息
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("URI\t\t\tCount\tAvgTime\n");
        
        statistics.forEach((uri, data) -> {
            long count = data[0];
            long totalTime = data[1];
            long avgTime = count > 0 ? totalTime / count : 0;
            sb.append(uri).append("\t\t")
              .append(count).append("\t")
              .append(avgTime).append("ms\n");
        });
        
        return sb.toString();
    }
}
```

---

## 四、Tomcat 类加载机制

### 4.1 类加载器层次结构

```
+---------------------- Tomcat 类加载器结构 ----------------------+
|                                                                 |
|                    +-------------+                              |
|                    | Bootstrap   |  加载 Java 核心类            |
|                    | ClassLoader |  ($JAVA_HOME/jre/lib/*.jar)  |
|                    +------+------+                              |
|                           |                                     |
|                           v                                     |
|                    +-------------+                              |
|                    | Extension   |  加载 Java 扩展类            |
|                    | ClassLoader |  ($JAVA_HOME/jre/lib/ext/)   |
|                    +------+------+                              |
|                           |                                     |
|                           v                                     |
|                    +-------------+                              |
|                    | Application |  加载应用类路径              |
|                    | ClassLoader |  ($CLASSPATH)                |
|                    +------+------+                              |
|                           |                                     |
|                           v                                     |
|                    +-------------+                              |
|                    |   Common    |  加载 Tomcat 公共类          |
|                    | ClassLoader |  ($CATALINA_HOME/lib/)       |
|                    +------+------+                              |
|                           |                                     |
|         +-----------------+-----------------+                  |
|         v                                   v                   |
|  +-------------+                    +-------------+            |
|  |  Catalina   |                    |  Shared     |            |
|  | ClassLoader |                    | ClassLoader |            |
|  | (Tomcat内部)|                    | (Web应用共享)|            |
|  +-------------+                    +------+------+            |
|                                            |                    |
|                    +-----------------------+---------------+   |
|                    v                       v               v   |
|             +----------+           +----------+     +----------+
|             | WebApp 1 |           | WebApp 2 |     | WebApp N |
|             |ClassLoader|          |ClassLoader|    |ClassLoader|
|             +----------+           +----------+     +----------+
|                                                                 |
+-----------------------------------------------------------------+
```

### 4.2 类加载顺序

**Tomcat 类加载顺序（打破双亲委派）：**

```
1. Bootstrap ClassLoader
   +--> 加载 Java 核心类库

2. System ClassLoader
   +--> 加载 CLASSPATH 下的类

3. WebApp ClassLoader（打破双亲委派）
   +--> /WEB-INF/classes
   +--> /WEB-INF/lib/*.jar
   +--> 父类加载器（Common ClassLoader）

注意：
- WebApp ClassLoader 先加载 Web 应用自己的类
- 这打破了标准双亲委派模型
- 目的：让 Web 应用可以使用不同版本的库
```

**配置示例（catalina.properties）：**

```properties
# Common ClassLoader 加载的类路径
common.loader="${catalina.base}/lib","${catalina.base}/lib/*.jar","${catalina.home}/lib","${catalina.home}/lib/*.jar"

# Server ClassLoader 加载的类路径（Tomcat 内部使用）
server.loader=

# Shared ClassLoader 加载的类路径（多个 WebApp 共享）
shared.loader=
```

---

## 五、Tomcat 调优实战

### 5.1 内存配置

```bash
# setenv.sh（Linux）或 setenv.bat（Windows）
# 放在 $CATALINA_HOME/bin/ 目录下

# JVM 堆内存配置
JAVA_OPTS="-Xms2g -Xmx2g"

# 新生代配置
JAVA_OPTS="$JAVA_OPTS -Xmn1g"

# 元空间配置（JDK 8+）
JAVA_OPTS="$JAVA_OPTS -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"

# GC 配置
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
JAVA_OPTS="$JAVA_OPTS -XX:MaxGCPauseMillis=200"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=/var/log/tomcat/heapdump.hprof"

# GC 日志
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=/var/log/tomcat/gc.log:time,uptime:filecount=5,filesize=10m"
```

### 5.2 连接器调优

```xml
<!-- server.xml 连接器优化配置 -->
<Connector 
    port="8080" 
    protocol="org.apache.coyote.http11.Http11NioProtocol"
    
    <!-- 连接超时 -->
    connectionTimeout="20000"
    
    <!-- 线程池配置 -->
    maxThreads="500"
    minSpareThreads="50"
    maxSpareThreads="100"
    
    <!-- 连接配置 -->
    maxConnections="10000"
    acceptCount="200"
    
    <!-- Socket 配置 -->
    socket.soKeepAlive="true"
    socket.soTimeout="60000"
    socket.tcpNoDelay="true"
    
    <!-- HTTP 配置 -->
    maxHttpHeaderSize="8192"
    maxPostSize="2097152"
    maxSavePostSize="4096"
    
    <!-- 压缩配置 -->
    compression="on"
    compressionMinSize="2048"
    noCompressionUserAgents="gozilla, traviata"
    compressableMimeType="text/html,text/xml,text/plain,text/css,application/json,application/javascript"
    
    <!-- 性能优化 -->
    disableUploadTimeout="true"
    enableLookups="false"
    URIEncoding="UTF-8"
/>
```

### 5.3 性能监控

```java
package cn.itzixiao.interview.tomcat.monitor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Tomcat 性能监控端点
 * 
 * <p>提供 Tomcat 运行状态的关键指标</p>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@RestController
@RequestMapping("/monitor")
public class TomcatMonitorController {

    /**
     * 获取线程状态
     */
    @GetMapping("/threads")
    public Map<String, Object> getThreadStatus() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        
        Map<String, Object> status = new HashMap<>();
        status.put("threadCount", threadMXBean.getThreadCount());
        status.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        status.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        status.put("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount());
        
        return status;
    }

    /**
     * 获取内存状态
     */
    @GetMapping("/memory")
    public Map<String, Object> getMemoryStatus() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        
        Map<String, Object> status = new HashMap<>();
        
        // 堆内存
        Map<String, Long> heap = new HashMap<>();
        heap.put("used", memoryMXBean.getHeapMemoryUsage().getUsed());
        heap.put("max", memoryMXBean.getHeapMemoryUsage().getMax());
        heap.put("committed", memoryMXBean.getHeapMemoryUsage().getCommitted());
        status.put("heap", heap);
        
        // 非堆内存
        Map<String, Long> nonHeap = new HashMap<>();
        nonHeap.put("used", memoryMXBean.getNonHeapMemoryUsage().getUsed());
        nonHeap.put("committed", memoryMXBean.getNonHeapMemoryUsage().getCommitted());
        status.put("nonHeap", nonHeap);
        
        return status;
    }

    /**
     * 获取运行时状态
     */
    @GetMapping("/runtime")
    public Map<String, Object> getRuntimeStatus() {
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> status = new HashMap<>();
        status.put("availableProcessors", runtime.availableProcessors());
        status.put("freeMemory", runtime.freeMemory());
        status.put("totalMemory", runtime.totalMemory());
        status.put("maxMemory", runtime.maxMemory());
        status.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        
        return status;
    }
}
```

---

## 六、高频面试题

**问题 1：Tomcat 的整体架构是怎样的？**

**答：**

Tomcat 采用分层架构，主要组件包括：

1. **Server**：顶层容器，代表整个 Tomcat 实例
2. **Service**：包含一个 Engine 和多个 Connector
3. **Connector**：连接器，负责接收请求、解析协议
4. **Engine**：引擎，请求处理的核心容器
5. **Host**：虚拟主机，可配置多个域名
6. **Context**：Web 应用上下文
7. **Wrapper**：Servlet 包装器

请求处理流程：Connector 接收请求 → 解析协议 → 封装 Request/Response → 通过 Pipeline 传递 → 最终调用 Servlet。

**问题 2：Tomcat 的 BIO、NIO、APR 模式有什么区别？**

**答：**

| 模式 | IO 模型 | 线程模型 | 性能 | 适用场景 |
|------|---------|---------|------|---------|
| BIO | 阻塞 IO | 一请求一线程 | 低 | 低并发 |
| NIO | 非阻塞 IO | IO 多路复用 | 中高 | 高并发 |
| APR | 本地库 | 使用操作系统原生 | 最高 | 生产环境 |

推荐：生产环境使用 NIO 或 APR 模式。

**问题 3：Tomcat 如何实现热部署？**

**答：**

热部署实现原理：

1. **类加载机制**：每个 WebApp 有独立的 ClassLoader
2. **文件监控**：后台线程定期检查类文件时间戳
3. **重新加载**：检测到变化时，销毁旧 Context，创建新 Context

配置方式：
```xml
<Context docBase="myapp" path="/myapp" reloadable="true"/>
```

注意：生产环境建议关闭热部署（reloadable="false"），避免性能损耗。

**问题 4：Tomcat 的类加载机制有什么特点？**

**答：**

Tomcat 的类加载机制**打破了双亲委派模型**：

1. **标准双亲委派**：先委托父类加载器加载
2. **Tomcat 的打破**：WebApp ClassLoader 先加载自己的类（/WEB-INF/classes 和 /WEB-INF/lib）
3. **目的**：让不同 Web 应用可以使用不同版本的库（如 Spring 4 和 Spring 5）
4. **类加载顺序**：Bootstrap → System → WebApp → Common

**问题 5：如何优化 Tomcat 性能？**

**答：**

**1. 连接器优化：**
- 使用 NIO/APR 协议
- 调整 maxThreads、maxConnections
- 启用压缩

**2. JVM 优化：**
- 调整堆内存大小
- 选择合适的 GC 算法
- 配置元空间大小

**3. 线程池优化：**
- 合理设置核心线程数和最大线程数
- 使用共享线程池

**4. 应用优化：**
- 关闭热部署
- 禁用 DNS 解析（enableLookups="false"）
- 配置连接超时

---

## 七、最佳实践

### 7.1 生产环境配置清单

```
[ ] Tomcat 配置
  [ ] 使用 NIO/APR 连接器
  [ ] 合理配置线程池（maxThreads = 500）
  [ ] 调整连接数（maxConnections = 10000）
  [ ] 启用 Gzip 压缩
  [ ] 关闭热部署（reloadable="false"）
  [ ] 禁用 DNS 解析（enableLookups="false"）
  [ ] 配置访问日志

[ ] JVM 配置
  [ ] 堆内存：-Xms = -Xmx（避免动态调整）
  [ ] 新生代：-Xmn（堆内存的 1/3 ~ 1/2）
  [ ] GC：G1GC（JDK 11+）或 CMS（JDK 8）
  [ ] 元空间：-XX:MaxMetaspaceSize=512m
  [ ] 开启 GC 日志

[ ] 监控告警
  [ ] JVM 监控（内存、GC、线程）
  [ ] Tomcat 监控（线程池、连接数）
  [ ] 配置告警阈值
```

### 7.2 性能调优公式

```
Tomcat 线程数：
  最优线程数 = CPU 核心数 × (1 + 等待时间/计算时间)
  
JVM 堆内存：
  堆内存 = (系统内存 - 操作系统内存 - 其他服务内存) × 70%
```

---

**维护者：** itzixiao  
**最后更新：** 2026-03-21  
**问题反馈：** 欢迎提 Issue 或 PR
