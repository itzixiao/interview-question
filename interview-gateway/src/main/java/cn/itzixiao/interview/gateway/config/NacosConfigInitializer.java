package cn.itzixiao.interview.gateway.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Nacos 配置初始化器
 * <p>
 * 大厂规范：项目启动时自动检测 Nacos 中是否存在 common.yml，
 * 若不存在则自动上传默认配置，确保多服务共享配置的一致性。
 * <p>
 * 使用场景：
 * 1. 新环境部署：自动初始化公共配置，无需手动操作 Nacos 控制台
 * 2. 开发环境：本地启动时自动同步 JWT 密钥等公共配置
 * 3. CI/CD：容器启动时自动完成配置初始化
 * <p>
 * 安全说明：仅在 Nacos 中不存在该配置时才上传，不会覆盖已有配置。
 *
 * @author itzixiao
 * @date 2026-03-25
 */
@Slf4j
@Component
public class NacosConfigInitializer implements ApplicationRunner {

    private static final String COMMON_DATA_ID = "common.yml";
    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";

    @Value("${spring.cloud.nacos.config.server-addr:localhost:8848}")
    private String nacosServerAddr;

    @Value("${spring.cloud.nacos.config.namespace:}")
    private String namespace;

    /**
     * common.yml 默认内容
     * <p>
     * 包含所有微服务共享的公共配置：
     * - JWT 密钥（Gateway 和 Provider 统一引用，确保一致性）
     * - 可扩展其他公共配置
     */
    private static final String COMMON_YML_CONTENT =
            "# ============================================================\n" +
            "# Nacos 公共配置 - common.yml\n" +
            "# 由 NacosConfigInitializer 自动初始化（首次启动时写入）\n" +
            "#\n" +
            "# 适用范围：所有微服务共享\n" +
            "# 引用方式：在 shared-configs 中声明 data-id: common.yml\n" +
            "# ============================================================\n" +
            "\n" +
            "# JWT 公共配置（Gateway 和 Provider 统一引用此处的值）\n" +
            "# 生产环境请务必修改 secret-key，或通过 JWT_SECRET_KEY 环境变量覆盖\n" +
            "jwt:\n" +
            "  secret-key: itzixiao-interview-system-secret-key-2026-this-is-a-demo-for-learning\n" +
            "  issuer: interview-provider\n" +
            "  expiration: 7200        # Access Token 有效期（秒），默认 2 小时\n" +
            "  refresh-expiration: 604800  # Refresh Token 有效期（秒），默认 7 天\n" +
            "  token-type: Bearer\n" +
            "\n" +
            "# 全局日志级别（可按需调整）\n" +
            "logging:\n" +
            "  level:\n" +
            "    cn.itzixiao: INFO\n";

    @Override
    public void run(ApplicationArguments args) {
        try {
            initCommonConfig();
        } catch (Exception e) {
            // 初始化失败不阻断启动，仅打印警告日志
            log.warn("【Nacos初始化】common.yml 初始化失败（可忽略，Nacos 不可用时的正常现象）: {}", e.getMessage());
        }
    }

    /**
     * 初始化 common.yml 到 Nacos
     * <p>
     * 逻辑：
     * 1. 连接 Nacos ConfigService
     * 2. 查询 common.yml 是否已存在
     * 3. 不存在则发布默认配置；已存在则跳过（不覆盖）
     */
    private void initCommonConfig() throws NacosException {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", nacosServerAddr);
        if (namespace != null && !namespace.isEmpty()) {
            properties.setProperty("namespace", namespace);
        }

        ConfigService configService = NacosFactory.createConfigService(properties);

        // 查询 common.yml 是否已存在（3 秒超时）
        String existingConfig = configService.getConfig(COMMON_DATA_ID, DEFAULT_GROUP, 3000);

        if (existingConfig != null && !existingConfig.isEmpty()) {
            log.info("【Nacos初始化】common.yml 已存在，跳过初始化（不覆盖现有配置）");
            return;
        }

        // 发布默认 common.yml 配置
        boolean published = configService.publishConfig(
                COMMON_DATA_ID, DEFAULT_GROUP, COMMON_YML_CONTENT, ConfigType.YAML.getType());

        if (published) {
            log.info("【Nacos初始化】common.yml 初始化成功！已上传默认 JWT 公共配置到 Nacos（namespace: {}, group: {}）",
                    namespace.isEmpty() ? "public" : namespace, DEFAULT_GROUP);
            log.info("【Nacos初始化】提示：生产环境请在 Nacos 控制台修改 jwt.secret-key 为强密钥，或配置环境变量 JWT_SECRET_KEY");
        } else {
            log.warn("【Nacos初始化】common.yml 上传失败，请检查 Nacos 连接和权限配置");
        }
    }
}
