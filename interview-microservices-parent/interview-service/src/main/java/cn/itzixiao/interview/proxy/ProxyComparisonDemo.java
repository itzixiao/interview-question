package cn.itzixiao.interview.proxy;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JDK动态代理 vs CGLIB 对比示例
 * <p>
 * 核心区别：
 * ┌─────────────┬────────────────────────┬────────────────────────┐
 * │   特性       │     JDK动态代理         │       CGLIB            │
 * ├─────────────┼────────────────────────┼────────────────────────┤
 * │  实现方式    │ 实现InvocationHandler   │ 继承目标类，生成子类     │
 * ├─────────────┼────────────────────────┼────────────────────────┤
 * │  目标要求    │ 必须实现接口            │ 不需要接口，不能是final  │
 * ├─────────────┼────────────────────────┼────────────────────────┤
 * │  生成类位置  │ 在JDK内部生成           │ 在运行时生成字节码       │
 * ├─────────────┼────────────────────────┼────────────────────────┤
 * │  性能        │ 反射调用，稍慢          │ 使用FastClass，更快      │
 * ├─────────────┼────────────────────────┼────────────────────────┤
 * │  依赖        │ JDK内置                 │ 需要引入cglib库          │
 * └─────────────┴────────────────────────┴────────────────────────┘
 */
public class ProxyComparisonDemo {

    public static void main(String[] args) {
        System.out.println("========== JDK动态代理 vs CGLIB 对比 ==========\n");

        // 1. JDK动态代理演示
        jdkProxyDemo();

        System.out.println("\n" + repeatString("=", 50) + "\n");

        // 2. CGLIB代理演示
        cglibProxyDemo();

        System.out.println("\n" + repeatString("=", 50) + "\n");

        // 3. 核心区别总结
        showDifferences();
    }

    /**
     * 1. JDK动态代理演示
     * 原理：基于接口，生成一个实现相同接口的代理类
     */
    private static void jdkProxyDemo() {
        System.out.println("【1. JDK动态代理演示】");
        System.out.println("原理：为目标接口生成实现类，通过反射调用目标方法\n");

        // 创建目标对象
        UserService target = new UserServiceImpl();

        // 创建代理对象
        UserService proxy = (UserService) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),  // 类加载器
                target.getClass().getInterfaces(),   // 目标类实现的接口
                new JdkInvocationHandler(target)      // 调用处理器
        );

        System.out.println("代理类类型: " + proxy.getClass().getName());
        System.out.println("代理类父类: " + proxy.getClass().getSuperclass().getName());
        System.out.println("实现的接口: ");
        for (Class<?> iface : proxy.getClass().getInterfaces()) {
            System.out.println("  - " + iface.getName());
        }

        System.out.println("\n调用代理方法:");
        proxy.addUser("张三");
        proxy.deleteUser(1);
    }

    /**
     * 2. CGLIB代理演示
     * 原理：继承目标类，生成子类作为代理
     */
    private static void cglibProxyDemo() {
        System.out.println("【2. CGLIB代理演示】");
        System.out.println("原理：为目标类生成子类，通过方法重写拦截调用\n");

        // 创建Enhancer
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(ProductService.class);  // 设置父类（目标类）
        enhancer.setCallback(new CglibMethodInterceptor());  // 设置方法拦截器

        // 创建代理对象
        ProductService proxy = (ProductService) enhancer.create();

        System.out.println("代理类类型: " + proxy.getClass().getName());
        System.out.println("代理类父类: " + proxy.getClass().getSuperclass().getName());

        System.out.println("\n调用代理方法:");
        proxy.addProduct("iPhone");
        proxy.deleteProduct(100);
    }

    /**
     * 重复字符串（JDK8兼容）
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 3. 核心区别总结
     */
    private static void showDifferences() {
        System.out.println("【3. 核心区别详解】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                    实现机制差异                              │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│                                                             │");
        System.out.println("│  JDK动态代理：                                               │");
        System.out.println("│  ┌─────────────┐         ┌─────────────┐                   │");
        System.out.println("│  │   目标类     │ ──────> │  实现接口    │                   │");
        System.out.println("│  │ UserServiceImpl│      │ UserService │                   │");
        System.out.println("│  └─────────────┘         └─────────────┘                   │");
        System.out.println("│         ↑                                                   │");
        System.out.println("│         │ implements                                        │");
        System.out.println("│  ┌─────────────┐         ┌─────────────┐                   │");
        System.out.println("│  │   代理类     │ ──────> │  实现接口    │                   │");
        System.out.println("│  │ Proxy$XXX   │         │ UserService │                   │");
        System.out.println("│  └─────────────┘         └─────────────┘                   │");
        System.out.println("│                                                             │");
        System.out.println("│  代理类继承 Proxy，实现目标接口，通过反射调用目标方法          │");
        System.out.println("│                                                             │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│                                                             │");
        System.out.println("│  CGLIB代理：                                                 │");
        System.out.println("│  ┌─────────────┐                                            │");
        System.out.println("│  │   目标类     │                                            │");
        System.out.println("│  │ProductService│                                            │");
        System.out.println("│  └─────────────┘                                            │");
        System.out.println("│         ↑                                                   │");
        System.out.println("│         │ extends                                           │");
        System.out.println("│  ┌─────────────┐                                            │");
        System.out.println("│  │   代理类     │  生成子类，重写方法，通过FastClass调用        │");
        System.out.println("│  │ Enhancer$XXX │                                            │");
        System.out.println("│  └─────────────┘                                            │");
        System.out.println("│                                                             │");
        System.out.println("│  代理类继承目标类，重写方法，使用MethodProxy快速调用           │");
        System.out.println("│                                                             │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                    使用场景区别                              │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│                                                             │");
        System.out.println("│  使用JDK动态代理：                                            │");
        System.out.println("│  - 目标类实现了接口                                           │");
        System.out.println("│  - 希望保持松耦合，基于接口编程                                 │");
        System.out.println("│  - Spring默认方式（当类实现接口时）                             │");
        System.out.println("│                                                             │");
        System.out.println("│  使用CGLIB代理：                                              │");
        System.out.println("│  - 目标类没有实现接口                                          │");
        System.out.println("│  - 需要代理类中的非接口方法                                     │");
        System.out.println("│  - 对性能要求较高（CGLIB更快）                                  │");
        System.out.println("│  - Spring中设置 @EnableAspectJAutoProxy(proxyTargetClass=true) │");
        System.out.println("│                                                             │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                    性能差异原因                                │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│                                                             │");
        System.out.println("│  JDK动态代理慢的原因：                                         │");
        System.out.println("│  1. 使用反射调用 Method.invoke()                               │");
        System.out.println("│  2. 每次调用都要检查访问权限                                    │");
        System.out.println("│  3. 无法内联优化                                               │");
        System.out.println("│                                                             │");
        System.out.println("│  CGLIB快的原因：                                              │");
        System.out.println("│  1. 使用 MethodProxy.invokeSuper()                             │");
        System.out.println("│  2. 通过FastClass索引直接调用，避免反射                         │");
        System.out.println("│  3. 生成的字节码更接近直接调用                                  │");
        System.out.println("│                                                             │");
        System.out.println("│  FastClass机制：                                             │");
        System.out.println("│  - 为每个方法生成一个索引号                                     │");
        System.out.println("│  - 调用时通过索引号直接定位方法                                 │");
        System.out.println("│  - switch-case 或数组访问，O(1)复杂度                          │");
        System.out.println("│                                                             │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    // ==================== JDK动态代理相关类 ====================

    /**
     * 目标接口（JDK动态代理必须有接口）
     */
    interface UserService {
        void addUser(String name);

        void deleteUser(int id);
    }

    /**
     * 目标实现类
     */
    static class UserServiceImpl implements UserService {
        @Override
        public void addUser(String name) {
            System.out.println("  [目标方法] 添加用户: " + name);
        }

        @Override
        public void deleteUser(int id) {
            System.out.println("  [目标方法] 删除用户: " + id);
        }
    }

    /**
     * JDK调用处理器
     */
    static class JdkInvocationHandler implements InvocationHandler {
        private final Object target;

        public JdkInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("  [JDK代理] 方法调用前增强: " + method.getName());

            // 通过反射调用目标方法
            Object result = method.invoke(target, args);

            System.out.println("  [JDK代理] 方法调用后增强: " + method.getName());
            return result;
        }
    }

    // ==================== CGLIB代理相关类 ====================

    /**
     * 目标类（CGLIB不需要接口）
     */
    static class ProductService {
        public void addProduct(String name) {
            System.out.println("  [目标方法] 添加产品: " + name);
        }

        public void deleteProduct(int id) {
            System.out.println("  [目标方法] 删除产品: " + id);
        }

        // final方法不能被CGLIB代理
        public final void finalMethod() {
            System.out.println("  [目标方法] final方法");
        }
    }

    /**
     * CGLIB方法拦截器
     */
    static class CglibMethodInterceptor implements MethodInterceptor {
        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            System.out.println("  [CGLIB代理] 方法调用前增强: " + method.getName());

            // 使用MethodProxy调用父类（目标类）的方法
            // 比反射更快，使用FastClass机制
            Object result = proxy.invokeSuper(obj, args);

            System.out.println("  [CGLIB代理] 方法调用后增强: " + method.getName());
            return result;
        }
    }
}
