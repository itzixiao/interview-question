package cn.itzixiao.interview.openfeign.config;

import feign.Logger;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenFeign 全局配置类
 * <p>
 * 配置 Feign 的各种组件：日志、重试、错误解码器等
 *
 * <pre>
 * 配置方式说明：
 * 1. 全局配置：@Configuration 类中定义 @Bean
 * 2. 特定服务配置：@FeignClient(configuration = XxxConfig.class)
 * 3. 配置文件配置：feign.client.config.xxx
 *
 * 优先级（从高到低）：
 * 1. @FeignClient 的 configuration 属性
 * 2. 配置文件中的特定服务配置
 * 3. 配置文件中的 default 配置
 * 4. @Configuration 类中的全局配置
 * </pre>
 *
 * @author itzixiao
 * @since 1.0
 */
@Slf4j
@Configuration
public class FeignGlobalConfig {

    /**
     * 配置 Feign 日志级别
     * <p>
     * 日志级别说明：
     * - NONE: 不记录任何日志（默认，生产推荐）
     * - BASIC: 仅记录请求方法、URL、响应状态码和执行时间
     * - HEADERS: BASIC + 请求和响应的头信息
     * - FULL: HEADERS + 请求和响应的 Body（开发调试用）
     *
     * @return 日志级别
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        // 开发环境可以使用 FULL，生产环境建议 BASIC 或 NONE
        return Logger.Level.BASIC;
    }

    /**
     * 配置 Feign 重试策略
     * <p>
     * 注意：如果使用了熔断器（Sentinel/Resilience4j），建议禁用 Feign 的重试，
     * 由熔断器统一管理重试和降级逻辑。
     *
     * @return 重试器
     */
    @Bean
    public Retryer feignRetryer() {
        // 禁用重试（推荐配合熔断器使用）
        return Retryer.NEVER_RETRY;

        // 或者配置重试策略
        // return new Retryer.Default(
        //     100,      // 初始间隔 100ms
        //     1000,     // 最大间隔 1s
        //     3         // 最大重试次数 3
        // );
    }

    /**
     * 自定义错误解码器
     * <p>
     * 用于处理非 2xx 响应，可以根据不同的状态码返回不同的异常
     *
     * @return 错误解码器
     */
    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new CustomErrorDecoder();
    }

    /**
     * 自定义错误解码器实现
     */
    @Slf4j
    public static class CustomErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            log.error("Feign 调用异常: methodKey={}, status={}, reason={}",
                    methodKey, response.status(), response.reason());

            // 根据状态码返回不同的异常
            switch (response.status()) {
                case 400:
                    return new IllegalArgumentException("请求参数错误: " + methodKey);
                case 401:
                    return new RuntimeException("未授权: " + methodKey);
                case 403:
                    return new RuntimeException("禁止访问: " + methodKey);
                case 404:
                    return new RuntimeException("资源不存在: " + methodKey);
                case 500:
                    return new RuntimeException("服务内部错误: " + methodKey);
                case 503:
                    return new RuntimeException("服务不可用: " + methodKey);
                default:
                    return defaultDecoder.decode(methodKey, response);
            }
        }
    }
}
