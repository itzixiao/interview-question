package cn.itzixiao.interview.gateway.filter;

import lombok.extern.slf4j.Slf4j;
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
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.info("请求路径: {}", path);

        // 跳过登录接口
        if (path.contains("/login") || path.contains("/register")) {
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
}
