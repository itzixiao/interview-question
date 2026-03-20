package cn.itzixiao.interview.security.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API 网关鉴权过滤器 - 统一认证授权
 * <p>
 * 功能：
 * 1. JWT Token 校验
 * 2. 白名单路径管理
 * 3. 用户信息传递给下游服务
 * 4. 统一错误响应格式
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final WhiteListProperties whiteListProperties;
    private final GatewayJwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthenticationFilter(WhiteListProperties whiteListProperties, GatewayJwtService jwtService) {
        this.whiteListProperties = whiteListProperties;
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("【网关鉴权】请求路径: {}", path);

        // 白名单路径，直接放行（无需鉴权）
        if (isWhitePath(path)) {
            log.debug("【网关鉴权】白名单路径，直接放行: {}", path);
            return chain.filter(exchange);
        }

        // 获取 Authorization header
        String authorizationHeader = request.getHeaders().getFirst("Authorization");

        // 检查 Authorization header 是否存在
        if (!StringUtils.hasText(authorizationHeader)) {
            log.warn("【网关鉴权】缺少 Authorization header, path: {}", path);
            return buildUnauthorizedResponse(exchange, "缺少认证令牌");
        }

        // 检查 Bearer 前缀
        if (!authorizationHeader.startsWith("Bearer ")) {
            log.warn("【网关鉴权】Authorization header 格式错误, path: {}", path);
            return buildUnauthorizedResponse(exchange, "认证令牌格式错误，需要 Bearer 前缀");
        }

        // 提取 token（去掉 "Bearer " 前缀）
        String token = authorizationHeader.substring(7);

        // 验证 token 有效性
        if (!jwtService.validateToken(token)) {
            log.warn("【网关鉴权】Token 无效或已过期, path: {}", path);
            return buildUnauthorizedResponse(exchange, "认证令牌无效或已过期");
        }

        // 从 token 中提取用户信息
        String username = jwtService.getUsernameFromToken(token);
        Object roles = jwtService.getRolesFromToken(token);
        String userId = jwtService.getUserIdFromToken(token);

        log.debug("【网关鉴权】Token 验证通过, username: {}, path: {}", username, path);

        // 传递用户信息到下游服务
        ServerHttpRequest mutableReq = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Name", username)
                .header("X-User-Roles", roles != null ? roles.toString() : "")
                .header("X-Auth-Source", "gateway")
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
     * 判断是否属于白名单路径
     * <p>
     * 支持两种匹配模式：
     * 1. 前缀匹配：路径以 /** 结尾，如 /api/interview/**
     * 2. 包含匹配：路径中包含该字符串，如 /login
     */
    private boolean isWhitePath(String path) {
        List<String> paths = whiteListProperties.getPaths();
        if (paths == null || paths.isEmpty()) {
            return false;
        }

        return paths.stream().anyMatch(white -> {
            if (white.endsWith("/**")) {
                // 前缀匹配：去掉 ** 后判断路径是否以其开头
                String prefix = white.substring(0, white.length() - 2);
                return path.startsWith(prefix);
            }
            // 包含匹配
            return path.contains(white);
        });
    }

    /**
     * 构建未授权响应
     *
     * @param exchange ServerWebExchange
     * @param message  错误消息
     * @return Mono<Void>
     */
    private Mono<Void> buildUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", message);
        result.put("data", null);

        String responseBody;
        try {
            responseBody = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            responseBody = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        }

        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
