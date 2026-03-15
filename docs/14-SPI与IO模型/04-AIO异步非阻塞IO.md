# AIO 异步非阻塞 IO

## 一、什么是 AIO？

AIO（Asynchronous IO）是**异步非阻塞 IO**模型，真正的异步，IO 操作立即返回，操作完成后通过回调通知应用程序。

---

## 二、核心特点

### 2.1 异步非阻塞

- IO 操作立即返回，不阻塞线程
- 操作系统内核主动通知完成
- 真正的异步

### 2.2 回调机制

提供 **CompletionHandler** 接口：
- `completed()`: 操作成功时回调
- `failed()`: 操作失败时回调

### 2.3 事件驱动

基于操作系统异步 IO：
- **Windows**: IOCP（IO Completion Port）
- **Linux**: AIO（支持不完善）

---

## 三、与 NIO 的区别

### 3.1 NIO（同步非阻塞）

- 应用程序主动轮询 Selector
- 需要不断检查是否有事件发生
- "询问"模式

### 3.2 AIO（异步非阻塞）

- 操作系统主动通知
- 不需要轮询
- "回调"模式

---

## 四、工作流程

```
Step1: 创建 AsynchronousServerSocketChannel
Step2: 注册 CompletionHandler
Step3: 客户端连接时触发 completed 回调
Step4: 异步读写，完成后回调通知
Step5: 在回调中处理业务逻辑
```

---

## 五、代码示例

### 5.1 AIO 服务器示例

```java
package cn.itzixiao.interview.transaction.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;

public class AioServerDemo {
    
    private static final int BUFFER_SIZE = 1024;
    private static final int PORT = 8080;
    
    public static void main(String[] args) throws IOException {
        // Step1: 创建 AsynchronousServerSocketChannel
        AsynchronousServerSocketChannel serverChannel = 
            AsynchronousServerSocketChannel.open();
        
        // 绑定端口
        serverChannel.bind(new InetSocketAddress(PORT));
        System.out.println("AIO 服务器启动，监听端口：" + PORT);
        
        // Step2: 异步接受连接
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                // Step3: 接受下一个连接（继续监听）
                serverChannel.accept(null, this);
                
                System.out.println("新客户端连接：" + clientChannel.toString());
                
                // Step4: 异步读取数据
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                clientChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer bytesRead, ByteBuffer readBuffer) {
                        readBuffer.flip();
                        byte[] data = new byte[readBuffer.remaining()];
                        readBuffer.get(data);
                        
                        String message = new String(data);
                        System.out.println("收到客户端消息：" + message);
                        
                        // Step5: 异步响应
                        ByteBuffer writeBuffer = ByteBuffer.wrap(
                            ("服务器收到：" + message).getBytes());
                        clientChannel.write(writeBuffer, writeBuffer, 
                            new CompletionHandler<Integer, ByteBuffer>() {
                                @Override
                                public void completed(Integer bytesWritten, ByteBuffer writeBuf) {
                                    System.out.println("响应客户端成功");
                                    // 继续读取下一条消息
                                    ByteBuffer newBuffer = ByteBuffer.allocate(BUFFER_SIZE);
                                    clientChannel.read(newBuffer, newBuffer, this);
                                }
                                
                                @Override
                                public void failed(Throwable exc, ByteBuffer writeBuf) {
                                    exc.printStackTrace();
                                }
                            });
                    }
                    
                    @Override
                    public void failed(Throwable exc, ByteBuffer readBuffer) {
                        exc.printStackTrace();
                        try {
                            clientChannel.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            
            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
            }
        });
        
        // 保持主线程运行
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

### 5.2 AIO 客户端示例

```java
package cn.itzixiao.interview.transaction.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AioClientDemo {
    
    public static void main(String[] args) throws IOException, 
            InterruptedException, ExecutionException {
        // 创建 AsynchronousSocketChannel
        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
        
        // 异步连接服务器
        Future<Void> connectFuture = channel.connect(
            new InetSocketAddress("localhost", 8080));
        
        // 等待连接完成
        connectFuture.get();
        
        System.out.println("连接服务器成功！");
        
        // 发送消息
        String message = "Hello AIO!";
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes("UTF-8"));
        Future<Integer> writeFuture = channel.write(buffer);
        
        // 等待写入完成
        Integer bytesWritten = writeFuture.get();
        System.out.println("发送消息：" + message + "，字节数：" + bytesWritten);
        
        // 读取响应
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        Future<Integer> readFuture = channel.read(readBuffer);
        
        // 等待读取完成
        Integer bytesRead = readFuture.get();
        
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

- ✓ 真正的异步，不阻塞线程
- ✓ 高并发性能优秀
- ✓ 线程资源消耗少
- ✓ 编程模型比 NIO 简单（无需轮询）

### 6.2 缺点

- ✗ 依赖操作系统支持
- ✗ Linux 支持不完善
- ✗ 生态不如 NIO 成熟
- ✗ 调试相对复杂

---

## 七、应用场景

- ✓ 高并发网络服务器
- ✓ 文件传输服务
- ✓ 实时数据处理
- ✓ Windows 平台高性能应用

---

## 八、AIO 相关面试题

### 1. 什么是 AIO？与 NIO 的区别？

**参考答案：**

AIO（Asynchronous IO）是异步非阻塞 IO模型。

**与 NIO 的区别：**
| 特性 | NIO | AIO |
|------|-----|-----|
| 阻塞方式 | 非阻塞 | 非阻塞 |
| 通知方式 | 轮询 | 回调 |
| 编程模型 | 复杂 | 中等 |
| 操作系统支持 | 完善 | 不完善 |

### 2. AIO 的回调机制是如何工作的？

**参考答案：**

AIO 通过 **CompletionHandler** 接口实现回调：

```java
channel.read(buffer, attachment, new CompletionHandler<Integer, Object>() {
    @Override
    public void completed(Integer result, Object att) {
        // 操作成功时的回调
    }
    
    @Override
    public void failed(Throwable exc, Object att) {
        // 操作失败时的回调
    }
});
```

### 3. CompletionHandler 接口的作用？

**参考答案：**

CompletionHandler 是 AIO 的核心接口，提供两个回调方法：
- `completed(V result, A attachment)`: 操作成功时调用
- `failed(Throwable exc, A attachment)`: 操作失败时调用

### 4. AIO 在 Linux 和 Windows 上的差异？

**参考答案：**

**Windows：**
- 使用 IOCP（IO Completion Port）
- 支持完善，性能好
- 真正的异步 IO

**Linux：**
- 使用 AIO（Asynchronous IO）
- 支持不完善，主要支持文件 IO
- 网络 IO 仍基于 epoll 模拟

### 5. 为什么 AIO 没有 NIO 流行？

**参考答案：**

1. **操作系统支持差异**：
   - Linux 支持不完善
   - 跨平台性差

2. **生态成熟度**：
   - NIO 框架成熟（Netty、Mina）
   - AIO 框架较少

3. **性能优势不明显**：
   - 在高并发场景下，NIO 已经足够优秀
   - AIO 的优势主要体现在特定场景

---

## 九、API 接口测试

```bash
# AIO 详解
curl http://localhost:8084/api/io/aio-summary
```

---

## 十、示例代码位置

```
interview-transaction-demo/
├── src/main/java/cn/itzixiao/interview/transaction/
│   └── io/
│       └── AioServerDemo.java        # AIO 示例
└── src/main/resources/
    └── application.yml
```

---

## 十一、三种 IO模型对比总结

| 特性 | BIO | NIO | AIO |
|------|-----|-----|-----|
| IO模型 | 同步阻塞 | 同步非阻塞 | 异步非阻塞 |
| 阻塞方式 | 阻塞 | 非阻塞 | 非阻塞 |
| 线程模型 | 一对一 | 多对一 | 多对一 |
| 实现难度 | 简单 | 复杂 | 中等 |
| 并发能力 | 低 | 高 | 高 |
| 适用场景 | 少连接 | 高并发 | 高并发 |

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
