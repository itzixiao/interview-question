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
