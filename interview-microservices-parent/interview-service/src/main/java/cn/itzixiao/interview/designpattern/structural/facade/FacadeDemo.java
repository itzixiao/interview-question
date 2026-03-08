package cn.itzixiao.interview.designpattern.structural.facade;

/**
 * =====================================================================================
 * 外观模式（Facade Pattern）
 * =====================================================================================
 * 
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 为子系统中的一组接口提供一个一致的界面，外观模式定义了一个高层接口，
 * 这个接口使得这一子系统更加容易使用。
 * 
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 外观（Facade）：提供简化的高层接口
 * 2. 子系统（Subsystems）：复杂的底层系统
 * 3. 客户端（Client）：通过外观访问子系统
 * 
 * 三、外观模式的作用
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │                    没有外观模式                    有外观模式        │
 * │                    ┌─────────┐                    ┌─────┐          │
 * │                    │ Client  │                    │Client│          │
 * │                    └────┬────┘                    └──┬──┘          │
 * │            ┌───────────┼───────────┐                │              │
 * │            ↓           ↓           ↓           ┌────┴────┐        │
 * │       ┌─────────┐ ┌─────────┐ ┌─────────┐       │  Facade  │        │
 * │       │System A │ │System B │ │System C │       └────┬────┘        │
 * │       └─────────┘ └─────────┘ └─────────┘            │              │
 * │                                        ┌───────────┼───────────┐   │
 * │                                        ↓           ↓           ↓   │
 * │                                   ┌─────────┐ ┌─────────┐ ┌─────────┐│
 * │                                   │System A │ │System B │ │System C ││
 * │                                   └─────────┘ └─────────┘ └─────────┘│
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * 四、外观模式 vs 适配器模式
 * -------------------------------------------------------------------------------------
 * | 特性       | 外观模式                   | 适配器模式             |
 * |------------|----------------------------|------------------------|
 * | 目的       | 简化接口                   | 接口转换               |
 * | 接口数量   | 一对多（一个外观多个子系统）| 一对一                 |
 * | 使用场景   | 简化复杂系统               | 接口不兼容             |
 * 
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - JdbcUtils：封装 JDBC 操作
 * - Spring BeanFactory：封装 Bean 创建过程
 * - Tomcat：RequestFacade 封装 Request
 * - 电脑启动：BIOS、引导程序、操作系统等统一封装
 */
public class FacadeDemo {

    public static void main(String[] args) {
        System.out.println("========== 外观模式（Facade Pattern）==========\n");

        System.out.println("【场景：家庭影院系统】\n");

        // 方式一：不使用外观模式
        System.out.println("【方式一：不使用外观模式（客户端直接操作子系统）】");
        System.out.println("启动流程：");
        System.out.println("  1. 打开 DVD 播放器");
        System.out.println("  2. 打开投影仪");
        System.out.println("  3. 设置投影仪输入源");
        System.out.println("  4. 打开音响");
        System.out.println("  5. 设置音响模式");
        System.out.println("  6. 播放电影");
        System.out.println("  → 客户端需要了解所有子系统，复杂且容易出错\n");

        // 方式二：使用外观模式
        System.out.println("【方式二：使用外观模式（通过外观统一操作）】");
        HomeTheaterFacade homeTheater = new HomeTheaterFacade(
                new DVDPlayer(),
                new Projector(),
                new SoundSystem(),
                new Lights());
        
        System.out.println("\n一键观看电影：");
        homeTheater.watchMovie("《阿凡达》");

        System.out.println("\n一键关闭电影：");
        homeTheater.endMovie();

        System.out.println("\n【模式分析】：");
        System.out.println("  - 为复杂子系统提供简化的高层接口");
        System.out.println("  - 客户端与子系统解耦，降低复杂度");
        System.out.println("  - 不限制客户端直接访问子系统（松耦合）");
        System.out.println("  - 可以创建多个外观，提供不同级别的简化");
        System.out.println("  - 符合迪米特法则：最少知识原则");
    }
}

/**
 * =====================================================================================
 * 子系统：DVD 播放器
 * =====================================================================================
 */
class DVDPlayer {
    public void on() {
        System.out.println("    [DVD播放器] 开机");
    }

    public void off() {
        System.out.println("    [DVD播放器] 关机");
    }

    public void play(String movie) {
        System.out.println("    [DVD播放器] 播放电影: " + movie);
    }

    public void stop() {
        System.out.println("    [DVD播放器] 停止播放");
    }
}

/**
 * =====================================================================================
 * 子系统：投影仪
 * =====================================================================================
 */
class Projector {
    public void on() {
        System.out.println("    [投影仪] 开机");
    }

    public void off() {
        System.out.println("    [投影仪] 关机");
    }

    public void setInput(String input) {
        System.out.println("    [投影仪] 设置输入源: " + input);
    }

    public void wideScreenMode() {
        System.out.println("    [投影仪] 宽屏模式");
    }
}

/**
 * =====================================================================================
 * 子系统：音响系统
 * =====================================================================================
 */
class SoundSystem {
    public void on() {
        System.out.println("    [音响系统] 开机");
    }

    public void off() {
        System.out.println("    [音响系统] 关机");
    }

    public void setVolume(int level) {
        System.out.println("    [音响系统] 音量: " + level);
    }

    public void setSurroundSound() {
        System.out.println("    [音响系统] 环绕立体声模式");
    }
}

/**
 * =====================================================================================
 * 子系统：灯光
 * =====================================================================================
 */
class Lights {
    public void dim(int level) {
        System.out.println("    [灯光] 调暗至 " + level + "%");
    }

    public void on() {
        System.out.println("    [灯光] 打开");
    }
}

/**
 * =====================================================================================
 * 外观（Facade）
 * =====================================================================================
 * 提供简化的高层接口，封装子系统操作
 */
class HomeTheaterFacade {
    private DVDPlayer dvdPlayer;
    private Projector projector;
    private SoundSystem soundSystem;
    private Lights lights;

    public HomeTheaterFacade(DVDPlayer dvdPlayer, Projector projector,
                             SoundSystem soundSystem, Lights lights) {
        this.dvdPlayer = dvdPlayer;
        this.projector = projector;
        this.soundSystem = soundSystem;
        this.lights = lights;
    }

    /**
     * 一键观看电影
     * 封装所有子系统的复杂操作
     */
    public void watchMovie(String movie) {
        System.out.println("  === 准备观看电影 ===");
        lights.dim(10);
        projector.on();
        projector.setInput("DVD");
        projector.wideScreenMode();
        soundSystem.on();
        soundSystem.setSurroundSound();
        soundSystem.setVolume(50);
        dvdPlayer.on();
        dvdPlayer.play(movie);
        System.out.println("  === 电影开始 ===");
    }

    /**
     * 一键关闭电影
     */
    public void endMovie() {
        System.out.println("  === 关闭电影 ===");
        dvdPlayer.stop();
        dvdPlayer.off();
        soundSystem.off();
        projector.off();
        lights.on();
        System.out.println("  === 电影结束 ===");
    }
}
