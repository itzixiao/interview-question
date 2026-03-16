package cn.itzixiao.interview.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 自定义 GatewayFilter 示例 - IP 黑名单过滤器
 * <p>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                    自定义 GatewayFilter 开发要点                            │
 * │                                                                             │
 * │  1. 继承 AbstractGatewayFilterFactory<T>                                    │
 * │     - T 是配置类，用于接收 YAML 中的参数                                    │
 * │                                                                             │
 * │  2. 命名规范：XxxGatewayFilterFactory                                       │
 * │     - 使用时去掉 GatewayFilterFactory 后缀                                  │
 * │     - 如：IpBlacklistGatewayFilterFactory → filters: - IpBlacklist         │
 * │                                                                             │
 * │  3. 重写 shortcutFieldOrder() 方法                                          │
 * │     - 定义简写配置的参数顺序                                                │
 * │     - 如：- IpBlacklist=192.168.1.1,192.168.1.2                             │
 * │                                                                             │
 * │  4. 实现 apply() 方法返回 GatewayFilter                                     │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * <p>
 * 使用方式（application.yml）：
 * spring:
 * cloud:
 * gateway:
 * routes:
 * - id: test-route
 * uri: lb://test-service
 * predicates:
 * - Path=/api/test/**
 * filters:
 * - name: IpBlacklist
 * args:
 * blacklist: 192.168.1.100,10.0.0.1
 */
@Slf4j
@Component
public class IpBlacklistGatewayFilterFactory
        extends AbstractGatewayFilterFactory<IpBlacklistGatewayFilterFactory.Config> {

    public IpBlacklistGatewayFilterFactory() {
        super(Config.class);
    }

    /**
     * 定义简写配置的参数顺序
     * 支持：- IpBlacklist=192.168.1.1,192.168.1.2
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("blacklist");
    }

    /**
     * 创建过滤器实例
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 获取客户端 IP
            String clientIp = getClientIp(request);
            log.info("[IpBlacklist] 客户端IP: {}", clientIp);

            // 检查是否在黑名单中
            if (config.getBlacklistSet().contains(clientIp)) {
                log.warn("[IpBlacklist] IP {} 在黑名单中，拒绝访问", clientIp);
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }

            return chain.filter(exchange);
        };
    }

    /**
     * 获取客户端真实 IP
     * 考虑代理场景（X-Forwarded-For、X-Real-IP）
     */
    private String getClientIp(ServerHttpRequest request) {
        // 优先从代理头获取
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个 IP，取第一个
            return ip.split(",")[0].trim();
        }

        ip = request.getHeaders().getFirst("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 直接获取远程地址
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * 配置类
     */
    public static class Config {
        /**
         * 黑名单 IP 列表（逗号分隔）
         */
        private String blacklist = "";

        private java.util.Set<String> blacklistSet;

        public String getBlacklist() {
            return blacklist;
        }

        public void setBlacklist(String blacklist) {
            this.blacklist = blacklist;
            // 解析为 Set
            this.blacklistSet = new java.util.HashSet<>();
            if (blacklist != null && !blacklist.isEmpty()) {
                String[] ips = blacklist.split(",");
                for (String ip : ips) {
                    this.blacklistSet.add(ip.trim());
                }
            }
        }

        public java.util.Set<String> getBlacklistSet() {
            return blacklistSet != null ? blacklistSet : java.util.Collections.emptySet();
        }
    }
}
