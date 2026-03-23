package cn.itzixiao.interview.tomcat.interceptor;

import cn.itzixiao.interview.tomcat.service.TomcatPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tomcat 性能统计拦截器
 * 
 * <p>自动拦截所有 HTTP 请求，统计请求处理时间和结果</p>
 * 
 * <p>统计指标：</p>
 * <ul>
 *   <li>请求次数：每个 URI 的请求总数</li>
 *   <li>响应时间：平均响应时间、总响应时间</li>
 *   <li>错误率：每个 URI 的错误比例</li>
 *   <li>慢请求：超过阈值的请求数量</li>
 * </ul>
 * 
 * <p>配置方式：</p>
 * <pre>
 * {@code
 * @Configuration
 * public class WebConfig implements WebMvcConfigurer {
 *     @Autowired
 *     private PerformanceInterceptor performanceInterceptor;
 *     
 *     @Override
 *     public void addInterceptors(InterceptorRegistry registry) {
 *         registry.addInterceptor(performanceInterceptor)
 *                 .addPathPatterns("/**")
 *                 .excludePathPatterns("/health", "/actuator/**");
 *     }
 * }
 * }
 * </pre>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PerformanceInterceptor implements HandlerInterceptor {

    private final TomcatPerformanceService performanceService;
    
    /**
     * 请求开始时间属性名
     */
    private static final String START_TIME_ATTRIBUTE = "performance_start_time";
    
    /**
     * 请求 URI 属性名
     */
    private static final String REQUEST_URI_ATTRIBUTE = "performance_request_uri";

    /**
     * 请求预处理
     * 
     * <p>在请求处理之前记录开始时间</p>
     * 
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return 是否继续处理
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler) {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);
        
        // 获取请求 URI（去除查询参数）
        String uri = request.getRequestURI();
        request.setAttribute(REQUEST_URI_ATTRIBUTE, uri);
        
        // 记录请求开始
        performanceService.recordRequestStart(uri);
        
        return true;
    }

    /**
     * 请求后处理
     * 
     * <p>在请求处理之后记录响应时间和状态</p>
     * 
     * @param request   HTTP 请求
     * @param response  HTTP 响应
     * @param handler   处理器
     * @param ex        异常（如果有）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        // 获取开始时间
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        String uri = (String) request.getAttribute(REQUEST_URI_ATTRIBUTE);
        
        if (startTime != null && uri != null) {
            // 判断请求是否成功
            boolean success = (ex == null && response.getStatus() < 400);
            
            // 记录请求结束
            performanceService.recordRequestEnd(uri, startTime, success);
            
            // 记录慢请求日志
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 1000) {
                log.warn("Slow request: uri={}, method={}, duration={}ms, status={}", 
                    uri, request.getMethod(), duration, response.getStatus());
            }
        }
    }
}
