package cn.itzixiao.interview.springboot;

/**
 * Spring Boot 自动装配原理详解
 * <p>
 * 自动装配核心流程：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. @SpringBootApplication                                   │
 * │     └─ @EnableAutoConfiguration                              │
 * │         └─ @Import(AutoConfigurationImportSelector.class)    │
 * │                                                              │
 * │  2. AutoConfigurationImportSelector                          │
 * │     └─ selectImports()                                       │
 * │         └─ getAutoConfigurationEntry()                       │
 * │             └─ getCandidateConfigurations()                  │
 * │                 └─ SpringFactoriesLoader.loadFactoryNames()  │
 * │                     └─ 读取 META-INF/spring.factories          │
 * │                                                              │
 * │  3. 过滤配置类                                                │
 * │     └─ @ConditionalOnClass / @ConditionalOnMissingClass      │
 * │     └─ @ConditionalOnBean / @ConditionalOnMissingBean        │
 * │     └─ @ConditionalOnProperty                                │
 * │                                                              │
 * │  4. 执行自动配置类                                            │
 * │     └─ 创建 Bean，注入属性（@EnableConfigurationProperties） │
 * └─────────────────────────────────────────────────────────────┘
 */
public class AutoConfigurationDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring Boot 自动装配原理 ==========\n");

        demonstrateAutoConfigurationPrinciple();
        demonstrateConditionalAnnotations();
        demonstrateSpringFactoriesLoader();
        demonstrateCustomStarter();
    }

    /**
     * 1. 自动装配原理
     */
    private static void demonstrateAutoConfigurationPrinciple() {
        System.out.println("【1. 自动装配核心原理】\n");

        System.out.println("@SpringBootApplication 组合注解：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  @SpringBootConfiguration  （标记为配置类）                   │");
        System.out.println("│  └─ @Configuration                                          │");
        System.out.println("│                                                             │");
        System.out.println("│  @EnableAutoConfiguration  （启用自动装配）                   │");
        System.out.println("│  └─ @AutoConfigurationPackage                                │");
        System.out.println("│  └─ @Import(AutoConfigurationImportSelector.class)          │");
        System.out.println("│                                                             │");
        System.out.println("│  @ComponentScan  （组件扫描）                                │");
        System.out.println("│  └─ basePackages = 当前包及其子包                            │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("自动装配流程：");
        System.out.println("┌─────────────┐     ┌─────────────────────┐     ┌─────────────┐");
        System.out.println("│  启动类      │────→│  @EnableAutoConfig  │────→│  读取 spring│");
        System.out.println("│  @SpringBoot │     │  导入 Selector      │     │  .factories │");
        System.out.println("│  Application │     └─────────────────────┘     └──────┬──────┘");
        System.out.println("└─────────────┘                                        │");
        System.out.println("                                                       ↓");
        System.out.println("┌─────────────┐     ┌─────────────────────┐     ┌─────────────┐");
        System.out.println("│  创建 Bean   │←────│  过滤（@Conditional）│←────│  获取配置类  │");
        System.out.println("│  注入容器    │     │  条件匹配            │     │  列表        │");
        System.out.println("└─────────────┘     └─────────────────────┘     └─────────────┘\n");
    }

    /**
     * 2. 条件注解
     */
    private static void demonstrateConditionalAnnotations() {
        System.out.println("【2. 条件注解（@Conditional）】\n");

        System.out.println("常用条件注解：");
        System.out.println("┌─────────────────────────────┬─────────────────────────────────┐");
        System.out.println("│  @ConditionalOnClass         │  类路径存在指定类时生效          │");
        System.out.println("│  示例：@ConditionalOnClass({RedisOperations.class})           │");
        System.out.println("├─────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnMissingClass  │  类路径不存在指定类时生效        │");
        System.out.println("├─────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnBean          │  容器中存在指定 Bean 时生效      │");
        System.out.println("├─────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnMissingBean   │  容器中不存在指定 Bean 时生效    │");
        System.out.println("├─────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnProperty      │  指定属性满足条件时生效          │");
        System.out.println("│  示例：@ConditionalOnProperty(prefix=\"redis\", name=\"host\")  │");
        System.out.println("├─────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnWebApplication│  是 Web 应用时生效               │");
        System.out.println("├─────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnExpression    │  SpEL 表达式为 true 时生效       │");
        System.out.println("└─────────────────────────────┴─────────────────────────────────┘\n");

        System.out.println("条件注解原理：");
        System.out.println("- 每个 @Conditional 对应一个 Condition 接口实现");
        System.out.println("- matches() 方法返回 true 则配置类生效");
        System.out.println("- Spring Boot 在刷新上下文时评估条件\n");

        System.out.println("自定义条件注解示例：");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│  @Conditional(OnLinuxCondition.class)   │");
        System.out.println("│  public @interface ConditionalOnLinux { │");
        System.out.println("│  }                                      │");
        System.out.println("│                                         │");
        System.out.println("│  class OnLinuxCondition implements      │");
        System.out.println("│          Condition {                    │");
        System.out.println("│      @Override                          │");
        System.out.println("│      public boolean matches(...) {      │");
        System.out.println("│          return System.getProperty(\"os.name\")│");
        System.out.println("│                 .contains(\"Linux\");     │");
        System.out.println("│      }                                  │");
        System.out.println("│  }                                      │");
        System.out.println("└─────────────────────────────────────────┘\n");
    }

    /**
     * 3. SpringFactoriesLoader 机制
     */
    private static void demonstrateSpringFactoriesLoader() {
        System.out.println("【3. SpringFactoriesLoader 机制】\n");

        System.out.println("META-INF/spring.factories 文件：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  # Auto Configure                                           │");
        System.out.println("│  org.springframework.boot.autoconfigure.EnableAutoConfiguration=│");
        System.out.println("│  org.springframework.boot.autoconfigure.web.servlet.        │");
        System.out.println("│      DispatcherServletAutoConfiguration,                    │");
        System.out.println("│  org.springframework.boot.autoconfigure.web.servlet.        │");
        System.out.println("│      WebMvcAutoConfiguration,                               │");
        System.out.println("│  org.springframework.boot.autoconfigure.jdbc.               │");
        System.out.println("│      DataSourceAutoConfiguration,                           │");
        System.out.println("│  org.springframework.boot.autoconfigure.data.redis.         │");
        System.out.println("│      RedisAutoConfiguration,                                │");
        System.out.println("│  ...                                                        │");
        System.out.println("│                                                             │");
        System.out.println("│  # Application Listeners                                    │");
        System.out.println("│  org.springframework.context.ApplicationListener=           │");
        System.out.println("│  org.springframework.boot.ClearCachesApplicationListener,   │");
        System.out.println("│  ...                                                        │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("Spring Boot 2.7+ 新特性：");
        System.out.println("- 使用 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        System.out.println("- 替代 spring.factories 中的 EnableAutoConfiguration 配置");
        System.out.println("- 每行一个自动配置类，更清晰\n");

        System.out.println("自动配置类结构示例（RedisAutoConfiguration）：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Configuration(proxyBeanMethods = false)                   │");
        System.out.println("│  @ConditionalOnClass(RedisOperations.class)                 │");
        System.out.println("│  @EnableConfigurationProperties(RedisProperties.class)      │");
        System.out.println("│  public class RedisAutoConfiguration {                      │");
        System.out.println("│                                                             │");
        System.out.println("│      @Bean                                                  │");
        System.out.println("│      @ConditionalOnMissingBean(RedisConnectionFactory.class)│");
        System.out.println("│      public RedisConnectionFactory          │");
        System.out.println("│              redisConnectionFactory(...) { ... }            │");
        System.out.println("│                                                             │");
        System.out.println("│      @Bean                                                  │");
        System.out.println("│      @ConditionalOnMissingBean(RedisTemplate.class)         │");
        System.out.println("│      public RedisTemplate<Object, Object> redisTemplate(...)│");
        System.out.println("│      { ... }                                                │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 4. 自定义 Starter
     */
    private static void demonstrateCustomStarter() {
        System.out.println("【4. 自定义 Starter】\n");

        System.out.println("Starter 命名规范：");
        System.out.println("- 官方：spring-boot-starter-*（如 spring-boot-starter-web）");
        System.out.println("- 第三方：*-spring-boot-starter（如 mybatis-spring-boot-starter）\n");

        System.out.println("自定义 Starter 结构：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  my-spring-boot-starter/                                    │");
        System.out.println("│  ├── src/main/java/                                         │");
        System.out.println("│  │   └── com/example/autoconfigure/                         │");
        System.out.println("│  │       ├── MyAutoConfiguration.java      # 自动配置类     │");
        System.out.println("│  │       ├── MyProperties.java             # 配置属性类     │");
        System.out.println("│  │       └── MyService.java                # 核心服务类     │");
        System.out.println("│  └── src/main/resources/                                    │");
        System.out.println("│      └── META-INF/                                          │");
        System.out.println("│          └── spring/                                        │");
        System.out.println("│              └── org.springframework.boot.autoconfigure.    │");
        System.out.println("│                  AutoConfiguration.imports   # 自动配置导入  │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("核心代码示例：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 1. 配置属性类                                           │");
        System.out.println("│  @ConfigurationProperties(prefix = \"my.service\")            │");
        System.out.println("│  public class MyProperties {                                │");
        System.out.println("│      private boolean enabled = true;                        │");
        System.out.println("│      private String name = \"default\";                       │");
        System.out.println("│      // getter/setter                                       │");
        System.out.println("│  }                                                          │");
        System.out.println("│                                                             │");
        System.out.println("│  // 2. 自动配置类                                           │");
        System.out.println("│  @AutoConfiguration                                         │");
        System.out.println("│  @ConditionalOnClass(MyService.class)                       │");
        System.out.println("│  @EnableConfigurationProperties(MyProperties.class)         │");
        System.out.println("│  @ConditionalOnProperty(prefix=\"my.service\",               │");
        System.out.println("│                          name=\"enabled\", matchIfMissing=true)│");
        System.out.println("│  public class MyAutoConfiguration {                         │");
        System.out.println("│                                                             │");
        System.out.println("│      @Bean                                                  │");
        System.out.println("│      @ConditionalOnMissingBean                              │");
        System.out.println("│      public MyService myService(MyProperties props) {       │");
        System.out.println("│          return new MyService(props.getName());             │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("│                                                             │");
        System.out.println("│  // 3. spring.factories 或 AutoConfiguration.imports        │");
        System.out.println("│  com.example.autoconfigure.MyAutoConfiguration              │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("使用自定义 Starter：");
        System.out.println("1. 在 pom.xml 引入依赖");
        System.out.println("2. application.yml 配置属性");
        System.out.println("   my.service.name=MyCustomName");
        System.out.println("3. 直接注入使用");
        System.out.println("   @Autowired");
        System.out.println("   private MyService myService;\n");
    }
}
