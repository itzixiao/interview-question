package cn.itzixiao.interview.designpattern.structural.adapter;

/**
 * =====================================================================================
 * 适配器模式（Adapter Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 将一个类的接口转换成客户希望的另一个接口，使得原本由于接口不兼容而不能一起工作的
 * 那些类可以一起工作。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 目标接口（Target）：客户端期望的接口
 * 2. 被适配者（Adaptee）：需要被适配的类
 * 3. 适配器（Adapter）：将 Adaptee 转换为 Target
 * <p>
 * 三、两种实现方式
 * -------------------------------------------------------------------------------------
 * 1. 类适配器（继承方式）
 * - 适配器继承 Adaptee，实现 Target 接口
 * - Java 单继承限制，灵活性差
 * <p>
 * 2. 对象适配器（组合方式）【推荐】
 * - 适配器持有 Adaptee 对象（组合/委托）
 * - 更灵活，推荐使用
 * <p>
 * 四、应用场景
 * -------------------------------------------------------------------------------------
 * - java.io.InputStreamReader：将 InputStream 适配为 Reader
 * - java.io.OutputStreamWriter：将 OutputStream 适配为 Writer
 * - Arrays.asList()：将数组适配为 List
 * - Spring MVC HandlerAdapter：适配不同类型的 Controller
 * - 第三方库接口适配
 */
public class AdapterDemo {

    public static void main(String[] args) {
        System.out.println("========== 适配器模式（Adapter Pattern）==========\n");

        System.out.println("【场景：电压适配器】");
        System.out.println("家用电压 220V，手机充电需要 5V，需要适配器转换\n");

        // 1. 类适配器
        System.out.println("【1. 类适配器（继承方式）】");
        Voltage5V classAdapter = new ClassAdapter();
        System.out.println("    输出电压: " + classAdapter.output5V() + "V");

        // 2. 对象适配器（推荐）
        System.out.println("\n【2. 对象适配器（组合方式，推荐）】");
        Voltage220V voltage220V = new Voltage220V();
        Voltage5V objectAdapter = new ObjectAdapter(voltage220V);
        System.out.println("    输出电压: " + objectAdapter.output5V() + "V");

        // 3. Java 标准库中的适配器
        System.out.println("\n【3. Java 标准库中的适配器模式】");
        System.out.println("    InputStreamReader: InputStream → Reader");
        System.out.println("    OutputStreamWriter: OutputStream → Writer");
        System.out.println("    Arrays.asList(): 数组 → List");

        System.out.println("\n【模式分析】：");
        System.out.println("  - 解决接口不兼容问题");
        System.out.println("  - 类适配器使用继承，Java 单继承限制");
        System.out.println("  - 对象适配器使用组合，更灵活，推荐使用");
        System.out.println("  - 符合开闭原则：无需修改现有代码");
    }
}

/**
 * =====================================================================================
 * 目标接口（Target）
 * =====================================================================================
 * 客户端期望的接口：输出 5V 电压
 */
interface Voltage5V {
    int output5V();
}

/**
 * =====================================================================================
 * 被适配者（Adaptee）
 * =====================================================================================
 * 现有的类：输出 220V 电压
 */
class Voltage220V {
    public int output220V() {
        System.out.println("    [电源] 输出 220V 电压");
        return 220;
    }
}

/**
 * =====================================================================================
 * 类适配器（继承方式）
 * =====================================================================================
 * 继承被适配者，实现目标接口
 * <p>
 * 缺点：Java 单继承限制，不够灵活
 */
class ClassAdapter extends Voltage220V implements Voltage5V {
    @Override
    public int output5V() {
        // 获取 220V 电压
        int voltage = output220V();
        // 转换为 5V
        System.out.println("    [类适配器] 220V → 5V 转换");
        return voltage / 44;
    }
}

/**
 * =====================================================================================
 * 对象适配器（组合方式）【推荐】
 * =====================================================================================
 * 持有被适配者对象，实现目标接口
 * <p>
 * 优点：
 * - 使用组合而非继承，更灵活
 * - 可以适配多个被适配者
 */
class ObjectAdapter implements Voltage5V {

    // 组合：持有被适配者对象
    private Voltage220V voltage220V;

    public ObjectAdapter(Voltage220V voltage220V) {
        this.voltage220V = voltage220V;
    }

    @Override
    public int output5V() {
        // 委托给被适配者
        int voltage = voltage220V.output220V();
        // 转换为 5V
        System.out.println("    [对象适配器] 220V → 5V 转换");
        return voltage / 44;
    }
}
