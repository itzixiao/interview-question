package cn.itzixiao.interview.security.gateway;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * 网关白名单配置属性
 * <p>
 * 配置项：gateway.white-list.paths
 * <p>
 * 支持两种匹配模式：
 * 1. 前缀匹配：路径以 /** 结尾，如 /api/interview/**
 * 2. 包含匹配：路径中包含该字符串，如 /login
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Data
@ConfigurationProperties(prefix = "gateway.white-list")
public class WhiteListProperties {

    /**
     * 白名单路径列表
     * <p>
     * 示例：
     * - /api/auth/login      → 精确匹配登录接口
     * - /api/auth/register   → 精确匹配注册接口
     * - /api/public/**       → 前缀匹配公共接口
     * - /actuator/health     → 健康检查接口
     */
    private List<String> paths = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health"
    );
}
