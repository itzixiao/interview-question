package cn.itzixiao.interview.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Spring Bean 生命周期详解
 *
 * Bean 生命周期流程：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 实例化（Instantiation）                                  │
 * │     - 调用构造方法创建 Bean 实例                             │
 * │     - BeanPostProcessor.postProcessBeforeInstantiation      │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. 属性赋值（Population）                                   │
 * │     - 注入依赖（@Autowired、@Value）                         │
 * │     - InstantiationAwareBeanPostProcessor.postProcessProperties│
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. 初始化（Initialization）                                 │
 * │     - Aware 接口回调                                         │
 * │     - @PostConstruct 方法                                    │
 * │     - InitializingBean.afterPropertiesSet()                  │
 * │     - 自定义 init-method                                     │
 * │     - BeanPostProcessor.postProcessAfterInitialization      │
 * │       （AOP 代理在此创建）                                    │
 * ├─────────────────────────────────────────────────────────────┤
 * │  4. 使用（In Use）                                           │
 * │     - Bean 就绪，可以被使用                                  │
 * ├─────────────────────────────────────────────────────────────┤
 * │  5. 销毁（Destruction）                                      │
 * │     - @PreDestroy 方法                                       │
 * │     - DisposableBean.destroy()                               │
 * │     - 自定义 destroy-method                                  │
 * └─────────────────────────────────────────────────────────────┘
 */
public class BeanLifecycleDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring Bean 生命周期详解 ==========\n");

        System.out.println("【创建 Spring 容器】\n");
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(LifecycleConfig.class);

        System.out.println("\n【从容器获取 Bean】\n");
        LifecycleBean bean = context.getBean(LifecycleBean.class);
        bean.doSomething();

        System.out.println("\n【关闭容器】\n");
        context.close();
    }

    @Configuration
    static class LifecycleConfig {
        @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
        public LifecycleBean lifecycleBean() {
            return new LifecycleBean();
        }
    }

    /**
     * 演示完整生命周期的 Bean
     */
    static class LifecycleBean implements
            BeanNameAware,
            BeanFactoryAware,
            ApplicationContextAware,
            InitializingBean,
            DisposableBean {

        @Value("${app.name:MyApp}")
        private String appName;

        private String beanName;

        // ==================== 1. 构造方法（实例化）====================
        public LifecycleBean() {
            System.out.println("  1. 【构造方法】LifecycleBean 实例化");
            System.out.println("     - appName: " + appName + " (还未注入)");
        }

        // ==================== 2. 依赖注入 ====================
        @Autowired
        public void setAppName(@Value("${app.name:MyApp}") String appName) {
            System.out.println("  2. 【Setter 注入】appName = " + appName);
        }

        // ==================== 3. Aware 接口回调 ====================
        @Override
        public void setBeanName(String name) {
            this.beanName = name;
            System.out.println("  3. 【BeanNameAware】Bean 名称: " + name);
        }

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            System.out.println("  4. 【BeanFactoryAware】BeanFactory: " + beanFactory.getClass().getSimpleName());
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            System.out.println("  5. 【ApplicationContextAware】ApplicationContext: " +
                    applicationContext.getClass().getSimpleName());
        }

        // ==================== 4. 初始化方法 ====================
        @PostConstruct
        public void postConstruct() {
            System.out.println("  6. 【@PostConstruct】注解初始化方法");
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            System.out.println("  7. 【InitializingBean】afterPropertiesSet()");
        }

        public void customInit() {
            System.out.println("  8. 【自定义 init-method】customInit()");
        }

        // ==================== 5. 业务方法 ====================
        public void doSomething() {
            System.out.println("  9. 【业务方法】doSomething() - Bean 正在使用中");
        }

        // ==================== 6. 销毁方法 ====================
        @PreDestroy
        public void preDestroy() {
            System.out.println("  10. 【@PreDestroy】注解销毁方法");
        }

        @Override
        public void destroy() throws Exception {
            System.out.println("  11. 【DisposableBean】destroy()");
        }

        public void customDestroy() {
            System.out.println("  12. 【自定义 destroy-method】customDestroy()");
        }
    }
}

/**
 * 生命周期执行顺序总结：
 *
 * 实例化阶段：
 * 1. 构造方法
 *
 * 属性赋值阶段：
 * 2. Setter 注入 / @Autowired 字段注入
 *
 * 初始化阶段：
 * 3. BeanNameAware.setBeanName()
 * 4. BeanFactoryAware.setBeanFactory()
 * 5. ApplicationContextAware.setApplicationContext()
 * 6. @PostConstruct
 * 7. InitializingBean.afterPropertiesSet()
 * 8. 自定义 init-method
 *
 * 使用阶段：
 * 9. 业务方法
 *
 * 销毁阶段：
 * 10. @PreDestroy
 * 11. DisposableBean.destroy()
 * 12. 自定义 destroy-method
 */
