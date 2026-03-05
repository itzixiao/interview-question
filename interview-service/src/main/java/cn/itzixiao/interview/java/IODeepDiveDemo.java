package cn.itzixiao.interview.java;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * Java IO/NIO 深入理解
 *
 * IO 分类：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 按流向分类：                                             │
 * │     - 输入流（InputStream/Reader）：从外部读入数据           │
 * │     - 输出流（OutputStream/Writer）：向外部写出数据          │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. 按处理单位分类：                                         │
 * │     - 字节流（InputStream/OutputStream）：处理二进制数据     │
 * │     - 字符流（Reader/Writer）：处理文本数据                  │
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. 按功能分类：                                             │
 * │     - 节点流：直接操作数据源（FileInputStream）              │
 * │     - 处理流：包装节点流，提供额外功能（BufferedInputStream）│
 * └─────────────────────────────────────────────────────────────┘
 *
 * NIO（New IO）：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  - 通道（Channel）：双向数据传输                              │
 * │  - 缓冲区（Buffer）：数据容器                                 │
 * │  - 选择器（Selector）：多路复用                               │
 * │  - 非阻塞 IO：提高并发处理能力                                │
 * └─────────────────────────────────────────────────────────────┘
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

    public static void main(String[] args) throws Exception {
        IODeepDiveDemo demo = new IODeepDiveDemo();
        demo.byteStreamVsCharStream();
        demo.nodeStreamVsProcessStream();
        demo.objectSerialization();
        demo.nioBasics();
        demo.nioFileOperations();
        demo.zeroCopy();
        demo.ioModels();

        // 清理测试文件
        Files.deleteIfExists(Paths.get(TEST_FILE));
        Files.deleteIfExists(Paths.get("copy.txt"));
        Files.deleteIfExists(Paths.get("copy2.txt"));
        Files.deleteIfExists(Paths.get("data.bin"));
        Files.deleteIfExists(Paths.get("person.ser"));
    }
}
