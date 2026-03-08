package cn.itzixiao.interview.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring 核心原理总结
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                         Spring 框架核心架构                              │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │   ┌─────────────────────────────────────────────────────────────────┐  │
 * │   │                        核心容器（Core Container）                │  │
 * │   │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │  │
 * │   │  │  Beans   │  │  Core    │  │ Context  │  │  SpEL    │        │  │
 * │   │  │ (Bean管理)│  │(工具类)  │  │(上下文)  │  │(表达式)  │        │  │
 * │   │  └────┬─────┘  └──────────┘  └────┬─────┘  └──────────┘        │  │
 * │   │       │                           │                            │  │
 * │   │       └───────────┬───────────────┘                            │  │
 * │   │                   ↓                                            │  │
 * │   │           ApplicationContext                                   │  │
 * │   │         (BeanFactory 的扩展)                                   │  │
 * │   └─────────────────────────────────────────────────────────────────┘  │
 * │                              │                                          │
 * │                              ↓                                          │
 * │   ┌─────────────────────────────────────────────────────────────────┐  │
 * │   │                        数据访问/集成（Data Access/Integration）  │  │
 * │   │  JDBC / ORM / OXM / JMS / Transactions                          │  │
 * │   └─────────────────────────────────────────────────────────────────┘  │
 * │                              │                                          │
 * │                              ↓                                          │
 * │   ┌─────────────────────────────────────────────────────────────────┐  │
 * │   │                        Web 层                                    │  │
 * │   │  Servlet / WebSocket / WebFlux / Portlet                        │  │
 * │   └─────────────────────────────────────────────────────────────────┘  │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
public class SpringCorePrinciplesDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring 核心原理总结 ==========\n");

        demonstrateIOCPrinciple();
        demonstrateAOPPrinciple();
        demonstrateBeanLifecyclePrinciple();
        demonstrateIntegration();
    }

    /**
     * 1. IOC 原理总结
     */
    private static void demonstrateIOCPrinciple() {
        System.out.println("【1. IOC 控制反转原理】\n");

        System.out.println("IOC 容器核心接口：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  BeanFactory（基础 IOC 容器）                                │");
        System.out.println("│  ├── getBean()          // 获取 Bean                        │");
        System.out.println("│  ├── containsBean()     // 判断是否包含                     │");
        System.out.println("│  └── isSingleton()      // 判断是否单例                     │");
        System.out.println("│                                                             │");
        System.out.println("│  ApplicationContext（高级 IOC 容器）                         │");
        System.out.println("│  ├── 继承 BeanFactory，扩展了：                             │");
        System.out.println("│  ├── 国际化（MessageSource）                                │");
        System.out.println("│  ├── 资源加载（ResourcePatternResolver）                    │");
        System.out.println("│  ├── 事件发布（ApplicationEventPublisher）                  │");
        System.out.println("│  └── 环境配置（EnvironmentCapable）                         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("BeanDefinition - Bean 定义信息：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  - Bean 的类名                                              │");
        System.out.println("│  - Bean 的作用域（singleton/prototype）                     │");
        System.out.println("│  - 构造方法参数                                             │");
        System.out.println("│  - 属性值                                                   │");
        System.out.println("│  - 初始化/销毁方法                                          │");
        System.out.println("│  - 延迟初始化标志                                           │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("BeanFactory 继承体系：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  BeanFactory（根接口）                                       │");
        System.out.println("│       ↑                                                     │");
        System.out.println("│  HierarchicalBeanFactory（支持层级）                         │");
        System.out.println("│       ↑                                                     │");
        System.out.println("│  AutowireCapableBeanFactory（支持自动装配）                  │");
        System.out.println("│       ↑                                                     │");
        System.out.println("│  ConfigurableBeanFactory（支持配置）                         │");
        System.out.println("│       ↑                                                     │");
        System.out.println("│  ConfigurableListableBeanFactory（最常用）                   │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. AOP 原理总结
     */
    private static void demonstrateAOPPrinciple() {
        System.out.println("【2. AOP 面向切面编程原理】\n");

        System.out.println("AOP 代理创建时机：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. Bean 实例化                                              │");
        System.out.println("│  2. 属性赋值                                                 │");
        System.out.println("│  3. 初始化（InitializingBean.afterPropertiesSet）            │");
        System.out.println("│  4. 【BeanPostProcessor.postProcessAfterInitialization】     │");
        System.out.println("│     → 在此创建代理对象                                       │");
        System.out.println("│     → AnnotationAwareAspectJAutoProxyCreator                 │");
        System.out.println("│  5. 返回代理对象给容器                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("AOP 代理创建流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  AbstractAutoProxyCreator.postProcessAfterInitialization()   │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  1. 判断是否需要创建代理（有 Advice 匹配）                   │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  2. 选择代理方式：                                           │");
        System.out.println("│     - 有接口：JDK 动态代理（Proxy.newProxyInstance）         │");
        System.out.println("│     - 无接口：CGLIB 代理（Enhancer.create）                  │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  3. 创建代理对象，包装目标对象                               │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  4. 调用代理对象方法时，执行拦截器链                         │");
        System.out.println("│     - ReflectiveMethodInvocation.proceed()                  │");
        System.out.println("│     - 依次执行：Around → Before → Target → AfterReturning   │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("代理方法调用流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  客户端调用                                                  │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  【代理对象】                                                │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  InvocationHandler.invoke() / MethodInterceptor.intercept() │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  获取方法匹配的 Advice 链                                    │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  ReflectiveMethodInvocation.proceed()                       │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  依次执行 Advice（责任链模式）                               │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  【目标对象方法】                                            │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 3. Bean 生命周期原理总结
     */
    private static void demonstrateBeanLifecyclePrinciple() {
        System.out.println("【3. Bean 生命周期原理】\n");

        System.out.println("BeanPostProcessor 扩展点：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  public interface BeanPostProcessor {                       │");
        System.out.println("│      // 初始化前调用                                         │");
        System.out.println("│      Object postProcessBeforeInitialization(Object bean,    │");
        System.out.println("│                                           String beanName); │");
        System.out.println("│                                                             │");
        System.out.println("│      // 初始化后调用（AOP 代理在此创建）                     │");
        System.out.println("│      Object postProcessAfterInitialization(Object bean,     │");
        System.out.println("│                                          String beanName);  │");
        System.out.println("│  }                                                          │");
        System.out.println("│                                                             │");
        System.out.println("│  常用实现类：                                                │");
        System.out.println("│  - AutowiredAnnotationBeanPostProcessor（处理 @Autowired）   │");
        System.out.println("│  - CommonAnnotationBeanPostProcessor（处理 @PostConstruct）  │");
        System.out.println("│  - AbstractAutoProxyCreator（创建 AOP 代理）                 │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("Spring Bean 创建完整流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 加载 BeanDefinition（从 XML/注解/配置类）                │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  2. BeanFactoryPostProcessor.postProcessBeanFactory()       │");
        System.out.println("│     → 修改 BeanDefinition（如 PropertyPlaceholderConfigurer）│");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  3. InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation│");
        System.out.println("│     → 可以返回代理对象替代正常实例化                         │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  4. 【实例化】调用构造方法 createBeanInstance()              │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  5. InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation│");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  6. 【属性赋值】populateBean()                               │");
        System.out.println("│     → InstantiationAwareBeanPostProcessor.postProcessProperties│");
        System.out.println("│     → @Autowired、@Value 注入                              │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  7. 【初始化】initializeBean()                               │");
        System.out.println("│     a. BeanPostProcessor.postProcessBeforeInitialization    │");
        System.out.println("│     b. @PostConstruct                                       │");
        System.out.println("│     c. InitializingBean.afterPropertiesSet()                │");
        System.out.println("│     d. init-method                                          │");
        System.out.println("│     e. BeanPostProcessor.postProcessAfterInitialization     │");
        System.out.println("│        → AOP 代理创建                                       │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  8. Bean 就绪，放入单例池（singletonObjects）                │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  9. 【销毁】                                                 │");
        System.out.println("│     a. @PreDestroy                                          │");
        System.out.println("│     b. DisposableBean.destroy()                             │");
        System.out.println("│     c. destroy-method                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 4. 综合示例
     */
    private static void demonstrateIntegration() {
        System.out.println("【4. IOC + AOP + Bean 生命周期 综合】\n");

        System.out.println("Spring 启动到请求处理完整流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  启动阶段：                                                  │");
        System.out.println("│  1. 加载配置，解析 BeanDefinition                           │");
        System.out.println("│  2. 执行 BeanFactoryPostProcessor                           │");
        System.out.println("│  3. 注册 BeanPostProcessor                                  │");
        System.out.println("│  4. 实例化所有非延迟单例 Bean                                │");
        System.out.println("│     → 每个 Bean 经历完整生命周期                             │");
        System.out.println("│     → 需要代理的 Bean 创建 AOP 代理                          │");
        System.out.println("│  5. 发布 ContextRefreshedEvent，启动完成                     │");
        System.out.println("│                                                             │");
        System.out.println("│  运行阶段（处理请求）：                                      │");
        System.out.println("│  1. 请求到达 DispatcherServlet                              │");
        System.out.println("│  2. 调用 Controller 方法（代理对象）                         │");
        System.out.println("│  3. AOP 拦截：事务、日志、权限等                             │");
        System.out.println("│  4. 执行业务逻辑，调用 Service、DAO                          │");
        System.out.println("│  5. 返回结果                                                │");
        System.out.println("│                                                             │");
        System.out.println("│  关闭阶段：                                                  │");
        System.out.println("│  1. 发布 ContextClosedEvent                                 │");
        System.out.println("│  2. 销毁所有单例 Bean                                        │");
        System.out.println("│  3. 释放资源                                                │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("设计模式在 Spring 中的应用：");
        System.out.println("┌─────────────────┬─────────────────────────────────────────────┐");
        System.out.println("│  工厂模式       │  BeanFactory、ApplicationContext            │");
        System.out.println("│  单例模式       │  默认 Bean 作用域                            │");
        System.out.println("│  代理模式       │  AOP 动态代理（JDK/CGLIB）                   │");
        System.out.println("│  观察者模式     │  事件监听（ApplicationListener）             │");
        System.out.println("│  模板方法模式   │  JdbcTemplate、各种 Template                 │");
        System.out.println("│  策略模式       │  ResourceLoader、InstantiationStrategy       │");
        System.out.println("│  责任链模式     │  AOP 拦截器链                                │");
        System.out.println("│  装饰器模式     │  BeanWrapper、各种 Decorator                 │");
        System.out.println("└─────────────────┴─────────────────────────────────────────────┘\n");

        System.out.println("Spring 学习建议：");
        System.out.println("1. 先理解 IOC 核心：BeanDefinition、BeanFactory、ApplicationContext");
        System.out.println("2. 掌握 Bean 生命周期：实例化 → 属性赋值 → 初始化 → 销毁");
        System.out.println("3. 理解 AOP 原理：代理创建时机、拦截器链执行");
        System.out.println("4. 熟悉常用扩展点：BeanPostProcessor、Aware 接口");
        System.out.println("5. 阅读源码：从 getBean() 开始，跟踪 Bean 创建流程");
        System.out.println("6. 实践：手写简易 IOC/AOP 容器，加深理解\n");
    }
}
