package cn.itzixiao.interview.tomcat.config;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tomcat 配置类
 * 
 * <p>配置嵌入式 Tomcat 的各项参数，包括：</p>
 * <ul>
 *   <li>连接器配置（线程池、连接数、超时等）</li>
 *   <li>性能优化（压缩、缓存等）</li>
 *   <li>监控配置（访问日志等）</li>
 * </ul>
 * 
 * <p>配置参数说明：</p>
 * <pre>
 * # application.yml 配置示例
 * server:
 *   tomcat:
 *     threads:
 *       max: 500           # 最大工作线程数
 *       min-spare: 50      # 最小空闲线程数
 *     max-connections: 10000  # 最大连接数
 *     accept-count: 200       # 等待队列长度
 *     connection-timeout: 20000  # 连接超时（毫秒）
 * </pre>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Configuration
public class TomcatConfig {

    /**
     * 最大工作线程数
     * 建议：CPU 密集型 = CPU 核心数 + 1，IO 密集型 = CPU 核心数 * 2
     */
    @Value("${server.tomcat.threads.max:500}")
    private int maxThreads;

    /**
     * 最小空闲线程数
     * 保持一定数量的空闲线程，避免请求来时创建线程的开销
     */
    @Value("${server.tomcat.threads.min-spare:50}")
    private int minSpareThreads;

    /**
     * 最大连接数
     * Tomcat 允许同时处理的最大连接数
     */
    @Value("${server.tomcat.max-connections:10000}")
    private int maxConnections;

    /**
     * 等待队列长度
     * 当所有工作线程都在忙时，新请求进入等待队列
     */
    @Value("${server.tomcat.accept-count:200}")
    private int acceptCount;

    /**
     * 连接超时时间（毫秒）
     */
    @Value("${server.connection-timeout:20000}")
    private int connectionTimeout;

    /**
     * 是否启用压缩
     */
    @Value("${server.compression.enabled:true}")
    private boolean compressionEnabled;

    /**
     * 压缩最小响应大小
     */
    @Value("${server.compression.min-response-size:2048}")
    private int compressionMinResponseSize;

    /**
     * 自定义 Tomcat 容器配置
     * 
     * <p>通过 WebServerFactoryCustomizer 自定义嵌入式 Tomcat 配置</p>
     * <p>这种方式比 application.yml 配置更灵活，可以进行更细粒度的控制</p>
     * 
     * @return WebServerFactoryCustomizer 实例
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> 
            tomcatCustomizer() {
        return factory -> {
            // 连接器配置
            factory.addConnectorCustomizers(connector -> {
                Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                
                // 线程池配置
                // maxThreads: 最大工作线程数，决定并发处理能力
                protocol.setMaxThreads(maxThreads);
                
                // minSpareThreads: 最小空闲线程数
                // 保持一定数量的线程待命，减少线程创建开销
                protocol.setMinSpareThreads(minSpareThreads);
                
                // 连接配置
                // maxConnections: 最大连接数，超过此值的连接会被拒绝
                protocol.setMaxConnections(maxConnections);
                
                // acceptCount: 等待队列长度
                // 当工作线程都在忙时，新请求进入等待队列
                protocol.setAcceptCount(acceptCount);
                
                // 超时配置
                // connectionTimeout: 连接超时时间
                protocol.setConnectionTimeout(connectionTimeout);
                
                // 性能优化
                // disableUploadTimeout: 禁用上传超时
                protocol.setDisableUploadTimeout(true);
                
                // 压缩配置
                if (compressionEnabled) {
                    protocol.setCompression("on");
                    protocol.setCompressionMinSize(compressionMinResponseSize);
                    protocol.setCompressibleMimeType(
                        "text/html,text/xml,text/plain,text/css," +
                        "application/json,application/javascript,application/xml"
                    );
                }
            });
            
            // Context 配置
            factory.addContextCustomizers(context -> {
                // 配置访问日志（可选）
                // context.getPipeline().addValve(new AccessLogValve());
            });
        };
    }

    /**
     * Tomcat 连接器自定义器
     * 
     * <p>用于更细粒度的连接器配置</p>
     * 
     * @return TomcatConnectorCustomizer 实例
     */
    @Bean
    public TomcatConnectorCustomizer tomcatConnectorCustomizer() {
        return connector -> {
            Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
            
            // Socket 配置
            // TCP_NODELAY: 禁用 Nagle 算法，减少延迟
            protocol.setTcpNoDelay(true);
            
            // SO_KEEPALIVE: 启用 TCP Keep-Alive
            protocol.setKeepAliveTimeout(60000);
            
            // 最大 Keep-Alive 请求数
            // 超过此值后关闭连接，防止连接长时间占用
            protocol.setMaxKeepAliveRequests(100);
            
            // 配置 socket 缓冲区大小
            // protocol.setSocketBuffer(9000);
        };
    }

    /**
     * 后台任务线程池
     * 
     * <p>用于执行后台定时任务，如监控数据采集</p>
     * 
     * @return Executor 实例
     */
    @Bean("tomcatBackgroundExecutor")
    public Executor tomcatBackgroundExecutor() {
        return Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("tomcat-background-" + counter.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                }
            }
        );
    }
}
