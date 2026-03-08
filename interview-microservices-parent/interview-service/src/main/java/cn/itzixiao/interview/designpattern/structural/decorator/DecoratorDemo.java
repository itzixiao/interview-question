package cn.itzixiao.interview.designpattern.structural.decorator;

/**
 * =====================================================================================
 * 装饰器模式（Decorator Pattern）
 * =====================================================================================
 * 
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 动态地给一个对象添加一些额外的职责。就增加功能来说，装饰器模式比生成子类更为灵活。
 * 
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 组件接口（Component）：定义对象接口
 * 2. 具体组件（Concrete Component）：实现组件接口的原始对象
 * 3. 装饰器基类（Decorator）：实现组件接口，持有组件引用
 * 4. 具体装饰器（Concrete Decorator）：添加具体功能
 * 
 * 三、装饰器模式 vs 继承
 * -------------------------------------------------------------------------------------
 * | 特性       | 继承                     | 装饰器模式                 |
 * |------------|--------------------------|----------------------------|
 * | 功能扩展   | 静态（编译时确定）       | 动态（运行时确定）         |
 * | 灵活性     | 低（类爆炸）             | 高（自由组合）             |
 * | 扩展时机   | 编译时                   | 运行时                     |
 * | 多功能组合 | 需要创建大量子类         | 装饰器自由组合             |
 * 
 * 四、装饰器链
 * -------------------------------------------------------------------------------------
 * 装饰器可以嵌套使用，形成装饰器链：
 * 
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  new DecoratorB(                                                    │
 * │      new DecoratorA(                                                │
 * │          new ConcreteComponent()                                    │
 * │      )                                                              │
 * │  )                                                                  │
 * │                                                                     │
 * │  调用顺序：DecoratorB → DecoratorA → ConcreteComponent              │
 * │  执行顺序：ConcreteComponent → DecoratorA → DecoratorB              │
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - Java I/O：InputStream、Reader 等都是装饰器模式
 *   new BufferedReader(new InputStreamReader(new FileInputStream("file")))
 * - Spring Session：包装 HttpSession
 * - Servlet API：HttpServletRequestWrapper
 */
public class DecoratorDemo {

    public static void main(String[] args) {
        System.out.println("========== 装饰器模式（Decorator Pattern）==========\n");

        System.out.println("【场景：咖啡订单系统】\n");

        // 1. 基础咖啡
        System.out.println("1. 基础咖啡：");
        Coffee espresso = new Espresso();
        System.out.println("    " + espresso.getDescription());
        System.out.println("    价格: ¥" + espresso.getCost());

        // 2. 加牛奶
        System.out.println("\n2. 加牛奶：");
        Coffee milkCoffee = new MilkDecorator(espresso);
        System.out.println("    " + milkCoffee.getDescription());
        System.out.println("    价格: ¥" + milkCoffee.getCost());

        // 3. 加糖
        System.out.println("\n3. 加糖：");
        Coffee sugarCoffee = new SugarDecorator(espresso);
        System.out.println("    " + sugarCoffee.getDescription());
        System.out.println("    价格: ¥" + sugarCoffee.getCost());

        // 4. 加牛奶和糖（装饰器链）
        System.out.println("\n4. 加牛奶和糖（装饰器链）：");
        Coffee milkSugarCoffee = new SugarDecorator(new MilkDecorator(new Espresso()));
        System.out.println("    " + milkSugarCoffee.getDescription());
        System.out.println("    价格: ¥" + milkSugarCoffee.getCost());

        // 5. 另一种咖啡
        System.out.println("\n5. 另一种组合：");
        Coffee fancyCoffee = new WhipDecorator(
                new MilkDecorator(
                        new SugarDecorator(
                                new Latte())));
        System.out.println("    " + fancyCoffee.getDescription());
        System.out.println("    价格: ¥" + fancyCoffee.getCost());

        // 6. Java I/O 中的装饰器
        System.out.println("\n【Java I/O 中的装饰器模式】：");
        System.out.println("    new BufferedReader(");
        System.out.println("        new InputStreamReader(");
        System.out.println("            new FileInputStream(\"file.txt\")))");
        System.out.println("    FileInputStream: 具体组件");
        System.out.println("    InputStreamReader: 装饰器（字节→字符）");
        System.out.println("    BufferedReader: 装饰器（缓冲）");

        System.out.println("\n【模式分析】：");
        System.out.println("  - 动态扩展对象功能，比继承更灵活");
        System.out.println("  - 装饰器与组件类型相同，可无限嵌套");
        System.out.println("  - 符合开闭原则：无需修改现有代码");
        System.out.println("  - 符合单一职责原则：每个装饰器只负责一个功能");
        System.out.println("  - 注意：多层装饰会增加复杂度");
    }
}

/**
 * =====================================================================================
 * 组件接口（Component）
 * =====================================================================================
 * 定义咖啡的接口
 */
interface Coffee {
    /**
     * 获取描述
     */
    String getDescription();

    /**
     * 获取价格
     */
    double getCost();
}

/**
 * =====================================================================================
 * 具体组件：浓缩咖啡
 * =====================================================================================
 */
class Espresso implements Coffee {
    @Override
    public String getDescription() {
        return "浓缩咖啡";
    }

    @Override
    public double getCost() {
        return 15.0;
    }
}

/**
 * =====================================================================================
 * 具体组件：拿铁
 * =====================================================================================
 */
class Latte implements Coffee {
    @Override
    public String getDescription() {
        return "拿铁咖啡";
    }

    @Override
    public double getCost() {
        return 20.0;
    }
}

/**
 * =====================================================================================
 * 装饰器基类（Decorator）
 * =====================================================================================
 * 实现组件接口，持有组件引用
 */
abstract class CoffeeDecorator implements Coffee {
    // 持有被装饰的组件
    protected Coffee coffee;

    public CoffeeDecorator(Coffee coffee) {
        this.coffee = coffee;
    }

    @Override
    public String getDescription() {
        return coffee.getDescription();
    }

    @Override
    public double getCost() {
        return coffee.getCost();
    }
}

/**
 * =====================================================================================
 * 具体装饰器：牛奶
 * =====================================================================================
 */
class MilkDecorator extends CoffeeDecorator {

    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + " + 牛奶";
    }

    @Override
    public double getCost() {
        return coffee.getCost() + 5.0;  // 牛奶加 5 元
    }
}

/**
 * =====================================================================================
 * 具体装饰器：糖
 * =====================================================================================
 */
class SugarDecorator extends CoffeeDecorator {

    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + " + 糖";
    }

    @Override
    public double getCost() {
        return coffee.getCost() + 2.0;  // 糖加 2 元
    }
}

/**
 * =====================================================================================
 * 具体装饰器：奶油
 * =====================================================================================
 */
class WhipDecorator extends CoffeeDecorator {

    public WhipDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + " + 奶油";
    }

    @Override
    public double getCost() {
        return coffee.getCost() + 8.0;  // 奶油加 8 元
    }
}
