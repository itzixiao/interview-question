package cn.itzixiao.interview.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Spring 注解生命周期详解
 * <p>
 * 重点讲解：@PostConstruct、@PreDestroy、@Bean(initMethod/destroyMethod) 的执行顺序
 * <p>
 * Bean 生命周期流程（注解版）：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 实例化（Instantiation）                                  │
 * │     - 调用构造方法创建 Bean 实例                             │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. 属性赋值（Population）                                   │
 * │     - 注入依赖（@Autowired、@Value）                         │
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. 初始化（Initialization）                                 │
 * │     - @PostConstruct 方法（JSR-250）                        │
 * │       （优先级最高，最先执行）                                │
 * │     - InitializingBean.afterPropertiesSet()                  │
 * │     - @Bean(initMethod) 或 XML init-method                   │
 * │       （优先级最低，最后执行）                                │
 * │     - AOP 代理在此创建                                        │
 * ├─────────────────────────────────────────────────────────────┤
 * │  4. 使用（In Use）                                           │
 * │     - Bean 就绪，可以被使用                                  │
 * ├─────────────────────────────────────────────────────────────┤
 * │  5. 销毁（Destruction）                                      │
 * │     - @PreDestroy 方法（JSR-250）                           │
 * │       （优先级最高，最先执行）                                │
 * │     - DisposableBean.destroy()                               │
 * │     - @Bean(destroyMethod) 或 XML destroy-method             │
 * │       （优先级最低，最后执行）                                │
 * └─────────────────────────────────────────────────────────────┘
 */
public class AnnotationLifecycleDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring 注解生命周期详解 ==========\n");

        System.out.println("【步骤 1：创建 Spring 容器（AnnotationConfigApplicationContext）】\n");
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        System.out.println("\n【步骤 2：从容器获取 Bean（此时 Bean 已完成初始化）】\n");
        UserService userService = context.getBean(UserService.class);

        System.out.println("\n【步骤 3：执行业务方法（Bean 正在使用中）】\n");
        userService.process();

        System.out.println("\n【步骤 4：关闭容器（触发销毁方法）】\n");
        ((AnnotationConfigApplicationContext) context).close();
    }
}

/**
 * 配置类
 * 使用 @Configuration + @Bean 定义 Bean
 */
@Configuration
class AppConfig {

    /**
     * 使用 @Bean 注解定义 Bean
     * initMethod = "init" - 指定初始化方法
     * destroyMethod = "cleanup" - 指定销毁方法
     * <p>
     * 注意：@Bean 方法的名称默认为 beanName（这里是 "lifecycleUserService"）
     */
    @Bean(name = "lifecycleUserService", initMethod = "init", destroyMethod = "cleanup")
    public UserService lifecycleUserService() {
        return new UserService();
    }
}

/**
 * 用户服务类 - 演示完整的注解生命周期
 * <p>
 * 可以使用 @Component 注解让 Spring 自动扫描
 * 也可以像上面一样用 @Bean 手动定义
 */
@Component
class UserService {

    private String serviceName;

    // ==================== 1. 构造方法（实例化）====================
    public UserService() {
        System.out.println("✓ [构造方法] UserService 实例化");
        System.out.println("  - 这是 Bean 生命周期的起点\n");
    }

    // ==================== 2. Setter 注入（属性赋值）====================

    /**
     * Spring 容器会在属性注入时调用这个方法
     * 如果是字段注入（@Autowired 在字段上），则没有这个方法
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
        System.out.println("✓ [Setter 注入] serviceName = " + serviceName);
        System.out.println("  - 依赖注入完成\n");
    }

    // ==================== 3. @PostConstruct 初始化方法====================

    /**
     * JSR-250 规范定义的注解
     * 优先级：最高（第一个执行的初始化方法）
     * <p>
     * 特点：
     * 1. 由 JDK 提供，不是 Spring 的注解
     * 2. 只能有一个方法标注此注解
     * 3. 不能有参数，返回值会被忽略
     * 4. 不能抛出受检异常
     * 5. 方法必须是非静态的
     */
    @PostConstruct
    public void postConstruct() {
        System.out.println("✓ [@PostConstruct] 注解初始化方法");
        System.out.println("  - 优先级最高，最先执行");
        System.out.println("  - 通常用于：资源初始化、配置校验\n");
    }

    // ==================== 4. InitializingBean 接口方法====================
    /**
     * Spring 框架提供的接口
     * 优先级：中等（第二个执行）
     *
     * 注意：这里为了演示，故意不实现这个接口
     * 实际开发中可以实现 InitializingBean 来添加初始化逻辑
     */

    // ==================== 5. @Bean 指定的初始化方法====================

    /**
     * 通过 @Bean(initMethod = "init") 指定的自定义初始化方法
     * 优先级：最低（最后一个执行的初始化方法）
     * <p>
     * 特点：
     * 1. 方法名可以自定义
     * 2. 方法必须是 public 的
     * 3. 不能有参数
     * 4. 可以抛出异常
     */
    public void init() {
        System.out.println("✓ [@Bean(initMethod)] 自定义初始化方法 init()");
        System.out.println("  - 优先级最低，最后执行");
        System.out.println("  - 通常用于：复杂的业务初始化\n");
    }

    // ==================== 6. 业务方法（使用阶段）====================

    /**
     * Bean 已经完成所有初始化，处于就绪状态
     * 可以被正常调用执行业务逻辑
     */
    public void process() {
        System.out.println("✓ [业务方法] process() - Bean 正在使用中");
        System.out.println("  - 所有初始化方法都已执行完毕");
        System.out.println("  - Bean 已完全准备好\n");
    }

    // ==================== 7. @PreDestroy 销毁方法====================

    /**
     * JSR-250 规范定义的注解
     * 优先级：最高（第一个执行的销毁方法）
     * <p>
     * 特点：
     * 1. 由 JDK 提供，不是 Spring 的注解
     * 2. 只能有一个方法标注此注解
     * 3. 不能有参数，返回值会被忽略
     * 4. 不能抛出受检异常
     * 5. 方法必须是非静态的
     * <p>
     * 注意：只有容器正常关闭时才会调用
     * System.exit() 或直接杀死进程不会调用
     */
    @PreDestroy
    public void preDestroy() {
        System.out.println("✓ [@PreDestroy] 注解销毁方法");
        System.out.println("  - 优先级最高，最先执行");
        System.out.println("  - 通常用于：释放资源、保存状态\n");
    }

    // ==================== 8. DisposableBean 接口方法====================
    /**
     * Spring 框架提供的接口
     * 优先级：中等（第二个执行）
     *
     * 注意：这里为了演示，故意不实现这个接口
     */

    // ==================== 9. @Bean 指定的销毁方法====================

    /**
     * 通过 @Bean(destroyMethod = "cleanup") 指定的自定义销毁方法
     * 优先级：最低（最后一个执行的销毁方法）
     * <p>
     * 特点：
     * 1. 方法名可以自定义
     * 2. 方法必须是 public 的
     * 3. 不能有参数
     * 4. 可以抛出异常
     */
    public void cleanup() {
        System.out.println("✓ [@Bean(destroyMethod)] 自定义销毁方法 cleanup()");
        System.out.println("  - 优先级最低，最后执行");
        System.out.println("  - 通常用于：清理连接、关闭线程池\n");
    }
}

/**
 * 执行顺序总结（重要！面试高频考点）
 * <p>
 * 【初始化阶段】（3 个方法的执行顺序）
 * ┌──────────────────────────────────────────┐
 * │ 第 1 个：@PostConstruct                    │
 * │      （JSR-250，JDK 标准，优先级最高）     │
 * ├──────────────────────────────────────────┤
 * │ 第 2 个：InitializingBean.afterPropertiesSet() │
 * │      （Spring 接口，优先级中等）            │
 * ├──────────────────────────────────────────┤
 * │ 第 3 个：@Bean(initMethod) 或 XML init-method │
 * │      （自定义方法，优先级最低）             │
 * └──────────────────────────────────────────┘
 * <p>
 * 【销毁阶段】（3 个方法的执行顺序）
 * ┌──────────────────────────────────────────┐
 * │ 第 1 个：@PreDestroy                       │
 * │      （JSR-250，JDK 标准，优先级最高）     │
 * ├──────────────────────────────────────────┤
 * │ 第 2 个：DisposableBean.destroy()          │
 * │      （Spring 接口，优先级中等）            │
 * ├──────────────────────────────────────────┤
 * │ 第 3 个：@Bean(destroyMethod) 或 XML destroy-method │
 * │      （自定义方法，优先级最低）             │
 * └──────────────────────────────────────────┘
 * <p>
 * 记忆口诀：
 * 先 JDK 后 Spring 最后自定义
 * 初始化：Post → Bean → init
 * 销毁：Pre → Bean → cleanup
 */
