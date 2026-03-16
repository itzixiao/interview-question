package cn.itzixiao.interview.proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;

/**
 * Spring AOP 中的代理选择机制演示
 * <p>
 * Spring选择代理方式的规则：
 * 1. 如果目标类实现了接口，默认使用JDK动态代理
 * 2. 如果目标类没有实现接口，使用CGLIB代理
 * 3. 可以通过配置强制使用CGLIB：proxyTargetClass = true
 */
public class SpringAopProxyDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring AOP 代理机制演示 ==========\n");

        // 1. 有接口时使用JDK代理（Spring默认行为）
        demoWithInterface();

        System.out.println("\n" + repeatString("=", 50) + "\n");

        // 2. 无接口时使用CGLIB代理
        demoWithoutInterface();

        System.out.println("\n" + repeatString("=", 50) + "\n");

        // 3. 强制使用CGLIB代理
        demoForceCglib();
    }

    /**
     * 场景1：目标类实现了接口 - Spring默认使用JDK动态代理
     */
    private static void demoWithInterface() {
        System.out.println("【场景1：目标类实现接口 - Spring默认使用JDK代理】\n");

        // 创建代理工厂
        ProxyFactory proxyFactory = new ProxyFactory(new OrderServiceImpl());
        // 添加通知（拦截器）
        proxyFactory.addAdvice(new LogAdvice());

        // 获取代理对象
        OrderService proxy = (OrderService) proxyFactory.getProxy();

        System.out.println("代理类: " + proxy.getClass().getName());
        System.out.println("是否JDK代理: " + proxy.getClass().getName().contains("Proxy"));
        System.out.println("是否CGLIB代理: " + proxy.getClass().getName().contains("Enhancer"));

        System.out.println("\n执行方法:");
        proxy.createOrder("ORD-001");
    }

    /**
     * 场景2：目标类没有实现接口 - Spring使用CGLIB代理
     */
    private static void demoWithoutInterface() {
        System.out.println("【场景2：目标类无接口 - Spring使用CGLIB代理】\n");

        // 创建代理工厂，目标类没有接口
        ProxyFactory proxyFactory = new ProxyFactory(new InventoryService());
        proxyFactory.addAdvice(new LogAdvice());

        // 获取代理对象
        InventoryService proxy = (InventoryService) proxyFactory.getProxy();

        System.out.println("代理类: " + proxy.getClass().getName());
        System.out.println("是否JDK代理: " + proxy.getClass().getName().contains("Proxy"));
        System.out.println("是否CGLIB代理: " + proxy.getClass().getName().contains("Enhancer"));

        System.out.println("\n执行方法:");
        proxy.checkStock("SKU-001");
    }

    /**
     * 场景3：强制使用CGLIB代理
     */
    private static void demoForceCglib() {
        System.out.println("【场景3：强制使用CGLIB代理 - proxyTargetClass=true】\n");

        // 创建代理工厂，强制使用CGLIB
        ProxyFactory proxyFactory = new ProxyFactory(new OrderServiceImpl());
        proxyFactory.setProxyTargetClass(true);  // 强制使用CGLIB
        proxyFactory.addAdvice(new LogAdvice());

        // 获取代理对象
        OrderService proxy = (OrderService) proxyFactory.getProxy();

        System.out.println("代理类: " + proxy.getClass().getName());
        System.out.println("是否JDK代理: " + proxy.getClass().getName().contains("Proxy"));
        System.out.println("是否CGLIB代理: " + proxy.getClass().getName().contains("Enhancer"));

        System.out.println("\n执行方法:");
        proxy.createOrder("ORD-002");
    }

    // ==================== 业务类定义 ====================

    /**
     * 订单服务接口
     */
    interface OrderService {
        void createOrder(String orderId);

        void cancelOrder(String orderId);
    }

    /**
     * 订单服务实现（有接口）
     */
    static class OrderServiceImpl implements OrderService {
        @Override
        public void createOrder(String orderId) {
            System.out.println("  [业务] 创建订单: " + orderId);
        }

        @Override
        public void cancelOrder(String orderId) {
            System.out.println("  [业务] 取消订单: " + orderId);
        }
    }

    /**
     * 库存服务（无接口）
     */
    static class InventoryService {
        public void checkStock(String sku) {
            System.out.println("  [业务] 检查库存: " + sku);
        }

        public void updateStock(String sku, int quantity) {
            System.out.println("  [业务] 更新库存: " + sku + ", 数量: " + quantity);
        }
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
     * 日志通知（AOP Advice）
     */
    static class LogAdvice implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            String methodName = invocation.getMethod().getName();
            System.out.println("  [AOP] 方法 " + methodName + " 开始执行");

            long start = System.currentTimeMillis();
            Object result = invocation.proceed();  // 执行目标方法
            long end = System.currentTimeMillis();

            System.out.println("  [AOP] 方法 " + methodName + " 执行完成，耗时: " + (end - start) + "ms");
            return result;
        }
    }
}
