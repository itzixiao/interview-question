# Java IO 与 NIO

## IO 分类

### 按流向

| 类型 | 说明 |
|------|------|
| 输入流 | 从外部读入数据（InputStream/Reader）|
| 输出流 | 向外部写出数据（OutputStream/Writer）|

### 按处理单位

| 类型 | 说明 | 抽象类 |
|------|------|--------|
| 字节流 | 处理二进制数据 | InputStream/OutputStream |
| 字符流 | 处理文本数据 | Reader/Writer |

### 按功能

| 类型 | 说明 | 示例 |
|------|------|------|
| 节点流 | 直接操作数据源 | FileInputStream |
| 处理流 | 包装节点流，提供额外功能 | BufferedInputStream |

## 字节流 vs 字符流

### 字节流

```java
// FileInputStream/FileOutputStream
FileInputStream fis = new FileInputStream("file.txt");
byte[] buffer = new byte[1024];
int len;
while ((len = fis.read(buffer)) != -1) {
    // 处理 buffer
}
```

### 字符流

```java
// FileReader/FileWriter（使用系统默认编码，不推荐）
FileReader fr = new FileReader("file.txt");

// 推荐：指定编码
InputStreamReader isr = new InputStreamReader(
    new FileInputStream("file.txt"), StandardCharsets.UTF_8);
```

### 转换

```java
// 字节流 → 字符流
InputStreamReader isr = new InputStreamReader(inputStream, charset);

// 字符流 → 字节流
OutputStreamWriter osw = new OutputStreamWriter(outputStream, charset);
```

## 常用处理流

### Buffered 流

```java
// 带缓冲的字节流
BufferedInputStream bis = new BufferedInputStream(
    new FileInputStream("file.txt"));

// 带缓冲的字符流
BufferedReader br = new BufferedReader(
    new FileReader("file.txt"));
String line;
while ((line = br.readLine()) != null) {
    System.out.println(line);
}
```

**缓冲大小**：默认 8KB

### Data 流

```java
// 读写基本数据类型
DataOutputStream dos = new DataOutputStream(
    new FileOutputStream("data.bin"));
dos.writeInt(100);
dos.writeDouble(3.14);
dos.writeBoolean(true);
dos.writeUTF("Hello");

DataInputStream dis = new DataInputStream(
    new FileInputStream("data.bin"));
int i = dis.readInt();
double d = dis.readDouble();
```

### Object 流（序列化）

```java
// 序列化
ObjectOutputStream oos = new ObjectOutputStream(
    new FileOutputStream("obj.ser"));
oos.writeObject(object);

// 反序列化
ObjectInputStream ois = new ObjectInputStream(
    new FileInputStream("obj.ser"));
Object obj = ois.readObject();
```

**注意**：
- 实现 `Serializable` 接口
- 定义 `serialVersionUID`
- `transient` 修饰的字段不序列化

## NIO（New IO）

### 核心组件

| 组件 | 说明 |
|------|------|
| Channel | 数据传输通道，双向 |
| Buffer | 数据容器 |
| Selector | 多路复用器 |

### Buffer

```java
// 创建 Buffer
ByteBuffer buffer = ByteBuffer.allocate(1024);

// 写入数据（写模式）
buffer.put("Hello".getBytes());

// 切换为读模式
buffer.flip();

// 读取数据
byte[] data = new byte[buffer.limit()];
buffer.get(data);

// 清空，切换为写模式
buffer.clear();
```

**Buffer 属性**：
- capacity：容量
- position：当前位置
- limit：限制
- mark：标记位置

### Channel

```java
// FileChannel
RandomAccessFile raf = new RandomAccessFile("file.txt", "rw");
FileChannel channel = raf.getChannel();

// 从 Channel 读入 Buffer
channel.read(buffer);

// 从 Buffer 写入 Channel
channel.write(buffer);
```

**Channel 类型**：
- FileChannel：文件
- SocketChannel：TCP 客户端
- ServerSocketChannel：TCP 服务端
- DatagramChannel：UDP

### NIO.2（Path/Files）

```java
// Path
Path path = Paths.get("/home/user/file.txt");
Path fileName = path.getFileName();
Path parent = path.getParent();

// Files 操作
byte[] bytes = Files.readAllBytes(path);
List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
Files.write(path, "content".getBytes());
Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
Files.move(source, target);
Files.delete(path);

// 遍历目录
try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
    for (Path entry : stream) {
        System.out.println(entry.getFileName());
    }
}

// 递归遍历
Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        System.out.println(file);
        return FileVisitResult.CONTINUE;
    }
});
```

## 零拷贝（Zero Copy）

### 传统 IO 拷贝

```
1. 磁盘 → 内核缓冲区（DMA 拷贝）
2. 内核缓冲区 → 用户缓冲区（CPU 拷贝）
3. 用户缓冲区 → Socket 缓冲区（CPU 拷贝）
4. Socket 缓冲区 → 网卡（DMA 拷贝）
共 4 次拷贝，3 次状态切换
```

### NIO 零拷贝

```java
FileChannel sourceChannel = new RandomAccessFile("source.txt", "r").getChannel();
FileChannel destChannel = new RandomAccessFile("dest.txt", "rw").getChannel();

// transferTo：零拷贝传输
sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
```

```
1. 磁盘 → 内核缓冲区（DMA 拷贝）
2. 内核缓冲区 → Socket 缓冲区（CPU 拷贝，数据描述符）
3. Socket 缓冲区 → 网卡（DMA 拷贝）
共 3 次拷贝，2 次状态切换
```

## IO 模型对比

| 模型 | 特点 | 适用场景 |
|------|------|----------|
| BIO（阻塞 IO）| 同步阻塞，一个连接一个线程 | 连接数少 |
| NIO（非阻塞 IO）| 同步非阻塞，多路复用 | 连接数多，数据量小 |
| AIO（异步 IO）| 异步非阻塞，回调通知 | 连接数多，数据量大 |

### BIO

```java
ServerSocket serverSocket = new ServerSocket(8080);
while (true) {
    Socket socket = serverSocket.accept();  // 阻塞
    new Thread(() -> {
        // 处理请求
    }).start();
}
```

### NIO

```java
Selector selector = Selector.open();
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.bind(new InetSocketAddress(8080));
serverChannel.configureBlocking(false);
serverChannel.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();  // 阻塞等待就绪事件
    Set<SelectionKey> keys = selector.selectedKeys();
    for (SelectionKey key : keys) {
        if (key.isAcceptable()) {
            // 处理连接
        } else if (key.isReadable()) {
            // 处理读
        }
    }
}
```

## 最佳实践

1. **指定编码**：始终指定字符编码，避免乱码
2. **使用缓冲**：Buffered 流提高性能
3. **及时关闭**：使用 try-with-resources
4. **大文件处理**：使用 NIO 的 Channel 和 Buffer
5. **序列化注意**：定义 serialVersionUID，控制序列化字段

---

## 💡 高频面试题

**问题 1：字节流和字符流有什么区别？**

| 对比项 | 字节流 | 字符流 |
|--------|--------|--------|
| 处理单位 | 8位字节 | 16位Unicode字符 |
| 基类 | InputStream/OutputStream | Reader/Writer |
| 适用场景 | 二进制文件（图片、音视频） | 文本文件（txt、xml、json） |
| 编码处理 | 不处理编码 | 自动处理编码转换 |

**关键点：**
- 字符流 = 字节流 + 编码表
- InputStreamReader/OutputStreamWriter 是字节流和字符流的桥梁

**问题 2:BIO、NIO、AIO 有什么区别？**

| 对比项 | BIO | NIO | AIO |
|--------|-----|-----|-----|
| 全称 | Blocking IO | Non-blocking IO | Async IO |
| 阻塞性 | 阻塞 | 非阻塞 | 非阻塞 |
| 同步/异步 | 同步 | 同步 | 异步 |
| 线程模型 | 一连接一线程 | 一线程多连接 | 回调模型 |
| 核心组件 | Stream | Channel/Buffer/Selector | Future/Callback |
| 适用场景 | 连接数少且固定 | 连接数多，数据量小 | 连接数多，数据量大 |
| 例子 | 传统 Socket | Netty | Windows IOCP |

**问题 3：为什么需要 BufferedInputStream/BufferedOutputStream？**

**问题：** 直接使用 FileInputStream 每次读取一个字节：
- 每次读取都涉及系统调用（用户态→内核态切换）
- 频繁的系统调用开销大

**解决：** BufferedInputStream 内部维护一个缓冲区（默认8KB）：
- 一次性从磁盘读取 8KB 到缓冲区
- 后续读取直接从缓冲区获取（内存操作）
- 大大减少系统调用次数

**性能提升：**
- 无缓冲：读取 1MB 需要 ~100万次系统调用
- 有缓冲：读取 1MB 只需 ~128次系统调用（1MB/8KB）

**问题 4:serialVersionUID 有什么作用？**

用于版本控制，反序列化时验证版本一致性：
- 一致：正常反序列化
- 不一致：抛出 InvalidClassException

**最佳实践：** 显式定义
```java
private static final long serialVersionUID = 1L;
```

**问题 5:transient 关键字的作用？**

transient 修饰的字段不会被序列化。

**使用场景：**
1. 敏感信息：密码、密钥等
2. 不可序列化对象：如 Thread、Socket
3. 可计算字段：如缓存、派生值

**注意：** 反序列化后 transient 字段为默认值（null/0/false）

**问题 6：什么是零拷贝？Java 如何实现？**

**定义：** 减少/避免数据在用户态和内核态之间的拷贝

**传统 IO 拷贝流程（4次拷贝，3次上下文切换）：**
```
磁盘 → 内核缓冲区 → 用户缓冲区 → Socket缓冲区 → 网卡
      (DMA)      (CPU)        (CPU)       (DMA)
```

**Java 实现：**
```java
FileChannel src = new FileInputStream(src).getChannel();
FileChannel dest = new FileOutputStream(dest).getChannel();
src.transferTo(0, src.size(), dest);  // 零拷贝传输
```

**应用场景：** Kafka、Netty 等高性能框架

**问题 7:Java IO 使用了什么设计模式？**

**装饰器模式（Decorator Pattern）：**
- 抽象构件：InputStream/OutputStream/Reader/Writer
- 具体构件：FileInputStream/ByteArrayInputStream
- 装饰器基类：FilterInputStream/FilterOutputStream
- 具体装饰器：BufferedInputStream/DataInputStream

```java
InputStream is = new BufferedInputStream(
    new DataInputStream(
        new FileInputStream("file.txt")));
```

**适配器模式：**
- InputStreamReader/OutputStreamWriter 将字节流适配为字符流

**问题 8:try-with-resources 的原理是什么？**

**语法糖：** 自动关闭实现 AutoCloseable 的资源

```java
// 源代码
try (FileInputStream fis = new FileInputStream("file.txt")) {
    // 使用 fis
}

// 编译后等价代码
FileInputStream fis = new FileInputStream("file.txt");
try {
    // 使用 fis
} finally {
    if (fis != null) {
        fis.close();
    }
}
```

**问题 9:Files.readAllLines() 为什么不适合读大文件？**

一次性将所有行加载到内存，大文件会导致 OOM。

**正确做法：流式处理**
```java
// 方式1：Files.lines()（推荐）
try (Stream<String> lines = Files.lines(path)) {
    lines.forEach(line -> process(line));
}

// 方式2：BufferedReader
try (BufferedReader br = Files.newBufferedReader(path)) {
    String line;
    while ((line = br.readLine()) != null) {
        process(line);
    }
}
```

**问题 10:NIO 的 Buffer 为什么需要 flip()？**

**Buffer 核心属性：**
- capacity：缓冲区容量（固定不变）
- position：当前位置指针
- limit：限制位置

**写模式后：**
- position = 写入的字节数
- limit = capacity

**flip() 的作用：切换到读模式**
- limit = position（限制 = 已写入的数据量）
- position = 0（回到开头准备读取）

**问题 11:Java IO 乱码问题如何解决？**

**乱码原因：** 读写编码不一致

**正确做法：** 始终显式指定编码
```java
// 方式1：InputStreamReader
InputStreamReader isr = new InputStreamReader(
    new FileInputStream("file.txt"), StandardCharsets.UTF_8);

// 方式2：Files.newBufferedReader
BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8);

// 方式3：Files.readAllLines
List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
```

**问题 12：如何实现文件断点续传？**

**核心思路：** 记录已传输位置，从断点处继续

```java
public void downloadWithResume(String url, File localFile) {
    long startPos = localFile.exists() ? localFile.length() : 0;
    
    // HTTP Range 请求
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setRequestProperty("Range", "bytes=" + startPos + "-");
    
    // RandomAccessFile 随机写入
    try (RandomAccessFile raf = new RandomAccessFile(localFile, "rw")) {
        raf.seek(startPos);  // 定位到断点位置
        // 写入数据...
    }
}
```

**关键点：**
1. HTTP Range 头指定下载范围
2. RandomAccessFile.seek() 定位到断点
3. 本地记录已下载位置
4. 服务端需支持 Range 请求

**问题 13：节点流和处理流的区别？**

| 类型 | 说明 | 示例 |
|------|------|------|
| 节点流 | 直接操作数据源 | FileInputStream、FileReader |
| 处理流 | 包装节点流，增强功能 | BufferedInputStream、DataInputStream |

**处理流的优点：**
- 提供缓冲功能，提高读写效率
- 提供数据类型转换功能
- 灵活组合，增强扩展性

**问题 14:RandomAccessFile 有什么特点？**

**特点：**
- 可读可写，支持随机访问
- 通过 seek() 定位到任意位置
- 适合：断点续传、文件分片、多线程下载

**模式：**
| 模式 | 说明 |
|------|------|
| r | 只读 |
| rw | 读写，文件不存在则创建 |
| rwd | 读写，同步更新到磁盘（内容） |
| rws | 读写，同步更新到磁盘（内容+元数据） |

**问题 15:NIO Selector 的作用是什么？**

**作用：** 多路复用器，一个线程管理多个 Channel

**SelectionKey 事件类型：**
| 事件 | 说明 |
|------|------|
| OP_ACCEPT | 连接就绪，ServerSocketChannel 专用 |
| OP_CONNECT | 连接就绪，SocketChannel 专用 |
| OP_READ | 读就绪 |
| OP_WRITE | 写就绪 |

**优势：**
- 一个线程处理多个连接
- 减少线程数量，提高系统吞吐量
- 适合高并发场景

