package cn.itzixiao.interview.transaction.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * AIO（Asynchronous IO）示例：异步非阻塞 IO
 * <p>
 * AIO 核心特点：
 * 1. 异步非阻塞：IO 操作立即返回，不阻塞线程
 * 2. 回调机制：操作完成后通过 CompletionHandler 通知
 * 3. 事件驱动：基于操作系统异步 IO（Windows IOCP，Linux AIO）
 * 4. 高并发：少量线程处理大量连接
 * <p>
 * 工作流程：
 * 1. 服务器启动，创建 AsynchronousServerSocketChannel
 * 2. 注册接受连接事件，提供 CompletionHandler
 * 3. 客户端连接时，自动触发 completed 方法
 * 4. 读写操作都是异步的，完成后回调通知
 *
 * @author itzixiao
 * @since 2026-03-14
 */
public class AioServerDemo {

    /**
     * AIO 服务器
     */
    public static void startServer(int port) throws IOException, InterruptedException {
        System.out.println("========== AIO 服务器启动 ==========");
        System.out.println("监听端口：" + port);

        // 1. 创建异步服务器通道
        AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));

        CountDownLatch latch = new CountDownLatch(1);

        // 2. 接受客户端连接（异步）
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                // 接受下一个连接（继续监听）
                serverChannel.accept(null, this);

                try {
                    System.out.println("新客户端连接：" + clientChannel.getRemoteAddress());

                    // 3. 读取数据（异步）
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    clientChannel.read(buffer, null, new CompletionHandler<Integer, Void>() {
                        @Override
                        public void completed(Integer bytesRead, Void attachment) {
                            if (bytesRead > 0) {
                                buffer.flip();
                                byte[] data = new byte[buffer.remaining()];
                                buffer.get(data);
                                String message = new String(data, java.nio.charset.StandardCharsets.UTF_8).trim();
                                System.out.println("收到消息：" + message);

                                // 4. 响应客户端（异步）
                                ByteBuffer writeBuffer = ByteBuffer.wrap(("服务器收到：" + message).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                Future<Integer> writeResult = clientChannel.write(writeBuffer);

                                try {
                                    writeResult.get(); // 等待写入完成
                                    System.out.println("响应已发送");
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                            }

                            // 继续读取
                            buffer.clear();
                            clientChannel.read(buffer, null, this);
                        }

                        @Override
                        public void failed(Throwable exc, Void attachment) {
                            System.err.println("读取失败：" + exc.getMessage());
                            try {
                                clientChannel.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                System.err.println("接受连接失败：" + exc.getMessage());
                latch.countDown();
            }
        });

        System.out.println("AIO 服务器运行中...");
        latch.await(60, TimeUnit.SECONDS); // 等待 60 秒
        serverChannel.close();
    }

    /**
     * AIO 客户端示例
     */
    public static void aioClientExample(String host, int port) throws IOException, InterruptedException, ExecutionException {
        System.out.println("========== AIO 客户端示例 ==========");

        // 1. 创建异步客户端通道
        AsynchronousSocketChannel clientChannel = AsynchronousSocketChannel.open();

        // 2. 连接服务器（异步）
        Future<Void> connectFuture = clientChannel.connect(new InetSocketAddress(host, port));
        connectFuture.get(); // 等待连接成功

        System.out.println("连接到服务器：" + host + ":" + port);

        // 3. 发送数据（异步）
        ByteBuffer writeBuffer = ByteBuffer.wrap("Hello AIO Server".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Future<Integer> writeFuture = clientChannel.write(writeBuffer);
        Integer bytesWritten = writeFuture.get();
        System.out.println("已发送 " + bytesWritten + " 字节");

        // 4. 读取响应（异步）
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        Future<Integer> readFuture = clientChannel.read(readBuffer);
        Integer bytesRead = readFuture.get();

        if (bytesRead > 0) {
            readBuffer.flip();
            byte[] data = new byte[readBuffer.remaining()];
            readBuffer.get(data);
            String response = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("服务器响应：" + response);
        }

        // 5. 关闭连接
        clientChannel.close();
    }

    /**
     * AIO 模式总结
     */
    public static void printSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append("=== AIO（异步非阻塞 IO）总结 ===\n\n");

        summary.append("1. 核心特点：\n");
        summary.append("   - 异步非阻塞：IO 操作立即返回，不阻塞线程\n");
        summary.append("   - 回调机制：通过 CompletionHandler 处理结果\n");
        summary.append("   - 事件驱动：操作系统内核主动通知\n");
        summary.append("   - 高并发：少量线程处理大量连接\n\n");

        summary.append("2. 工作流程：\n");
        summary.append("   Step1: 创建 AsynchronousServerSocketChannel\n");
        summary.append("   Step2: 注册 CompletionHandler\n");
        summary.append("   Step3: 客户端连接时触发 completed 回调\n");
        summary.append("   Step4: 异步读写，完成后回调通知\n\n");

        summary.append("3. 优缺点：\n");
        summary.append("   优点：\n");
        summary.append("     - 真正的异步，不阻塞线程\n");
        summary.append("     - 高并发性能优秀\n");
        summary.append("     - 线程资源消耗少\n\n");
        summary.append("   缺点：\n");
        summary.append("     - 编程模型复杂\n");
        summary.append("     - 依赖操作系统支持\n");
        summary.append("     - Linux 支持不完善（Windows IOCP 更好）\n\n");

        summary.append("4. 应用场景：\n");
        summary.append("   - 高并发网络服务器\n");
        summary.append("   - 文件传输服务\n");
        summary.append("   - 实时数据处理\n\n");

        summary.append("5. 三种 IO 模型对比：\n");
        summary.append("   BIO：同步阻塞，一对一，简单但性能差\n");
        summary.append("   NIO：同步非阻塞，多路复用，性能好但复杂\n");
        summary.append("   AIO：异步非阻塞，回调机制，高性能但依赖系统\n\n");

        System.out.println(summary.toString());
    }
}
