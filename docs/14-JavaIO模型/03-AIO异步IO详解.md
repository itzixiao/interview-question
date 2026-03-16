# AIO异步非阻塞IO详解

## 一、AIO工作原理

### 1.1 核心特点

**异步非阻塞（Asynchronous IO）**：

- ✅ 真正的异步
- ✅ 操作系统完成IO操作后通知应用
- ✅ 无需轮询

### 1.2 AIO vs NIO

| 特性         | NIO        | AIO                      |
|------------|------------|--------------------------|
| **同步/异步**  | 同步非阻塞      | 异步非阻塞                    |
| **实现方式**   | Selector轮询 | CompletionHandler回调      |
| **操作系统支持** | 跨平台        | Windows(IOCP)、Linux(AIO) |
| **编程复杂度**  | 复杂         | 最复杂                      |

---

## 二、AIO代码示例

### 2.1 AIO服务器

```java
public class AioServerDemo {
    private static final int PORT = 8080;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) throws IOException {
        // 1. 打开异步服务器通道
        AsynchronousServerSocketChannel serverChannel =
                AsynchronousServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(PORT));

        System.out.println("AIO服务器启动，监听端口：" + PORT);

        // 2. 异步接受连接
        acceptClient(serverChannel);

        // 保持主线程运行
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void acceptClient(AsynchronousServerSocketChannel serverChannel) {
        // 异步接受连接，带回调
        serverChannel.accept(null, new CompletionHandler<
                AsynchronousSocketChannel, Void>() {

            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Void att) {
                // 继续接受下一个连接
                acceptClient(serverChannel);

                System.out.println("新客户端连接：" + clientChannel.toString());

                // 3. 异步读取数据
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                clientChannel.read(buffer, buffer,
                        new CompletionHandler<Integer, ByteBuffer>() {
                            @Override
                            public void completed(Integer bytesRead, ByteBuffer readBuffer) {
                                // 读取完成回调
                                readBuffer.flip();
                                byte[] data = new byte[readBuffer.remaining()];
                                readBuffer.get(data);
                                String message = new String(data);

                                System.out.println("收到消息：" + message);

                                // 4. 异步响应
                                ByteBuffer writeBuffer = ByteBuffer.wrap(
                                        ("服务器收到：" + message).getBytes());
                                clientChannel.write(writeBuffer, writeBuffer,
                                        new CompletionHandler<Integer, ByteBuffer>() {
                                            @Override
                                            public void completed(Integer bytesWritten,
                                                                  ByteBuffer writeBuf) {
                                                System.out.println("响应完成");
                                            }

                                            @Override
                                            public void failed(Throwable exc, ByteBuffer att) {
                                                exc.printStackTrace();
                                            }
                                        });
                            }

                            @Override
                            public void failed(Throwable exc, ByteBuffer att) {
                                exc.printStackTrace();
                            }
                        });
            }

            @Override
            public void failed(Throwable exc, Void att) {
                exc.printStackTrace();
            }
        });
    }
}
```

### 2.2 AIO客户端

```java
public class AioClientDemo {
    public static void main(String[] args) throws Exception {
        // 打开异步Socket通道
        AsynchronousSocketChannel clientChannel =
                AsynchronousSocketChannel.open();

        // 异步连接服务器
        clientChannel.connect(
                new InetSocketAddress("localhost", 8080),
                null,
                new CompletionHandler<Void, Void>() {
                    @Override
                    public void completed(Void result, Void attachment) {
                        System.out.println("连接服务器成功");

                        // 发送消息
                        ByteBuffer buffer = ByteBuffer.wrap(
                                "Hello, AIO Server!".getBytes());
                        clientChannel.write(buffer);

                        // 读取响应
                        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                        clientChannel.read(readBuffer, readBuffer,
                                new CompletionHandler<Integer, ByteBuffer>() {
                                    @Override
                                    public void completed(Integer bytesRead,
                                                          ByteBuffer readBuf) {
                                        readBuf.flip();
                                        byte[] data = new byte[readBuf.remaining()];
                                        readBuf.get(data);
                                        System.out.println("服务器响应：" +
                                                new String(data));

                                        try {
                                            clientChannel.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void failed(Throwable exc, ByteBuffer att) {
                                        exc.printStackTrace();
                                    }
                                });
                    }

                    @Override
                    public void failed(Throwable exc, Void attachment) {
                        System.out.println("连接失败：" + exc.getMessage());
                    }
                });

        // 等待一段时间让异步操作完成
        Thread.sleep(3000);
    }
}
```

---

## 三、CompletionHandler回调接口

### 3.1 接口定义

```java
public interface CompletionHandler<V, A> {
    // 操作成功完成时的回调
    void completed(V result, A attachment);
    
    // 操作失败时的回调
    void failed(Throwable exc, A attachment);
}
```

### 3.2 使用场景

- ✅ IO操作完成通知
- ✅ 避免轮询等待
- ✅ 事件驱动编程

---

## 四、AIO的实现机制

### 4.1 Windows - IOCP

**IOCP（I/O Completion Port）**：

- Windows特有的高性能IO模型
- 基于完成端口的异步IO
- 自动线程管理

**工作流程：**

```
应用程序发起IO请求
    ↓
操作系统执行IO操作
    ↓
IO完成后放入完成队列
    ↓
工作线程从队列获取完成通知
```

### 4.2 Linux - AIO

**Linux AIO（Native AIO）**：

- Linux 2.6+ 支持
- 仅支持磁盘IO（O_DIRECT）
- 网络IO支持有限

**替代方案：**

- epoll + 非阻塞IO（Netty采用）
- io_uring（Linux 5.1+ 新特性）

---

## 五、三种IO模型对比

### 5.1 完整对比表

| 特性       | BIO     | NIO     | AIO       |
|----------|---------|---------|-----------|
| **中文名**  | 同步阻塞IO  | 同步非阻塞IO | 异步非阻塞IO   |
| **阻塞方式** | 阻塞      | 非阻塞     | 异步        |
| **实现难度** | 简单      | 复杂      | 最复杂       |
| **并发能力** | 低（~500） | 高（~10万） | 最高        |
| **适用系统** | 所有      | 所有      | Windows为主 |
| **典型应用** | 传统应用    | Netty   | Windows服务 |

### 5.2 选择建议

```
连接数 < 100    → BIO（简单）
连接数 100-1万   → NIO（Netty）
连接数 > 1万     → NIO（Netty调优）
Windows高性能   → AIO（IOCP）
Linux高并发     → NIO（epoll）
```

---

## 六、AIO相关面试题

### 1. AIO的工作原理是什么？

**参考答案：**

AIO（Asynchronous IO）是异步非阻塞IO。

**工作流程：**

1. 应用程序发起IO请求
2. 操作系统执行IO操作（不阻塞）
3. IO完成后通过回调通知应用程序
4. 应用程序处理结果

**特点：**

- 真正的异步
- 无需轮询
- 基于事件驱动

### 2. AIO和NIO的区别？

**参考答案：**

**NIO（同步非阻塞）：**

- 需要Selector轮询
- 应用程序主动检查就绪状态
- 同步等待IO就绪

**AIO（异步非阻塞）：**

- 基于CompletionHandler回调
- 操作系统完成IO后通知
- 无需轮询

### 3. AIO在哪些操作系统上支持？

**参考答案：**

**Windows：**

- 支持IOCP（I/O Completion Port）
- 性能优秀
- 广泛应用于Windows服务

**Linux：**

- 原生AIO支持有限
- 主要支持磁盘IO
- 网络IO推荐用epoll

**实际应用：**

- Netty默认使用NIO（epoll）
- Windows特定场景可用AIO

### 4. CompletionHandler的作用？

**参考答案：**

CompletionHandler是AIO的回调接口。

**两个方法：**

- `completed()`：IO操作成功完成时调用
- `failed()`：IO操作失败时调用

**优势：**

- 事件驱动
- 无需轮询
- 代码解耦

### 5. 为什么Netty不使用AIO？

**参考答案：**

**原因：**

1. Linux下AIO不成熟（主要支持磁盘IO）
2. epoll已经足够高效
3. AIO编程复杂度高
4. 跨平台兼容性考虑

**Netty的选择：**

- Linux：epoll（NIO）
- Windows：可选IOCP
- 默认：NIO + Reactor模式

---

## 七、示例代码位置

```
interview-transaction-demo/
└── src/main/java/cn/itzixiao/interview/transaction/
    └── io/
        ├── AioServerDemo.java      # AIO服务器
        └── AioClientDemo.java      # AIO客户端
```

---

## 八、下一步学习

- ⬅️ **[03-NIO同步非阻塞IO](./03-NIO同步非阻塞IO.md)** - NIO是基础
- 📚 **[中间件/Netty](../../09-中间件/README.md)** - NIO框架实战
- 🔗 **[Reactor模式](./03-NIO同步非阻塞IO.md#五reactor模式)** - 深入理解

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
