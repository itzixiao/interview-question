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

## 五、Reactor模式详解

### 5.1 Reactor模式概述

**Reactor模式** 是一种基于事件驱动的设计模式，用于处理一个或多个客户端并发交付给应用程序的服务请求。它是NIO编程的核心架构模式，被广泛应用于高性能网络服务器框架（如Netty、Redis、Nginx）。

**核心思想：**
- **事件驱动**：IO事件触发回调处理
- **分离关注点**：IO操作与业务逻辑分离
- **单线程多路复用**：一个线程管理多个连接

**Reactor模式的五个核心角色：**

| 角色 | 职责 | 对应NIO组件 |
|------|------|------------|
| **Handle（句柄）** | 标识事件资源 | SocketChannel |
| **Synchronous Event Demultiplexer** | 同步事件分离器 | Selector |
| **Event Handler** | 事件处理器接口 | SelectionKey + Handler |
| **Concrete Event Handler** | 具体事件处理器 | 自定义Handler |
| **Initiation Dispatcher** | 初始分发器 | Reactor主循环 |

**工作流程：**

```
┌─────────────────────────────────────────────────────────────┐
│                    Reactor 工作流程                          │
├─────────────────────────────────────────────────────────────┤
│  1. 注册Handler → 2. 轮询事件 → 3. 分发事件 → 4. 回调处理    │
└─────────────────────────────────────────────────────────────┘
```

---

### 5.2 单线程Reactor

**架构图：**

```
┌─────────────────────────────────────────────────────────────┐
│                    单线程Reactor架构                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │              Reactor Thread (单线程)                 │  │
│   │  ┌─────────────────┐    ┌─────────────────────────┐ │  │
│   │  │   Selector      │    │   Event Loop (while)    │ │  │
│   │  │  (事件多路复用)  │←──→│  1. select() 阻塞等待    │ │  │
│   │  └────────┬────────┘    │  2. 获取就绪Channel      │ │  │
│   │           │             │  3. dispatch() 分发      │ │  │
│   │    ┌──────┴──────┐      │  4. handle() 处理        │ │  │
│   │    │             │      └─────────────────────────┘ │  │
│   │  读事件        写事件                                  │  │
│   │    │             │                                    │  │
│   │    ▼             ▼                                    │  │
│   │ ┌──────┐     ┌──────┐                                 │  │
│   │ │Accept│     │ Read │                                 │  │
│   │ │Handler│    │Handler│ ← 业务处理也在同一线程           │  │
│   │ └──┬───┘     └──┬───┘                                 │  │
│   │    │             │                                    │  │
│   │    └──────┬──────┘                                    │  │
│   │           ▼                                           │  │
│   │      SocketChannel                                    │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**实现代码示例：**

```java
public class SingleReactor {
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    
    public SingleReactor(int port) throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        
        // 注册Accept事件
        SelectionKey key = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(new AcceptorHandler());
    }
    
    public void run() {
        while (!Thread.interrupted()) {
            try {
                selector.select(); // 阻塞等待事件
                Set<SelectionKey> selected = selector.selectedKeys();
                Iterator<SelectionKey> it = selected.iterator();
                
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove(); // 必须移除，否则重复处理
                    dispatch(key); // 分发事件
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void dispatch(SelectionKey key) {
        Runnable handler = (Runnable) key.attachment();
        if (handler != null) {
            handler.run(); // 直接在当前线程执行
        }
    }
}
```

**特点分析：**

| 维度 | 说明 |
|------|------|
| **优点** | ✅ 实现简单，无线程切换开销<br>✅ 无锁编程，无并发问题<br>✅ 适用于IO密集型、业务逻辑简单的场景 |
| **缺点** | ❌ 单线程处理所有IO和业务逻辑<br>❌ 一个Handler阻塞会影响整个系统<br>❌ 无法利用多核CPU |
| **适用场景** | 低并发（<1000连接）、业务处理极快的场景<br>如：Redis早期版本、简单的代理服务 |

---

### 5.3 多线程Reactor

**架构图：**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       多线程Reactor架构                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    Main Reactor Thread (主线程)                  │   │
│  │  ┌─────────────────┐         ┌───────────────────────────────┐  │   │
│  │  │   Selector      │         │  只处理 ACCEPT 事件            │  │   │
│  │  │  (监听新连接)    │←───────→│  - serverSocket.accept()      │  │   │
│  │  └────────┬────────┘         │  - 将新连接交给Sub Reactor    │  │   │
│  │           │                  └───────────────────────────────┘  │   │
│  │           │                                                     │   │
│  │           ▼                                                     │   │
│  │    ┌──────────────┐                                             │   │
│  │    │ 新连接Socket  │────────────────────────────────────────┐   │   │
│  │    └──────────────┘                                        │   │   │
│  └────────────────────────────────────────────────────────────┼───┘   │
│                                                               │        │
│  ┌────────────────────────────────────────────────────────────┼───┐   │
│  │                    Worker Thread Pool (线程池)              │   │   │
│  │                                                            ▼   │   │
│  │  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐ │
│  │  │  Sub Reactor 1  │    │  Sub Reactor 2  │    │  Sub Reactor N  │ │
│  │  │  ┌───────────┐  │    │  ┌───────────┐  │    │  ┌───────────┐  │ │
│  │  │  │ Selector  │  │    │  │ Selector  │  │    │  │ Selector  │  │ │
│  │  │  │ (子选择器) │  │    │  │ (子选择器) │  │    │  │ (子选择器) │  │ │
│  │  │  └─────┬─────┘  │    │  └─────┬─────┘  │    │  └─────┬─────┘  │ │
│  │  │        │        │    │        │        │    │        │        │ │
│  │  │   ┌────┴────┐   │    │   ┌────┴────┐   │    │   ┌────┴────┐   │ │
│  │  │   ▼         ▼   │    │   ▼         ▼   │    │   ▼         ▼   │ │
│  │  │ Read/Write     │    │ Read/Write     │    │ Read/Write     │ │
│  │  │ (IO操作+业务)   │    │ (IO操作+业务)   │    │ (IO操作+业务)   │ │
│  │  └─────────────────┘    └─────────────────┘    └─────────────────┘ │
│  └────────────────────────────────────────────────────────────────────┘
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**实现代码示例：**

```java
public class MultiThreadReactor {
    private final Selector mainSelector;
    private final ExecutorService workerPool;
    private final int workerCount;
    
    public MultiThreadReactor(int port, int workerCount) throws IOException {
        this.workerCount = workerCount;
        this.mainSelector = Selector.open();
        this.workerPool = Executors.newFixedThreadPool(workerCount);
        
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(mainSelector, SelectionKey.OP_ACCEPT);
    }
    
    public void run() {
        while (!Thread.interrupted()) {
            try {
                mainSelector.select();
                Iterator<SelectionKey> it = mainSelector.selectedKeys().iterator();
                
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    
                    if (key.isAcceptable()) {
                        // 主线程只处理accept
                        handleAccept(key);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        
        // 提交给工作线程处理IO
        workerPool.submit(new WorkerHandler(client));
    }
    
    // 工作线程处理读写
    class WorkerHandler implements Runnable {
        private final SocketChannel client;
        private final Selector selector;
        
        WorkerHandler(SocketChannel client) throws IOException {
            this.client = client;
            this.selector = Selector.open();
            client.register(selector, SelectionKey.OP_READ, this);
        }
        
        @Override
        public void run() {
            // 处理读写事件
            try {
                selector.select();
                // ... 处理IO事件
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

**特点分析：**

| 维度 | 说明 |
|------|------|
| **优点** | ✅ 主线程只处理accept，职责清晰<br>✅ 工作线程池处理IO，提升并发能力<br>✅ 可以充分利用多核CPU |
| **缺点** | ❌ 线程切换开销增加<br>❌ 线程同步复杂，需要处理竞态条件<br>❌ 子Reactor与Handler仍可能在同一线程 |
| **适用场景** | 中等并发（1000-10000连接）<br>业务处理有一定耗时，但不算复杂的场景 |

---

### 5.4 主从Reactor（Netty采用）

**架构图：**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         主从Reactor架构 (Netty模型)                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                     Boss Group (主Reactor组)                         │  │
│   │  ┌─────────────────────────────────────────────────────────────┐   │  │
│   │  │  Main Reactor (Boss) - 通常1个线程                           │   │  │
│   │  │  ┌─────────────────┐         ┌───────────────────────────┐  │   │  │
│   │  │  │   Selector      │         │  职责：                   │  │   │  │
│   │  │  │  (监听连接事件)  │←───────→│  1. 监听端口              │  │   │  │
│   │  │  └────────┬────────┘         │  2. 处理OP_ACCEPT         │  │   │  │
│   │  │           │                  │  3. 将Socket注册到Worker  │  │   │  │
│   │  │           ▼                  └───────────────────────────┘  │   │  │
│   │  │    ┌──────────────┐                                         │   │  │
│   │  │    │ 新连接Channel │────────────────────────────────────┐   │   │  │
│   │  │    └──────────────┘                                    │   │   │  │
│   │  └────────────────────────────────────────────────────────┼───┘   │  │
│   └───────────────────────────────────────────────────────────┼───────┘  │
│                                                               │           │
│   ┌───────────────────────────────────────────────────────────┼───────┐  │
│   │                     Worker Group (从Reactor组)             │       │  │
│   │                                                           ▼       │  │
│   │  ┌─────────────────────────────────────────────────────────────┐ │  │
│   │  │  Sub Reactor 1          Sub Reactor 2          Sub Reactor N│ │  │
│   │  │  (NIO EventLoop)        (NIO EventLoop)        (NIO EventLoop)│ │  │
│   │  │  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐│ │  │
│   │  │  │    Selector     │   │    Selector     │   │    Selector     ││ │  │
│   │  │  │ (管理多个Socket) │   │ (管理多个Socket) │   │ (管理多个Socket) ││ │  │
│   │  │  └────────┬────────┘   └────────┬────────┘   └────────┬────────┘│ │  │
│   │  │           │                     │                     │         │ │  │
│   │  │      ┌────┴────┐           ┌────┴────┐           ┌────┴────┐    │ │  │
│   │  │      ▼         ▼           ▼         ▼           ▼         ▼    │ │  │
│   │  │   Socket1   Socket2    Socket3   Socket4    Socket5   Socket6   │ │  │
│   │  │      │         │          │         │          │         │      │ │  │
│   │  │      ▼         ▼          ▼         ▼          ▼         ▼      │ │  │
│   │  │   Handler   Handler    Handler   Handler    Handler   Handler   │ │  │
│   │  │   (IO+业务)  (IO+业务)   (IO+业务)  (IO+业务)   (IO+业务)  (IO+业务)│ │  │
│   │  │                                                                 │ │  │
│   │  │  特点：每个Sub Reactor是独立线程，负责一组连接的IO读写+编解码    │ │  │
│   │  │  优势：连接均匀分配，避免单个线程过载                            │ │  │
│   │  └─────────────────────────────────────────────────────────────────┘ │  │
│   └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   实际部署建议：                                                             │
│   - Boss Group: 1个线程（除非绑定多个端口）                                  │
│   - Worker Group: CPU核心数 * 2（默认），可根据业务调整                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Netty中的实现对应：**

```java
// Netty主从Reactor模型配置
public class NettyServer {
    public static void main(String[] args) {
        // Boss Group: 主Reactor，处理连接建立
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        
        // Worker Group: 从Reactor，处理IO读写
        EventLoopGroup workerGroup = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors() * 2
        );
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                            new DecoderHandler(),   // 解码
                            new BusinessHandler(),  // 业务处理
                            new EncoderHandler()    // 编码
                        );
                    }
                });
            
            ChannelFuture future = bootstrap.bind(8080).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

**三种Reactor模式对比：**

| 特性 | 单线程Reactor | 多线程Reactor | 主从Reactor |
|------|-------------|--------------|------------|
| **线程数** | 1 | 1+N | M+N（通常M=1） |
| **Accept处理** | 主线程 | 主线程 | Boss线程 |
| **IO处理** | 主线程 | 工作线程 | Worker线程 |
| **业务处理** | 主线程 | 工作线程 | Worker线程（可再分发） |
| **并发能力** | 低 | 中 | **高** |
| **复杂度** | 低 | 中 | 高 |
| **适用场景** | 低并发 | 中等并发 | **高并发** |
| **代表框架** | Redis早期 | 自定义实现 | **Netty、Nginx** |

**主从Reactor的核心优势：**

1. **职责完全分离**
   - Boss只关注连接建立，快速响应
   - Worker专注IO处理，互不干扰

2. **负载均衡**
   - 新连接轮询分配到不同Worker
   - 避免单个线程处理过多连接

3. **水平扩展**
   - 增加Worker线程即可提升处理能力
   - 充分利用多核CPU

4. **高可用性**
   - 单个Worker异常不影响其他连接
   - 可以动态调整线程数

**适用场景：**
- 高并发服务器（>10000连接）
- 长连接应用（游戏、IM、推送服务）
- 对延迟敏感的业务场景
- **Netty、Redis、Nginx、Memcached** 均采用此模式

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
