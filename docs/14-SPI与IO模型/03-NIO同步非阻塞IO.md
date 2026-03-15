# NIO同步非阻塞IO

## 一、什么是 NIO？

NIO（Non-blocking IO）是**同步非阻塞 IO**模型，基于 IO 多路复用技术（Selector），一个线程可以管理多个连接。

---

## 二、核心组件

### 2.1 Channel（通道）

- 双向的，可以读写
- 类似管道，但更高效
- 常用类型：SocketChannel、ServerSocketChannel

### 2.2 Buffer（缓冲区）

- 所有数据都通过 Buffer 读写
- 常用：ByteBuffer、CharBuffer
- 核心方法：flip()、put()、get()

### 2.3 Selector（选择器）

- **核心组件**，实现 IO 多路复用
- 监听多个 Channel 的事件
- **事件类型**：
  - OP_ACCEPT：接受连接
  - OP_READ：可读
  - OP_WRITE：可写
  - OP_CONNECT：连接

---

## 三、Reactor 模式

### 3.1 单线程 Reactor

- 一个线程处理所有事件
- 简单但性能受限
- 适合低并发场景

### 3.2 多线程 Reactor

- Reactor 负责事件分发
- Worker 线程池处理业务逻辑
- **Netty 采用此模式**

### 3.3 主从 Reactor

- Main Reactor：只处理连接事件
- Sub Reactor：处理 IO 事件
- 适合超高并发场景

---

## 四、工作流程

```
Step1: 创建 Selector（事件分发器）
Step2: Channel 配置为非阻塞并注册到 Selector
Step3: Selector 轮询就绪的事件
Step4: 分发到对应的 Handler 处理
Step5: Handler 执行实际的 IO 操作
```

---

## 五、代码示例

### 5.1 NIO 服务器示例

```java
package cn.itzixiao.interview.transaction.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class NioServerDemo {
    
    // 缓冲区大小
    private static final int BUFFER_SIZE = 1024;
    
    public static void main(String[] args) throws IOException {
        // Step1: 创建 Selector
        Selector selector = Selector.open();
        
        // Step2: 创建 ServerSocketChannel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false); // 设置为非阻塞
        serverChannel.bind(new InetSocketAddress(8080));
        
        // 注册 accept 事件
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        System.out.println("NIO 服务器启动，监听端口：" + 8080);
        
        // Step3: 轮询事件
        while (true) {
            // 阻塞等待就绪事件
            selector.select();
            
            // 获取就绪的 SelectionKey
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove(); // 必须移除
                
                try {
                    if (key.isAcceptable()) {
                        // 处理连接事件
                        handleAccept(key, selector);
                    } else if (key.isReadable()) {
                        // 处理读事件
                        handleRead(key);
                    }
                } catch (IOException e) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
    
    /**
     * 处理客户端连接
     */
    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        
        // 设置为非阻塞
        clientChannel.configureBlocking(false);
        
        // 注册读事件
        clientChannel.register(selector, SelectionKey.OP_READ);
        
        System.out.println("新客户端连接：" + clientChannel.getRemoteAddress());
    }
    
    /**
     * 处理读事件
     */
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        
        // 读取数据
        int bytesRead = channel.read(buffer);
        
        if (bytesRead > 0) {
            buffer.flip(); // 切换到读模式
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            
            String message = new String(data, "UTF-8");
            System.out.println("收到客户端消息：" + message);
            
            // 响应客户端
            ByteBuffer responseBuffer = ByteBuffer.wrap(
                ("服务器收到：" + message).getBytes("UTF-8"));
            channel.write(responseBuffer);
        } else if (bytesRead == -1) {
            // 客户端断开连接
            System.out.println("客户端断开连接");
            key.cancel();
            channel.close();
        }
    }
}
```

### 5.2 NIO 客户端示例

```java
package cn.itzixiao.interview.transaction.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioClientDemo {
    
    public static void main(String[] args) throws IOException {
        // 创建 SocketChannel
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        
        // 连接服务器
        channel.connect(new InetSocketAddress("localhost", 8080));
        
        // 等待连接完成
        while (!channel.finishConnect()) {
            System.out.println("正在连接服务器...");
        }
        
        System.out.println("连接服务器成功！");
        
        // 发送消息
        String message = "Hello NIO!";
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes("UTF-8"));
        channel.write(buffer);
        
        System.out.println("发送消息：" + message);
        
        // 读取响应
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(readBuffer);
        
        if (bytesRead > 0) {
            readBuffer.flip();
            byte[] data = new byte[readBuffer.remaining()];
            readBuffer.get(data);
            String response = new String(data, "UTF-8");
            System.out.println("收到服务器响应：" + response);
        }
        
        channel.close();
    }
}
```

---

## 六、优缺点分析

### 6.1 优点

- ✓ 高并发性能好（一个线程管理多个连接）
- ✓ 资源利用率高（减少线程数量）
- ✓ 可扩展性强（支持百万级并发）
- ✓ 无阻塞开销（非阻塞 IO）

### 6.2 缺点

- ✗ 编程模型复杂
- ✗ 学习成本高
- ✗ 代码可读性差
- ✗ 需要理解 IO 多路复用原理

---

## 七、应用场景

- ✓ 高并发网络服务器
- ✓ IM 即时通讯系统
- ✓ 游戏服务器
- ✓ RPC 框架（Dubbo、gRPC）
- ✓ Web 服务器（Tomcat NIO Connector）
- ✓ Netty 框架

---

## 八、NIO 相关面试题

### 1. 什么是 NIO？与 BIO 的区别？

**参考答案：**

NIO（Non-blocking IO）是同步非阻塞 IO模型。

**与 BIO 的区别：**
| 特性 | BIO | NIO |
|------|-----|-----|
| 阻塞方式 | 阻塞 | 非阻塞 |
| 线程模型 | 一对一 | 多对一 |
| 并发能力 | 低 | 高 |
| 编程难度 | 简单 | 复杂 |

### 2. NIO 的核心组件有哪些？

**参考答案：**

1. **Channel**：双向通道，可读写
2. **Buffer**：数据缓冲区
3. **Selector**：选择器，实现 IO 多路复用

### 3. Selector、Channel、Buffer 的作用？

**参考答案：**

- **Selector**：监听多个 Channel 的事件，实现一个线程管理多个连接
- **Channel**：类似流，但双向，可以异步读写
- **Buffer**：所有数据都通过 Buffer 读写，提供 flip/put/get 等操作

### 4. 什么是 IO 多路复用？

**参考答案：**

IO 多路复用是指**一个线程同时监控多个 IO 事件**，当任意事件就绪时通知应用程序处理。

**优势：**
- 减少线程数量
- 提高资源利用率
- 支持高并发

### 5. Select、Poll、Epoll 的区别？

**参考答案：**

| 特性 | Select | Poll | Epoll |
|------|--------|------|-------|
| 实现方式 | 数组 | 链表 | 红黑树 + 双向链表 |
| 性能 | O(n) | O(n) | O(1) |
| 最大连接数 | 1024 | 无限制 | 无限制 |
| 操作系统 | 跨平台 | 跨平台 | Linux |

### 6. Reactor 模式的三种变体？

**参考答案：**

1. **单线程 Reactor**：一个线程处理所有事件
2. **多线程 Reactor**：Reactor 分发，Worker 线程池处理
3. **主从 Reactor**：Main Reactor 处理连接，Sub Reactor 处理 IO

### 7. Netty 为什么性能高？

**参考答案：**

1. **NIO 基础**：基于 NIO，支持高并发
2. **Reactor 模式**：采用主从多线程 Reactor
3. **零拷贝**：减少内存复制
4. **内存池**：ByteBuf 对象池
5. **高效编码解码**：自定义协议支持

### 8. 零拷贝原理？

**参考答案：**

**传统 IO：** 4 次拷贝
1. 磁盘 → 内核缓冲区
2. 内核缓冲区 → 用户缓冲区
3. 用户缓冲区 → Socket 缓冲区
4. Socket 缓冲区 → 网卡

**零拷贝（mmap）：** 2 次拷贝
1. 磁盘 → 内核缓冲区
2. 内核缓冲区 → 网卡

**零拷贝（sendfile）：** 1.5 次拷贝
1. 磁盘 → 内核缓冲区
2. 内核缓冲区描述符 → Socket 缓冲区

### 9. 直接内存 vs 堆内存？

**参考答案：**

| 特性 | 堆内存 | 直接内存 |
|------|--------|----------|
| 分配位置 | JVM 堆内 | 堆外 native 区 |
| GC 影响 | 会被 GC | 不受 GC 管理 |
| 访问速度 | 慢 | 快（无需拷贝） |
| 大小限制 | -Xmx | 物理内存限制 |
| 适用场景 | 频繁 GC 对象 | 大缓冲区 |

---

## 九、API 接口测试

```bash
# NIO 详解
curl http://localhost:8084/api/io/nio-summary

# Reactor Core 示例
curl http://localhost:8084/api/reactor/core-demo

# Reactor IO 示例
curl http://localhost:8084/api/reactor/io-demo

# NIO Reactor 模式说明
curl http://localhost:8084/api/reactor/nio-reactor-demo
```

---

## 十、示例代码位置

```
interview-transaction-demo/
├── src/main/java/cn/itzixiao/interview/transaction/
│   ├── reactor/                      # Reactor 模式示例
│   │   ├── ReactorCoreDemo.java      # Reactor Core
│   │   ├── ReactorIODemo.java        # Reactor IO
│   │   └── NioReactorDemo.java       # NIO Reactor
│   └── controller/
│       └── IoModelController.java    # IO模型控制器
└── src/main/resources/
    └── application.yml
```

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
