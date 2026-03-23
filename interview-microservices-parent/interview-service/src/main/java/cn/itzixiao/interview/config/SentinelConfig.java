package cn.itzixiao.interview.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Sentinel 配置类
 * 统一处理限流、降级、系统保护等异常
 */
@Slf4j
@Configuration
public class SentinelConfig implements BlockExceptionHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
        log.warn("Sentinel 拦截请求: {}, 原因: {}", request.getRequestURI(), e.getMessage());

        String message = "访问被拒绝";
        int status = 429;

        if (e instanceof FlowException) {
            message = "请求过于频繁，已被限流";
        } else if (e instanceof DegradeException) {
            message = "服务暂时不可用，请稍后再试";
            status = 503;
        } else if (e instanceof ParamFlowException) {
            message = "热点参数限流";
        } else if (e instanceof SystemBlockException) {
            message = "系统负载过高，请稍后再试";
            status = 503;
        } else if (e instanceof AuthorityException) {
            message = "无权限访问";
            status = 403;
        }

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=utf-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", status);
        result.put("message", message);
        result.put("path", request.getRequestURI());

        PrintWriter out = response.getWriter();
        out.print(JSON.toJSONString(result));
        out.flush();
        out.close();
    }
}
