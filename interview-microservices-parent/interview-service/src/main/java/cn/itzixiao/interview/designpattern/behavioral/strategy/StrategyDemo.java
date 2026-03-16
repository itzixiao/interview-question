package cn.itzixiao.interview.designpattern.behavioral.strategy;

/**
 * =====================================================================================
 * 策略模式（Strategy Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 定义一系列算法，把它们一个个封装起来，并且使它们可相互替换。
 * 策略模式让算法独立于使用它的客户端而变化。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 策略接口（Strategy）：定义算法接口
 * 2. 具体策略（Concrete Strategy）：实现具体的算法
 * 3. 上下文（Context）：持有策略引用，调用策略方法
 * <p>
 * 三、策略模式结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Client                                                              │
 * │     │                                                                │
 * │     ↓                                                                │
 * │  Context ──────────→ Strategy（接口）                                │
 * │  - strategy              ↑                                           │
 * │  - executeStrategy()     │                                           │
 * │                          ├── ConcreteStrategyA                       │
 * │                          ├── ConcreteStrategyB                       │
 * │                          └── ConcreteStrategyC                       │
 * └─────────────────────────────────────────────────────────────────────┘
 * <p>
 * 四、策略模式 vs 状态模式
 * -------------------------------------------------------------------------------------
 * | 特性       | 策略模式                   | 状态模式                 |
 * |------------|----------------------------|--------------------------|
 * | 目的       | 算法切换                   | 状态切换                 |
 * | 切换者     | 客户端决定使用哪个策略     | 状态对象内部决定         |
 * | 关注点     | 算法封装                   | 状态转换                 |
 * <p>
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - 支付方式：支付宝、微信、银行卡
 * - 排序算法：冒泡、快排、归并
 * - 促销策略：满减、折扣、返现
 * - 出行策略：公交、地铁、打车、骑行
 */
public class StrategyDemo {

    public static void main(String[] args) {
        System.out.println("========== 策略模式（Strategy Pattern）==========\n");

        System.out.println("【场景：支付方式选择】\n");

        // 创建支付上下文
        PaymentContext context = new PaymentContext();

        // 使用支付宝支付
        System.out.println("1. 使用支付宝支付：");
        context.setPaymentStrategy(new AlipayStrategy());
        context.pay(100.0);

        // 使用微信支付
        System.out.println("\n2. 使用微信支付：");
        context.setPaymentStrategy(new WeChatPayStrategy());
        context.pay(200.0);

        // 使用银行卡支付
        System.out.println("\n3. 使用银行卡支付：");
        context.setPaymentStrategy(new BankCardStrategy());
        context.pay(300.0);

        System.out.println("\n【模式分析】：");
        System.out.println("  - 将算法封装成独立的类，可相互替换");
        System.out.println("  - 客户端决定使用哪个策略，策略可动态切换");
        System.out.println("  - 消除大量的 if-else 或 switch 语句");
        System.out.println("  - 符合开闭原则：新增策略无需修改现有代码");
        System.out.println("  - 符合单一职责原则：每个策略只负责一种算法");
    }
}

/**
 * =====================================================================================
 * 策略接口
 * =====================================================================================
 */
interface PaymentStrategy {
    /**
     * 支付
     *
     * @param amount 支付金额
     */
    void pay(double amount);
}

/**
 * =====================================================================================
 * 具体策略：支付宝支付
 * =====================================================================================
 */
class AlipayStrategy implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("    [支付宝支付] 支付金额: ¥" + amount);
        System.out.println("    调用支付宝 API 完成支付...");
        System.out.println("    支付成功！");
    }
}

/**
 * =====================================================================================
 * 具体策略：微信支付
 * =====================================================================================
 */
class WeChatPayStrategy implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("    [微信支付] 支付金额: ¥" + amount);
        System.out.println("    调用微信支付 API 完成支付...");
        System.out.println("    支付成功！");
    }
}

/**
 * =====================================================================================
 * 具体策略：银行卡支付
 * =====================================================================================
 */
class BankCardStrategy implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("    [银行卡支付] 支付金额: ¥" + amount);
        System.out.println("    调用银行网关 API 完成支付...");
        System.out.println("    支付成功！");
    }
}

/**
 * =====================================================================================
 * 上下文（Context）
 * =====================================================================================
 * 持有策略引用，调用策略方法
 */
class PaymentContext {
    private PaymentStrategy paymentStrategy;

    /**
     * 设置支付策略
     */
    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    /**
     * 执行支付
     */
    public void pay(double amount) {
        if (paymentStrategy == null) {
            throw new IllegalStateException("未设置支付策略");
        }
        paymentStrategy.pay(amount);
    }
}
