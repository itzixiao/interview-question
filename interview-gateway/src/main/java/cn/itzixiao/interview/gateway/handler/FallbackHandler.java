package cn.itzixiao.interview.gateway.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 熔断降级处理器
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                        熔断器工作原理                                        │
 * │                                                                             │
 * │  三种状态：                                                                  │
 * │                                                                             │
 * │  ┌──────────────┐     失败率超过阈值      ┌──────────────┐                 │
 * │  │              │ ────────────────────→ │              │                 │
 * │  │   CLOSED     │                       │    OPEN      │                 │
 * │  │  (正常状态)   │ ←──────────────────── │  (熔断状态)   │                 │
 * │  │              │     成功探测恢复       │              │                 │
 * │  └──────────────┘                       └───────┬──────┘                 │
 * │         ↑                                       │                         │
 * │         │                                       │ 等待超时后              │
 * │         │                                       ↓                         │
 * │         │                               ┌──────────────┐                 │
 * │         └─────────────────────────────── │  HALF_OPEN   │                 │
 * │                     探测成功             │ (半开状态)    │                 │
 * │                                         │ 放行部分请求  │                 │
 * │                                         └──────────────┘                 │
 * │                                                                             │
 * │  CLOSED：正常状态，请求正常转发到下游服务                                   │
 * │  OPEN：熔断状态，请求直接走降级逻辑，不调用下游服务                         │
 * │  HALF_OPEN：半开状态，放行部分请求探测下游服务是否恢复                       │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 配置示例（application.yml）：
 *
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: circuit-breaker-route
 *           uri: lb://some-service
 *           predicates:
 *             - Path=/api/cb/**
 *           filters:
 *             - name: CircuitBreaker
 *               args:
 *                 name: myCircuitBreaker
 *                 fallbackUri: forward:/fallback
 *
 * 需要添加依赖：
 * <dependency>
 *     <groupId>org.springframework.cloud</groupId>
 *     <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
 * </dependency>
 *
 * Resilience4j 配置（application.yml）：
 *
 * resilience4j:
 *   circuitbreaker:
 *     configs:
 *       default:
 *         slidingWindowSize: 10                  # 滑动窗口大小
 *         minimumNumberOfCalls: 5                # 最小调用次数
 *         failureRateThreshold: 50               # 失败率阈值（%）
 *         waitDurationInOpenState: 10000         # 熔断等待时间（ms）
 *         permittedNumberOfCallsInHalfOpenState: 3  # 半开状态允许的调用次数
 *     instances:
 *       myCircuitBreaker:
 *         baseConfig: default
 *   timelimiter:
 *     configs:
 *       default:
 *         timeoutDuration: 3s                    # 超时时间
 */
@Component
public class FallbackHandler implements HandlerFunction<ServerResponse> {

    /**
     * 处理降级请求
     */
    @Override
    public Mono<ServerResponse> handle(ServerRequest request) {
        // 获取异常信息（如果有）
        Throwable throwable = request.attribute("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION")
                .map(obj -> (Throwable) obj)
                .orElse(null);

        // 构建降级响应
        Map<String, Object> result = new HashMap<>();
        result.put("code", HttpStatus.SERVICE_UNAVAILABLE.value());
        result.put("message", "服务暂时不可用，请稍后重试");
        result.put("path", request.path());
        result.put("timestamp", System.currentTimeMillis());

        if (throwable != null) {
            result.put("error", throwable.getMessage());
        }

        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(result));
    }
}
