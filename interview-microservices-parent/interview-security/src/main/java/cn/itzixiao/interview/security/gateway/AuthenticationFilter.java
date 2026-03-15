package cn.itzixiao.interview.security.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * API 网关鉴权过滤器 - 统一认证授权
 * 
 * 功能：
 * 1. JWT Token 校验
 * 2. 黑名单/白名单管理
 * 3. 限流防刷
 * 4. 日志记录
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class AuthenticationFilter implements WebFilter {
    
    /**
     * 不需要认证的白名单路径
     */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/public/**",
            "/actuator/health"
    };
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // 白名单放行
        if (isWhiteList(path)) {
            log.debug("White list path: {}", path);
            return chain.filter(exchange);
        }
        
        // 获取 Token
        String token = extractToken(exchange);
        
        if (token == null) {
            log.warn("Missing authorization header for path: {}", path);
            return unauthorized(exchange, "Missing authorization header");
        }
        
        // 验证 Token（这里简化处理，实际应该调用认证服务）
        if (!validateToken(token)) {
            log.warn("Invalid token for path: {}", path);
            return unauthorized(exchange, "Invalid or expired token");
        }
        
        log.debug("Authenticated request to path: {}", path);
        return chain.filter(exchange);
    }
    
    /**
     * 判断是否在白名单中
     */
    private boolean isWhiteList(String path) {
        for (String whitePath : WHITE_LIST) {
            if (whitePath.endsWith("**")) {
                String prefix = whitePath.substring(0, whitePath.length() - 2);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (path.equals(whitePath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 从请求头中提取 Token
     */
    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }
    
    /**
     * 验证 Token（示例代码，实际需要调用认证服务）
     */
    private boolean validateToken(String token) {
        // TODO: 实际应该调用 OAuth2 认证服务或 JWT 验证
        return token != null && !token.isEmpty();
    }
    
    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set("Content-Type", "application/json;charset=UTF-8");
        
        String body = String.format("{\"error\":\"unauthorized\",\"message\":\"%s\"}", message);
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(bytes)));
    }
}
