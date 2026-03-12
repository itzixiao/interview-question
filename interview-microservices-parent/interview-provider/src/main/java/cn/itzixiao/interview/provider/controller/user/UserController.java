package cn.itzixiao.interview.provider.controller.user;

import cn.itzixiao.interview.common.result.Result;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户服务接口（服务提供者）
 * <p>
 * 提供用户相关的 REST API，供 Feign 客户端调用
 *
 * @author itzixiao
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 模拟数据库存储
     */
    private final Map<Long, UserDTO> userStorage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 初始化一些测试数据
     */
    public UserController() {
        // 初始化测试数据
        UserDTO user1 = UserDTO.builder()
                .id(idGenerator.getAndIncrement())
                .username("zhangsan")
                .email("zhangsan@example.com")
                .phone("13800138001")
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        userStorage.put(user1.getId(), user1);

        UserDTO user2 = UserDTO.builder()
                .id(idGenerator.getAndIncrement())
                .username("lisi")
                .email("lisi@example.com")
                .phone("13800138002")
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        userStorage.put(user2.getId(), user2);

        log.info("UserController 初始化完成，已创建 {} 条测试数据", userStorage.size());
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable("id") Long id) {
        log.info("收到查询用户请求: id={}", id);
        UserDTO user = userStorage.get(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/username")
    public Result<UserDTO> getByUsername(@RequestParam("username") String username) {
        log.info("收到查询用户请求: username={}", username);
        UserDTO user = userStorage.values().stream()
                .filter(u -> username.equals(u.getUsername()))
                .findFirst()
                .orElse(null);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 查询用户列表
     */
    @GetMapping("/list")
    public Result<List<UserDTO>> list(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "status", required = false) Integer status) {
        log.info("收到查询用户列表请求: page={}, size={}, status={}", page, size, status);

        List<UserDTO> result = new ArrayList<>();
        for (UserDTO user : userStorage.values()) {
            if (status == null || status.equals(user.getStatus())) {
                result.add(user);
            }
        }

        // 简单分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, result.size());
        if (start >= result.size()) {
            return Result.success(new ArrayList<>());
        }
        return Result.success(result.subList(start, end));
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<UserDTO> create(@RequestBody UserDTO user) {
        log.info("收到创建用户请求: {}", user);
        user.setId(idGenerator.getAndIncrement());
        user.setCreateTime(LocalDateTime.now());
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userStorage.put(user.getId(), user);
        return Result.success(user);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<UserDTO> update(@PathVariable("id") Long id, @RequestBody UserDTO user) {
        log.info("收到更新用户请求: id={}, user={}", id, user);
        UserDTO existing = userStorage.get(id);
        if (existing == null) {
            return Result.error(404, "用户不存在");
        }
        user.setId(id);
        user.setCreateTime(existing.getCreateTime());
        userStorage.put(id, user);
        return Result.success(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        log.info("收到删除用户请求: id={}", id);
        UserDTO removed = userStorage.remove(id);
        if (removed == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(null);
    }

    /**
     * 带认证的查询
     */
    @GetMapping("/auth/{id}")
    public Result<UserDTO> getByIdWithAuth(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        log.info("收到认证查询请求: id={}, token={}", id, token);
        if (token == null || token.isEmpty()) {
            return Result.error(401, "未授权");
        }
        return getById(id);
    }

    /**
     * 用户 DTO（服务提供者本地定义）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String username;
        private String email;
        private String phone;
        private Integer status;
        private LocalDateTime createTime;
    }
}
