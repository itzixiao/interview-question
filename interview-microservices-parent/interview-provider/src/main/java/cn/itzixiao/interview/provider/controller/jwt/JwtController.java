package cn.itzixiao.interview.provider.controller.jwt;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.provider.service.business.JwtService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT 模拟接口
 * <p>
 * 接口列表：
 * 1. POST /api/auth/login    - 登录获取 token（生成包含 jti + userId 的标准 token，Redis 可用时存入 Redis）
 * 2. POST /api/auth/logout   - 登出（将 jti 加入 Redis 黑名单，实现主动吹销）
 * 3. GET  /api/auth/verify   - 验证 token 有效性（包含黑名单检查）
 * 4. GET  /api/auth/info     - 从网关透传的头获取用户信息
 * 5. POST /api/auth/kick     - 踢下线（管理员将指定用户强制下线）
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class JwtController {

    private final JwtService jwtService;
    /** 可选依赖：登出时写黑名单；未引入 Redis 时仅日志警告 */
    private final StringRedisTemplate redisTemplate;

    public JwtController(JwtService jwtService,
                         @org.springframework.beans.factory.annotation.Autowired(required = false)
                         StringRedisTemplate redisTemplate) {
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 模拟登录接口 - 获取 JWT token
     * <p>
     * 大厂规范：生成包含 jti（唯一 ID）和 userId（数字主键）的标准 token。
     * <p>
     * 请求示例：
     * POST http://localhost:8082/provider/api/auth/login
     * {"username": "admin", "password": "123456"}
     *
     * @param loginRequest 登录请求（用户名 + 密码）
     * @return Result - 包含生成的 token
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Result.error("用户名和密码不能为空");
        }

        // TODO: 实际项目中应该验证数据库中的用户名和密码
        // User user = userService.authenticate(username, password);
        // if (user == null) return Result.error("用户名或密码错误");

        // 模拟验证成功，形如登录用户的数字 ID
        Long simulatedUserId = (long) username.hashCode();

        // 构建角色和权限（实际项目应从数据库中查该用户的角色）
        Map<String, Object> roles = new HashMap<>();
        roles.put("role", "ADMIN");
        roles.put("permissions", new String[]{"read", "write", "delete"});

        // 生成包含 userId + jti 的标准 token
        String token = jwtService.generateToken(username, simulatedUserId, roles);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("tokenType", jwtService.getJwtConfig().getTokenType());
        response.put("expiresIn", jwtService.getJwtConfig().getExpiration());
        response.put("username", username);
        response.put("userId", simulatedUserId);

        log.info("【登录】用户登录成功, username: {}, userId: {}", username, simulatedUserId);
        return Result.success(response);
    }

    /**
     * 登出接口 - 将 Token 加入黑名单（主动吹销）
     * <p>
     * 大厂规范：使用 Redis 存储 jti 黑名单，TTL = Token 剩余有效时间，到期自动清除。
     * <p>
     * 请求示例：
     * POST http://localhost:8082/provider/api/auth/logout
     * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     *
     * @param authorizationHeader Authorization header
     * @return Result - 登出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return Result.error("缺少 Authorization header 或格式不正确");
        }

        String token = authorizationHeader.substring(7);

        // 解析 Token 获取 jti 和剩余有效时间
        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (Exception e) {
            // Token 已无效或过期，无需操作，直接返回成功
            log.debug("【登出】Token 已无效，无需加入黑名单: {}", e.getMessage());
            return Result.success(null);
        }

        String jti = claims.getId();
        String username = claims.getSubject();

        if (StringUtils.hasText(jti) && redisTemplate != null) {
            // 计算剩余有效时间（秒）
            long remainTtl = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
            if (remainTtl > 0) {
                // jti 加入黑名单，TTL 与 Token 剩余有效期保持一致，到期自动清除
                redisTemplate.opsForValue().set(
                        JwtService.BLACKLIST_PREFIX + jti, username, remainTtl, TimeUnit.SECONDS);
                log.info("【登出】Token 已加入黑名单, username: {}, jti: {}, ttl: {}s",
                        username, jti, remainTtl);
            }
        } else {
            log.warn("【登出】黑名单功能不可用（无 Redis 或 jti）, username: {}", username);
        }

        return Result.success(null);
    }

    /**
     * 验证 token 有效性
     * <p>
     * 请求示例：
     * GET http://localhost:8082/provider/api/auth/verify?token=eyJhbGciOiJIUzI1NiJ9...
     *
     * @param token JWT token
     * @return Result - 验证结果
     */
    @GetMapping("/verify")
    public Result<Map<String, Object>> verifyToken(@RequestParam String token) {
        boolean isValid = jwtService.validateToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);

        if (isValid) {
            try {
                Claims claims = jwtService.parseToken(token);
                response.put("username", claims.getSubject());
                response.put("userId", claims.get("userId"));
                response.put("jti", claims.getId());
                response.put("issuer", claims.getIssuer());
                response.put("issuedAt", claims.getIssuedAt());
                response.put("expiration", claims.getExpiration());
                response.put("roles", claims.get("roles"));
            } catch (Exception e) {
                response.put("error", e.getMessage());
            }
        } else {
            response.put("error", "Token 无效或已过期");
        }

        return Result.success(response);
    }

    /**
     * 从网关透传的头中获取用户信息
     * <p>
     * 大厂规范：下游服务不需自己解析 JWT，直接读取网关透传的标准头即可。
     * <p>
     * 请求示例：
     * GET http://localhost:8082/provider/api/auth/info
     * X-User-Id: 12345
     * X-User-Name: admin
     * X-Auth-Source: gateway
     *
     * @param userId      网关透传的用户 ID
     * @param username    网关透传的用户名
     * @param roles       网关透传的角色信息
     * @param authSource  认证来源（gateway）
     * @return Result - 用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Auth-Source", required = false) String authSource) {

        if (!StringUtils.hasText(userId)) {
            return Result.error("未获取到用户信息，请先通过网关登录");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", username);
        userInfo.put("roles", roles);
        userInfo.put("authSource", authSource);

        return Result.success(userInfo);
    }

    /**
     * 踢下线接口 - 管理员将指定用户强制下线
     * <p>
     * 大厂规范：
     * 1. 从 Redis 获取该用户当前在线 token
     * 2. 解析 jti 后将其写入黑名单（TTL = token 剩余有效期）
     * 3. 删除 jwt:token:{username}，断开登录状态
     * 4. 用户再次请求时 Gateway 返回 401
     * <p>
     * 请求示例：
     * POST http://localhost:8082/provider/api/auth/kick
     * {"username": "admin"}
     *
     * @param body 请求体，必须包含 username 字段
     * @return Result - 踢下线结果
     */
    @PostMapping("/kick")
    public Result<String> kickOut(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (!StringUtils.hasText(username)) {
            return Result.error("用户名不能为空");
        }
        String result = jwtService.kickOut(username);
        log.info("【踢下线】操作完成, username: {}, result: {}", username, result);
        return Result.success(result);
    }
}
