package cn.itzixiao.interview.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 网关白名单配置（无需鉴权的路径）
 * 配置项：gateway.white-list.paths
 */
@Component
@ConfigurationProperties(prefix = "gateway.white-list")
public class WhiteListProperties {

    /**
     * 白名单路径列表，支持前缀匹配（以 ** 结尾）和精确包含匹配
     * 示例：
     *   - /api/interview/**  → 前缀匹配，/api/interview/ 开头的所有路径
     *   - /login             → 包含匹配，路径中含有 /login 的请求
     */
    private List<String> paths = Arrays.asList("/login", "/register", "/api/interview/**");

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}
