package cn.itzixiao.interview.openfeign.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Feign 请求拦截器
 * <p>
 * 用于在 Feign 调用前对请求进行统一处理
 *
 * <pre>
 * 常见使用场景：
 * 1. 传递认证信息（Token、签名）
 * 2. 添加公共请求头（TraceId、来源标识）
 * 3. 请求日志记录
 * 4. 参数加密/签名
 * </pre>
 *
 * @author itzixiao
 * @since 1.0
 */
@Slf4j
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    /**
     * 需要传递的请求头列表
     */
    private static final String[] HEADERS_TO_PROPAGATE = {
            "Authorization",
            "X-Request-Id",
            "X-User-Id",
            "X-Tenant-Id"
    };

    @Override
    public void apply(RequestTemplate template) {
        // 1. 生成或传递 TraceId（链路追踪）
        String traceId = getOrGenerateTraceId();
        template.header("X-Trace-Id", traceId);

        // 2. 添加来源标识
        template.header("X-Request-Source", "interview-service");

        // 3. 传递上游请求头
        propagateHeaders(template);

        log.debug("Feign 请求拦截: {} {}, TraceId={}",
                template.method(), template.url(), traceId);
    }

    /**
     * 获取或生成 TraceId
     */
    private String getOrGenerateTraceId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId != null && !traceId.isEmpty()) {
                return traceId;
            }
        }
        // 生成新的 TraceId
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 传递上游请求头
     * <p>
     * 解决 Feign 调用丢失 Header 的问题
     */
    private void propagateHeaders(RequestTemplate template) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes)) {
            log.debug("无法获取当前请求上下文，跳过请求头传递");
            return;
        }

        HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();

        for (String headerName : HEADERS_TO_PROPAGATE) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && !headerValue.isEmpty()) {
                template.header(headerName, headerValue);
                log.debug("传递请求头: {}={}", headerName, headerValue);
            }
        }
    }
}
