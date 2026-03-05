package cn.itzixiao.interview.spring;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Spring AOP (Aspect Oriented Programming) 面向切面编程详解
 *
 * AOP 核心概念：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Aspect（切面）    - 横切关注点的模块化，包含通知和切点        │
 * │  JoinPoint（连接点）- 程序执行过程中的点，如方法调用           │
 * │  Pointcut（切点）  - 匹配连接点的表达式                        │
 * │  Advice（通知）    - 在切点处执行的增强逻辑                    │
 * │  Target（目标对象）- 被代理的原始对象                           │
 * │  Proxy（代理）     - AOP 框架创建的对象，包含增强逻辑           │
 * │  Weaving（织入）   - 将增强应用到目标对象的过程                 │
 * └─────────────────────────────────────────────────────────────┘
 *
 * AOP 实现方式：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  JDK 动态代理：目标类实现接口时使用                            │
 * │  CGLIB 代理：  目标类没有接口时使用（继承方式）                │
 * │  强制 CGLIB：  @EnableAspectJAutoProxy(proxyTargetClass=true) │
 * └─────────────────────────────────────────────────────────────┘
 */
@EnableAspectJAutoProxy
@Configuration
public class AOPDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring AOP 详解 ==========\n");

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(AOPConfig.class);

        // 获取代理对象
        UserService userService = context.getBean(UserService.class);

        System.out.println("【测试正常方法】");
        userService.addUser("张三");

        System.out.println("\n【测试异常方法】");
        try {
            userService.deleteUser(-1);
        } catch (Exception e) {
            System.out.println("  [Main] 捕获异常: " + e.getMessage());
        }

        System.out.println("\n【测试环绕通知】");
        userService.updateUser("李四");

        context.close();
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AOPConfig {
        @Bean
        public UserService userService() {
            return new UserService();
        }

        @Bean
        public LoggingAspect loggingAspect() {
            return new LoggingAspect();
        }
    }

    // ==================== 目标类 ====================
    static class UserService {
        public void addUser(String name) {
            System.out.println("  [UserService] 添加用户: " + name);
        }

        public void updateUser(String name) {
            System.out.println("  [UserService] 更新用户: " + name);
        }

        public void deleteUser(int id) {
            System.out.println("  [UserService] 删除用户: " + id);
            if (id < 0) {
                throw new IllegalArgumentException("用户ID不能为负数");
            }
        }
    }

    // ==================== 切面类 ====================
    @Aspect
    static class LoggingAspect {

        /**
         * 定义切点：匹配 UserService 的所有方法
         */
        @Pointcut("execution(* cn.itzixiao.interview.spring.AOPDemo.UserService.*(..))")
        public void userServicePointcut() {}

        /**
         * 前置通知：方法执行前执行
         */
        @Before("userServicePointcut()")
        public void beforeAdvice(JoinPoint joinPoint) {
            System.out.println("  [Before] 方法 " + joinPoint.getSignature().getName() + " 即将执行");
        }

        /**
         * 后置通知：方法执行后执行（无论是否异常）
         */
        @After("userServicePointcut()")
        public void afterAdvice(JoinPoint joinPoint) {
            System.out.println("  [After] 方法 " + joinPoint.getSignature().getName() + " 执行完毕");
        }

        /**
         * 返回通知：方法成功返回后执行
         */
        @AfterReturning(pointcut = "userServicePointcut()", returning = "result")
        public void afterReturningAdvice(JoinPoint joinPoint, Object result) {
            System.out.println("  [AfterReturning] 方法 " + joinPoint.getSignature().getName() + " 返回: " + result);
        }

        /**
         * 异常通知：方法抛出异常后执行
         */
        @AfterThrowing(pointcut = "userServicePointcut()", throwing = "ex")
        public void afterThrowingAdvice(JoinPoint joinPoint, Exception ex) {
            System.out.println("  [AfterThrowing] 方法 " + joinPoint.getSignature().getName() +
                    " 抛出异常: " + ex.getMessage());
        }

        /**
         * 环绕通知：包围方法执行，可以控制是否执行目标方法
         */
        @Around("userServicePointcut()")
        public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
            String methodName = joinPoint.getSignature().getName();
            System.out.println("  [Around] 方法 " + methodName + " 环绕通知 - 前");

            long start = System.currentTimeMillis();
            Object result = null;
            try {
                // 执行目标方法
                result = joinPoint.proceed();
                System.out.println("  [Around] 方法 " + methodName + " 执行成功");
            } catch (Exception e) {
                System.out.println("  [Around] 方法 " + methodName + " 执行异常: " + e.getMessage());
                throw e;
            } finally {
                long end = System.currentTimeMillis();
                System.out.println("  [Around] 方法 " + methodName + " 耗时: " + (end - start) + "ms");
                System.out.println("  [Around] 方法 " + methodName + " 环绕通知 - 后");
            }
            return result;
        }
    }
}

/**
 * 通知执行顺序（Spring 4.x / 5.x）：
 *
 * 正常情况：
 * 1. @Around（前半部分）
 * 2. @Before
 * 3. 目标方法
 * 4. @Around（后半部分）
 * 5. @AfterReturning
 * 6. @After
 *
 * 异常情况：
 * 1. @Around（前半部分）
 * 2. @Before
 * 3. 目标方法（抛出异常）
 * 4. @AfterThrowing
 * 5. @After
 * 6. @Around（捕获异常后）
 *
 * 注意：
 * - @After 总是执行（类似 finally）
 * - @Around 可以阻止目标方法执行
 * - @Around 需要手动调用 proceed() 才会执行目标方法
 */
