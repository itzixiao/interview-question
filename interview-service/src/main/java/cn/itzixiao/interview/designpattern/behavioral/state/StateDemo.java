package cn.itzixiao.interview.designpattern.behavioral.state;

/**
 * =====================================================================================
 * 状态模式（State Pattern）
 * =====================================================================================
 * 
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 允许一个对象在其内部状态改变时改变它的行为。对象看起来似乎修改了它的类。
 * 
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 状态接口（State）：定义状态行为接口
 * 2. 具体状态（Concrete State）：实现具体状态行为
 * 3. 上下文（Context）：维护当前状态，委托状态行为
 * 
 * 三、状态模式结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Context（上下文）                                                   │
 * │  ├── state: State            // 当前状态                            │
 * │  ├── setState(State)         // 设置状态                            │
 * │  └── request()               // 委托给状态处理                      │
 * │              │                                                       │
 * │              ↓                                                       │
 * │  State（状态接口）                                                   │
 * │  └── handle()                // 状态行为                            │
 * │              ↑                                                       │
 * │  ConcreteState（具体状态）                                           │
 * │  ├── ConcreteStateA                                                 │
 * │  ├── ConcreteStateB                                                 │
 * │  └── ConcreteStateC                                                 │
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * 四、状态模式 vs 策略模式
 * -------------------------------------------------------------------------------------
 * | 特性       | 状态模式                   | 策略模式                 |
 * |------------|----------------------------|--------------------------|
 * | 目的       | 状态转换                   | 算法切换                 |
 * | 切换者     | 状态对象内部决定           | 客户端决定               |
 * | 状态数量   | 状态有限且预定             | 策略可以无限扩展         |
 * | 关系       | 状态之间可能相互转换       | 策略之间相互独立         |
 * 
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - 订单状态：待支付 → 已支付 → 已发货 → 已完成
 * - TCP连接：CLOSED → LISTEN → SYN_SENT → ESTABLISHED
 * - 工作流：审批流程状态流转
 * - 电梯：停止 → 上行 → 停止 → 开门
 */
public class StateDemo {

    public static void main(String[] args) {
        System.out.println("========== 状态模式（State Pattern）==========\n");

        System.out.println("【场景：订单状态流转】\n");

        // 创建订单
        Order order = new Order("ORD001");

        // 尝试各种操作
        System.out.println("【初始状态：" + order.getStateName() + "】\n");

        System.out.println("1. 尝试发货（待支付状态）：");
        order.ship();

        System.out.println("\n2. 支付订单：");
        order.pay();
        System.out.println("    当前状态: " + order.getStateName());

        System.out.println("\n3. 尝试取消（已支付状态）：");
        order.cancel();

        System.out.println("\n4. 发货：");
        order.ship();
        System.out.println("    当前状态: " + order.getStateName());

        System.out.println("\n5. 确认收货：");
        order.confirm();
        System.out.println("    当前状态: " + order.getStateName());

        System.out.println("\n6. 订单完成，尝试其他操作：");
        order.pay();
        order.ship();

        System.out.println("\n【模式分析】：");
        System.out.println("  - 状态变化导致行为变化");
        System.out.println("  - 状态转换逻辑分散在各个状态类中");
        System.out.println("  - 消除大量的 if-else 或 switch 语句");
        System.out.println("  - 符合开闭原则：新增状态无需修改现有代码");
        System.out.println("  - 符合单一职责原则：每个状态类只负责一种状态");
    }
}

/**
 * =====================================================================================
 * 状态接口
 * =====================================================================================
 */
interface OrderState {
    String getName();
    void pay(Order order);
    void cancel(Order order);
    void ship(Order order);
    void confirm(Order order);
}

/**
 * =====================================================================================
 * 具体状态：待支付
 * =====================================================================================
 */
class PendingPaymentState implements OrderState {
    @Override
    public String getName() {
        return "待支付";
    }

    @Override
    public void pay(Order order) {
        System.out.println("    支付成功！");
        order.setState(new PaidState());
    }

    @Override
    public void cancel(Order order) {
        System.out.println("    订单已取消");
        order.setState(new CancelledState());
    }

    @Override
    public void ship(Order order) {
        System.out.println("    操作失败：待支付状态无法发货");
    }

    @Override
    public void confirm(Order order) {
        System.out.println("    操作失败：待支付状态无法确认收货");
    }
}

/**
 * =====================================================================================
 * 具体状态：已支付
 * =====================================================================================
 */
class PaidState implements OrderState {
    @Override
    public String getName() {
        return "已支付";
    }

    @Override
    public void pay(Order order) {
        System.out.println("    操作失败：订单已支付");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("    操作失败：已支付订单无法直接取消，请联系客服");
    }

    @Override
    public void ship(Order order) {
        System.out.println("    发货成功！");
        order.setState(new ShippedState());
    }

    @Override
    public void confirm(Order order) {
        System.out.println("    操作失败：已支付状态无法确认收货");
    }
}

/**
 * =====================================================================================
 * 具体状态：已发货
 * =====================================================================================
 */
class ShippedState implements OrderState {
    @Override
    public String getName() {
        return "已发货";
    }

    @Override
    public void pay(Order order) {
        System.out.println("    操作失败：订单已支付");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("    操作失败：已发货订单无法取消");
    }

    @Override
    public void ship(Order order) {
        System.out.println("    操作失败：订单已发货");
    }

    @Override
    public void confirm(Order order) {
        System.out.println("    确认收货成功！");
        order.setState(new CompletedState());
    }
}

/**
 * =====================================================================================
 * 具体状态：已完成
 * =====================================================================================
 */
class CompletedState implements OrderState {
    @Override
    public String getName() {
        return "已完成";
    }

    @Override
    public void pay(Order order) {
        System.out.println("    操作失败：订单已完成");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("    操作失败：订单已完成");
    }

    @Override
    public void ship(Order order) {
        System.out.println("    操作失败：订单已完成");
    }

    @Override
    public void confirm(Order order) {
        System.out.println("    操作失败：订单已完成");
    }
}

/**
 * =====================================================================================
 * 具体状态：已取消
 * =====================================================================================
 */
class CancelledState implements OrderState {
    @Override
    public String getName() {
        return "已取消";
    }

    @Override
    public void pay(Order order) {
        System.out.println("    操作失败：订单已取消");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("    操作失败：订单已取消");
    }

    @Override
    public void ship(Order order) {
        System.out.println("    操作失败：订单已取消");
    }

    @Override
    public void confirm(Order order) {
        System.out.println("    操作失败：订单已取消");
    }
}

/**
 * =====================================================================================
 * 上下文：订单
 * =====================================================================================
 */
class Order {
    private String orderId;
    private OrderState state;

    public Order(String orderId) {
        this.orderId = orderId;
        this.state = new PendingPaymentState();
    }

    public void setState(OrderState state) {
        this.state = state;
    }

    public String getStateName() {
        return state.getName();
    }

    public void pay() {
        System.out.println("    [执行支付操作]");
        state.pay(this);
    }

    public void cancel() {
        System.out.println("    [执行取消操作]");
        state.cancel(this);
    }

    public void ship() {
        System.out.println("    [执行发货操作]");
        state.ship(this);
    }

    public void confirm() {
        System.out.println("    [执行确认收货操作]");
        state.confirm(this);
    }
}
