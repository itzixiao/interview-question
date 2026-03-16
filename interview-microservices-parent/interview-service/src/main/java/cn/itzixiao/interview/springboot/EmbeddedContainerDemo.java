package cn.itzixiao.interview.springboot;

/**
 * Spring Boot 嵌入式容器详解
 * <p>
 * Spring Boot 2.x vs 3.x 容器变化：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Spring Boot 2.x                                             │
 * │  - 默认：Tomcat                                              │
 * │  - 可选：Jetty、Undertow                                     │
 * │  - Servlet API：javax.servlet.*                              │
 * ├─────────────────────────────────────────────────────────────┤
 * │  Spring Boot 3.x                                             │
 * │  - 默认：Tomcat 10+                                          │
 * │  - 可选：Jetty 11+、Undertow 2+                              │
 * │  - Servlet API：jakarta.servlet.*（ Jakarta EE 9+）          │
 * └─────────────────────────────────────────────────────────────┘
 */
public class EmbeddedContainerDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring Boot 嵌入式容器详解 ==========\n");

        demonstrateContainerAutoConfiguration();
        demonstrateServletContext();
        demonstrateReactiveStack();
        demonstrateContainerComparison();
    }

    /**
     * 1. 容器自动配置
     */
    private static void demonstrateContainerAutoConfiguration() {
        System.out.println("【1. 嵌入式容器自动配置】\n");

        System.out.println("ServletWebServerFactoryAutoConfiguration：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  @AutoConfiguration                                         │");
        System.out.println("│  @ConditionalOnWebApplication(type = Type.SERVLET)          │");
        System.out.println("│  @EnableConfigurationProperties(ServerProperties.class)     │");
        System.out.println("│  public class ServletWebServerFactoryAutoConfiguration {    │");
        System.out.println("│                                                             │");
        System.out.println("│      @Bean                                                  │");
        System.out.println("│      @ConditionalOnClass(name = \"org.apache.catalina.startup.Tomcat\")│");
        System.out.println("│      public TomcatServletWebServerFactory tomcatFactory() { │");
        System.out.println("│          return new TomcatServletWebServerFactory();        │");
        System.out.println("│      }                                                      │");
        System.out.println("│                                                             │");
        System.out.println("│      @Bean                                                  │");
        System.out.println("│      @ConditionalOnClass(name = \"org.eclipse.jetty.server.Server\")│");
        System.out.println("│      public JettyServletWebServerFactory jettyFactory() {   │");
        System.out.println("│          return new JettyServletWebServerFactory();         │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("切换容器：");
        System.out.println("1. 排除 Tomcat，引入 Jetty：");
        System.out.println("   <dependency>");
        System.out.println("       <groupId>org.springframework.boot</groupId>");
        System.out.println("       <artifactId>spring-boot-starter-web</artifactId>");
        System.out.println("       <exclusions>");
        System.out.println("           <exclusion>");
        System.out.println("               <groupId>org.springframework.boot</groupId>");
        System.out.println("               <artifactId>spring-boot-starter-tomcat</artifactId>");
        System.out.println("           </exclusion>");
        System.out.println("       </exclusions>");
        System.out.println("   </dependency>");
        System.out.println("   <dependency>");
        System.out.println("       <groupId>org.springframework.boot</groupId>");
        System.out.println("       <artifactId>spring-boot-starter-jetty</artifactId>");
        System.out.println("   </dependency>\n");

        System.out.println("2. 通过配置切换：");
        System.out.println("   spring.main.web-application-type=servlet  # servlet/reactive/none\n");
    }

    /**
     * 2. Servlet 上下文初始化
     */
    private static void demonstrateServletContext() {
        System.out.println("【2. Servlet 上下文初始化】\n");

        System.out.println("ServletContextInitializer 接口：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  作用：在嵌入式容器中注册 Servlet、Filter、Listener          │");
        System.out.println("│                                                             │");
        System.out.println("│  @Bean                                                      │");
        System.out.println("│  public ServletContextInitializer myServletInitializer() {  │");
        System.out.println("│      return servletContext -> {                             │");
        System.out.println("│          // 注册 Servlet                                     │");
        System.out.println("│          ServletRegistration.Dynamic servlet =              │");
        System.out.println("│              servletContext.addServlet(\"myServlet\",        │");
        System.out.println("│                  new MyServlet());                          │");
        System.out.println("│          servlet.addMapping(\"/my/*\");                      │");
        System.out.println("│                                                             │");
        System.out.println("│          // 注册 Filter                                      │");
        System.out.println("│          FilterRegistration.Dynamic filter =                │");
        System.out.println("│              servletContext.addFilter(\"myFilter\",          │");
        System.out.println("│                  new MyFilter());                           │");
        System.out.println("│          filter.addMappingForUrlPatterns(                   │");
        System.out.println("│              EnumSet.of(DispatcherType.REQUEST),            │");
        System.out.println("│              false, \"/*\");                                 │");
        System.out.println("│      };                                                     │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("Spring Boot 注册方式对比：");
        System.out.println("┌─────────────────────────┬─────────────────────────────────────┐");
        System.out.println("│  @WebServlet + @ServletComponentScan │  传统方式，需要扫描   │");
        System.out.println("├─────────────────────────┼─────────────────────────────────────┤");
        System.out.println("│  @Bean ServletRegistrationBean     │  Spring 方式，推荐    │");
        System.out.println("├─────────────────────────┼─────────────────────────────────────┤");
        System.out.println("│  ServletContextInitializer         │  底层方式，最灵活     │");
        System.out.println("└─────────────────────────┴─────────────────────────────────────┘\n");

        System.out.println("DispatcherServlet 自动配置：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  DispatcherServletAutoConfiguration：                       │");
        System.out.println("│  1. 创建 DispatcherServlet（前端控制器）                     │");
        System.out.println("│  2. 设置 load-on-startup = -1（延迟加载）                    │");
        System.out.println("│  3. 注册到 ServletContext，映射路径 \"/\"                     │");
        System.out.println("│                                                             │");
        System.out.println("│  DispatcherServletRegistrationConfiguration：               │");
        System.out.println("│  - 确保 DispatcherServlet 被注册                             │");
        System.out.println("│  - 可以配置 server.servlet.dispatcher-servlet.path          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 3. 响应式编程栈
     */
    private static void demonstrateReactiveStack() {
        System.out.println("【3. 响应式编程栈（WebFlux）】\n");

        System.out.println("Spring Boot 两种 Web 技术栈：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  Servlet Stack（Spring MVC）                                 │");
        System.out.println("│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐  │");
        System.out.println("│  │  Tomcat │ ←→ │ Servlet │ ←→ │ Filter  │ ←→ │ Dispatcher│");
        System.out.println("│  │  Jetty  │    │ Container│   │ Chain   │    │ Servlet   │");
        System.out.println("│  │Undertow│    └─────────┘    └─────────┘    └────┬────┘  │");
        System.out.println("│  └─────────┘                                       │        │");
        System.out.println("│                                                    ↓        │");
        System.out.println("│                                            @Controller      │");
        System.out.println("│                                            （同步阻塞）      │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  Reactive Stack（Spring WebFlux）                            │");
        System.out.println("│  ┌─────────┐    ┌─────────┐    ┌─────────────────────────┐  │");
        System.out.println("│  │ Netty   │ ←→ │ Reactive│ ←→ │    DispatcherHandler    │  │");
        System.out.println("│  │ Tomcat  │    │ Streams │    │  （基于 Reactive Streams）│  │");
        System.out.println("│  │ Jetty   │    └─────────┘    └───────────┬─────────────┘  │");
        System.out.println("│  │Undertow│                               │                 │");
        System.out.println("│  └─────────┘                               ↓                 │");
        System.out.println("│                                    @RestController           │");
        System.out.println("│                                    （异步非阻塞）             │");
        System.out.println("│                                    Mono<T> / Flux<T>         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("Reactive 核心接口：");
        System.out.println("- Publisher：数据发布者");
        System.out.println("- Subscriber：数据订阅者");
        System.out.println("- Subscription：订阅关系，控制数据流速");
        System.out.println("- Processor：既是 Publisher 又是 Subscriber\n");

        System.out.println("Mono vs Flux：");
        System.out.println("┌─────────────────┬─────────────────────────────────────────┐");
        System.out.println("│  Mono<T>        │  0 或 1 个元素的异步序列                │");
        System.out.println("│                 │  类似 Optional，但支持异步              │");
        System.out.println("├─────────────────┼─────────────────────────────────────────┤");
        System.out.println("│  Flux<T>        │  0 到 N 个元素的异步序列                │");
        System.out.println("│                 │  类似 Stream，但支持异步和背压          │");
        System.out.println("└─────────────────┴─────────────────────────────────────────┘\n");
    }

    /**
     * 4. 容器对比
     */
    private static void demonstrateContainerComparison() {
        System.out.println("【4. 嵌入式容器对比】\n");

        System.out.println("Tomcat vs Jetty vs Undertow：");
        System.out.println("┌───────────┬───────────┬───────────┬───────────┐");
        System.out.println("│   特性    │   Tomcat  │   Jetty   │  Undertow │");
        System.out.println("├───────────┼───────────┼───────────┼───────────┤");
        System.out.println("│  成熟度   │    高     │    高     │    中     │");
        System.out.println("│  性能     │    中     │    中     │    高     │");
        System.out.println("│  内存占用 │    高     │    中     │    低     │");
        System.out.println("│  启动速度 │    慢     │    快     │    快     │");
        System.out.println("│  线程模型 │  阻塞IO   │  阻塞IO   │ 非阻塞IO  │");
        System.out.println("│  默认支持 │    是     │    否     │    否     │");
        System.out.println("└───────────┴───────────┴───────────┴───────────┘\n");

        System.out.println("选择建议：");
        System.out.println("- Tomcat：默认选择，成熟稳定，生态丰富");
        System.out.println("- Jetty：嵌入式场景，启动快，内存占用小");
        System.out.println("- Undertow：高性能场景，支持异步IO，内存占用最小\n");

        System.out.println("性能优化配置：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  # Tomcat 配置                                               │");
        System.out.println("│  server.tomcat.threads.max=200              # 最大线程数     │");
        System.out.println("│  server.tomcat.threads.min-spare=10         # 最小空闲线程   │");
        System.out.println("│  server.tomcat.max-connections=10000        # 最大连接数     │");
        System.out.println("│  server.tomcat.accept-count=100             # 等待队列长度   │");
        System.out.println("│                                                             │");
        System.out.println("│  # Undertow 配置                                             │");
        System.out.println("│  server.undertow.threads.worker=64          # 工作线程数     │");
        System.out.println("│  server.undertow.threads.io=4               # IO线程数       │");
        System.out.println("│  server.undertow.direct-buffers=true        # 使用堆外内存   │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("Spring Boot 3.x 重要变化：");
        System.out.println("1. 最低要求 Java 17");
        System.out.println("2. 迁移到 Jakarta EE 9（javax.* → jakarta.*）");
        System.out.println("3. 移除对 Java EE 的支持");
        System.out.println("4. 升级嵌入式容器版本");
        System.out.println("5. 原生镜像支持（GraalVM）\n");
    }
}
