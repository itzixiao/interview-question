package cn.itzixiao.interview.springboot.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义 Starter 开发完整流程
 *
 * 开发步骤：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 创建 Maven 项目                                          │
 * │     - 命名规范：xxx-spring-boot-starter                      │
 * │     - 类型：jar 包                                           │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. 添加依赖                                                 │
 * │     - spring-boot-autoconfigure                              │
 * │     - 功能所需的依赖（如 httpclient、redis等）               │
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. 创建自动配置类                                           │
 * │     - XxxAutoConfiguration                                   │
 * │     - XxxProperties（配置属性类）                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │  4. 创建 spring.factories                                    │
 * │     - 路径：META-INF/spring.factories                        │
 * │     - 内容：注册自动配置类                                   │
 * ├─────────────────────────────────────────────────────────────┤
 * │  5. 打包发布                                                 │
 * │     - mvn clean install                                      │
 * │     - 或发布到 Maven 仓库                                    │
 * └─────────────────────────────────────────────────────────────┘
 *
 * 项目结构：
 * my-spring-boot-starter/
 * ├── pom.xml
 * └── src/
 *     └── main/
 *         ├── java/
 *         │   └── com/example/autoconfigure/
 *         │       ├── MyAutoConfiguration.java
 *         │       ├── MyProperties.java
 *         │       └── MyService.java
 *         └── resources/
 *             └── META-INF/
 *                 ├── spring.factories (Spring Boot 2.7-)
 *                 └── spring/
 *                     └── org.springframework.boot.autoconfigure.AutoConfiguration.imports (Spring Boot 2.7+)
 */
public class CustomStarterDevelopmentDemo {

    /**
     * 步骤说明
     */
    public static void main(String[] args) {
        System.out.println("========== 自定义 Starter 开发流程 ==========\n");

        System.out.println("【步骤1】创建 Maven 项目");
        System.out.println("  ArtifactId: my-spring-boot-starter");
        System.out.println("  Packaging: jar\n");

        System.out.println("【步骤2】添加 pom.xml 依赖");
        System.out.println("  <dependencies>");
        System.out.println("    <!-- 自动配置 -->");
        System.out.println("    <dependency>");
        System.out.println("      <groupId>org.springframework.boot</groupId>");
        System.out.println("      <artifactId>spring-boot-autoconfigure</artifactId>");
        System.out.println("    </dependency>");
        System.out.println("    <!-- 配置处理器（生成配置元数据） -->");
        System.out.println("    <dependency>");
        System.out.println("      <groupId>org.springframework.boot</groupId>");
        System.out.println("      <artifactId>spring-boot-configuration-processor</artifactId>");
        System.out.println("      <optional>true</optional>");
        System.out.println("    </dependency>");
        System.out.println("  </dependencies>\n");

        System.out.println("【步骤3】创建自动配置类");
        System.out.println("  - MyProperties.java：配置属性");
        System.out.println("  - MyService.java：核心服务");
        System.out.println("  - MyAutoConfiguration.java：自动配置\n");

        System.out.println("【步骤4】创建 spring.factories");
        System.out.println("  路径：src/main/resources/META-INF/spring.factories");
        System.out.println("  内容：");
        System.out.println("  org.springframework.boot.autoconfigure.EnableAutoConfiguration=\\");
        System.out.println("    com.example.autoconfigure.MyAutoConfiguration\n");

        System.out.println("【步骤5】打包安装");
        System.out.println("  mvn clean install\n");

        System.out.println("【使用】在其他项目中引入");
        System.out.println("  <dependency>");
        System.out.println("    <groupId>com.example</groupId>");
        System.out.println("    <artifactId>my-spring-boot-starter</artifactId>");
        System.out.println("    <version>1.0.0</version>");
        System.out.println("  </dependency>\n");
    }
}

/**
 * 示例：自定义短信发送 Starter
 *
 * 功能：封装短信发送功能，自动配置短信服务
 */

/**
 * 1. 配置属性类
 */
@ConfigurationProperties(prefix = "sms")
class SmsProperties {
    /**
     * 短信服务商：aliyun、tencent、huawei
     */
    private String provider = "aliyun";

    /**
     * 访问密钥 ID
     */
    private String accessKeyId;

    /**
     * 访问密钥 Secret
     */
    private String accessKeySecret;

    /**
     * 短信签名
     */
    private String signName;

    /**
     * 连接超时（毫秒）
     */
    private int connectionTimeout = 10000;

    /**
     * 读取超时（毫秒）
     */
    private int readTimeout = 10000;

    // Getters and Setters
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
    public String getAccessKeySecret() { return accessKeySecret; }
    public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
    public String getSignName() { return signName; }
    public void setSignName(String signName) { this.signName = signName; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
}

/**
 * 2. 短信服务接口
 */
interface SmsService {
    /**
     * 发送短信
     * @param phoneNumber 手机号
     * @param templateCode 模板代码
     * @param params 模板参数
     * @return 是否发送成功
     */
    boolean sendSms(String phoneNumber, String templateCode, java.util.Map<String, String> params);
}

/**
 * 3. 阿里云短信服务实现
 */
class AliyunSmsService implements SmsService {
    private final SmsProperties properties;

    public AliyunSmsService(SmsProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean sendSms(String phoneNumber, String templateCode, java.util.Map<String, String> params) {
        System.out.println("【阿里云短信】发送短信到 " + phoneNumber);
        System.out.println("  签名: " + properties.getSignName());
        System.out.println("  模板: " + templateCode);
        System.out.println("  参数: " + params);
        // 实际调用阿里云 SDK
        return true;
    }
}

/**
 * 4. 腾讯云短信服务实现
 */
class TencentSmsService implements SmsService {
    private final SmsProperties properties;

    public TencentSmsService(SmsProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean sendSms(String phoneNumber, String templateCode, java.util.Map<String, String> params) {
        System.out.println("【腾讯云短信】发送短信到 " + phoneNumber);
        System.out.println("  签名: " + properties.getSignName());
        System.out.println("  模板: " + templateCode);
        System.out.println("  参数: " + params);
        // 实际调用腾讯云 SDK
        return true;
    }
}

/**
 * 5. 自动配置类
 */
@Configuration(proxyBeanMethods = false)
@org.springframework.boot.autoconfigure.condition.ConditionalOnClass(SmsService.class)
@org.springframework.boot.context.properties.EnableConfigurationProperties(SmsProperties.class)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "sms",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
class SmsAutoConfiguration {

    /**
     * 创建短信服务 Bean
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(SmsService.class)
    public SmsService smsService(SmsProperties properties) {
        System.out.println("【自动配置】创建短信服务，提供商: " + properties.getProvider());

        switch (properties.getProvider()) {
            case "tencent":
                return new TencentSmsService(properties);
            case "aliyun":
            default:
                return new AliyunSmsService(properties);
        }
    }
}
