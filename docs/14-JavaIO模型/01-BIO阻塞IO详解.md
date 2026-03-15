# BIO同步阻塞IO详解

## 一、BIO工作原理

### 1.1 核心特点

**同步阻塞（Blocking IO）**：
- ✅ 简单易用
- ❌ 阻塞式调用
- ❌ 一对一模型（一个连接一个线程）

### 1.2 工作流程

```
客户端                服务器端
  |                     |
  |--connect 请求-------->|
  |                     | 接受连接 (accept) - 阻塞
  |<--建立连接-----------|
  |                     |
  |--read/write 请求----->|
  |                     | 处理请求 - 阻塞
  |<--响应--------------|
  |                     |
```

---

## 二、代码示例

### 2.1 BIO服务器

```java
public class BioServerDemo {
    private static final int PORT = 8080;
    private static final ExecutorService executor = 
        Executors.newFixedThreadPool(10);
    
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("服务器启动，监听端口：" + PORT);
        
        while (true) {
            // 阻塞等待客户端连接
            Socket socket = serverSocket.accept();
            System.out.println("新客户端连接：" + socket.getRemoteSocketAddress());
            
            // 提交到线程池处理
            executor.submit(() -> handleClient(socket));
        }
    }
    
    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true)) {
            
            String inputLine;
            // 阻塞读取客户端数据
            while ((inputLine = in.readLine()) != null) {
                String response = process(inputLine);
                out.println(response); // 阻塞写入
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static String process(String input) {
        return "服务器收到：" + input;
    }
}
```

### 2.2 BIO客户端

```java
public class BioClientDemo {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 8080);
        
        try (PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
            
            // 发送消息
            out.println("Hello, Server!");
            
            // 接收响应
            String response = in.readLine();
            System.out.println("服务器响应：" + response);
        }
    }
}
```

---

## 三、BIO模型分析

### 3.1 一对一模式

```
连接1 -----> 线程1
连接2 -----> 线程2
连接3 -----> 线程3
...
连接N -----> 线程N
```

**问题：**
- 每个连接都需要独立线程
- 线程资源有限（通常最多几百个）
- 大量空闲连接浪费线程资源

### 3.2 性能瓶颈

| 指标 | 数值 | 说明 |
|------|------|------|
| **最大连接数** | ~500 | 受线程数限制 |
| **并发能力** | 低 | 阻塞导致资源浪费 |
| **适用场景** | < 100 连接 | 内部系统、测试环境 |

---

## 四、BIO的优缺点

### 4.1 优点

✅ **简单易用**：API直观，容易理解  
✅ **编程简单**：同步代码，逻辑清晰  
✅ **适合少连接**：连接数固定且较少时表现良好

### 4.2 缺点

❌ **扩展性差**：无法支持高并发  
❌ **资源浪费**：大量线程等待IO  
❌ **性能低下**：线程切换开销大  
❌ **O(N)复杂度**：需要N个线程处理N个连接

---

## 五、应用场景

### 5.1 适合使用BIO的场景

- ✅ 内部管理系统（连接数<50）
- ✅ 快速原型开发
- ✅ 学习和理解IO基础
- ✅ 低并发、长连接场景

### 5.2 不适合使用BIO的场景

- ❌ 互联网高并发应用
- ❌ 即时通讯系统
- ❌ 游戏服务器
- ❌ 金融交易系统

---

## 六、BIO相关面试题

### 1. BIO的工作原理是什么？

**参考答案：**

BIO（Blocking IO）是同步阻塞IO模型。

**工作流程：**
1. 服务器通过 `ServerSocket.accept()` 阻塞等待连接
2. 建立连接后，通过 `Socket.read()/write()` 阻塞读写
3. 一个连接对应一个线程处理

**特点：**
- 阻塞式调用
- 一对一模型
- 简单易用但扩展性差

### 2. BIO的优缺点？

**参考答案：**

**优点：**
- API简单，易于理解和编程
- 适合连接数较少的场景
- 代码逻辑清晰

**缺点：**
- 每个连接需要一个线程，资源消耗大
- 无法支持高并发
- 线程切换开销大
- 扩展性差

### 3. BIO适用于什么场景？

**参考答案：**

**适用场景：**
- 连接数固定且较少（<100）
- 内部管理系统
- 快速原型开发
- 低并发、长连接

**不适用场景：**
- 高并发互联网应用
- 即时通讯
- 游戏服务器

### 4. 如何改进BIO的性能？

**参考答案：**

**改进方案：**
1. **使用线程池**：复用线程，减少创建销毁开销
2. **改为NIO**：使用非阻塞IO+多路复用
3. **使用Netty**：成熟的NIO框架
4. **连接复用**：减少连接数

---

## 七、示例代码位置

```
interview-transaction-demo/
└── src/main/java/cn/itzixiao/interview/transaction/
    └── io/
        ├── BioServerDemo.java      # BIO服务器示例
        └── BioClientDemo.java      # BIO客户端示例
```

---

## 八、下一步学习

- ➡️ **[03-NIO同步非阻塞IO](./03-NIO同步非阻塞IO.md)** - 解决BIO性能问题
- ⬅️ **[传统IO流](../../01-Java基础/09-Java-IO与NIO.md)** - 字节流、字符流基础

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
