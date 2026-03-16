# NIO非阻塞IO详解

## 一、NIO核心组件

### 1.1 三大核心组件

| 组件           | 作用     | 特点       |
|--------------|--------|----------|
| **Channel**  | 数据传输通道 | 双向、异步    |
| **Buffer**   | 数据容器   | 直接内存、高效  |
| **Selector** | 多路复用器  | 单线程管理多连接 |

### 1.2 NIO vs BIO

| 特性       | BIO | NIO         |
|----------|-----|-------------|
| **IO模型** | 面向流 | 面向缓冲/通道     |
| **阻塞方式** | 阻塞  | 非阻塞         |
| **选择器**  | 无   | 有（Selector） |
| **并发能力** | 低   | 高           |

---

## 二、Buffer缓冲区

### 2.1 Buffer结构

```java
// 创建 Buffer
ByteBuffer buffer = ByteBuffer.allocate(1024);

// 写模式
buffer.put("Hello".getBytes());

// 切换为读模式
buffer.flip();

// 读取数据
byte[] data = new byte[buffer.remaining()];
buffer.get(data);

// 清空，切换为写模式
buffer.clear();
```

### 2.2 Buffer属性

```
capacity: 容量（固定不变）
position: 当前位置（读写位置）
limit: 限制（读模式下最多能读多少）
mark: 标记位置（可选）
```

**关系：** `0 <= mark <= position <= limit <= capacity`

---

## 三、Channel通道

### 3.1 Channel类型

| 类型                      | 用途     |
|-------------------------|--------|
| **FileChannel**         | 文件读写   |
| **SocketChannel**       | TCP客户端 |
| **ServerSocketChannel** | TCP服务端 |
| **DatagramChannel**     | UDP通信  |

### 3.2 FileChannel示例

```java
// 从文件读取
RandomAccessFile raf = new RandomAccessFile("input.txt", "r");
FileChannel inChannel = raf.getChannel();
ByteBuffer buffer = ByteBuffer.allocate(1024);
inChannel.read(buffer);

// 写入文件
RandomAccessFile rafOut = new RandomAccessFile("output.txt", "rw");
FileChannel outChannel = rafOut.getChannel();
buffer.flip(); // 切换为读模式
outChannel.write(buffer);
```

---

## 四、Selector选择器

### 4.1 Selector工作原理

```
Selector.select() - 阻塞等待就绪事件
    ↓
获取就绪的 SelectionKey
    ↓
遍历处理事件（读/写/连接/接受）
    ↓
循环继续
```

### 4.2 NIO服务器示例

```java
public class NioServerDemo {
    public static void main(String[] args) throws IOException {
        // 1. 打开Selector
        Selector selector = Selector.open();

        // 2. 创建ServerSocketChannel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false); // 设置为非阻塞
        serverChannel.bind(new InetSocketAddress(8080));

        // 3. 注册接受连接事件
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("NIO服务器启动...");

        // 4. 轮询就绪事件
        while (true) {
            int readyChannels = selector.select(); // 阻塞

            if (readyChannels == 0) continue;

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove(); // 必须移除

                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key);
                }
            }
        }
    }

    private static void handleAccept(SelectionKey key, Selector selector)
            throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        // 注册读事件
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("客户端连接：" + clientChannel.getRemoteAddress());
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        int bytesRead = clientChannel.read(buffer);
        if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String message = new String(data);
            System.out.println("收到消息：" + message);

            // 响应客户端
            ByteBuffer responseBuffer = ByteBuffer.wrap(
                    ("服务器收到：" + message).getBytes());
            clientChannel.write(responseBuffer);
        } else if (bytesRead == -1) {
            // 客户端断开
            key.cancel();
            clientChannel.close();
        }
    }
}
```

---

## 五、Reactor模式

### 5.1 单线程Reactor

```
┌─────────────────┐
│   Selector      │ ← 单线程处理所有事件
│   (主循环)      │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
  读事件    写事件
```

**特点：**

- ✅ 简单，无线程切换
- ❌ 性能低，一个线程处理所有
- 适用：低并发场景

### 5.2 多线程Reactor

```
┌──────────────┐
│ Main Reactor │ ← 主线程（accept）
└──────┬───────┘
       │
   ┌───┴───┐
   │       │
Sub1     Sub2  ← 子线程池（read/write）
```

**特点：**

- ✅ 性能提升
- ❌ 线程同步复杂
- 适用：中等并发

### 5.3 主从Reactor（Netty采用）

```
┌──────────────┐     ┌──────────────┐
│ Main Reactor │────>| Sub Reactor 1│
│   (Boss)     │     ├──────────────┤
└──────────────┘     │ Sub Reactor 2│
                     ├──────────────┤
                     │ Sub Reactor N│
                     └──────────────┘
```

**特点：**

- ✅ 高性能，职责分离
- ❌ 最复杂
- 适用：高并发（Netty、Redis）

---

## 六、高级特性

### 6.1 IO多路复用

**原理：** 一个线程同时监控多个连接

**实现方式：**

- **Select**：轮询所有FD（文件描述符）
- **Poll**：改进的Select
- **Epoll**（Linux）：事件驱动，最高效

### 6.2 零拷贝技术

**传统IO：** 4次上下文切换，4次数据拷贝

```
磁盘 → 内核缓冲 → 用户缓冲 → Socket缓冲 → 网络
```

**零拷贝（mmap）：** 2次上下文切换，2次数据拷贝

```
磁盘 → 内核缓冲 → Socket缓冲 → 网络
         ↑___________↓
```

**零拷贝（sendfile）：** 1次上下文切换，1次数据拷贝

```
磁盘 → 内核缓冲（带Socket描述符）→ 网络
```

### 6.3 直接内存

```java
// 堆外内存
ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);

// 优点：减少一次拷贝（直接从磁盘到网络）
// 缺点：分配回收开销大
```

---

## 七、NIO相关面试题

### 1. NIO的核心组件有哪些？

**参考答案：**

**三大核心组件：**

1. **Channel**：数据传输通道，双向
2. **Buffer**：数据容器，直接内存
3. **Selector**：多路复用器，单线程管理多连接

### 2. Selector的作用是什么？

**参考答案：**

Selector是NIO的核心，实现IO多路复用。

**作用：**

- 单线程同时监控多个连接
- 轮询就绪事件（读/写/连接/接受）
- 提高并发性能

### 3. BIO、NIO、AIO的区别？

**参考答案：**

| 特性       | BIO | NIO | AIO        |
|----------|-----|-----|------------|
| **模型**   | 阻塞  | 非阻塞 | 异步非阻塞      |
| **编程难度** | 简单  | 复杂  | 最复杂        |
| **并发能力** | 低   | 高   | 最高         |
| **适用场景** | 少连接 | 高并发 | Windows高性能 |

### 4. Reactor模式有哪些变体？

**参考答案：**

**三种变体：**

1. **单线程Reactor**：简单，性能低
2. **多线程Reactor**：性能提升，同步复杂
3. **主从Reactor**：高性能，Netty采用

### 5. Netty为什么性能高？

**参考答案：**

**原因：**

1. 采用主从Reactor模式
2. 零拷贝技术
3. 内存池优化
4. 高效的编解码
5. 无锁串行化设计

---

## 八、示例代码位置

```
interview-transaction-demo/
└── src/main/java/cn/itzixiao/interview/transaction/
    └── reactor/
        ├── NioServerDemo.java      # NIO服务器
        ├── SingleReactor.java      # 单线程Reactor
        ├── MultiReactor.java       # 多线程Reactor
        └── MainSubReactor.java     # 主从Reactor
```

---

## 九、下一步学习

- ➡️ **[03-AIO异步IO](./03-AIO异步IO详解.md)** - 更高效的IO模型
- ⬅️ **[01-BIO阻塞IO](./01-BIO阻塞IO详解.md)** - 理解BIO局限
- 📚 **[中间件/Netty](../09-中间件/README.md)** - NIO框架实战

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
