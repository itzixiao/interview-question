package cn.itzixiao.interview.reactive.webflux;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring WebFlux 响应式服务层
 * 
 * 特点：
 * - 非阻塞 I/O
 * - 响应式数据流
 * - 背压支持
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Service
@RequiredArgsConstructor
public class ReactiveUserService {
    
    private final ConcurrentHashMap<Long, User> database = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    /**
     * 根据 ID 查询用户（返回 Mono）
     */
    public Mono<User> getUserById(Long id) {
        return Mono.justOrEmpty(database.get(id));
    }
    
    /**
     * 查询所有用户（返回 Flux）
     */
    public Flux<User> getAllUsers() {
        return Flux.fromIterable(database.values());
    }
    
    /**
     * 创建用户
     */
    public Mono<User> createUser(User user) {
        Long newId = idGenerator.getAndIncrement();
        user.setId(newId);
        database.put(newId, user);
        return Mono.just(user);
    }
    
    /**
     * 更新用户
     */
    public Mono<User> updateUser(Long id, User updatedUser) {
        return Mono.justOrEmpty(database.get(id))
                .flatMap(existingUser -> {
                    existingUser.setName(updatedUser.getName());
                    existingUser.setEmail(updatedUser.getEmail());
                    database.put(id, existingUser);
                    return Mono.just(existingUser);
                });
    }
    
    /**
     * 删除用户
     */
    public Mono<Void> deleteUser(Long id) {
        database.remove(id);
        return Mono.empty();
    }
    
    /**
     * 搜索用户（模拟异步操作）
     */
    public Flux<User> searchUsers(String keyword) {
        return Flux.fromIterable(database.values())
                .filter(user -> user.getName().contains(keyword))
                .delayElements(Duration.ofMillis(100));  // 模拟延迟
    }
}

@Data
class User {
    private Long id;
    private String name;
    private String email;
}
