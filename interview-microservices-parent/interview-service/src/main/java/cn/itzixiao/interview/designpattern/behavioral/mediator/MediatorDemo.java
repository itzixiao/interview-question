package cn.itzixiao.interview.designpattern.behavioral.mediator;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================================
 * 中介者模式（Mediator Pattern）
 * =====================================================================================
 * 
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 用一个中介对象来封装一系列的对象交互。中介者使各对象不需要显式地相互引用，
 * 从而使其耦合松散，而且可以独立地改变它们之间的交互。
 * 
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 中介者接口（Mediator）：定义同事对象通信的接口
 * 2. 具体中介者（Concrete Mediator）：实现协调逻辑
 * 3. 同事类（Colleague）：通过中介者进行通信
 * 
 * 三、中介者模式结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │                     Mediator（中介者）                               │
 * │                          ↑                                          │
 * │            ┌─────────────┼─────────────┐                           │
 * │            ↓             ↓             ↓                           │
 * │       ColleagueA    ColleagueB    ColleagueC                       │
 * │            │             │             │                           │
 * │            └─────────────┴─────────────┘                           │
 * │                     通过中介者通信                                   │
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * 四、应用场景
 * -------------------------------------------------------------------------------------
 * - MVC 框架：Controller 作为 Mediator
 * - 聊天室：服务器作为中介者转发消息
 * - 机场塔台：塔台协调飞机起降
 * - GUI 组件：窗体协调按钮、文本框等组件
 */
public class MediatorDemo {

    public static void main(String[] args) {
        System.out.println("========== 中介者模式（Mediator Pattern）==========\n");

        System.out.println("【场景：聊天室】\n");

        // 创建聊天室（中介者）
        ChatRoom chatRoom = new ChatRoom();

        // 创建用户（同事类）
        User alice = new User("Alice", chatRoom);
        User bob = new User("Bob", chatRoom);
        User charlie = new User("Charlie", chatRoom);

        // 注册用户
        chatRoom.register(alice);
        chatRoom.register(bob);
        chatRoom.register(charlie);

        // 发送消息
        System.out.println("【群聊消息】：");
        alice.send("大家好！");

        System.out.println("\n【私聊消息】：");
        bob.sendPrivate("Alice", "你好 Alice！");

        System.out.println("\n【模式分析】：");
        System.out.println("  - 将多对多的交互转化为一对多");
        System.out.println("  - 对象之间不再直接引用，降低耦合");
        System.out.println("  - 中介者集中控制交互逻辑");
        System.out.println("  - 符合迪米特法则：最少知识原则");
        System.out.println("  - 缺点：中介者可能过于复杂");
    }
}

/**
 * 中介者接口
 */
interface ChatMediator {
    void register(User user);
    void sendMessage(String message, User sender);
    void sendPrivate(String message, String toUser, User sender);
}

/**
 * 具体中介者：聊天室
 */
class ChatRoom implements ChatMediator {
    private List<User> users = new ArrayList<>();

    @Override
    public void register(User user) {
        users.add(user);
        System.out.println("    [系统] " + user.getName() + " 加入聊天室");
    }

    @Override
    public void sendMessage(String message, User sender) {
        // 广播给所有用户（除发送者外）
        for (User user : users) {
            if (user != sender) {
                user.receive(message, sender.getName());
            }
        }
    }

    @Override
    public void sendPrivate(String message, String toUser, User sender) {
        // 发送给指定用户
        for (User user : users) {
            if (user.getName().equals(toUser)) {
                user.receive(message, sender.getName() + "（私聊）");
                return;
            }
        }
        System.out.println("    [系统] 用户 " + toUser + " 不存在");
    }
}

/**
 * 同事类：用户
 */
class User {
    private String name;
    private ChatMediator mediator;

    public User(String name, ChatMediator mediator) {
        this.name = name;
        this.mediator = mediator;
    }

    public String getName() {
        return name;
    }

    /**
     * 发送群消息
     */
    public void send(String message) {
        System.out.println("    [" + name + "] 发送: " + message);
        mediator.sendMessage(message, this);
    }

    /**
     * 发送私聊消息
     */
    public void sendPrivate(String toUser, String message) {
        System.out.println("    [" + name + "] 私聊 " + toUser + ": " + message);
        mediator.sendPrivate(message, toUser, this);
    }

    /**
     * 接收消息
     */
    public void receive(String message, String from) {
        System.out.println("    [" + name + "] 收到来自 " + from + " 的消息: " + message);
    }
}
