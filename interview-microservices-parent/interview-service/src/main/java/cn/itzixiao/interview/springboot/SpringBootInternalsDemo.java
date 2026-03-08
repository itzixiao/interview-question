package cn.itzixiao.interview.springboot;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.time.Duration;

/**
 * Spring Boot 核心组件详解
 *
 * 核心扩展点：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. ApplicationContextInitializer                            │
 * │     - 在 Context 创建后，刷新前执行                          │
 * │     - 用于修改 Context 状态，如添加 PropertySource           │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. ApplicationListener                                      │
 * │     - 监听 Spring Boot 生命周期事件                          │
 * │     - 如 ApplicationStartingEvent、ApplicationReadyEvent     │
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. SpringApplicationRunListener                             │
 * │     - 监听 SpringApplication 的 run 方法执行过程             │
 * │     - 更底层的监听机制                                       │
 * ├─────────────────────────────────────────────────────────────┤
 * │  4. EnvironmentPostProcessor                                 │
 * │     - 在 Environment 创建后，用于修改 Environment            │
 * │     - 如加载自定义配置文件                                   │
 * └─────────────────────────────────────────────────────────────┘
 */
public class SpringBootInternalsDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring Boot 核心组件详解 ==========\n");

        demonstrateStartupEvents();
        demonstrateExtensionPoints();
        demonstrateBeanLifecycle();
        demonstrateConfigurationProperties();
    }

    /**
     * 1. 启动事件详解
     */
    private static void demonstrateStartupEvents() {
        System.out.println("【1. Spring Boot 启动事件】\n");

        System.out.println("事件触发顺序：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. ApplicationStartingEvent                                 │");
        System.out.println("│     - 启动开始时触发，Environment 还未创建                   │");
        System.out.println("│     - 用于最早期初始化（如日志系统）                         │");
        System.out.println("│                                                             │");
        System.out.println("│  2. ApplicationEnvironmentPreparedEvent                      │");
        System.out.println("│     - Environment 准备好后触发                               │");
        System.out.println("│     - 可以修改 Environment（添加 PropertySource）            │");
        System.out.println("│                                                             │");
        System.out.println("│  3. ApplicationContextInitializedEvent                       │");
        System.out.println("│     - ApplicationContext 创建并初始化后触发                  │");
        System.out.println("│     - 所有 Initializer 执行完毕                              │");
        System.out.println("│                                                             │");
        System.out.println("│  4. ApplicationPreparedEvent                                 │");
        System.out.println("│     - Context 准备好，Bean 定义加载完成，未刷新              │");
        System.out.println("│     - 可以添加 BeanPostProcessor                             │");
        System.out.println("│                                                             │");
        System.out.println("│  5. ContextRefreshedEvent （Spring 标准事件）                 │");
        System.out.println("│     - Context 刷新完成，所有单例 Bean 创建完成               │");
        System.out.println("│                                                             │");
        System.out.println("│  6. ApplicationStartedEvent                                  │");
        System.out.println("│     - 应用启动完成，CommandLineRunner 未执行                 │");
        System.out.println("│                                                             │");
        System.out.println("│  7. ApplicationReadyEvent                                    │");
        System.out.println("│     - 应用完全就绪，可以接收请求                             │");
        System.out.println("│     - 所有 Runner 执行完毕                                   │");
        System.out.println("│                                                             │");
        System.out.println("│  8. ApplicationFailedEvent （失败时）                        │");
        System.out.println("│     - 启动失败时触发                                         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. 扩展点详解
     */
    private static void demonstrateExtensionPoints() {
        System.out.println("【2. Spring Boot 扩展点】\n");

        System.out.println("ApplicationContextInitializer：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  作用：在 Context 刷新前自定义 Context                       │");
        System.out.println("│                                                             │");
        System.out.println("│  public class MyInitializer implements                      │");
        System.out.println("│          ApplicationContextInitializer {                    │");
        System.out.println("│      @Override                                              │");
        System.out.println("│      public void initialize(ConfigurableApplicationContext) │");
        System.out.println("│          context) {                                         │");
        System.out.println("│          // 添加 PropertySource                             │");
        System.out.println("│          // 注册 BeanDefinition                             │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("│                                                             │");
        System.out.println("│  注册方式：                                                  │");
        System.out.println("│  1. spring.factories:                                        │");
        System.out.println("│     org.springframework.context.ApplicationContextInitializer=│");
        System.out.println("│     com.example.MyInitializer                               │");
        System.out.println("│  2. application.properties:                                  │");
        System.out.println("│     context.initializer.classes=com.example.MyInitializer   │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("EnvironmentPostProcessor：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  作用：在 Environment 创建后修改它                          │");
        System.out.println("│                                                             │");
        System.out.println("│  public class MyEnvPostProcessor implements                 │");
        System.out.println("│          EnvironmentPostProcessor {                         │");
        System.out.println("│      @Override                                              │");
        System.out.println("│      public void postProcessEnvironment(ConfigurableEnvironment│");
        System.out.println("│          environment, SpringApplication application) {      │");
        System.out.println("│          // 从配置中心加载配置                               │");
        System.out.println("│          // 解密加密配置                                     │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("│                                                             │");
        System.out.println("│  注册：META-INF/spring.factories                             │");
        System.out.println("│  org.springframework.boot.env.EnvironmentPostProcessor=      │");
        System.out.println("│  com.example.MyEnvPostProcessor                             │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("ApplicationRunner vs CommandLineRunner：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  共同点：                                                    │");
        System.out.println("│  - 都在 Spring Boot 启动完成后执行                           │");
        System.out.println("│  - 可以通过 Order 控制执行顺序                               │");
        System.out.println("│                                                             │");
        System.out.println("│  区别：                                                      │");
        System.out.println("│  ┌─────────────────┬─────────────────────────────────────┐  │");
        System.out.println("│  │ ApplicationRunner│ 参数是 ApplicationArguments        │  │");
        System.out.println("│  │                 │ 支持 --key=value 和 非选项参数      │  │");
        System.out.println("│  ├─────────────────┼─────────────────────────────────────┤  │");
        System.out.println("│  │CommandLineRunner│ 参数是 String... args（原始参数）   │  │");
        System.out.println("│  │                 │ 需要手动解析参数                     │  │");
        System.out.println("│  └─────────────────┴─────────────────────────────────────┘  │");
        System.out.println("│                                                             │");
        System.out.println("│  示例：                                                      │");
        System.out.println("│  @Component                                                 │");
        System.out.println("│  @Order(1)  // 执行顺序                                      │");
        System.out.println("│  public class MyRunner implements ApplicationRunner {       │");
        System.out.println("│      @Override                                              │");
        System.out.println("│      public void run(ApplicationArguments args) {           │");
        System.out.println("│          // 获取 --name=xxx                                  │");
        System.out.println("│          String name = args.getOptionValues(\"name\").get(0);│");
        System.out.println("│          // 执行业务初始化                                   │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 3. Bean 生命周期
     */
    private static void demonstrateBeanLifecycle() {
        System.out.println("【3. Spring Boot Bean 生命周期】\n");

        System.out.println("完整生命周期：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 实例化（Instantiation）                                  │");
        System.out.println("│     - 调用构造方法创建 Bean 实例                             │");
        System.out.println("│     - BeanPostProcessor.postProcessBeforeInstantiation      │");
        System.out.println("│                                                             │");
        System.out.println("│  2. 属性赋值（Population）                                   │");
        System.out.println("│     - 注入依赖（@Autowired、@Value）                         │");
        System.out.println("│     - InstantiationAwareBeanPostProcessor.postProcessProperties│");
        System.out.println("│                                                             │");
        System.out.println("│  3. 初始化（Initialization）                                 │");
        System.out.println("│     - BeanNameAware.setBeanName()                           │");
        System.out.println("│     - BeanClassLoaderAware.setBeanClassLoader()             │");
        System.out.println("│     - BeanFactoryAware.setBeanFactory()                     │");
        System.out.println("│     - EnvironmentAware.setEnvironment()                     │");
        System.out.println("│     - ResourceLoaderAware.setResourceLoader()               │");
        System.out.println("│     - ApplicationContextAware.setApplicationContext()       │");
        System.out.println("│     - @PostConstruct 方法                                   │");
        System.out.println("│     - InitializingBean.afterPropertiesSet()                 │");
        System.out.println("│     - 自定义 init-method                                    │");
        System.out.println("│     - BeanPostProcessor.postProcessAfterInitialization      │");
        System.out.println("│       （AOP 代理在此创建）                                    │");
        System.out.println("│                                                             │");
        System.out.println("│  4. 使用（In Use）                                           │");
        System.out.println("│     - Bean 就绪，可以被使用                                  │");
        System.out.println("│                                                             │");
        System.out.println("│  5. 销毁（Destruction）                                      │");
        System.out.println("│     - @PreDestroy 方法                                      │");
        System.out.println("│     - DisposableBean.destroy()                              │");
        System.out.println("│     - 自定义 destroy-method                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("BeanPostProcessor 作用：");
        System.out.println("- 在 Bean 初始化前后进行处理");
        System.out.println("- 可以对 Bean 进行包装（如创建代理）");
        System.out.println("- Spring AOP、事务、@Async 等都基于此实现\n");
    }

    /**
     * 4. 配置属性绑定
     */
    private static void demonstrateConfigurationProperties() {
        System.out.println("【4. 配置属性绑定（@ConfigurationProperties）】\n");

        System.out.println("绑定原理：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  application.yml                                            │");
        System.out.println("│  server:                                                    │");
        System.out.println("│    port: 8080                                               │");
        System.out.println("│    servlet:                                                 │");
        System.out.println("│      context-path: /api                                     │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  @ConfigurationProperties(prefix = \"server\")               │");
        System.out.println("│  public class ServerProperties {                            │");
        System.out.println("│      private Integer port;      // 8080                     │");
        System.out.println("│      private Servlet servlet;   // context-path=/api        │");
        System.out.println("│  }                                                          │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  绑定方式：                                                  │");
        System.out.println("│  1. @EnableConfigurationProperties(ServerProperties.class)  │");
        System.out.println("│  2. @ConfigurationPropertiesScan(\"com.example.props\")       │");
        System.out.println("│  3. 直接加 @Component（不推荐）                              │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("松散绑定（Relaxed Binding）：");
        System.out.println("┌─────────────────┬───────────────────────────────────────────┐");
        System.out.println("│  属性           │  可以绑定的形式                            │");
        System.out.println("├─────────────────┼───────────────────────────────────────────┤");
        System.out.println("│  firstName      │  firstName、first-name、first_name、FIRSTNAME │");
        System.out.println("│  server.port    │  server.port、SERVER_PORT                  │");
        System.out.println("└─────────────────┴───────────────────────────────────────────┘\n");

        System.out.println("配置属性校验：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  @ConfigurationProperties(prefix = \"app\")                  │");
        System.out.println("│  @Validated                                                 │");
        System.out.println("│  public class AppProperties {                               │");
        System.out.println("│                                                             │");
        System.out.println("│      @NotNull                                               │");
        System.out.println("│      @Size(min = 5, max = 20)                               │");
        System.out.println("│      private String name;                                   │");
        System.out.println("│                                                             │");
        System.out.println("│      @Min(1024)                                             │");
        System.out.println("│      @Max(65535)                                            │");
        System.out.println("│      private int port;                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("│                                                             │");
        System.out.println("│  需要引入：spring-boot-starter-validation                   │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("@ConfigurationProperties vs @Value：");
        System.out.println("┌─────────────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│   特性          │ @ConfigurationProperties│ @Value           │");
        System.out.println("├─────────────────┼─────────────────────┼─────────────────────┤");
        System.out.println("│  批量绑定       │  支持               │  单个属性           │");
        System.out.println("│  松散绑定       │  支持               │  不支持（严格匹配）  │");
        System.out.println("│  SpEL 表达式    │  不支持             │  支持               │");
        System.out.println("│  JSR-303 校验   │  支持               │  不支持             │");
        System.out.println("│  复杂类型       │  支持               │  不支持             │");
        System.out.println("│  类型安全       │  是                 │  否（运行时解析）    │");
        System.out.println("└─────────────────┴─────────────────────┴─────────────────────┘\n");
    }
}

// ==================== 自定义组件示例 ====================

/**
 * 自定义 ApplicationContextInitializer
 */
class CustomInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.out.println("[CustomInitializer] 执行初始化...");
        // 可以在这里添加自定义的 PropertySource
    }
}

/**
 * 自定义 ApplicationListener
 */
class CustomListener implements ApplicationListener<org.springframework.boot.context.event.ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(org.springframework.boot.context.event.ApplicationReadyEvent event) {
        System.out.println("[CustomListener] 应用已就绪！");
    }
}

/**
 * 自定义 SpringApplicationRunListener
 */
class CustomRunListener implements SpringApplicationRunListener {

    public CustomRunListener(SpringApplication application, String[] args) {
    }

    @Override
    public void starting(ConfigurableBootstrapContext bootstrapContext) {
        System.out.println("[CustomRunListener] starting...");
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
        System.out.println("[CustomRunListener] environmentPrepared...");
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        System.out.println("[CustomRunListener] contextPrepared...");
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
        System.out.println("[CustomRunListener] contextLoaded...");
    }

    @Override
    public void started(ConfigurableApplicationContext context, Duration timeTaken) {
        System.out.println("[CustomRunListener] started...");
    }

    @Override
    public void ready(ConfigurableApplicationContext context, Duration timeTaken) {
        System.out.println("[CustomRunListener] ready...");
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        System.out.println("[CustomRunListener] failed...");
    }
}
