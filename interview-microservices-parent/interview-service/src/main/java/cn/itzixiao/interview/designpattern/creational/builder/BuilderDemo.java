package cn.itzixiao.interview.designpattern.creational.builder;

/**
 * =====================================================================================
 * 建造者模式（Builder Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 将一个复杂对象的构建与它的表示分离，使得同样的构建过程可以创建不同的表示。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 产品（Product）：最终要构建的复杂对象
 * 2. 抽象建造者（Builder）：定义构建产品各个部件的抽象接口
 * 3. 具体建造者（Concrete Builder）：实现 Builder 接口，构造和装配各个部件
 * 4. 指挥者（Director）：构建一个使用 Builder 接口的对象
 * <p>
 * 三、两种实现方式
 * -------------------------------------------------------------------------------------
 * 1. 传统方式：包含 Director（指挥者）
 * - 优点：封装构建逻辑，客户端无需了解构建细节
 * - 缺点：增加复杂度
 * <p>
 * 2. 简化方式：省略 Director，Builder 内部提供链式调用
 * - 优点：代码简洁，使用灵活
 * - 缺点：构建逻辑分散
 * <p>
 * 四、应用场景
 * -------------------------------------------------------------------------------------
 * - StringBuilder：构建字符串
 * - StringBuilder
 * - DocumentBuilder：构建XML文档
 * - AlertDialog.Builder：Android 对话框
 * - RocketMQ DefaultMQProducer：构建消息生产者
 * <p>
 * 五、使用场景
 * -------------------------------------------------------------------------------------
 * - 对象有多个属性，需要灵活组合
 * - 对象创建过程复杂，需要分步骤构建
 * - 需要创建不可变对象
 */
public class BuilderDemo {

    public static void main(String[] args) {
        System.out.println("========== 建造者模式（Builder Pattern）==========\n");

        // 方式一：传统方式（带 Director）
        System.out.println("【方式一：传统方式（带 Director）】\n");
        System.out.println("1. 构建豪华电脑：");
        ComputerBuilder luxuryBuilder = new LuxuryComputerBuilder();
        ComputerDirector director = new ComputerDirector(luxuryBuilder);
        Computer luxuryComputer = director.construct();
        System.out.println("    " + luxuryComputer);

        System.out.println("\n2. 构建经济电脑：");
        ComputerBuilder economyBuilder = new EconomyComputerBuilder();
        director = new ComputerDirector(economyBuilder);
        Computer economyComputer = director.construct();
        System.out.println("    " + economyComputer);

        // 方式二：链式调用（省略 Director）
        System.out.println("\n【方式二：链式调用（省略 Director）】\n");
        System.out.println("3. 自定义配置电脑：");
        Computer customComputer = Computer.builder()
                .cpu("Intel i9")
                .ram("64GB DDR5")
                .storage("2TB NVMe SSD")
                .gpu("NVIDIA RTX 4090")
                .build();
        System.out.println("    " + customComputer);

        // 方式三：StringBuilder
        System.out.println("\n【方式三：Java 标准库中的建造者模式】\n");
        StringBuilder sb = new StringBuilder()
                .append("Hello")
                .append(" ")
                .append("World");
        System.out.println("    StringBuilder: " + sb.toString());

        System.out.println("\n【模式分析】：");
        System.out.println("  - 分步创建复杂对象，控制创建过程");
        System.out.println("  - 相同的构建过程可以创建不同的表示");
        System.out.println("  - 客户端无需了解内部构建细节");
        System.out.println("  - 适合创建不可变对象（属性只读）");
        System.out.println("  - 链式调用使代码更简洁、可读");
    }
}

/**
 * =====================================================================================
 * 产品：电脑
 * =====================================================================================
 * 包含多个属性的复杂对象
 */
class Computer {
    private final String cpu;      // CPU
    private final String ram;      // 内存
    private final String storage;  // 存储
    private final String gpu;      // 显卡
    private final String monitor;  // 显示器

    /**
     * 私有构造函数，只能通过 Builder 创建
     */
    private Computer(Builder builder) {
        this.cpu = builder.cpu;
        this.ram = builder.ram;
        this.storage = builder.storage;
        this.gpu = builder.gpu;
        this.monitor = builder.monitor;
    }

    /**
     * 静态方法获取 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("电脑配置 [CPU: %s, 内存: %s, 存储: %s, 显卡: %s, 显示器: %s]",
                cpu, ram, storage, gpu, monitor);
    }

    /**
     * 静态内部类 Builder
     * 实现链式调用
     */
    public static class Builder {
        private String cpu;
        private String ram;
        private String storage;
        private String gpu;
        private String monitor;

        public Builder cpu(String cpu) {
            this.cpu = cpu;
            return this;
        }

        public Builder ram(String ram) {
            this.ram = ram;
            return this;
        }

        public Builder storage(String storage) {
            this.storage = storage;
            return this;
        }

        public Builder gpu(String gpu) {
            this.gpu = gpu;
            return this;
        }

        public Builder monitor(String monitor) {
            this.monitor = monitor;
            return this;
        }

        /**
         * 构建 Computer 对象
         */
        public Computer build() {
            // 可以在这里添加校验逻辑
            if (cpu == null || cpu.isEmpty()) {
                throw new IllegalStateException("CPU 不能为空");
            }
            return new Computer(this);
        }
    }
}

/**
 * =====================================================================================
 * 抽象建造者（传统方式）
 * =====================================================================================
 */
interface ComputerBuilder {
    void buildCpu();

    void buildRam();

    void buildStorage();

    void buildGpu();

    void buildMonitor();

    Computer getResult();
}

/**
 * =====================================================================================
 * 具体建造者：豪华电脑
 * =====================================================================================
 */
class LuxuryComputerBuilder implements ComputerBuilder {
    private Computer.Builder builder = Computer.builder();

    @Override
    public void buildCpu() {
        System.out.println("    安装 CPU: Intel i9-13900K");
        builder.cpu("Intel i9-13900K");
    }

    @Override
    public void buildRam() {
        System.out.println("    安装内存: 64GB DDR5 6000MHz");
        builder.ram("64GB DDR5 6000MHz");
    }

    @Override
    public void buildStorage() {
        System.out.println("    安装存储: 2TB Samsung 990 Pro");
        builder.storage("2TB Samsung 990 Pro");
    }

    @Override
    public void buildGpu() {
        System.out.println("    安装显卡: NVIDIA RTX 4090");
        builder.gpu("NVIDIA RTX 4090");
    }

    @Override
    public void buildMonitor() {
        System.out.println("    安装显示器: 32寸 4K 144Hz");
        builder.monitor("32寸 4K 144Hz");
    }

    @Override
    public Computer getResult() {
        return builder.build();
    }
}

/**
 * =====================================================================================
 * 具体建造者：经济电脑
 * =====================================================================================
 */
class EconomyComputerBuilder implements ComputerBuilder {
    private Computer.Builder builder = Computer.builder();

    @Override
    public void buildCpu() {
        System.out.println("    安装 CPU: Intel i5-12400");
        builder.cpu("Intel i5-12400");
    }

    @Override
    public void buildRam() {
        System.out.println("    安装内存: 16GB DDR4 3200MHz");
        builder.ram("16GB DDR4 3200MHz");
    }

    @Override
    public void buildStorage() {
        System.out.println("    安装存储: 512GB NVMe SSD");
        builder.storage("512GB NVMe SSD");
    }

    @Override
    public void buildGpu() {
        System.out.println("    使用核显: Intel UHD 730");
        builder.gpu("Intel UHD 730");
    }

    @Override
    public void buildMonitor() {
        System.out.println("    安装显示器: 24寸 1080P 60Hz");
        builder.monitor("24寸 1080P 60Hz");
    }

    @Override
    public Computer getResult() {
        return builder.build();
    }
}

/**
 * =====================================================================================
 * 指挥者（Director）
 * =====================================================================================
 * 控制构建顺序和过程
 */
class ComputerDirector {
    private ComputerBuilder builder;

    public ComputerDirector(ComputerBuilder builder) {
        this.builder = builder;
    }

    /**
     * 定义构建流程
     */
    public Computer construct() {
        builder.buildCpu();
        builder.buildRam();
        builder.buildStorage();
        builder.buildGpu();
        builder.buildMonitor();
        return builder.getResult();
    }
}
