package cn.itzixiao.interview.transaction.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BIO（Blocking IO）示例：同步阻塞 IO
 * <p>
 * BIO 核心特点：
 * 1. 同步阻塞：客户端连接、读取、写入都会阻塞线程
 * 2. 一对一模型：一个连接对应一个线程
 * 3. 简单易懂：编程模型简单
 * 4. 并发受限：高并发场景下线程资源耗尽
 * <p>
 * 工作流程：
 * 1. 服务器启动，绑定端口
 * 2. 客户端发起连接请求
 * 3. 服务器接受连接，创建新线程处理
 * 4. 客户端发送数据，服务器读取并响应
 * 5. 通信结束，关闭连接
 *
 * @author itzixiao
 * @since 2026-03-14
 */
public class BioServerDemo {

    // 线程池
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * 启动 BIO 服务器
     */
    public static void startServer(int port) throws IOException {
        System.out.println("========== BIO 服务器启动 ==========");
        System.out.println("监听端口：" + port);

        // 1. 创建 ServerSocket
        ServerSocket serverSocket = new ServerSocket(port);

        // 2. 循环接受客户端连接
        while (true) {
            // 阻塞方法：等待客户端连接
            Socket clientSocket = serverSocket.accept();
            System.out.println("新客户端连接：" + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

            // 3. 为每个连接创建独立线程处理
            executor.submit(() -> {
                try {
                    handleClient(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 处理客户端请求
     */
    private static void handleClient(Socket socket) throws IOException {
        System.out.println("处理客户端：" + socket.getInetAddress() + ":" + socket.getPort());

        // 4. 获取输入流，读取客户端数据（阻塞）
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        String inputLine;
        // 阻塞方法：读取数据
        while ((inputLine = in.readLine()) != null) {
            System.out.println("收到消息：" + inputLine);

            // 5. 响应客户端（阻塞）
            out.println("服务器收到：" + inputLine);

            if ("bye".equals(inputLine)) {
                break;
            }
        }

        // 6. 关闭连接
        in.close();
        out.close();
        socket.close();
        System.out.println("客户端断开连接");
    }

    /**
     * BIO 客户端示例
     */
    public static void bioClientExample(String host, int port) throws IOException {
        System.out.println("========== BIO 客户端示例 ==========");

        // 1. 创建 Socket 连接
        Socket socket = new Socket(host, port);
        System.out.println("连接到服务器：" + host + ":" + port);

        // 2. 获取流
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        // 3. 发送数据
        String userInput;
        System.out.print("输入消息（输入 bye 退出）：");
        while ((userInput = stdIn.readLine()) != null) {
            // 发送数据（阻塞）
            out.println(userInput);

            // 接收响应（阻塞）
            String response = in.readLine();
            System.out.println("服务器响应：" + response);

            if ("bye".equals(userInput)) {
                break;
            }
            System.out.print("输入消息：");
        }

        // 4. 关闭连接
        stdIn.close();
        out.close();
        in.close();
        socket.close();
    }

    /**
     * BIO 模式总结
     */
    public static void printSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append("=== BIO（同步阻塞 IO）总结 ===\n\n");

        summary.append("1. 核心特点：\n");
        summary.append("   - 同步阻塞：所有 IO 操作都会阻塞当前线程\n");
        summary.append("   - 一对一模型：一个连接需要一个独立线程\n");
        summary.append("   - 简单易懂：编程模型最简单\n\n");

        summary.append("2. 工作流程：\n");
        summary.append("   Step1: 服务器启动，绑定端口\n");
        summary.append("   Step2: 客户端发起连接（阻塞）\n");
        summary.append("   Step3: 服务器接受连接（阻塞）\n");
        summary.append("   Step4: 读写数据（阻塞）\n");
        summary.append("   Step5: 关闭连接\n\n");

        summary.append("3. 优缺点：\n");
        summary.append("   优点：\n");
        summary.append("     - 编程简单，易于理解\n");
        summary.append("     - 适合连接数少且固定的场景\n\n");
        summary.append("   缺点：\n");
        summary.append("     - 线程资源消耗大\n");
        summary.append("     - 高并发下性能差\n");
        summary.append("     - 无法处理百万级并发\n\n");

        summary.append("4. 应用场景：\n");
        summary.append("   - 连接数较少的内部系统\n");
        summary.append("   - 对性能要求不高的应用\n");
        summary.append("   - 学习和理解 IO 的基础模型\n\n");

        summary.append("5. 性能瓶颈：\n");
        summary.append("   - 每个连接占用一个线程\n");
        summary.append("   - 线程切换开销大\n");
        summary.append("   - 受限于最大线程数\n\n");

        System.out.println(summary.toString());
    }
}
