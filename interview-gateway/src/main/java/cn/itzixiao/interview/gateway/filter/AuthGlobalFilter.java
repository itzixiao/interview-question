package cn.itzixiao.interview.gateway.filter;

import cn.itzixiao.interview.gateway.config.WhiteListProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 认证全局过滤器
 * 白名单路径通过配置文件动态读取：gateway.white-list.paths
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private WhiteListProperties whiteListProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.info("请求路径: {}", path);

        // 白名单路径，直接放行（无需鉴权）
        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        // 获取 token
        String token = request.getHeaders().getFirst("Authorization");

        if (!StringUtils.hasText(token)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // TODO: 验证 token

        // 传递用户信息到下游服务
        ServerHttpRequest mutableReq = request.mutate()
                .header("X-User-Id", "user123")
                .build();
        ServerWebExchange mutableExchange = exchange.mutate()
                .request(mutableReq)
                .build();

        return chain.filter(mutableExchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 判断是否属于白名单路径（从配置文件读取）
     * 支持两种匹配模式：
     * - 前缀匹配：路径以 /** 结尾，如 /api/interview/**
     * - 包含匹配：路径中包含该字符串，如 /login
     */
    private boolean isWhitePath(String path) {
        return whiteListProperties.getPaths().stream().anyMatch(white -> {
            if (white.endsWith("/**")) {
                // 前缀匹配：去掉 ** 后判断路径是否以其开头
                String prefix = white.substring(0, white.length() - 2);
                return path.startsWith(prefix);
            }
            // 包含匹配
            return path.contains(white);
        });
    }
}