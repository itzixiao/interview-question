package cn.itzixiao.interview.provider.controller.jwt;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.provider.service.business.JwtService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT 模拟接口
 * 
 * 作用：演示 JWT token 的生成和验证流程
 * 
 * 接口列表：
 * 1. POST /api/auth/login - 登录获取 token
 * 2. GET /api/auth/verify - 验证 token 有效性
 * 3. GET /api/auth/info - 从 token 中获取用户信息
 */
@RestController
@RequestMapping("/api/auth")
public class JwtController {
    
    private final JwtService jwtService;
    
    public JwtController(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    /**
     * 模拟登录接口 - 获取 JWT token
     * 
     * 请求示例：
     * POST http://localhost:8082/api/auth/login
     * Content-Type: application/json
     * {
     *   "username": "admin",
     *   "password": "123456"
     * }
     * 
     * @param loginRequest 登录请求（用户名 + 密码）
     * @return Result - 包含生成的 token
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        // TODO: 实际项目中应该验证数据库中的用户名和密码
        // if (!userService.validate(username, password)) {
        //     return Result.error("用户名或密码错误");
        // }
        
        // 模拟验证成功
        System.out.println("【登录请求】用户名：" + username + ", 密码：" + password);
        
        // 构建角色信息（自定义 claims）
        Map<String, Object> roles = new HashMap<>();
        roles.put("role", "ADMIN");
        roles.put("permissions", new String[]{"read", "write", "delete"});
        
        // 生成 token
        String token = jwtService.generateToken(username, roles);
        
        // 返回 token
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 86400); // 过期时间（秒）
        response.put("username", username);
        
        return Result.success(response);
    }
    
    /**
     * 验证 token 有效性
     * 
     * 请求示例：
     * GET http://localhost:8082/api/auth/verify?token=eyJhbGciOiJIUzI1NiJ9...
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
                // 解析 token 获取详细信息
                io.jsonwebtoken.Claims claims = jwtService.parseToken(token);
                response.put("username", claims.getSubject());
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
     * 从 token 中获取用户信息
     * 
     * 请求示例：
     * GET http://localhost:8082/api/auth/info
     * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     * 
     * @param authorizationHeader Authorization header
     * @return Result - 用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Result.error("缺少 Authorization header 或格式不正确");
        }
        
        // 提取 token（去掉 "Bearer " 前缀）
        String token = authorizationHeader.substring(7);
        
        // 验证 token
        if (!jwtService.validateToken(token)) {
            return Result.error("Token 无效或已过期");
        }
        
        // 解析 token
        io.jsonwebtoken.Claims claims = jwtService.parseToken(token);
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", claims.getSubject());
        userInfo.put("issuer", claims.getIssuer());
        userInfo.put("roles", claims.get("roles"));
        userInfo.put("issuedAt", claims.getIssuedAt());
        userInfo.put("expiration", claims.getExpiration());
        
        return Result.success(userInfo);
    }
}
