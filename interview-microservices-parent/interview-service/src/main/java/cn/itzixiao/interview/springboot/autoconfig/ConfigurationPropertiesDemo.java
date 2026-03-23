package cn.itzixiao.interview.springboot.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @ConfigurationProperties 配置绑定详解
 * <p>
 * 作用：将配置文件中的属性绑定到 Java 对象
 * <p>
 * 使用方式：
 * 1. @Component + @ConfigurationProperties
 * 2. @EnableConfigurationProperties + @ConfigurationProperties
 * 3. @ConfigurationPropertiesScan (Spring Boot 2.2+)
 * <p>
 * 配置文件示例（application.yml）：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  myapp:                                                      │
 * │    name: MyApplication                                       │
 * │    version: 1.0.0                                            │
 * │    server:                                                   │
 * │      host: localhost                                         │
 * │      port: 8080                                              │
 * │    features:                                                 │
 * │      - feature1                                              │
 * │      - feature2                                              │
 * │    databases:                                                │
 * │      - name: db1                                             │
 * │        url: jdbc:mysql://localhost/db1                       │
 * │      - name: db2                                             │
 * │        url: jdbc:mysql://localhost/db2                       │
 * └─────────────────────────────────────────────────────────────┘
 */
public class ConfigurationPropertiesDemo {

    /**
     * 配置属性类示例
     */
    @ConfigurationProperties(prefix = "myapp")
    @Validated  // 开启 JSR-303 校验
    public static class MyAppProperties {

        /**
         * 应用名称
         */
        @NotNull
        private String name;

        /**
         * 应用版本
         */
        private String version = "1.0.0";

        /**
         * 是否启用调试模式
         */
        private boolean debug = false;

        /**
         * 嵌套配置对象
         */
        @NestedConfigurationProperty
        private ServerProperties server = new ServerProperties();

        /**
         * 列表配置
         */
        private List<String> features = new ArrayList<>();

        /**
         * 复杂对象列表
         */
        private List<DatabaseProperties> databases = new ArrayList<>();

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public boolean isDebug() {
            return debug;
        }

        public void setDebug(boolean debug) {
            this.debug = debug;
        }

        public ServerProperties getServer() {
            return server;
        }

        public void setServer(ServerProperties server) {
            this.server = server;
        }

        public List<String> getFeatures() {
            return features;
        }

        public void setFeatures(List<String> features) {
            this.features = features;
        }

        public List<DatabaseProperties> getDatabases() {
            return databases;
        }

        public void setDatabases(List<DatabaseProperties> databases) {
            this.databases = databases;
        }
    }

    /**
     * 服务器配置
     */
    public static class ServerProperties {

        /**
         * 主机地址
         */
        private String host = "localhost";

        /**
         * 端口号
         */
        @Min(1024)
        @Max(65535)
        private int port = 8080;

        /**
         * 连接超时（秒）
         */
        private int timeout = 30;

        // Getters and Setters
        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * 数据库配置
     */
    public static class DatabaseProperties {

        /**
         * 数据库名称
         */
        private String name;

        /**
         * JDBC URL
         */
        private String url;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * 松散绑定示例
     * <p>
     * 以下配置都可以绑定到 myAppName：
     * - myapp.my-app-name
     * - myapp.myAppName
     * - myapp.my_app_name
     * - MYAPP_MYAPPNAME
     */
    @ConfigurationProperties(prefix = "myapp")
    public static class RelaxedBindingExample {
        private String myAppName;

        public String getMyAppName() {
            return myAppName;
        }

        public void setMyAppName(String myAppName) {
            this.myAppName = myAppName;
        }
    }

    /**
     * 配置元数据
     * <p>
     * 在 resources/META-INF/additional-spring-configuration-metadata.json
     * 中添加配置提示信息，IDE 会显示自动补全
     */
    public void configurationMetadata() {
        System.out.println("========== 配置元数据示例 ==========\n");

        System.out.println("文件：META-INF/additional-spring-configuration-metadata.json\n");

        System.out.println("{");
        System.out.println("  \"properties\": [");
        System.out.println("    {");
        System.out.println("      \"name\": \"myapp.name\",");
        System.out.println("      \"type\": \"java.lang.String\",");
        System.out.println("      \"description\": \"应用名称\"");
        System.out.println("    },");
        System.out.println("    {");
        System.out.println("      \"name\": \"myapp.server.port\",");
        System.out.println("      \"type\": \"java.lang.Integer\",");
        System.out.println("      \"description\": \"服务端口号\",");
        System.out.println("      \"defaultValue\": 8080");
        System.out.println("    }");
        System.out.println("  ],");
        System.out.println("  \"hints\": [");
        System.out.println("    {");
        System.out.println("      \"name\": \"myapp.server.port\",");
        System.out.println("      \"values\": [");
        System.out.println("        {\"value\": 8080, \"description\": \"HTTP默认端口\"},");
        System.out.println("        {\"value\": 8443, \"description\": \"HTTPS默认端口\"}");
        System.out.println("      ]");
        System.out.println("    }");
        System.out.println("  ]");
        System.out.println("}");
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        System.out.println("========== @ConfigurationProperties 详解 ==========\n");

        System.out.println("【使用方式1】@Component + @ConfigurationProperties");
        System.out.println("  @Component");
        System.out.println("  @ConfigurationProperties(prefix = \"myapp\")");
        System.out.println("  public class MyAppProperties { ... }\n");

        System.out.println("【使用方式2】@EnableConfigurationProperties");
        System.out.println("  @Configuration");
        System.out.println("  @EnableConfigurationProperties(MyAppProperties.class)");
        System.out.println("  public class MyConfig { ... }\n");

        System.out.println("【使用方式3】@ConfigurationPropertiesScan (Spring Boot 2.2+)");
        System.out.println("  @SpringBootApplication");
        System.out.println("  @ConfigurationPropertiesScan(\"com.example.properties\")");
        System.out.println("  public class Application { ... }\n");

        System.out.println("【配置提示】");
        System.out.println("  添加 spring-boot-configuration-processor 依赖");
        System.out.println("  编译后会在 META-INF 生成 spring-configuration-metadata.json");
        System.out.println("  IDE 会读取该文件提供配置自动补全\n");
    }
}
