package cn.itzixiao.interview.designpattern.creational.abstractfactory;

/**
 * =====================================================================================
 * 抽象工厂模式（Abstract Factory Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 提供一个创建一系列相关或相互依赖对象的接口，而无需指定它们具体的类。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 抽象工厂（Abstract Factory）：声明创建抽象产品对象的方法
 * 2. 具体工厂（Concrete Factory）：实现创建具体产品对象的方法
 * 3. 抽象产品（Abstract Product）：声明产品的接口
 * 4. 具体产品（Concrete Product）：实现抽象产品接口
 * <p>
 * 三、与工厂方法的区别
 * -------------------------------------------------------------------------------------
 * | 特性           | 工厂方法               | 抽象工厂               |
 * |----------------|------------------------|------------------------|
 * | 产品数量       | 一种产品               | 多种产品（产品族）     |
 * | 工厂方法数量   | 一个                   | 多个                   |
 * | 扩展方式       | 新增工厂类             | 新增产品族工厂         |
 * | 使用场景       | 产品单一               | 产品成族出现           |
 * <p>
 * 四、产品族与产品等级
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │                    │  按钮(Button)  │  文本框(TextField) │  复选框(CheckBox) │
 * │ 产品等级（纵向）   │                │                    │                  │
 * ├────────────────────┼────────────────┼────────────────────┼──────────────────┤
 * │ Windows风格        │ WindowsButton │ WindowsTextField   │ WindowsCheckBox  │
 * │ Mac风格（横向）     │ MacButton     │ MacTextField       │ MacCheckBox      │
 * │ Linux风格          │ LinuxButton   │ LinuxTextField     │ LinuxCheckBox    │
 * └─────────────────────────────────────────────────────────────────────┘
 * <p>
 * - 产品等级：同一种产品的不同实现（纵向）
 * - 产品族：不同产品的同一风格组合（横向）
 * <p>
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - 跨平台UI组件：Windows/Mac/Linux 风格组件
 * - 数据库访问：不同数据库的连接、命令、结果集
 * - 主题换肤：不同风格的颜色、字体、图标
 */
public class AbstractFactoryDemo {

    public static void main(String[] args) {
        System.out.println("========== 抽象工厂模式（Abstract Factory Pattern）==========\n");

        System.out.println("【场景：跨平台UI组件工厂】\n");

        // 使用 Windows 风格工厂
        System.out.println("1. 使用 Windows 风格工厂创建组件：");
        GUIFactory windowsFactory = new WindowsGUIFactory();
        createUI(windowsFactory);

        // 使用 Mac 风格工厂
        System.out.println("\n2. 使用 Mac 风格工厂创建组件：");
        GUIFactory macFactory = new MacGUIFactory();
        createUI(macFactory);

        System.out.println("\n【模式分析】：");
        System.out.println("  - 抽象工厂创建的是「产品族」，不是单个产品");
        System.out.println("  - 同一工厂创建的产品风格一致，保证兼容性");
        System.out.println("  - 新增产品族只需新增工厂类，无需修改现有代码");
        System.out.println("  - 新增产品等级需要修改所有工厂类（这是该模式的缺点）");
    }

    /**
     * 使用工厂创建UI组件
     * 客户端代码只依赖抽象接口，不依赖具体实现
     */
    private static void createUI(GUIFactory factory) {
        Button button = factory.createButton();
        TextField textField = factory.createTextField();
        CheckBox checkBox = factory.createCheckBox();

        button.paint();
        textField.render();
        checkBox.check();
    }
}

/**
 * =====================================================================================
 * 抽象产品：按钮
 * =====================================================================================
 */
interface Button {
    void paint();
}

/**
 * =====================================================================================
 * 抽象产品：文本框
 * =====================================================================================
 */
interface TextField {
    void render();
}

/**
 * =====================================================================================
 * 抽象产品：复选框
 * =====================================================================================
 */
interface CheckBox {
    void check();
}

/**
 * =====================================================================================
 * 具体产品：Windows风格按钮
 * =====================================================================================
 */
class WindowsButton implements Button {
    @Override
    public void paint() {
        System.out.println("    [Windows] 绘制方形按钮，灰色背景");
    }
}

/**
 * =====================================================================================
 * 具体产品：Windows风格文本框
 * =====================================================================================
 */
class WindowsTextField implements TextField {
    @Override
    public void render() {
        System.out.println("    [Windows] 渲染方形文本框，白色背景");
    }
}

/**
 * =====================================================================================
 * 具体产品：Windows风格复选框
 * =====================================================================================
 */
class WindowsCheckBox implements CheckBox {
    @Override
    public void check() {
        System.out.println("    [Windows] 方形复选框，打勾标记");
    }
}

/**
 * =====================================================================================
 * 具体产品：Mac风格按钮
 * =====================================================================================
 */
class MacButton implements Button {
    @Override
    public void paint() {
        System.out.println("    [Mac] 绘制圆角按钮，半透明效果");
    }
}

/**
 * =====================================================================================
 * 具体产品：Mac风格文本框
 * =====================================================================================
 */
class MacTextField implements TextField {
    @Override
    public void render() {
        System.out.println("    [Mac] 渲染圆角文本框，柔和边框");
    }
}

/**
 * =====================================================================================
 * 具体产品：Mac风格复选框
 * =====================================================================================
 */
class MacCheckBox implements CheckBox {
    @Override
    public void check() {
        System.out.println("    [Mac] 圆形复选框，填充标记");
    }
}

/**
 * =====================================================================================
 * 抽象工厂
 * =====================================================================================
 * 声明创建产品族的方法
 */
interface GUIFactory {
    /**
     * 创建按钮
     */
    Button createButton();

    /**
     * 创建文本框
     */
    TextField createTextField();

    /**
     * 创建复选框
     */
    CheckBox createCheckBox();
}

/**
 * =====================================================================================
 * 具体工厂：Windows风格工厂
 * =====================================================================================
 * 负责创建 Windows 风格的所有组件
 */
class WindowsGUIFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new WindowsButton();
    }

    @Override
    public TextField createTextField() {
        return new WindowsTextField();
    }

    @Override
    public CheckBox createCheckBox() {
        return new WindowsCheckBox();
    }
}

/**
 * =====================================================================================
 * 具体工厂：Mac风格工厂
 * =====================================================================================
 * 负责创建 Mac 风格的所有组件
 */
class MacGUIFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public TextField createTextField() {
        return new MacTextField();
    }

    @Override
    public CheckBox createCheckBox() {
        return new MacCheckBox();
    }
}
