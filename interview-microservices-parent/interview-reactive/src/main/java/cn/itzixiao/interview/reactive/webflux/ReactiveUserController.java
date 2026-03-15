package cn.itzixiao.interview.reactive.webflux;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring WebFlux 响应式控制器
 * 
 * 支持：
 * - RESTful API
 * - Server-Sent Events (SSE)
 * - 非阻塞 I/O
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ReactiveUserController {
    
    private final ReactiveUserService userService;
    
    /**
     * 获取单个用户
     */
    @GetMapping("/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    
    /**
     * 获取所有用户（返回 Flux）
     */
    @GetMapping
    public Flux<User> getAllUsers() {
        return userService.getAllUsers();
    }
    
    /**
     * 创建用户
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<User> createUser(@RequestBody User user) {
        return userService.createUser(user);
    }
    
    /**
     * 更新用户
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Mono<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }
    
    /**
     * 搜索用户（SSE 流式响应）
     */
    @GetMapping(value = "/search/{keyword}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<User> searchUsers(@PathVariable String keyword) {
        return userService.searchUsers(keyword);
    }
}
