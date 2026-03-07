package cn.itzixiao.interview.gateway.config;

import cn.itzixiao.interview.gateway.handler.FallbackHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * 路由配置 - 函数式端点
 *
 * 用于配置降级路由等功能
 */
@Configuration
public class RouterConfig {

    private final FallbackHandler fallbackHandler;

    public RouterConfig(FallbackHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
    }

    /**
     * 降级路由
     * 当服务熔断时，请求会转发到这个端点
     */
    @Bean
    public RouterFunction<ServerResponse> fallbackRoute() {
        return RouterFunctions.route()
                .GET("/fallback", fallbackHandler)
                .POST("/fallback", fallbackHandler)
                .PUT("/fallback", fallbackHandler)
                .DELETE("/fallback", fallbackHandler)
                .build();
    }
}
