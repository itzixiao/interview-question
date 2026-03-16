package cn.itzixiao.interview.designpattern.behavioral.templatemethod;

/**
 * =====================================================================================
 * 模板方法模式（Template Method Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 定义一个操作中算法的骨架，而将一些步骤延迟到子类中。
 * 模板方法使得子类可以不改变一个算法的结构即可重定义该算法的某些特定步骤。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 抽象类（Abstract Class）：定义算法骨架和基本方法
 * - 模板方法：定义算法骨架，调用基本方法
 * - 基本方法：
 * - 抽象方法：子类必须实现
 * - 具体方法：子类可直接使用
 * - 钩子方法：子类可选择重写
 * <p>
 * 2. 具体类（Concrete Class）：实现抽象方法
 * <p>
 * 三、模板方法结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  AbstractClass                                                       │
 * │  ├── templateMethod()        // 模板方法：定义算法骨架              │
 * │  │   ├── step1()             // 基本方法                            │
 * │  │   ├── step2()             // 基本方法                            │
 * │  │   └── step3()             // 基本方法                            │
 * │  │                                                                  │
 * │  ├── abstractMethod()        // 抽象方法：子类必须实现              │
 * │  ├── concreteMethod()        // 具体方法：子类可直接使用            │
 * │  └── hookMethod()            // 钩子方法：子类可选择重写            │
 * │                                                                      │
 * │  ConcreteClass extends AbstractClass                                 │
 * │  └── 实现抽象方法                                                    │
 * └─────────────────────────────────────────────────────────────────────┘
 * <p>
 * 四、钩子方法
 * -------------------------------------------------------------------------------------
 * 钩子方法是一种特殊的默认实现方法，子类可以选择性地重写来影响模板方法的行为。
 * <p>
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - Servlet：HttpServlet.service() 方法
 * - Spring JdbcTemplate：execute() 方法
 * - AQS：acquire() 方法
 * - 排序算法：Arrays.sort() + Comparable
 */
public class TemplateMethodDemo {

    public static void main(String[] args) {
        System.out.println("========== 模板方法模式（Template Method Pattern）==========\n");

        System.out.println("【场景：饮料制作流程】\n");

        // 制作咖啡
        System.out.println("1. 制作咖啡：");
        BeverageMaker coffee = new CoffeeMaker();
        coffee.makeBeverage();

        // 制作茶
        System.out.println("\n2. 制作茶：");
        BeverageMaker tea = new TeaMaker();
        tea.makeBeverage();

        // 制作奶茶（演示钩子方法）
        System.out.println("\n3. 制作奶茶（演示钩子方法）：");
        BeverageMaker milkTea = new MilkTeaMaker();
        milkTea.makeBeverage();

        System.out.println("\n【模式分析】：");
        System.out.println("  - 定义算法骨架，具体步骤由子类实现");
        System.out.println("  - 代码复用：公共逻辑放在父类");
        System.out.println("  - 扩展灵活：子类可以重写特定步骤");
        System.out.println("  - 钩子方法：提供扩展点，子类可选择重写");
        System.out.println("  - 符合开闭原则：新增子类无需修改父类");
        System.out.println("  - 符合好莱坞原则：不要调用我们，我们会调用你");
    }
}

/**
 * =====================================================================================
 * 抽象类：饮料制作
 * =====================================================================================
 */
abstract class BeverageMaker {

    /**
     * 模板方法：定义饮料制作的算法骨架
     * 使用 final 修饰，防止子类重写
     */
    public final void makeBeverage() {
        boilWater();           // 1. 烧水
        brew();                // 2. 冲泡（抽象方法，子类实现）
        pourInCup();           // 3. 倒入杯子
        if (wantCondiments()) {// 4. 添加调料（钩子方法控制）
            addCondiments();   // 5. 添加调料（抽象方法，子类实现）
        }
        serve();               // 6. 上桌
    }

    /**
     * 具体方法：烧水（公共逻辑）
     */
    private void boilWater() {
        System.out.println("    [烧水] 将水烧开");
    }

    /**
     * 抽象方法：冲泡（子类必须实现）
     */
    protected abstract void brew();

    /**
     * 具体方法：倒入杯子（公共逻辑）
     */
    private void pourInCup() {
        System.out.println("    [倒杯] 将饮料倒入杯子");
    }

    /**
     * 钩子方法：是否添加调料（子类可选择重写）
     * 默认返回 true
     */
    protected boolean wantCondiments() {
        return true;
    }

    /**
     * 抽象方法：添加调料（子类必须实现）
     */
    protected abstract void addCondiments();

    /**
     * 具体方法：上桌（公共逻辑）
     */
    private void serve() {
        System.out.println("    [上桌] 饮料制作完成，请享用！");
    }
}

/**
 * =====================================================================================
 * 具体类：咖啡
 * =====================================================================================
 */
class CoffeeMaker extends BeverageMaker {

    @Override
    protected void brew() {
        System.out.println("    [冲泡] 用沸水冲泡咖啡粉");
    }

    @Override
    protected void addCondiments() {
        System.out.println("    [调味] 添加糖和牛奶");
    }
}

/**
 * =====================================================================================
 * 具体类：茶
 * =====================================================================================
 */
class TeaMaker extends BeverageMaker {

    @Override
    protected void brew() {
        System.out.println("    [冲泡] 用沸水浸泡茶叶");
    }

    @Override
    protected void addCondiments() {
        System.out.println("    [调味] 添加柠檬片");
    }
}

/**
 * =====================================================================================
 * 具体类：奶茶（演示钩子方法）
 * =====================================================================================
 */
class MilkTeaMaker extends BeverageMaker {

    @Override
    protected void brew() {
        System.out.println("    [冲泡] 用沸水冲泡奶茶粉");
    }

    @Override
    protected void addCondiments() {
        System.out.println("    [调味] 添加珍珠和椰果");
    }

    /**
     * 重写钩子方法：奶茶店客户可以选择不加配料
     * 这里演示返回 true，实际可以由客户决定
     */
    @Override
    protected boolean wantCondiments() {
        System.out.println("    [询问] 是否需要添加配料？（默认需要）");
        return true;
    }
}
