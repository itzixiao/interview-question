package cn.itzixiao.interview.reactive.rsocket;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * RSocket 响应式控制器
 * 
 * RSocket 四种交互模式：
 * 1. Request-Response：请求 - 响应（单个响应）
 * 2. Request-Stream：请求 - 流（多个响应）
 * 3. Fire-and-Forget：发后即忘（无响应）
 * 4. Channel：双向流（客户端和服务端都发送流）
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RSocketController {
    
    /**
     * Request-Response 模式
     * 客户端发送一个请求，服务端返回一个响应
     */
    @MessageMapping("user.get")
    public Mono<User> getUser(Mono<Long> request) {
        log.info("Request-Response: 收到用户 ID 请求");
        return request.map(id -> new User(id, "User-" + id));
    }
    
    /**
     * Request-Stream 模式
     * 客户端发送一个请求，服务端返回流式响应
     */
    @MessageMapping("users.stream")
    public Flux<User> getUsersStream(Mono<Long> count) {
        log.info("Request-Stream: 请求获取用户流");
        return count
                .flatMapMany(n -> 
                    Flux.range(1, n.intValue())
                        .map(i -> new User((long) i, "User-" + i))
                        .delayElements(Duration.ofMillis(500))
                );
    }
    
    /**
     * Fire-and-Forget 模式
     * 客户端发送请求，不需要服务端响应
     */
    @MessageMapping("user.log")
    public Mono<Void> logUser(User user) {
        log.info("Fire-and-Forget: 记录用户信息 - {}", user);
        return Mono.empty();  // 不返回任何内容
    }
    
    /**
     * Channel 模式（双向流）
     * 客户端和服务端都可以发送流式数据
     */
    @MessageMapping("chat")
    public Flux<String> chat(Flux<String> clientMessages) {
        log.info("Channel: 开始双向聊天");
        return clientMessages
                .doOnNext(msg -> log.info("收到客户端消息：{}", msg))
                .map(msg -> "Server echo: " + msg)
                .delayElements(Duration.ofMillis(200));
    }
}

@Data
class User {
    private Long id;
    private String name;
    
    public User() {}
    
    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
