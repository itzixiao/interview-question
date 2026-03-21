package cn.itzixiao.interview.cache.memcached.config;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Memcached 配置类
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Configuration
@ConditionalOnProperty(name = "memcached.enabled", havingValue = "true", matchIfMissing = false)
public class MemcachedConfig {

    @Value("${memcached.servers:localhost:11211}")
    private String servers;

    @Value("${memcached.pool-size:10}")
    private int poolSize;

    @Value("${memcached.op-timeout:3000}")
    private long opTimeout;

    @Bean
    public MemcachedClientBuilder memcachedClientBuilder() {
        MemcachedClientBuilder builder = new XMemcachedClientBuilder(
            AddrUtil.getAddresses(servers)
        );
        builder.setConnectionPoolSize(poolSize);
        builder.setOpTimeout(opTimeout);
        builder.setFailureMode(false);
        return builder;
    }

    @Bean(destroyMethod = "shutdown")
    public MemcachedClient memcachedClient(MemcachedClientBuilder builder) 
            throws IOException {
        return builder.build();
    }
}
