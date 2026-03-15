# BIO 同步阻塞 IO

## 一、什么是 BIO？

BIO（Blocking IO）是传统的**同步阻塞 IO**模型，所有的 IO 操作（连接、读取、写入）都会阻塞当前线程，直到操作完成或发生错误。

---

## 二、核心特点

### 2.1 同步阻塞

✅ **accept() 阻塞**：等待客户端连接  
✅ **read() 阻塞**：等待数据到达  
✅ **write() 阻塞**：等待数据发送完成

### 2.2 一对一模型

- 一个连接需要一个独立线程处理
- 线程数 = 连接数
- 资源消耗大

### 2.3 简单易懂

- 编程模型最简单
- 代码直观易读

---

## 三、工作流程

```
Step1: 服务器启动，创建 ServerSocket，绑定端口
Step2: 调用 accept() 阻塞等待客户端连接
Step3: 客户端连接成功，创建新线程处理
Step4: 在线程中调用 read() 阻塞读取数据
Step5: 处理数据，调用 write() 阻塞响应
Step6: 通信结束，关闭连接和线程
```

---

## 四、代码示例

```java
package cn.itzixiao.interview.transaction.io;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BioServerDemo {
    
    // 线程池
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public static void main(String[] args) throws IOException {
        // 创建 ServerSocket，绑定端口
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("BIO 服务器启动，监听端口：" + 8080);
        
        while (true) {
            // accept() 阻塞等待客户端连接
            Socket socket = serverSocket.accept();
            System.out.println("新客户端连接：" + socket.getInetAddress());
            
            // 创建新线程处理
            executor.submit(() -> handleClient(socket));
        }
    }
    
    /**
     * 处理客户端请求
     */
    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true)) {
            
            String inputLine;
            // read() 阻塞读取数据
            while ((inputLine = in.readLine()) != null) {
                System.out.println("收到客户端消息：" + inputLine);
                
                // 处理业务逻辑
                String response = process(inputLine);
                
                // write() 阻塞响应
                out.println(response);
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
    
    /**
     * 处理业务逻辑
     */
    private static String process(String message) {
        return "服务器收到：" + message;
    }
}
```

---

## 五、优缺点分析

### 5.1 优点

- ✓ 编程简单，易于理解和维护
- ✓ 适合连接数少且固定的场景
- ✓ 不需要复杂的 IO 多路复用知识

### 5.2 缺点

- ✗ 线程资源消耗大（每个连接一个线程）
- ✗ 高并发下性能差（线程切换开销大）
- ✗ 无法处理百万级并发（受限于最大线程数）
- ✗ 资源利用率低（大量线程等待 IO）

---

## 六、应用场景

- ✓ 连接数较少的内部系统（<100 个连接）
- ✓ 对性能要求不高的应用
- ✓ 学习和理解 IO 的基础模型
- ✓ 快速原型开发

---

## 七、BIO 相关面试题

### 1. 什么是 BIO？工作原理是什么？

**参考答案：**

BIO（Blocking IO）是同步阻塞 IO模型。

**工作原理：**
1. 服务器创建 ServerSocket，绑定端口
2. 调用 accept() 阻塞等待客户端连接
3. 连接成功后创建新线程处理
4. 在线程中通过 read()/write() 进行阻塞式读写
5. 通信结束后关闭连接和线程

### 2. BIO 的优缺点有哪些？

**优点：**
- ✅ 编程简单直观
- ✅ 易于理解和维护
- ✅ 适合少连接场景

**缺点：**
- ❌ 线程消耗大
- ❌ 并发能力低
- ❌ 资源利用率低

### 3. 为什么 BIO 不适合高并发场景？

**参考答案：**

1. **线程资源限制**：
   - 一个连接一个线程
   - 线程数受限于系统资源（通常几千个）

2. **线程切换开销**：
   - 大量线程导致频繁上下文切换
   - CPU 时间片浪费在调度上

3. **内存消耗**：
   - 每个线程需要独立的栈空间（默认 1MB）
   - 1000 个线程 = 1GB 栈空间

### 4. BIO 的线程模型是什么？

**参考答案：**

BIO 采用**一对一**的线程模型：
- 一个连接对应一个独立线程
- 线程数 = 连接数
- 线程独占，无法复用

---

## 八、API 接口测试

```bash
# BIO 详解
curl http://localhost:8084/api/io/bio-summary
```

---

## 九、示例代码位置

```
interview-transaction-demo/
├── src/main/java/cn/itzixiao/interview/transaction/
│   └── io/
│       └── BioServerDemo.java        # BIO 示例
└── src/main/resources/
    └── application.yml               # 配置文件
```

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
