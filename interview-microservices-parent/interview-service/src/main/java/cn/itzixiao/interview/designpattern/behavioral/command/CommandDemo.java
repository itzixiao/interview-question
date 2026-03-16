package cn.itzixiao.interview.designpattern.behavioral.command;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================================
 * 命令模式（Command Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 将一个请求封装为一个对象，从而使你可用不同的请求对客户进行参数化；
 * 对请求排队或记录请求日志，以及支持可撤销的操作。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 命令接口（Command）：声明执行操作的接口
 * 2. 具体命令（Concrete Command）：实现命令接口，绑定接收者
 * 3. 接收者（Receiver）：执行实际工作
 * 4. 调用者（Invoker）：持有命令，调用命令执行
 * 5. 客户端（Client）：创建命令并设置接收者
 * <p>
 * 三、命令模式结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Client                                                              │
 * │     │                                                                │
 * │     ├──→ Receiver（接收者）                                          │
 * │     │                                                                │
 * │     └──→ ConcreteCommand（具体命令）                                 │
 * │              │                                                       │
 * │              └──→ Invoker（调用者）                                  │
 * │                       │                                              │
 * │                       └──→ execute() → Receiver.action()            │
 * └─────────────────────────────────────────────────────────────────────┘
 * <p>
 * 四、命令模式的优点
 * -------------------------------------------------------------------------------------
 * 1. 解耦：调用者和接收者解耦
 * 2. 可扩展：新增命令无需修改现有代码
 * 3. 可组合：命令可以组合成宏命令
 * 4. 可撤销：支持 undo 操作
 * 5. 可记录：可以记录命令日志
 * <p>
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - GUI 按钮：按钮点击执行命令
 * - 菜单项：菜单操作
 * - 事务处理：数据库事务
 * - 任务队列：线程池任务
 * - 撤销/重做：文本编辑器
 */
public class CommandDemo {

    public static void main(String[] args) {
        System.out.println("========== 命令模式（Command Pattern）==========\n");

        System.out.println("【场景：智能家居遥控器】\n");

        // 创建接收者
        Light livingRoomLight = new Light("客厅");
        Light bedroomLight = new Light("卧室");
        AirConditioner airConditioner = new AirConditioner();

        // 创建命令
        Command livingRoomLightOn = new LightOnCommand(livingRoomLight);
        Command livingRoomLightOff = new LightOffCommand(livingRoomLight);
        Command bedroomLightOn = new LightOnCommand(bedroomLight);
        Command airConditionerOn = new AirConditionerOnCommand(airConditioner);
        Command airConditionerOff = new AirConditionerOffCommand(airConditioner);

        // 创建宏命令（一键场景）
        List<Command> nightCommands = new ArrayList<>();
        nightCommands.add(livingRoomLightOff);
        nightCommands.add(bedroomLightOn);
        nightCommands.add(airConditionerOn);
        Command nightMode = new MacroCommand(nightCommands);

        // 创建调用者（遥控器）
        RemoteControl remote = new RemoteControl(5);
        remote.setCommand(0, livingRoomLightOn, livingRoomLightOff);
        remote.setCommand(1, bedroomLightOn, livingRoomLightOff);
        remote.setCommand(2, airConditionerOn, airConditionerOff);
        remote.setCommand(3, nightMode, null);  // 夜间模式

        // 使用遥控器
        System.out.println("【使用遥控器】：\n");

        System.out.println("1. 打开客厅灯：");
        remote.onButtonPressed(0);

        System.out.println("\n2. 关闭客厅灯：");
        remote.offButtonPressed(0);

        System.out.println("\n3. 撤销操作：");
        remote.undoButtonPressed();

        System.out.println("\n4. 一键夜间模式（宏命令）：");
        remote.onButtonPressed(3);

        System.out.println("\n【模式分析】：");
        System.out.println("  - 将请求封装为对象，解耦调用者和接收者");
        System.out.println("  - 支持撤销操作");
        System.out.println("  - 支持宏命令（命令组合）");
        System.out.println("  - 支持请求排队和日志记录");
        System.out.println("  - 符合开闭原则：新增命令无需修改现有代码");
        System.out.println("  - 符合单一职责原则：命令只负责调用接收者");
    }
}

/**
 * =====================================================================================
 * 命令接口
 * =====================================================================================
 */
interface Command {
    void execute();

    void undo();
}

/**
 * =====================================================================================
 * 接收者：灯
 * =====================================================================================
 */
class Light {
    private String location;
    private boolean on;

    public Light(String location) {
        this.location = location;
    }

    public void on() {
        on = true;
        System.out.println("    [" + location + "灯] 已打开");
    }

    public void off() {
        on = false;
        System.out.println("    [" + location + "灯] 已关闭");
    }

    public boolean isOn() {
        return on;
    }
}

/**
 * =====================================================================================
 * 接收者：空调
 * =====================================================================================
 */
class AirConditioner {
    private boolean on;

    public void on() {
        on = true;
        System.out.println("    [空调] 已打开，温度设置为 26°C");
    }

    public void off() {
        on = false;
        System.out.println("    [空调] 已关闭");
    }

    public boolean isOn() {
        return on;
    }
}

/**
 * =====================================================================================
 * 具体命令：开灯
 * =====================================================================================
 */
class LightOnCommand implements Command {
    private Light light;

    public LightOnCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() {
        light.on();
    }

    @Override
    public void undo() {
        light.off();
    }
}

/**
 * =====================================================================================
 * 具体命令：关灯
 * =====================================================================================
 */
class LightOffCommand implements Command {
    private Light light;

    public LightOffCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() {
        light.off();
    }

    @Override
    public void undo() {
        light.on();
    }
}

/**
 * =====================================================================================
 * 具体命令：开空调
 * =====================================================================================
 */
class AirConditionerOnCommand implements Command {
    private AirConditioner airConditioner;

    public AirConditionerOnCommand(AirConditioner airConditioner) {
        this.airConditioner = airConditioner;
    }

    @Override
    public void execute() {
        airConditioner.on();
    }

    @Override
    public void undo() {
        airConditioner.off();
    }
}

/**
 * =====================================================================================
 * 具体命令：关空调
 * =====================================================================================
 */
class AirConditionerOffCommand implements Command {
    private AirConditioner airConditioner;

    public AirConditionerOffCommand(AirConditioner airConditioner) {
        this.airConditioner = airConditioner;
    }

    @Override
    public void execute() {
        airConditioner.off();
    }

    @Override
    public void undo() {
        airConditioner.on();
    }
}

/**
 * =====================================================================================
 * 宏命令：组合多个命令
 * =====================================================================================
 */
class MacroCommand implements Command {
    private List<Command> commands;

    public MacroCommand(List<Command> commands) {
        this.commands = commands;
    }

    @Override
    public void execute() {
        System.out.println("    [宏命令] 执行一组命令：");
        for (Command command : commands) {
            command.execute();
        }
    }

    @Override
    public void undo() {
        System.out.println("    [宏命令] 撤销一组命令：");
        // 逆序撤销
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
}

/**
 * =====================================================================================
 * 空命令：什么都不做
 * =====================================================================================
 */
class NoCommand implements Command {
    @Override
    public void execute() {
    }

    @Override
    public void undo() {
    }
}

/**
 * =====================================================================================
 * 调用者：遥控器
 * =====================================================================================
 */
class RemoteControl {
    private Command[] onCommands;
    private Command[] offCommands;
    private Command undoCommand;

    public RemoteControl(int slots) {
        onCommands = new Command[slots];
        offCommands = new Command[slots];

        Command noCommand = new NoCommand();
        for (int i = 0; i < slots; i++) {
            onCommands[i] = noCommand;
            offCommands[i] = noCommand;
        }
        undoCommand = noCommand;
    }

    public void setCommand(int slot, Command onCommand, Command offCommand) {
        onCommands[slot] = onCommand;
        offCommands[slot] = offCommand;
    }

    public void onButtonPressed(int slot) {
        onCommands[slot].execute();
        undoCommand = onCommands[slot];
    }

    public void offButtonPressed(int slot) {
        offCommands[slot].execute();
        undoCommand = offCommands[slot];
    }

    public void undoButtonPressed() {
        System.out.println("    [撤销] 执行撤销操作");
        undoCommand.undo();
    }
}
