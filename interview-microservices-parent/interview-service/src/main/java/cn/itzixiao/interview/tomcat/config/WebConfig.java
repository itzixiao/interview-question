package cn.itzixiao.interview.tomcat.config;

import cn.itzixiao.interview.tomcat.interceptor.PerformanceInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 
 * <p>配置 Spring MVC 相关功能，包括：</p>
 * <ul>
 *   <li>性能统计拦截器</li>
 *   <li>跨域配置</li>
 *   <li>静态资源处理</li>
 * </ul>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PerformanceInterceptor performanceInterceptor;

    /**
     * 注册拦截器
     * 
     * <p>配置性能统计拦截器，拦截所有请求</p>
     * <p>排除健康检查和监控端点，避免干扰</p>
     * 
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/health",
                "/actuator/**",
                "/monitor/tomcat/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/v2/api-docs",
                "/v3/api-docs/**"
            );
    }
}
