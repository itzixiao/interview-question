package cn.itzixiao.interview.designpattern.behavioral.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================================
 * 观察者模式（Observer Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 定义对象间的一种一对多的依赖关系，当一个对象的状态发生改变时，所有依赖于它的对象
 * 都得到通知并被自动更新。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 主题（Subject）：被观察的对象，维护观察者列表
 * 2. 观察者（Observer）：订阅主题，接收通知
 * <p>
 * 三、观察者模式结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Subject（主题）                                                     │
 * │  ├── observers: List<Observer>                                      │
 * │  ├── attach(Observer)        // 添加观察者                          │
 * │  ├── detach(Observer)        // 移除观察者                          │
 * │  └── notify()                // 通知观察者                          │
 * │                                                                      │
 * │  Observer（观察者接口）                                              │
 * │  └── update()                // 更新方法                            │
 * │                                                                      │
 * │  ConcreteObserver（具体观察者）                                      │
 * │  └── 实现 update() 方法                                             │
 * └─────────────────────────────────────────────────────────────────────┘
 * <p>
 * 四、推模型 vs 拉模型
 * -------------------------------------------------------------------------------------
 * 1. 推模型：主题主动将数据传给观察者
 * - update(data)
 * - 观察者被动接收
 * <p>
 * 2. 拉模型：主题通知观察者，观察者主动获取数据
 * - update(subject)
 * - 观察者主动调用 subject.getData()
 * <p>
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - Java.util.Observable/Observer（已废弃）
 * - Java.util.EventListener
 * - Spring 事件机制：ApplicationEvent、ApplicationListener
 * - GUI 事件处理：按钮点击、键盘事件
 * - 消息队列：发布/订阅模式
 * - Vue/React 响应式原理
 */
public class ObserverDemo {

    public static void main(String[] args) {
        System.out.println("========== 观察者模式（Observer Pattern）==========\n");

        System.out.println("【场景：天气预报系统】\n");

        // 创建天气站（主题）
        WeatherStation weatherStation = new WeatherStation();

        // 创建观察者
        System.out.println("【注册观察者】：");
        Observer phoneApp = new PhoneAppDisplay();
        Observer webSite = new WebSiteDisplay();
        Observer ledScreen = new LEDScreenDisplay();

        weatherStation.registerObserver(phoneApp);
        weatherStation.registerObserver(webSite);
        weatherStation.registerObserver(ledScreen);

        // 天气变化，通知所有观察者
        System.out.println("\n【第一次天气更新】：");
        weatherStation.setWeather("晴天", 28);

        System.out.println("\n【第二次天气更新】：");
        weatherStation.setWeather("多云", 24);

        // 移除一个观察者
        System.out.println("\n【移除 LED 屏幕】：");
        weatherStation.removeObserver(ledScreen);

        System.out.println("\n【第三次天气更新】：");
        weatherStation.setWeather("下雨", 18);

        System.out.println("\n【模式分析】：");
        System.out.println("  - 定义一对多的依赖关系");
        System.out.println("  - 主题状态变化自动通知所有观察者");
        System.out.println("  - 观察者可以动态添加和移除");
        System.out.println("  - 符合开闭原则：新增观察者无需修改主题");
        System.out.println("  - 符合依赖倒置原则：面向接口编程");
        System.out.println("  - 注意：避免循环依赖，否则可能导致无限循环");
    }
}

/**
 * =====================================================================================
 * 观察者接口
 * =====================================================================================
 */
interface Observer {
    /**
     * 更新方法（推模型）
     *
     * @param weather     天气状况
     * @param temperature 温度
     */
    void update(String weather, double temperature);
}

/**
 * =====================================================================================
 * 主题接口
 * =====================================================================================
 */
interface Subject {
    void registerObserver(Observer observer);

    void removeObserver(Observer observer);

    void notifyObservers();
}

/**
 * =====================================================================================
 * 具体主题：天气站
 * =====================================================================================
 */
class WeatherStation implements Subject {
    private List<Observer> observers = new ArrayList<>();
    private String weather;
    private double temperature;

    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
        System.out.println("    注册观察者: " + observer.getClass().getSimpleName());
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
        System.out.println("    移除观察者: " + observer.getClass().getSimpleName());
    }

    @Override
    public void notifyObservers() {
        System.out.println("    通知所有观察者...");
        for (Observer observer : observers) {
            observer.update(weather, temperature);
        }
    }

    /**
     * 设置天气数据
     */
    public void setWeather(String weather, double temperature) {
        this.weather = weather;
        this.temperature = temperature;
        System.out.println("    天气变化: " + weather + ", 温度: " + temperature + "°C");
        notifyObservers();
    }
}

/**
 * =====================================================================================
 * 具体观察者：手机App
 * =====================================================================================
 */
class PhoneAppDisplay implements Observer {
    @Override
    public void update(String weather, double temperature) {
        System.out.println("    [手机App] 推送通知: 今天" + weather + "，气温" + temperature + "°C");
    }
}

/**
 * =====================================================================================
 * 具体观察者：网站
 * =====================================================================================
 */
class WebSiteDisplay implements Observer {
    @Override
    public void update(String weather, double temperature) {
        System.out.println("    [网站] 更新天气显示: " + weather + " | " + temperature + "°C");
    }
}

/**
 * =====================================================================================
 * 具体观察者：LED屏幕
 * =====================================================================================
 */
class LEDScreenDisplay implements Observer {
    @Override
    public void update(String weather, double temperature) {
        System.out.println("    [LED屏幕] 显示: 当前天气 " + weather + "，温度 " + temperature + "°C");
    }
}
