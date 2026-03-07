package cn.itzixiao.interview.designpattern.creational.factorymethod;

/**
 * =====================================================================================
 * 工厂方法模式（Factory Method Pattern）
 * =====================================================================================
 * 
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 定义一个创建对象的接口，让子类决定实例化哪一个类。
 * 工厂方法使一个类的实例化延迟到其子类。
 * 
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 产品接口（Product）：定义产品的通用接口
 * 2. 具体产品（Concrete Product）：实现产品接口的具体类
 * 3. 工厂接口（Factory）：声明工厂方法
 * 4. 具体工厂（Concrete Factory）：实现工厂方法，创建具体产品
 * 
 * 三、与简单工厂的区别
 * -------------------------------------------------------------------------------------
 * | 特性           | 简单工厂               | 工厂方法               |
 * |----------------|------------------------|------------------------|
 * | 设计模式类型   | 不属于GoF 23种模式     | 属于创建型模式         |
 * | 扩展性         | 需要修改工厂类         | 只需添加新工厂类       |
 * | 开闭原则       | 违反                   | 符合                   |
 * | 工厂类数量     | 一个                   | 多个（每种产品一个）   |
 * 
 * 四、应用场景
 * -------------------------------------------------------------------------------------
 * - 日志记录器：文件日志、数据库日志、控制台日志
 * - 数据库连接：MySQL、Oracle、PostgreSQL
 * - 支付方式：支付宝、微信、银行卡
 * - 消息发送：邮件、短信、推送
 */
public class FactoryMethodDemo {

    public static void main(String[] args) {
        System.out.println("========== 工厂方法模式（Factory Method Pattern）==========\n");

        System.out.println("【场景：日志记录器工厂】\n");

        // 创建文件日志
        System.out.println("1. 创建文件日志记录器：");
        LoggerFactory fileFactory = new FileLoggerFactory();
        Logger fileLogger = fileFactory.createLogger();
        fileLogger.log("这是一条文件日志");

        // 创建数据库日志
        System.out.println("\n2. 创建数据库日志记录器：");
        LoggerFactory databaseFactory = new DatabaseLoggerFactory();
        Logger databaseLogger = databaseFactory.createLogger();
        databaseLogger.log("这是一条数据库日志");

        // 创建控制台日志
        System.out.println("\n3. 创建控制台日志记录器：");
        LoggerFactory consoleFactory = new ConsoleLoggerFactory();
        Logger consoleLogger = consoleFactory.createLogger();
        consoleLogger.log("这是一条控制台日志");

        System.out.println("\n【模式分析】：");
        System.out.println("  - 每种产品都有对应的工厂类");
        System.out.println("  - 新增产品只需新增工厂类，无需修改现有代码");
        System.out.println("  - 符合开闭原则：对扩展开放，对修改关闭");
        System.out.println("  - 符合单一职责原则：每个工厂只负责创建一种产品");
    }
}

/**
 * =====================================================================================
 * 产品接口
 * =====================================================================================
 * 定义日志记录器的通用接口
 */
interface Logger {
    /**
     * 记录日志
     * @param message 日志消息
     */
    void log(String message);
}

/**
 * =====================================================================================
 * 具体产品：文件日志记录器
 * =====================================================================================
 */
class FileLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.println("    [文件日志] 写入文件: " + message);
    }
}

/**
 * =====================================================================================
 * 具体产品：数据库日志记录器
 * =====================================================================================
 */
class DatabaseLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.println("    [数据库日志] 写入数据库: " + message);
    }
}

/**
 * =====================================================================================
 * 具体产品：控制台日志记录器
 * =====================================================================================
 */
class ConsoleLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.println("    [控制台日志] " + message);
    }
}

/**
 * =====================================================================================
 * 工厂接口
 * =====================================================================================
 * 声明工厂方法，返回 Logger 类型的产品
 */
interface LoggerFactory {
    /**
     * 工厂方法：创建日志记录器
     * @return 日志记录器实例
     */
    Logger createLogger();
}

/**
 * =====================================================================================
 * 具体工厂：文件日志工厂
 * =====================================================================================
 * 专门负责创建 FileLogger 实例
 */
class FileLoggerFactory implements LoggerFactory {
    @Override
    public Logger createLogger() {
        System.out.println("    创建文件日志记录器...");
        return new FileLogger();
    }
}

/**
 * =====================================================================================
 * 具体工厂：数据库日志工厂
 * =====================================================================================
 * 专门负责创建 DatabaseLogger 实例
 */
class DatabaseLoggerFactory implements LoggerFactory {
    @Override
    public Logger createLogger() {
        System.out.println("    创建数据库日志记录器...");
        return new DatabaseLogger();
    }
}

/**
 * =====================================================================================
 * 具体工厂：控制台日志工厂
 * =====================================================================================
 * 专门负责创建 ConsoleLogger 实例
 */
class ConsoleLoggerFactory implements LoggerFactory {
    @Override
    public Logger createLogger() {
        System.out.println("    创建控制台日志记录器...");
        return new ConsoleLogger();
    }
}
