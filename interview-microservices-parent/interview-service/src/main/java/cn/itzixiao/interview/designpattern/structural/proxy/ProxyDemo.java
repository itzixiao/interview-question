package cn.itzixiao.interview.designpattern.structural.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * =====================================================================================
 * 代理模式（Proxy Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 为其他对象提供一种代理以控制对这个对象的访问。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 主题接口（Subject）：定义真实对象和代理对象的共同接口
 * 2. 真实对象（Real Subject）：被代理的实际对象
 * 3. 代理对象（Proxy）：持有真实对象引用，控制访问
 * <p>
 * 三、代理模式类型
 * -------------------------------------------------------------------------------------
 * 1. 远程代理（Remote Proxy）：为远程对象提供代理
 * - RMI、Dubbo、Feign
 * <p>
 * 2. 虚拟代理（Virtual Proxy）：延迟创建开销大的对象
 * - 图片懒加载、单例延迟初始化
 * <p>
 * 3. 保护代理（Protection Proxy）：控制访问权限
 * - 权限校验
 * <p>
 * 4. 智能引用代理（Smart Reference）：添加额外功能
 * - 日志记录、性能监控、缓存
 * <p>
 * 四、代理模式 vs 装饰器模式
 * -------------------------------------------------------------------------------------
 * | 特性       | 代理模式                   | 装饰器模式             |
 * |------------|----------------------------|------------------------|
 * | 目的       | 控制访问                   | 增强功能               |
 * | 对象创建   | 代理创建真实对象           | 客户端传入被装饰对象   |
 * | 关系       | 代理代表真实对象           | 装饰器包装组件         |
 * | 使用场景   | 权限控制、延迟加载、远程调用 | 动态添加功能         |
 * <p>
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - Spring AOP：JDK 动态代理、CGLIB
 * - MyBatis Mapper：接口代理
 * - RPC 框架：远程服务代理
 * - 图片懒加载
 */
public class ProxyDemo {

    public static void main(String[] args) {
        System.out.println("========== 代理模式（Proxy Pattern）==========\n");

        // 1. 静态代理
        System.out.println("【1. 静态代理】：\n");
        System.out.println("普通用户访问：");
        UserService staticProxy1 = new UserServiceProxy(new UserServiceImpl());
        staticProxy1.deleteUser("user1");

        System.out.println("\n管理员访问：");
        UserService staticProxy2 = new UserServiceProxy(new UserServiceImpl(), "admin");
        staticProxy2.deleteUser("user1");

        // 2. JDK 动态代理
        System.out.println("\n【2. JDK 动态代理】：\n");
        UserService realService = new UserServiceImpl();
        UserService dynamicProxy = (UserService) Proxy.newProxyInstance(
                realService.getClass().getClassLoader(),
                realService.getClass().getInterfaces(),
                new LoggingInvocationHandler(realService)
        );
        dynamicProxy.deleteUser("user2");

        // 3. CGLIB 代理（使用 JDK 动态代理模拟）
        System.out.println("\n【3. 代理模式应用场景】：");
        System.out.println("  - Spring AOP：JDK 动态代理（接口）/ CGLIB（类）");
        System.out.println("  - MyBatis Mapper：接口代理实现 SQL 执行");
        System.out.println("  - RPC 框架：远程服务本地代理");
        System.out.println("  - 图片懒加载：延迟加载大图");
        System.out.println("  - 权限控制：保护代理");

        System.out.println("\n【模式分析】：");
        System.out.println("  - 控制对真实对象的访问");
        System.out.println("  - 静态代理：代理类在编译时确定");
        System.out.println("  - 动态代理：代理类在运行时生成");
        System.out.println("  - JDK 动态代理：基于接口，使用反射");
        System.out.println("  - CGLIB 代理：基于继承，使用字节码增强");
    }
}

/**
 * =====================================================================================
 * 主题接口
 * =====================================================================================
 */
interface UserService {
    void addUser(String name);

    void deleteUser(String name);
}

/**
 * =====================================================================================
 * 真实对象
 * =====================================================================================
 */
class UserServiceImpl implements UserService {
    @Override
    public void addUser(String name) {
        System.out.println("    [真实对象] 添加用户: " + name);
    }

    @Override
    public void deleteUser(String name) {
        System.out.println("    [真实对象] 删除用户: " + name);
    }
}

/**
 * =====================================================================================
 * 静态代理
 * =====================================================================================
 * 保护代理：控制访问权限
 */
class UserServiceProxy implements UserService {
    private UserService realService;
    private String currentUser;

    public UserServiceProxy(UserService realService) {
        this(realService, "guest");
    }

    public UserServiceProxy(UserService realService, String currentUser) {
        this.realService = realService;
        this.currentUser = currentUser;
    }

    @Override
    public void addUser(String name) {
        System.out.println("    [静态代理] 记录日志: 添加用户 " + name);
        realService.addUser(name);
    }

    @Override
    public void deleteUser(String name) {
        // 保护代理：权限控制
        if (!"admin".equals(currentUser)) {
            System.out.println("    [静态代理] 权限不足，" + currentUser + " 无法删除用户");
            return;
        }
        System.out.println("    [静态代理] 记录日志: 删除用户 " + name);
        realService.deleteUser(name);
    }
}

/**
 * =====================================================================================
 * JDK 动态代理：InvocationHandler
 * =====================================================================================
 * 智能引用代理：添加日志功能
 */
class LoggingInvocationHandler implements InvocationHandler {
    private Object target;

    public LoggingInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 前置通知
        System.out.println("    [动态代理] 方法调用前: " + method.getName());

        // 调用真实对象方法
        long start = System.currentTimeMillis();
        Object result = method.invoke(target, args);
        long end = System.currentTimeMillis();

        // 后置通知
        System.out.println("    [动态代理] 方法调用后，耗时: " + (end - start) + "ms");

        return result;
    }
}
