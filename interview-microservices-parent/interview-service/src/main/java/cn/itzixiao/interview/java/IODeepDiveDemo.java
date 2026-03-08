package cn.itzixiao.interview.java;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Java IO/NIO 深入理解 - 教学型详解
 *
 * 本示例从底层原理到实际应用，系统性地讲解 Java IO 的各个方面
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                          Java IO 体系总览                                   │
 * │                                                                             │
 * │  按流向分类：                                                               │
 * │  ┌──────────────────────────────────────────────────────────────────────┐  │
 * │  │  输入流（Input）          │  输出流（Output）                          │  │
 * │  │  InputStream              │  OutputStream                              │  │
 * │  │  Reader                   │  Writer                                    │  │
 * │  └──────────────────────────────────────────────────────────────────────┘  │
 * │                                                                             │
 * │  按处理单位分类：                                                           │
 * │  ┌──────────────────────────────────────────────────────────────────────┐  │
 * │  │  字节流（8位字节）        │  字符流（16位Unicode字符）                 │  │
 * │  │  InputStream/OutputStream │  Reader/Writer                             │  │
 * │  │  适合：二进制文件         │  适合：文本文件                            │  │
 * │  └──────────────────────────────────────────────────────────────────────┘  │
 * │                                                                             │
 * │  按功能分类：                                                               │
 * │  ┌──────────────────────────────────────────────────────────────────────┐  │
 * │  │  节点流（低级流）         │  处理流（高级流/包装流）                   │  │
 * │  │  直接操作数据源           │  包装节点流，增强功能                      │  │
 * │  │  FileInputStream         │  BufferedInputStream                       │  │
 * │  │  FileOutputStream        │  DataInputStream                           │  │
 * │  │  FileReader              │  ObjectInputStream                         │  │
 * │  └──────────────────────────────────────────────────────────────────────┘  │
 * │                                                                             │
 * │  IO 设计模式：装饰器模式（Decorator Pattern）                              │
 * │  new BufferedReader(new InputStreamReader(new FileInputStream("file.txt")))│
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
public class IODeepDiveDemo {

    private static final String TEST_FILE = "test.txt";
    private static final String TEST_DIR = "testDir";

    /**
     * 1. 字节流 vs 字符流
     */
    public void byteStreamVsCharStream() throws IOException {
        System.out.println("========== 字节流 vs 字符流 ==========\n");

        String content = "Hello 世界";  // 包含中文

        // 字节流：按字节读写，不处理编码
        System.out.println("【字节流】FileInputStream/FileOutputStream");
        try (FileOutputStream fos = new FileOutputStream(TEST_FILE)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }

        try (FileInputStream fis = new FileInputStream(TEST_FILE)) {
            byte[] buffer = new byte[1024];
            int len = fis.read(buffer);
            String read = new String(buffer, 0, len, StandardCharsets.UTF_8);
            System.out.println("  写入: " + content);
            System.out.println("  读出: " + read);
        }

        // 字符流：按字符读写，自动处理编码
        System.out.println("\n【字符流】FileReader/FileWriter");
        try (FileWriter fw = new FileWriter(TEST_FILE)) {
            fw.write(content);
        }

        try (FileReader fr = new FileReader(TEST_FILE)) {
            char[] buffer = new char[1024];
            int len = fr.read(buffer);
            String read = new String(buffer, 0, len);
            System.out.println("  写入: " + content);
            System.out.println("  读出: " + read);
        }

        // 推荐：使用 InputStreamReader/OutputStreamWriter 指定编码
        System.out.println("\n【推荐】InputStreamReader/OutputStreamWriter（指定编码）");
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(TEST_FILE), StandardCharsets.UTF_8)) {
            osw.write(content);
        }

        try (InputStreamReader isr = new InputStreamReader(
                new FileInputStream(TEST_FILE), StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int len = isr.read(buffer);
            String read = new String(buffer, 0, len);
            System.out.println("  写入: " + content);
            System.out.println("  读出: " + read);
        }

        System.out.println();
    }

    /**
     * 2. 节点流 vs 处理流（装饰器模式）
     */
    public void nodeStreamVsProcessStream() throws IOException {
        System.out.println("========== 节点流 vs 处理流 ==========\n");

        // 节点流：直接操作文件
        System.out.println("【节点流】直接操作数据源");
        System.out.println("  FileInputStream/FileOutputStream");
        System.out.println("  FileReader/FileWriter\n");

        // 处理流：包装节点流，提供缓冲、编码等功能
        System.out.println("【处理流】包装节点流，增强功能");

        // BufferedInputStream/BufferedOutputStream：带缓冲
        System.out.println("  BufferedInputStream/BufferedOutputStream：");
        System.out.println("    - 内部缓冲区（默认 8KB）");
        System.out.println("    - 减少系统调用次数，提高性能\n");

        try (BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(TEST_FILE));
             BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream("copy.txt"))) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            System.out.println("  使用 Buffered 流复制文件完成\n");
        }

        // BufferedReader/BufferedWriter：带缓冲的字符流
        System.out.println("  BufferedReader/BufferedWriter：");
        System.out.println("    - 支持按行读写（readLine/newLine）\n");

        try (BufferedReader br = new BufferedReader(new FileReader(TEST_FILE));
             BufferedWriter bw = new BufferedWriter(new FileWriter("copy2.txt"))) {

            String line;
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.newLine();  // 写入换行符
            }
            System.out.println("  使用 Buffered 字符流复制文件完成\n");
        }

        // DataInputStream/DataOutputStream：读写基本数据类型
        System.out.println("  DataInputStream/DataOutputStream：");
        System.out.println("    - 读写 int、double、boolean 等基本类型\n");

        try (DataOutputStream dos = new DataOutputStream(
                new FileOutputStream("data.bin"))) {
            dos.writeInt(100);
            dos.writeDouble(3.14);
            dos.writeBoolean(true);
            dos.writeUTF("Hello");
        }

        try (DataInputStream dis = new DataInputStream(
                new FileInputStream("data.bin"))) {
            System.out.println("  读取: " + dis.readInt());
            System.out.println("  读取: " + dis.readDouble());
            System.out.println("  读取: " + dis.readBoolean());
            System.out.println("  读取: " + dis.readUTF());
        }

        System.out.println();
    }

    /**
     * 3. 对象序列化与反序列化
     */
    public void objectSerialization() throws IOException, ClassNotFoundException {
        System.out.println("========== 对象序列化 ==========\n");

        // 定义可序列化的类
        class Person implements Serializable {
            private static final long serialVersionUID = 1L;

            private String name;
            private int age;
            private transient String password;  // transient：不序列化

            public Person(String name, int age, String password) {
                this.name = name;
                this.age = age;
                this.password = password;
            }

            @Override
            public String toString() {
                return "Person{name='" + name + "', age=" + age + ", password='" + password + "'}";
            }
        }

        Person person = new Person("张三", 25, "secret123");

        // 序列化
        System.out.println("【序列化】ObjectOutputStream");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("person.ser"))) {
            oos.writeObject(person);
            System.out.println("  原始对象: " + person);
            System.out.println("  序列化完成\n");
        }

        // 反序列化
        System.out.println("【反序列化】ObjectInputStream");
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("person.ser"))) {
            Person restored = (Person) ois.readObject();
            System.out.println("  恢复对象: " + restored);
            System.out.println("  注意：transient 字段 password = " + restored.password);
        }

        System.out.println("\n【序列化要点】");
        System.out.println("  1. 实现 Serializable 接口");
        System.out.println("  2. 定义 serialVersionUID（版本控制）");
        System.out.println("  3. transient 修饰的字段不序列化");
        System.out.println("  4. 静态字段不序列化");
        System.out.println("  5. 父类序列化需要特殊处理\n");
    }

    /**
     * 4. NIO 核心组件
     */
    public void nioBasics() throws IOException {
        System.out.println("========== NIO 核心组件 ==========\n");

        // Buffer：数据容器
        System.out.println("【Buffer】数据容器");
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        System.out.println("  容量（capacity）: " + buffer.capacity());
        System.out.println("  位置（position）: " + buffer.position());
        System.out.println("  限制（limit）: " + buffer.limit());
        System.out.println("  标记（mark）: " + buffer.mark());

        // 写入数据
        String data = "Hello NIO";
        buffer.put(data.getBytes());
        System.out.println("\n  写入 \"" + data + "\" 后:");
        System.out.println("  position: " + buffer.position());

        // 切换为读模式
        buffer.flip();
        System.out.println("\n  flip() 后（读模式）:");
        System.out.println("  position: " + buffer.position());
        System.out.println("  limit: " + buffer.limit());

        // 读取数据
        byte[] readData = new byte[buffer.limit()];
        buffer.get(readData);
        System.out.println("\n  读取数据: " + new String(readData));

        // 切换为写模式
        buffer.clear();
        System.out.println("\n  clear() 后（写模式）:");
        System.out.println("  position: " + buffer.position());
        System.out.println("  limit: " + buffer.limit());

        System.out.println("\n【Buffer 类型】");
        System.out.println("  ByteBuffer、CharBuffer、ShortBuffer");
        System.out.println("  IntBuffer、LongBuffer、FloatBuffer、DoubleBuffer\n");

        // Channel：数据传输通道
        System.out.println("【Channel】数据传输通道");

        // FileChannel：文件通道
        try (RandomAccessFile raf = new RandomAccessFile(TEST_FILE, "rw");
             FileChannel channel = raf.getChannel()) {

            System.out.println("  FileChannel 操作文件");
            System.out.println("  文件大小: " + channel.size() + " 字节\n");
        }

        System.out.println("【Channel 类型】");
        System.out.println("  FileChannel：文件读写");
        System.out.println("  SocketChannel：TCP 客户端");
        System.out.println("  ServerSocketChannel：TCP 服务端");
        System.out.println("  DatagramChannel：UDP 通信\n");
    }

    /**
     * 5. NIO 文件操作
     */
    public void nioFileOperations() throws IOException {
        System.out.println("========== NIO 文件操作 ==========\n");

        // Path：文件路径
        Path path = Paths.get(TEST_FILE);
        System.out.println("【Path】文件路径");
        System.out.println("  路径: " + path);
        System.out.println("  文件名: " + path.getFileName());
        System.out.println("  父目录: " + path.getParent());
        System.out.println("  绝对路径: " + path.toAbsolutePath());
        System.out.println("  是否存在: " + Files.exists(path) + "\n");

        // Files：文件操作工具类
        System.out.println("【Files】文件操作");

        // 读取所有字节
        byte[] bytes = Files.readAllBytes(path);
        System.out.println("  readAllBytes: " + new String(bytes));

        // 读取所有行
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        System.out.println("  readAllLines: " + lines.size() + " 行");

        // 写入文件
        Path writePath = Paths.get("nio_write.txt");
        Files.write(writePath, "Hello NIO".getBytes());
        System.out.println("  write: 写入完成\n");

        // 文件复制
        Path copyPath = Paths.get("nio_copy.txt");
        Files.copy(writePath, copyPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("  copy: 复制完成");

        // 文件移动
        Path movePath = Paths.get("nio_moved.txt");
        Files.move(copyPath, movePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("  move: 移动完成");

        // 删除文件
        Files.deleteIfExists(movePath);
        Files.deleteIfExists(writePath);
        System.out.println("  delete: 删除完成\n");

        // 目录操作
        System.out.println("【目录操作】");
        Path dir = Paths.get(TEST_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
            System.out.println("  createDirectory: 创建目录");
        }

        // 遍历目录
        System.out.println("  遍历当前目录:");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("."))) {
            int count = 0;
            for (Path entry : stream) {
                if (count++ < 5) {
                    System.out.println("    " + entry.getFileName() +
                        (Files.isDirectory(entry) ? " [目录]" : " [文件]"));
                }
            }
        }

        // 递归删除
        Files.deleteIfExists(dir);
        System.out.println("\n  delete: 删除目录\n");
    }

    /**
     * 6. NIO 零拷贝（Zero Copy）
     */
    public void zeroCopy() throws IOException {
        System.out.println("========== NIO 零拷贝 ==========\n");

        System.out.println("【传统 IO 拷贝】");
        System.out.println("  1. 磁盘 → 内核缓冲区（DMA 拷贝）");
        System.out.println("  2. 内核缓冲区 → 用户缓冲区（CPU 拷贝）");
        System.out.println("  3. 用户缓冲区 → Socket 缓冲区（CPU 拷贝）");
        System.out.println("  4. Socket 缓冲区 → 网卡（DMA 拷贝）");
        System.out.println("  共 4 次拷贝，3 次状态切换\n");

        System.out.println("【NIO 零拷贝（FileChannel.transferTo）】");
        System.out.println("  1. 磁盘 → 内核缓冲区（DMA 拷贝）");
        System.out.println("  2. 内核缓冲区 → Socket 缓冲区（CPU 拷贝，数据描述符）");
        System.out.println("  3. Socket 缓冲区 → 网卡（DMA 拷贝）");
        System.out.println("  共 3 次拷贝，2 次状态切换\n");

        // 零拷贝示例
        try (RandomAccessFile sourceFile = new RandomAccessFile(TEST_FILE, "r");
             FileChannel sourceChannel = sourceFile.getChannel();
             RandomAccessFile destFile = new RandomAccessFile("zero_copy.txt", "rw");
             FileChannel destChannel = destFile.getChannel()) {

            long position = 0;
            long count = sourceChannel.size();

            // transferTo：零拷贝传输
            long transferred = sourceChannel.transferTo(position, count, destChannel);
            System.out.println("  transferTo 传输: " + transferred + " 字节\n");
        }

        // 清理
        Files.deleteIfExists(Paths.get("zero_copy.txt"));
    }

    /**
     * 7. IO 模型对比
     */
    public void ioModels() {
        System.out.println("========== IO 模型对比 ==========\n");

        System.out.println("【BIO（Blocking IO）】");
        System.out.println("  特点：同步阻塞");
        System.out.println("  模式：一个连接一个线程");
        System.out.println("  缺点：连接数多时线程开销大");
        System.out.println("  适用：连接数少的场景\n");

        System.out.println("【NIO（Non-blocking IO）】");
        System.out.println("  特点：同步非阻塞");
        System.out.println("  模式：一个线程处理多个连接（多路复用）");
        System.out.println("  核心：Channel、Buffer、Selector");
        System.out.println("  适用：连接数多、数据量小的场景\n");

        System.out.println("【AIO（Asynchronous IO）】");
        System.out.println("  特点：异步非阻塞");
        System.out.println("  模式：操作系统完成 IO 后回调通知");
        System.out.println("  适用：连接数多、数据量大的场景");
        System.out.println("  注意：Linux 下 AIO 实现不完善\n");

        System.out.println("【对比】");
        System.out.println("  ┌─────────┬──────────┬──────────┬──────────┐");
        System.out.println("  │  模型   │   BIO    │   NIO    │   AIO    │");
        System.out.println("  ├─────────┼──────────┼──────────┼──────────┤");
        System.out.println("  │ 阻塞    │   阻塞   │  非阻塞  │  非阻塞  │");
        System.out.println("  │ 同步    │   同步   │   同步   │   异步   │");
        System.out.println("  │ 线程数  │    多    │    少    │    少    │");
        System.out.println("  │ 复杂度  │    低    │    高    │    中    │");
        System.out.println("  │ 吞吐量  │    低    │    高    │    高    │");
        System.out.println("  └─────────┴──────────┴──────────┴──────────┘\n");
    }

    // ==================== 新增内容：更多IO示例 ====================

    /**
     * 8. RandomAccessFile 随机访问文件
     */
    public void randomAccessFileDemo() throws IOException {
        System.out.println("========== RandomAccessFile 随机访问文件 ==========\n");

        System.out.println("【特点】");
        System.out.println("  - 可读可写，支持随机访问");
        System.out.println("  - 通过 seek() 定位到任意位置");
        System.out.println("  - 适合：断点续传、文件分片、多线程下载\n");

        // 创建测试文件
        try (RandomAccessFile raf = new RandomAccessFile(TEST_FILE, "rw")) {
            // 写入数据
            raf.writeUTF("Hello World");
            raf.writeInt(12345);
            raf.writeDouble(3.14159);
            System.out.println("写入后文件指针位置: " + raf.getFilePointer());
            System.out.println("文件总长度: " + raf.length() + " 字节\n");

            // 定位到文件开头读取
            raf.seek(0);
            System.out.println("seek(0) 后读取:");
            System.out.println("  UTF: " + raf.readUTF());
            System.out.println("  Int: " + raf.readInt());
            System.out.println("  Double: " + raf.readDouble());
            System.out.println("  文件指针: " + raf.getFilePointer());

            // 定位到特定位置修改
            raf.seek(2); // 跳过 UTF 长度标记
            raf.writeUTF("Java");  // 覆盖部分内容
            System.out.println("\n修改后文件指针: " + raf.getFilePointer());
        }

        System.out.println("\n【模式说明】");
        System.out.println("  r   - 只读");
        System.out.println("  rw  - 读写，文件不存在则创建");
        System.out.println("  rwd - 读写，同步更新到磁盘（内容）");
        System.out.println("  rws - 读写，同步更新到磁盘（内容+元数据）\n");
    }

    /**
     * 9. ByteArrayInputStream/ByteArrayOutputStream 内存流
     */
    public void byteArrayStreamDemo() throws IOException {
        System.out.println("========== 内存流 ByteArrayStream ==========\n");

        System.out.println("【特点】");
        System.out.println("  - 数据存储在内存中，不涉及磁盘I/O");
        System.out.println("  - ByteArrayInputStream：从byte数组读取");
        System.out.println("  - ByteArrayOutputStream：写入到内部byte数组");
        System.out.println("  - 适合：内存中的数据处理、序列化\n");

        // ByteArrayOutputStream 示例
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Hello ".getBytes(StandardCharsets.UTF_8));
        baos.write("World".getBytes(StandardCharsets.UTF_8));

        // 获取数据
        byte[] data = baos.toByteArray();
        System.out.println("写入数据: " + new String(data, StandardCharsets.UTF_8));
        System.out.println("数据长度: " + data.length + " 字节\n");

        // ByteArrayInputStream 示例
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        byte[] buffer = new byte[1024];
        int len = bais.read(buffer);
        System.out.println("读取数据: " + new String(buffer, 0, len, StandardCharsets.UTF_8));

        // 支持重置和重复读取
        bais.reset();
        len = bais.read(buffer);
        System.out.println("reset 后再次读取: " + new String(buffer, 0, len, StandardCharsets.UTF_8));

        System.out.println("\n【应用场景】");
        System.out.println("  - 对象序列化到字节数组");
        System.out.println("  - 数据压缩/解压");
        System.out.println("  - 多个数据源合并\n");
    }

    /**
     * 10. PipedInputStream/PipedOutputStream 管道流
     */
    public void pipedStreamDemo() throws IOException {
        System.out.println("========== 管道流 PipedStream ==========\n");

        System.out.println("【特点】");
        System.out.println("  - 用于线程间通信");
        System.out.println("  - PipedInputStream + PipedOutputStream 配对使用");
        System.out.println("  - 一个线程写入，另一个线程读取\n");

        PipedInputStream pis = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream();

        // 连接管道
        pis.connect(pos);
        // 或者: pos.connect(pis);

        // 写线程
        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    String msg = "Message " + i + "\n";
                    pos.write(msg.getBytes(StandardCharsets.UTF_8));
                    pos.flush();
                    Thread.sleep(100);
                }
                pos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 读线程
        Thread reader = new Thread(() -> {
            try {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(pis, StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("  接收: " + line);
                }
                pis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        System.out.println("启动管道通信:");
        reader.start();
        writer.start();

        try {
            writer.join();
            reader.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n【注意事项】");
        System.out.println("  - 必须在不同线程中读写，否则会死锁");
        System.out.println("  - 管道缓冲区默认 1024 字节");
        System.out.println("  - 适合生产者-消费者场景\n");
    }

    /**
     * 11. SequenceInputStream 合并流
     */
    public void sequenceStreamDemo() throws IOException {
        System.out.println("========== 合并流 SequenceInputStream ==========\n");

        System.out.println("【特点】");
        System.out.println("  - 将多个输入流合并为一个");
        System.out.println("  - 按顺序依次读取各个流");
        System.out.println("  - 适合：多个文件合并读取\n");

        // 创建测试文件
        Files.write(Paths.get("seq1.txt"), "File1 Content ".getBytes());
        Files.write(Paths.get("seq2.txt"), "File2 Content ".getBytes());
        Files.write(Paths.get("seq3.txt"), "File3 Content".getBytes());

        // 合并多个文件流
        Vector<InputStream> streams = new Vector<>();
        streams.add(new FileInputStream("seq1.txt"));
        streams.add(new FileInputStream("seq2.txt"));
        streams.add(new FileInputStream("seq3.txt"));

        try (SequenceInputStream sis = new SequenceInputStream(streams.elements())) {
            byte[] buffer = new byte[1024];
            int len = sis.read(buffer);
            System.out.println("合并读取内容: " + new String(buffer, 0, len));
        }

        // 清理
        Files.deleteIfExists(Paths.get("seq1.txt"));
        Files.deleteIfExists(Paths.get("seq2.txt"));
        Files.deleteIfExists(Paths.get("seq3.txt"));

        System.out.println("\n【使用 Enumeration 合并两个流】");
        try (SequenceInputStream sis = new SequenceInputStream(
                Collections.enumeration(Arrays.asList(
                    new ByteArrayInputStream("A".getBytes()),
                    new ByteArrayInputStream("B".getBytes()))))) {
            System.out.println("  合并结果: " + (char) sis.read() + (char) sis.read());
        }
        System.out.println();
    }

    /**
     * 12. PrintStream/PrintWriter 打印流
     */
    public void printStreamDemo() throws IOException {
        System.out.println("========== 打印流 PrintStream/PrintWriter ==========\n");

        System.out.println("【特点】");
        System.out.println("  - 提供方便的打印方法（print/println/printf）");
        System.out.println("  - PrintStream：字节流，System.out 就是 PrintStream");
        System.out.println("  - PrintWriter：字符流，支持自动刷新\n");

        // PrintStream 示例
        try (PrintStream ps = new PrintStream(
                new FileOutputStream("print_test.txt"), true, "UTF-8")) {
            ps.println("Hello PrintStream");
            ps.printf("格式化输出: %d, %.2f, %s%n", 100, 3.14159, "test");
            ps.print("不换行输出");
        }

        // PrintWriter 示例
        try (PrintWriter pw = new PrintWriter(
                new FileWriter("print_test2.txt"), true)) {  // autoFlush
            pw.println("Hello PrintWriter");
            pw.printf("姓名: %s, 年龄: %d%n", "张三", 25);
        }

        // System.out 是 PrintStream
        System.out.println("\nSystem.out 类型: " + System.out.getClass().getName());
        System.out.println("System.err 类型: " + System.err.getClass().getName());

        // 重定向标准输出
        PrintStream originalOut = System.out;
        try (PrintStream fileOut = new PrintStream("redirect.txt")) {
            System.setOut(fileOut);
            System.out.println("这行会写入文件而不是控制台");
        }
        System.setOut(originalOut);
        System.out.println("已恢复标准输出");

        // 清理
        Files.deleteIfExists(Paths.get("print_test.txt"));
        Files.deleteIfExists(Paths.get("print_test2.txt"));
        Files.deleteIfExists(Paths.get("redirect.txt"));

        System.out.println("\n【PrintStream vs PrintWriter】");
        System.out.println("  PrintStream  - 继承 FilterOutputStream，字节流");
        System.out.println("  PrintWriter  - 继承 Writer，字符流，更好的国际化支持");
        System.out.println("  推荐：处理文本时使用 PrintWriter\n");
    }

    /**
     * 13. NIO Selector 多路复用详解
     */
    public void nioSelectorDemo() throws IOException {
        System.out.println("========== NIO Selector 多路复用详解 ==========\n");

        System.out.println("【Selector 核心概念】");
        System.out.println("  - 多路复用器，一个线程管理多个 Channel");
        System.out.println("  - 监控 Channel 的事件（连接就绪、读就绪、写就绪）");
        System.out.println("  - 大大减少线程数量，提高系统吞吐量\n");

        System.out.println("【SelectionKey 事件类型】");
        System.out.println("  OP_ACCEPT (1<<0) - 连接就绪，ServerSocketChannel 专用");
        System.out.println("  OP_CONNECT (1<<3) - 连接就绪，SocketChannel 专用");
        System.out.println("  OP_READ (1<<0) - 读就绪");
        System.out.println("  OP_WRITE (1<<2) - 写就绪\n");

        System.out.println("【NIO 服务端示例代码】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 1. 创建 Selector                                               │");
        System.out.println("│  Selector selector = Selector.open();                              │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 2. 创建 ServerSocketChannel                                    │");
        System.out.println("│  ServerSocketChannel serverChannel = ServerSocketChannel.open();   │");
        System.out.println("│  serverChannel.bind(new InetSocketAddress(8080));                  │");
        System.out.println("│  serverChannel.configureBlocking(false);  // 非阻塞模式            │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 3. 注册到 Selector，监听 OP_ACCEPT 事件                         │");
        System.out.println("│  serverChannel.register(selector, SelectionKey.OP_ACCEPT);         │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 4. 事件循环                                                    │");
        System.out.println("│  while (true) {                                                    │");
        System.out.println("│      selector.select();  // 阻塞，直到有事件就绪                   │");
        System.out.println("│      Set<SelectionKey> keys = selector.selectedKeys();              │");
        System.out.println("│      Iterator<SelectionKey> iter = keys.iterator();                │");
        System.out.println("│      while (iter.hasNext()) {                                      │");
        System.out.println("│          SelectionKey key = iter.next();                           │");
        System.out.println("│          if (key.isAcceptable()) { /* 处理连接 */ }                │");
        System.out.println("│          if (key.isReadable()) { /* 处理读 */ }                    │");
        System.out.println("│          iter.remove();  // 移除已处理的事件                       │");
        System.out.println("│      }                                                             │");
        System.out.println("│  }                                                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【Selector 工作原理图】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│     ┌──────────────────────────────────────────────────────────┐   │");
        System.out.println("│     │                      Selector                            │   │");
        System.out.println("│     │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐       │   │");
        System.out.println("│     │  │Channel 1│ │Channel 2│ │Channel 3│ │Channel N│       │   │");
        System.out.println("│     │  │ OP_READ │ │OP_WRITE │ │OP_ACCEPT│ │OP_READ  │       │   │");
        System.out.println("│     │  └─────────┘ └─────────┘ └─────────┘ └─────────┘       │   │");
        System.out.println("│     └──────────────────────────────────────────────────────────┘   │");
        System.out.println("│                              │                                      │");
        System.out.println("│                              ▼                                      │");
        System.out.println("│                    selector.select()                               │");
        System.out.println("│                              │                                      │");
        System.out.println("│                              ▼                                      │");
        System.out.println("│                    遍历就绪事件                                    │");
        System.out.println("│                    逐一处理                                        │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 14. 文件复制性能对比
     */
    public void fileCopyPerformanceDemo() throws IOException {
        System.out.println("========== 文件复制性能对比 ==========\n");

        // 创建测试文件
        Path testFile = Paths.get("perf_test.txt");
        byte[] content = new byte[10 * 1024 * 1024]; // 10MB
        new Random().nextBytes(content);
        Files.write(testFile, content);

        long start, end;

        // 1. 基本 FileInputStream/FileOutputStream
        start = System.currentTimeMillis();
        try (FileInputStream fis = new FileInputStream(testFile.toFile());
             FileOutputStream fos = new FileOutputStream("copy1.txt")) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
        end = System.currentTimeMillis();
        System.out.println("1. 基本字节流复制: " + (end - start) + " ms");

        // 2. BufferedInputStream/BufferedOutputStream
        start = System.currentTimeMillis();
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(testFile.toFile()));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("copy2.txt"))) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
        }
        end = System.currentTimeMillis();
        System.out.println("2. Buffered 流复制: " + (end - start) + " ms");

        // 3. NIO FileChannel
        start = System.currentTimeMillis();
        try (FileChannel srcChannel = FileChannel.open(testFile, StandardOpenOption.READ);
             FileChannel destChannel = FileChannel.open(Paths.get("copy3.txt"),
                 StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        }
        end = System.currentTimeMillis();
        System.out.println("3. NIO FileChannel 复制: " + (end - start) + " ms");

        // 4. Files.copy
        start = System.currentTimeMillis();
        Files.copy(testFile, Paths.get("copy4.txt"), StandardCopyOption.REPLACE_EXISTING);
        end = System.currentTimeMillis();
        System.out.println("4. Files.copy 复制: " + (end - start) + " ms");

        // 清理
        Files.deleteIfExists(testFile);
        for (int i = 1; i <= 4; i++) {
            Files.deleteIfExists(Paths.get("copy" + i + ".txt"));
        }

        System.out.println("\n【性能排名】(一般情况下)");
        System.out.println("  1. Files.copy / FileChannel.transferTo (底层零拷贝)");
        System.out.println("  2. BufferedInputStream/BufferedOutputStream");
        System.out.println("  3. 基本 FileInputStream/FileOutputStream");
        System.out.println("\n【为什么 Buffered 更快？】");
        System.out.println("  - 减少系统调用次数（用户态/内核态切换）");
        System.out.println("  - 批量读写，利用局部性原理");
        System.out.println("  - 默认缓冲区 8KB，可根据场景调整\n");
    }

    // ==================== Java IO 常见面试题 ====================

    /**
     * 15. Java IO 面试题汇总
     */
    public void interviewQuestions() {
        System.out.println("========== Java IO 常见面试题 ==========\n");

        question1();
        question2();
        question3();
        question4();
        question5();
        question6();
        question7();
        question8();
        question9();
        question10();
        question11();
        question12();
    }

    private void question1() {
        System.out.println("【Q1】字节流和字符流有什么区别？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  对比项        │  字节流                │  字符流                │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  处理单位      │  8位字节               │  16位Unicode字符       │");
        System.out.println("│  基类          │  InputStream/          │  Reader/Writer         │");
        System.out.println("│                │  OutputStream          │                        │");
        System.out.println("│  适用场景      │  二进制文件            │  文本文件              │");
        System.out.println("│                │  （图片、音视频等）    │  （txt、xml、json等）  │");
        System.out.println("│  编码处理      │  不处理编码            │  自动处理编码转换      │");
        System.out.println("│  缓冲区        │  无（需包装）          │  无（需包装）          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");
        System.out.println("\n【关键点】");
        System.out.println("  - 字节流操作的是 byte[]，字符流操作的是 char[]");
        System.out.println("  - 字符流 = 字节流 + 编码表");
        System.out.println("  - InputStreamReader/OutputStreamWriter 是桥梁，实现字节流到字符流的转换\n");
    }

    private void question2() {
        System.out.println("【Q2】BIO、NIO、AIO 有什么区别？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  对比项        │  BIO              │  NIO              │  AIO       │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  全称          │  Blocking IO      │  Non-blocking IO  │  Async IO  │");
        System.out.println("│  阻塞性        │  阻塞             │  非阻塞           │  非阻塞    │");
        System.out.println("│  同步/异步     │  同步             │  同步             │  异步      │");
        System.out.println("│  线程模型      │  一连接一线程     │  一线程多连接     │  回调模型  │");
        System.out.println("│  核心组件      │  Stream           │  Channel/Buffer   │  Future/   │");
        System.out.println("│                │                   │  Selector         │  Callback  │");
        System.out.println("│  适用场景      │  连接数少且固定   │  连接数多，       │  连接数多，│");
        System.out.println("│                │                   │  数据量小         │  数据量大  │");
        System.out.println("│  例子          │  传统Socket       │  Netty            │  Windows   │");
        System.out.println("│                │                   │                   │  IOCP      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");
        System.out.println("\n【关键点】");
        System.out.println("  - BIO：线程阻塞在 read/write 操作，适合连接数少的场景");
        System.out.println("  - NIO：线程轮询检查 IO 状态，通过 Selector 实现多路复用");
        System.out.println("  - AIO：操作系统完成 IO 后回调通知应用，真正的异步\n");
    }

    private void question3() {
        System.out.println("【Q3】为什么需要 BufferedInputStream/BufferedOutputStream？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  【问题】直接使用 FileInputStream 每次读取一个字节：               │");
        System.out.println("│  - 每次读取都涉及系统调用（用户态→内核态切换）                    │");
        System.out.println("│  - 频繁的系统调用开销大                                          │");
        System.out.println("│                                                                  │");
        System.out.println("│  【解决】BufferedInputStream 内部维护一个缓冲区（默认8KB）：      │");
        System.out.println("│  - 一次性从磁盘读取 8KB 到缓冲区                                 │");
        System.out.println("│  - 后续读取直接从缓冲区获取（内存操作）                          │");
        System.out.println("│  - 大大减少系统调用次数                                          │");
        System.out.println("│                                                                  │");
        System.out.println("│  【性能提升】                                                    │");
        System.out.println("│  - 无缓冲：读取 1MB 需要 ~100万次系统调用                        │");
        System.out.println("│  - 有缓冲：读取 1MB 只需 ~128次系统调用（1MB/8KB）               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    private void question4() {
        System.out.println("【Q4】Java 序列化中 serialVersionUID 有什么作用？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  serialVersionUID 用于版本控制：                                  │");
        System.out.println("│                                                                  │");
        System.out.println("│  【反序列化验证】                                                 │");
        System.out.println("│  - 反序列化时，JVM 比较 serialVersionUID 与本地类的版本号        │");
        System.out.println("│  - 一致：正常反序列化                                            │");
        System.out.println("│  - 不一致：抛出 InvalidClassException                            │");
        System.out.println("│                                                                  │");
        System.out.println("│  【未显式定义】                                                   │");
        System.out.println("│  - JVM 根据类结构自动生成（计算复杂，性能差）                    │");
        System.out.println("│  - 类结构变化（增删字段）会导致版本号变化                        │");
        System.out.println("│  - 可能导致旧数据无法反序列化                                    │");
        System.out.println("│                                                                  │");
        System.out.println("│  【最佳实践】显式定义：                                          │");
        System.out.println("│  private static final long serialVersionUID = 1L;                 │");
        System.out.println("│  - 类结构变化时手动更新版本号                                    │");
        System.out.println("│  - 兼容性由开发者控制                                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    private void question5() {
        System.out.println("【Q5】transient 关键字的作用？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  transient 修饰的字段不会被序列化                                 │");
        System.out.println("│                                                                  │");
        System.out.println("│  【使用场景】                                                     │");
        System.out.println("│  1. 敏感信息：密码、密钥等                                        │");
        System.out.println("│  2. 不可序列化对象：如 Thread、Socket                            │");
        System.out.println("│  3. 可计算字段：如缓存、派生值                                   │");
        System.out.println("│  4. 大对象：不需要持久化的数据                                   │");
        System.out.println("│                                                                  │");
        System.out.println("│  【示例】                                                         │");
        System.out.println("│  public class User implements Serializable {                      │");
        System.out.println("│      private String username;                                     │");
        System.out.println("│      private transient String password;  // 不序列化              │");
        System.out.println("│      private transient Thread worker;    // 线程不可序列化        │");
        System.out.println("│  }                                                                │");
        System.out.println("│                                                                  │");
        System.out.println("│  【注意】                                                         │");
        System.out.println("│  - 反序列化后 transient 字段为默认值（null/0/false）             │");
        System.out.println("│  - 静态字段本身就不参与序列化，无需 transient                    │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    private void question6() {
        System.out.println("【Q6】什么是零拷贝？Java 如何实现？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  【零拷贝定义】                                                   │");
        System.out.println("│  减少/避免数据在用户态和内核态之间的拷贝，提高 I/O 效率           │");
        System.out.println("│                                                                  │");
        System.out.println("│  【传统 IO 拷贝流程】（4次拷贝，3次上下文切换）                   │");
        System.out.println("│  磁盘 → 内核缓冲区 → 用户缓冲区 → Socket缓冲区 → 网卡            │");
        System.out.println("│        (DMA)      (CPU)        (CPU)       (DMA)                  │");
        System.out.println("│                                                                  │");
        System.out.println("│  【零拷贝优化】（2-3次拷贝，2次上下文切换）                       │");
        System.out.println("│  1. mmap：将文件映射到内存，减少一次内核→用户态拷贝              │");
        System.out.println("│  2. sendfile：直接在内核态传输，避免用户态参与                   │");
        System.out.println("│                                                                  │");
        System.out.println("│  【Java 实现】                                                    │");
        System.out.println("│  FileChannel.transferTo() / transferFrom()                        │");
        System.out.println("│  底层使用 sendfile 系统调用                                      │");
        System.out.println("│                                                                  │");
        System.out.println("│  FileChannel src = new FileInputStream(src).getChannel();         │");
        System.out.println("│  FileChannel dest = new FileOutputStream(dest).getChannel();      │");
        System.out.println("│  src.transferTo(0, src.size(), dest);  // 零拷贝传输              │");
        System.out.println("│                                                                  │");
        System.out.println("│  【应用场景】Kafka、Netty 等高性能框架                           │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    private void question7() {
        System.out.println("【Q7】Java IO 使用了什么设计模式？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  【装饰器模式（Decorator Pattern）】                              │");
        System.out.println("│                                                                  │");
        System.out.println("│  IO 流体系是装饰器模式的经典应用：                               │");
        System.out.println("│  - 抽象构件：InputStream/OutputStream/Reader/Writer              │");
        System.out.println("│  - 具体构件：FileInputStream/ByteArrayInputStream                │");
        System.out.println("│  - 装饰器基类：FilterInputStream/FilterOutputStream              │");
        System.out.println("│  - 具体装饰器：BufferedInputStream/DataInputStream               │");
        System.out.println("│                                                                  │");
        System.out.println("│  【示例】                                                         │");
        System.out.println("│  InputStream is = new BufferedInputStream(                        │");
        System.out.println("│      new DataInputStream(                                         │");
        System.out.println("│          new FileInputStream(\"file.txt\")));                      │");
        System.out.println("│  // 层层包装，每一层添加新功能                                    │");
        System.out.println("│                                                                  │");
        System.out.println("│  【装饰器 vs 继承】                                               │");
        System.out.println("│  - 继承：静态扩展，类数量爆炸                                    │");
        System.out.println("│  - 装饰器：动态扩展，灵活组合                                    │");
        System.out.println("│                                                                  │");
        System.out.println("│  【适配器模式】                                                   │");
        System.out.println("│  InputStreamReader/OutputStreamWriter                             │");
        System.out.println("│  将字节流适配为字符流                                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    private void question8() {
        System.out.println("【Q8】try-with-resources 的原理是什么？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  【语法糖】自动关闭实现 AutoCloseable 的资源                      │");
        System.out.println("│                                                                  │");
        System.out.println("│  【源代码】                                                       │");
        System.out.println("│  try (FileInputStream fis = new FileInputStream(\"file.txt\")) {   │");
        System.out.println("│      // 使用 fis                                                  │");
        System.out.println("│  }                                                                │");
        System.out.println("│                                                                  │");
        System.out.println("│  【编译后等价代码】                                               │");
        System.out.println("│  FileInputStream fis = new FileInputStream(\"file.txt\");          │");
        System.out.println("│  try {                                                            │");
        System.out.println("│      // 使用 fis                                                  │");
        System.out.println("│  } finally {                                                      │");
        System.out.println("│      if (fis != null) {                                           │");
        System.out.println("│          fis.close();  // 自动调用 close()                        │");
        System.out.println("│      }                                                            │");
        System.out.println("│  }                                                                │");
        System.out.println("│                                                                  │");
        System.out.println("│  【关键接口】                                                     │");
        System.out.println("│  public interface AutoCloseable {                                 │");
        System.out.println("│      void close() throws Exception;                               │");
        System.out.println("│  }                                                                │");
        System.out.println("│  - Java 7 引入                                                   │");
        System.out.println("│  - 所有 IO 流都实现了此接口                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    private void question9() {
        System.out.println("【Q9】Files.readAllLines() 为什么不适合读大文件？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  【问题】                                                         │");
        System.out.println("│  Files.readAllLines() 一次性将所有行加载到内存                   │");
        System.out.println("│  大文件会导致：                                                   │");
        System.out.println("│  - OOM（OutOfMemoryError）                                       │");
        System.out.println("│  - 响应延迟                                                       │");
        System.out.println("│                                                                  │");
        System.out.println("│  【正确做法】流式处理                                            │");
        System.out.println("│  // 方式1：Files.lines()（推荐）                                  │");
        System.out.println("│  try (Stream<String> lines = Files.lines(path)) {                 │");
        System.out.println("│      lines.forEach(line -> process(line));  // 逐行处理           │");
        System.out.println("│  }                                                                │");
        System.out.println("│                                                                  │");
        System.out.println("│  // 方式2：BufferedReader                                         │");
        System.out.println("│  try (BufferedReader br = Files.newBufferedReader(path)) {        │");
        System.out.println("│      String line;                                                 │");
        System.out.println("│      while ((line = br.readLine()) != null) {                     │");
        System.out.println("│          process(line);                                           │");
        System.out.println("│      }                                                            │");
        System.out.println("│  }                                                                │");
        System.out.println("│                                                                  │");
        System.out.println("│  【大文件写入】同样使用流式写入                                  │");
        System.out.println("│  try (BufferedWriter bw = Files.newBufferedWriter(path)) {        │");
        System.out.println("│      for (String line : largeData) {                              │");
        System.out.println("│          bw.write(line);                                          │");
        System.out.println("│          bw.newLine();                                            │");
        System.out.println("│      }                                                            │");
        System.out.println("│  }                                                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    private void question10() {
        System.out.println("【Q10】NIO 的 Buffer 为什么需要 flip()？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  【Buffer 核心属性】                                              │");
        System.out.println("│  - capacity：缓冲区容量（固定不变）                              │");
        System.out.println("│  - position：当前位置指针                                        │");
        System.out.println("│  - limit：限制位置（不能读写的位置）                             │");
        System.out.println("│                                                                  │");
        System.out.println("│  【写模式】写入数据后                                            │");
        System.out.println("│  position = 写入的字节数                                         │");
        System.out.println("│  limit = capacity                                                │");
        System.out.println("│                                                                  │");
        System.out.println("│  【问题】如果不 flip()，直接读取：                               │");
        System.out.println("│  - position 不在开头，读不到数据                                 │");
        System.out.println("│  - limit = capacity，可能读到无效数据                            │");
        System.out.println("│                                                                  │");
        System.out.println("│  【flip() 的作用】切换到读模式                                   │");
        System.out.println("│  limit = position  // 限制 = 已写入的数据量                      │");
        System.out.println("│  position = 0      // 回到开头准备读取                           │");
        System.out.println("│                                                                  │");
        System.out.println("│  【图示】                                                         │");
        System.out.println("│  写入 \"Hello\" 后：                                              │");
        System.out.println("│  [H][e][l][l][o][ ][ ][ ]...                                     │");
        System.out.println("│        position=5           limit=capacity                       │");
        System.out.println("│                                                                  │");
        System.out.println("│  flip() 后：                                                      │");
        System.out.println("│  [H][e][l][l][o][ ][ ][ ]...                                     │");
        System.out.println("│  position=0        limit=5                                       │");
        System.out.println("│  现在可以正确读取 0-5 位置的数据                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    private void question11() {
        System.out.println("【Q11】Java IO 乱码问题如何解决？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  【乱码原因】读写编码不一致                                      │");
        System.out.println("│                                                                  │");
        System.out.println("│  【常见场景】                                                     │");
        System.out.println("│  1. FileReader 使用系统默认编码，不同系统编码不同                │");
        System.out.println("│  2. 字节流转字符流未指定编码                                    │");
        System.out.println("│  3. 网络传输编码不一致                                          │");
        System.out.println("│                                                                  │");
        System.out.println("│  【错误示例】                                                     │");
        System.out.println("│  FileReader fr = new FileReader(\"file.txt\");  // 使用默认编码   │");
        System.out.println("│                                                                  │");
        System.out.println("│  【正确做法】始终显式指定编码                                    │");
        System.out.println("│  // 方式1：InputStreamReader                                     │");
        System.out.println("│  InputStreamReader isr = new InputStreamReader(                   │");
        System.out.println("│      new FileInputStream(\"file.txt\"), StandardCharsets.UTF_8);   │");
        System.out.println("│                                                                  │");
        System.out.println("│  // 方式2：Files.newBufferedReader                                │");
        System.out.println("│  BufferedReader br = Files.newBufferedReader(                    │");
        System.out.println("│      path, StandardCharsets.UTF_8);                              │");
        System.out.println("│                                                                  │");
        System.out.println("│  // 方式3：NIO Files.readAllLines                                 │");
        System.out.println("│  List<String> lines = Files.readAllLines(path,                   │");
        System.out.println("│      StandardCharsets.UTF_8);                                    │");
        System.out.println("│                                                                  │");
        System.out.println("│  【最佳实践】统一使用 UTF-8 编码                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    private void question12() {
        System.out.println("【Q12】如何实现文件断点续传？\n");
        System.out.println("【答案】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  【核心思路】记录已传输位置，从断点处继续                       │");
        System.out.println("│                                                                  │");
        System.out.println("│  【实现代码】                                                     │");
        System.out.println("│  public void downloadWithResume(String url, File localFile) {     │");
        System.out.println("│      long startPos = localFile.exists() ?                        │");
        System.out.println("│                        localFile.length() : 0;                    │");
        System.out.println("│                                                                  │");
        System.out.println("│      // HTTP Range 请求                                          │");
        System.out.println("│      HttpURLConnection conn = (HttpURLConnection)                 │");
        System.out.println("│          new URL(url).openConnection();                           │");
        System.out.println("│      conn.setRequestProperty(\"Range\",                           │");
        System.out.println("│          \"bytes=\" + startPos + \"-\");                            │");
        System.out.println("│                                                                  │");
        System.out.println("│      // RandomAccessFile 随机写入                                │");
        System.out.println("│      try (RandomAccessFile raf = new RandomAccessFile(           │");
        System.out.println("│              localFile, \"rw\")) {                                 │");
        System.out.println("│          raf.seek(startPos);  // 定位到断点位置                   │");
        System.out.println("│          // 写入数据...                                           │");
        System.out.println("│      }                                                            │");
        System.out.println("│  }                                                                │");
        System.out.println("│                                                                  │");
        System.out.println("│  【关键点】                                                       │");
        System.out.println("│  1. HTTP Range 头指定下载范围                                    │");
        System.out.println("│  2. RandomAccessFile.seek() 定位到断点                           │");
        System.out.println("│  3. 本地记录已下载位置（数据库或临时文件）                       │");
        System.out.println("│  4. 服务端需支持 Range 请求                                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    public static void main(String[] args) throws Exception {
        IODeepDiveDemo demo = new IODeepDiveDemo();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║            Java IO/NIO 教学型详解                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // 第一部分：IO 基础
        System.out.println("===== 第一部分：IO 基础 =====\n");
        demo.byteStreamVsCharStream();
        demo.nodeStreamVsProcessStream();
        demo.objectSerialization();

        // 第二部分：更多 IO 示例
        System.out.println("\n===== 第二部分：更多 IO 示例 =====\n");
        demo.randomAccessFileDemo();
        demo.byteArrayStreamDemo();
        demo.pipedStreamDemo();
        demo.sequenceStreamDemo();
        demo.printStreamDemo();

        // 第三部分：NIO 详解
        System.out.println("\n===== 第三部分：NIO 详解 =====\n");
        demo.nioBasics();
        demo.nioFileOperations();
        demo.nioSelectorDemo();
        demo.zeroCopy();
        demo.fileCopyPerformanceDemo();

        // 第四部分：IO 模型
        System.out.println("\n===== 第四部分：IO 模型 =====\n");
        demo.ioModels();

        // 第五部分：面试题
        System.out.println("\n===== 第五部分：Java IO 面试题 =====\n");
        demo.interviewQuestions();

        // 清理测试文件
        Files.deleteIfExists(Paths.get(TEST_FILE));
        Files.deleteIfExists(Paths.get("copy.txt"));
        Files.deleteIfExists(Paths.get("copy2.txt"));
        Files.deleteIfExists(Paths.get("data.bin"));
        Files.deleteIfExists(Paths.get("person.ser"));

        System.out.println("========== Java IO/NIO 教学型详解完成 ==========\n");
    }
}
