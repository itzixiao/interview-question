package cn.itzixiao.interview.transaction.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * Java NIO Reactor 模式示例：IO 多路复用
 * 
 * Reactor 模式核心概念：
 * 1. 反应堆（Reactor）：事件分发器，监听并分发 IO 事件
 * 2. 句柄（Handle）：资源标识符（如 Socket Channel）
 * 3. 事件（Event）：IO 就绪状态（读就绪、写就绪、连接就绪）
 * 4. 处理器（Handler）：实际处理 IO 操作的逻辑
 * 
 * 工作流程：
 * 1. 注册：将 Channel 注册到 Selector，关注特定事件
 * 2. 轮询：Selector 不断轮询就绪的 Channel
 * 3. 分发：将就绪的 Channel 分发给对应的 Handler
 * 4. 处理：Handler 执行实际的 IO 操作
 * 
 * @author itzixiao
 * @since 2026-03-14
 */
public class NioReactorDemo {
    
    /**
     * Selector 示例：IO 多路复用核心
     */
    public static void selectorExample() throws IOException, InterruptedException {
        System.out.println("========== Selector 示例 ==========");
        
        // 1. 创建 Selector（反应堆）
        Selector selector = Selector.open();
        
        // 2. 创建 ServerSocketChannel 并配置为非阻塞
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(8899));
        
        // 3. 注册接受连接事件（OP_ACCEPT）
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        System.out.println("服务器启动，监听端口：8899");
        System.out.println("等待客户端连接...");
        
        // 4. 事件循环（Reactor 模式核心）
        while (true) {
            // 轮询就绪的 Channel（阻塞直到有事件发生）
            int readyChannels = selector.select();
            
            if (readyChannels == 0) {
                continue;
            }
            
            // 获取所有就绪的 SelectionKey
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            
            // 5. 分发和处理事件
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove(); // 必须手动移除
                
                if (key.isAcceptable()) {
                    // 接受连接事件
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    // 读就绪事件
                    handleRead(key);
                }
            }
        }
    }
    
    /**
     * 处理接受连接事件
     */
    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        
        // 配置为非阻塞
        clientChannel.configureBlocking(false);
        
        // 注册读事件，并附加初始数据
        clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
        
        System.out.println("新客户端连接：" + clientChannel.getRemoteAddress());
    }
    
    /**
     * 处理读事件
     */
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        
        // 读取数据
        int bytesRead = clientChannel.read(buffer);
        
        if (bytesRead == -1) {
            // 连接关闭
            System.out.println("客户端断开连接");
            key.cancel();
            clientChannel.close();
        } else if (bytesRead > 0) {
            // 处理数据
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String message = new String(data, "UTF-8");
            System.out.println("收到消息：" + message);
            
            // 准备响应
            buffer.clear();
            buffer.put(("服务器收到：" + message).getBytes("UTF-8"));
            buffer.flip();
            
            // 注册写事件
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }
    
    /**
     * Reactor 模式三种变体：
     * 
     * 1. 单线程 Reactor：
     *    - 一个线程处理所有事件（连接、读、写、业务逻辑）
     *    - 简单但性能受限
     * 
     * 2. 多线程 Reactor：
     *    - Reactor 线程只负责事件分发
     *    - Worker 线程池处理业务逻辑
     *    - Netty 采用此模式
     * 
     * 3. 主从 Reactor：
     *    - Main Reactor：只处理连接事件
     *    - Sub Reactor：处理 IO 事件
     *    - 适合高并发场景
     */
}
