package cn.itzixiao.interview.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Spring IOC (Inversion of Control) 控制反转详解
 *
 * IOC 核心概念：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  控制反转：将对象的创建、依赖注入、生命周期管理交给 Spring 容器 │
 * │  传统方式：对象自己创建依赖（new）                            │
 * │  IOC方式：容器创建对象，注入依赖                              │
 * ├─────────────────────────────────────────────────────────────┤
 * │  依赖注入（DI）方式：                                         │
 * │  1. 构造器注入（推荐）- 依赖明确，不可变                      │
 * │  2. Setter 注入 - 可选依赖                                    │
 * │  3. 字段注入 - 简洁但不推荐（测试困难，隐藏依赖）              │
 * └─────────────────────────────────────────────────────────────┘
 */
public class IOCDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring IOC 控制反转详解 ==========\n");

        // 1. 创建 Spring 容器
        System.out.println("【1. 创建 Spring 容器】\n");
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(AppConfig.class);

        System.out.println("容器创建完成，Bean 定义数量: " + context.getBeanDefinitionCount());
        System.out.println();

        // 2. 获取 Bean
        System.out.println("【2. 从容器获取 Bean】\n");
        UserService userService = context.getBean(UserService.class);
        userService.sayHello();
        System.out.println();

        // 3. 演示依赖注入
        System.out.println("【3. 依赖注入效果】\n");
        OrderService orderService = context.getBean(OrderService.class);
        orderService.createOrder();
        System.out.println();

        // 4. 单例验证
        System.out.println("【4. 单例模式验证】\n");
        UserService userService2 = context.getBean(UserService.class);
        System.out.println("userService == userService2: " + (userService == userService2));
        System.out.println();

        // 5. 关闭容器
        context.close();
    }

    /**
     * 配置类
     */
    @Configuration
    @ComponentScan("cn.itzixiao.interview.spring")
    static class AppConfig {
    }

    /**
     * 数据访问层
     */
    @Component
    static class UserDao {
        public void save() {
            System.out.println("  [UserDao] 保存用户到数据库");
        }
    }

    /**
     * 业务层 - 构造器注入
     */
    @Service
    static class UserService {
        private final UserDao userDao;

        // 构造器注入（Spring 4.3+ 可以省略 @Autowired）
        @Autowired
        public UserService(UserDao userDao) {
            this.userDao = userDao;
            System.out.println("  [UserService] 构造器被调用，注入 UserDao");
        }

        public void sayHello() {
            System.out.println("  [UserService] Hello!");
            userDao.save();
        }
    }

    /**
     * 订单服务 - Setter 注入
     */
    @Service
    static class OrderService {
        private UserDao userDao;

        // Setter 注入
        @Autowired
        public void setUserDao(UserDao userDao) {
            this.userDao = userDao;
            System.out.println("  [OrderService] Setter 被调用，注入 UserDao");
        }

        public void createOrder() {
            System.out.println("  [OrderService] 创建订单");
            if (userDao != null) {
                System.out.println("  [OrderService] 使用 UserDao 保存订单信息");
            }
        }
    }
}
