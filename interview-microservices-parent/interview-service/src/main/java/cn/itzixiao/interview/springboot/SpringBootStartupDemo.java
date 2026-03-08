package cn.itzixiao.interview.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Spring Boot 启动过程详解
 *
 * Spring Boot 启动流程：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 构造 SpringApplication 对象                               │
 * │     - 推断应用类型（Servlet/Reactive/None）                   │
 * │     - 加载 ApplicationContextInitializer                     │
 * │     - 加载 ApplicationListener                               │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. 运行 run() 方法                                           │
 * │     - 准备 Environment（加载配置文件）                        │
 * │     - 创建 ApplicationContext                                 │
 * │     - 准备 Context（执行 Initializer）                        │
 * │     - 刷新 Context（加载 Bean 定义、自动装配）                 │
 * │     - 执行 Runner（ApplicationRunner/CommandLineRunner）      │
 * └─────────────────────────────────────────────────────────────┘
 */
@SpringBootApplication
public class SpringBootStartupDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring Boot 启动过程分析 ==========\n");

        // 方式1：标准启动
        // SpringApplication.run(SpringBootStartupDemo.class, args);

        // 方式2：分步启动（用于分析）
        System.out.println("【步骤1】构造 SpringApplication 对象\n");
        SpringApplication application = new SpringApplication(SpringBootStartupDemo.class);

        System.out.println("推断的应用类型: " + application.getWebApplicationType());
        System.out.println("初始器数量: " + application.getInitializers().size());
        System.out.println("监听器数量: " + application.getListeners().size());
        System.out.println();

        System.out.println("【步骤2】运行 run() 方法\n");
        ConfigurableApplicationContext context = application.run(args);

        System.out.println("\n【启动完成】");
        System.out.println("ApplicationContext 类型: " + context.getClass().getName());
        System.out.println("Bean 定义数量: " + context.getBeanDefinitionCount());

        context.close();
    }
}
