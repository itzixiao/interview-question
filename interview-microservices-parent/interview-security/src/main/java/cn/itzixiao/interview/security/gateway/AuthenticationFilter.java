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
 * 大厂规范设计：
 * 1. order = -100，确保在所有业务过滤器之前执行
 * 2. 白名单路径直接放行，无需鉴权
 * 3. JWT Token 校验：签名验证 + 过期检查 + 黑名单校验（主动吊销）+ 在线 token 一致性校验（踢下线）
 * 4. Claims 一次解析，避免重复解析带来的性能开销
 * 5. 透传标准化用户信息头：X-User-Id、X-User-Name、X-User-Roles、X-Auth-Source
 * 6. 移除下游 Authorization 头，防止下游服务进行二次鉴权
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final WhiteListProperties whiteListProperties;
    private final GatewayJwtService jwtService;
    /** 可选依赖：未引入 Redis 时为 null，黑名单和在线 token 校验自动跳过 */
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthenticationFilter(WhiteListProperties whiteListProperties,
                                GatewayJwtService jwtService,
                                TokenBlacklistService tokenBlacklistService) {
        this.whiteListProperties = whiteListProperties;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
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

        // 一次解析 Claims，避免重复解析带来的性能开销
        io.jsonwebtoken.Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (Exception e) {
            log.warn("【网关鉴权】Token 解析失败: {}, path: {}", e.getMessage(), path);
            return buildUnauthorizedResponse(exchange, "认证令牌无效或已过期");
        }

        // 检查 Token 是否过期
        if (claims.getExpiration().before(new java.util.Date())) {
            log.warn("【网关鉴权】Token 已过期, path: {}", path);
            return buildUnauthorizedResponse(exchange, "认证令牌已过期");
        }

        // Redis 可用时：黑名单 + 在线 token 一致性双重校验
        if (tokenBlacklistService != null) {
            // 1. jti 黑名单校验（logout 主动登出 / kickOut 踢下线时写入）
            String jti = claims.getId();
            if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                log.warn("【网关鉴权】Token 已被吊销（黑名单）, jti: {}, path: {}", jti, path);
                return buildUnauthorizedResponse(exchange, "认证令牌已被吊销，请重新登录");
            }
            // 2. 在线 token 一致性校验（踢下线 / 单设备登录失效的核心防线）
            //    jwt:token:{username} 不存在（被踢下线删除）或与当前 token 不一致 -> 401
            String usernameForCheck = claims.getSubject();
            if (!tokenBlacklistService.isOnlineTokenValid(usernameForCheck, token)) {
                log.warn("【网关鉴权】token 已失效（踢下线或已被新 token 替换）, username: {}, path: {}",
                        usernameForCheck, path);
                return buildUnauthorizedResponse(exchange, "认证令牌已失效，请重新登录");
            }
        }

        // 从 Claims 中提取用户信息
        String username = claims.getSubject();
        Object roles = claims.get("roles");
        // userId 支持数字 ID 和字符串 ID 两种写入方式
        Object userIdObj = claims.get("userId");
        String userId = userIdObj != null ? userIdObj.toString() : username;

        log.debug("【网关鉴权】Token 验证通过, username: {}, userId: {}, path: {}", username, userId, path);

        // 传递用户信息到下游服务，并移除 Authorization 头（防止下游二次鉴权）
        ServerHttpRequest mutableReq = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Name", username)
                .header("X-User-Roles", roles != null ? roles.toString() : "")
                .header("X-Auth-Source", "gateway")
                .headers(headers -> headers.remove("Authorization"))  // 屏蔽 Authorization 头，下游服务不再鉴权
                .build();
        ServerWebExchange mutableExchange = exchange.mutate()
                .request(mutableReq)
                .build();

        return chain.filter(mutableExchange);
    }

    @Override
    public int getOrder() {
        // 大厂规范：-100 确保在路由转发和所有业务过滤器之前执行鉴权
        return -100;
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
