package cn.itzixiao.interview.designpattern.behavioral.chainofresponsibility;

/**
 * =====================================================================================
 * 责任链模式（Chain of Responsibility Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 使多个对象都有机会处理请求，从而避免请求的发送者和接收者之间的耦合关系。
 * 将这些对象连成一条链，并沿着这条链传递该请求，直到有一个对象处理它为止。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 抽象处理者（Handler）：定义处理请求的接口，持有后继者引用
 * 2. 具体处理者（Concrete Handler）：处理请求，或转发给后继者
 * 3. 客户端（Client）：创建处理链，发起请求
 * <p>
 * 三、责任链结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Client → Handler1 → Handler2 → Handler3 → ... → null              │
 * │            处理/传递    处理/传递    处理/传递                       │
 * │                                                                      │
 * │  Handler（抽象处理者）                                               │
 * │  ├── successor: Handler      // 后继者                              │
 * │  ├── setNext(Handler)        // 设置后继者                          │
 * │  └── handleRequest()         // 处理请求                            │
 * │                                                                      │
 * │  ConcreteHandler（具体处理者）                                       │
 * │  └── handleRequest()                                                │
 * │      ├── 如果能处理 → 处理并返回                                    │
 * │      └── 如果不能处理 → 传递给后继者                                │
 * └─────────────────────────────────────────────────────────────────────┘
 * <p>
 * 四、纯责任链 vs 不纯责任链
 * -------------------------------------------------------------------------------------
 * 1. 纯责任链：每个处理者要么处理，要么转发，不能既处理又转发
 * 2. 不纯责任链：处理者可以处理后继续转发（如 Servlet Filter）
 * <p>
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - Servlet Filter：过滤器链
 * - Spring Interceptor：拦截器链
 * - Netty Pipeline：ChannelHandler 链
 * - 审批流程：经理 → 总监 → CEO
 * - 异常处理：异常捕获链
 */
public class ChainOfResponsibilityDemo {

    public static void main(String[] args) {
        System.out.println("========== 责任链模式（Chain of Responsibility Pattern）==========\n");

        System.out.println("【场景：采购审批流程】\n");

        // 创建审批者链
        Approver director = new Director("张经理");
        Approver vicePresident = new VicePresident("李副总");
        Approver president = new President("王总裁");
        Approver board = new Board("董事会");

        // 设置责任链
        director.setNext(vicePresident);
        vicePresident.setNext(president);
        president.setNext(board);

        // 发起采购请求
        System.out.println("【采购请求审批】：\n");

        PurchaseRequest request1 = new PurchaseRequest("采购办公用品", 5000);
        director.processRequest(request1);

        System.out.println();
        PurchaseRequest request2 = new PurchaseRequest("采购服务器", 80000);
        director.processRequest(request2);

        System.out.println();
        PurchaseRequest request3 = new PurchaseRequest("采购办公大楼", 5000000);
        director.processRequest(request3);

        System.out.println("\n【模式分析】：");
        System.out.println("  - 请求沿着链传递，直到被处理");
        System.out.println("  - 发送者和接收者解耦");
        System.out.println("  - 可以动态调整链的结构");
        System.out.println("  - 符合单一职责原则：每个处理者只负责自己的职责");
        System.out.println("  - 符合开闭原则：新增处理者无需修改现有代码");
    }
}

/**
 * =====================================================================================
 * 采购请求
 * =====================================================================================
 */
class PurchaseRequest {
    private String description;
    private double amount;

    public PurchaseRequest(String description, double amount) {
        this.description = description;
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }
}

/**
 * =====================================================================================
 * 抽象处理者
 * =====================================================================================
 */
abstract class Approver {
    protected String name;
    protected Approver next;  // 后继者

    public Approver(String name) {
        this.name = name;
    }

    /**
     * 设置后继者
     */
    public Approver setNext(Approver next) {
        this.next = next;
        return next;  // 返回后继者，支持链式调用
    }

    /**
     * 处理请求
     */
    public abstract void processRequest(PurchaseRequest request);
}

/**
 * =====================================================================================
 * 具体处理者：经理（1万以下）
 * =====================================================================================
 */
class Director extends Approver {
    public Director(String name) {
        super(name);
    }

    @Override
    public void processRequest(PurchaseRequest request) {
        if (request.getAmount() <= 10000) {
            System.out.println("    [经理 " + name + "] 审批通过: " +
                    request.getDescription() + ", 金额: ¥" + request.getAmount());
        } else if (next != null) {
            System.out.println("    [经理 " + name + "] 无权审批，转交上级...");
            next.processRequest(request);
        }
    }
}

/**
 * =====================================================================================
 * 具体处理者：副总（10万以下）
 * =====================================================================================
 */
class VicePresident extends Approver {
    public VicePresident(String name) {
        super(name);
    }

    @Override
    public void processRequest(PurchaseRequest request) {
        if (request.getAmount() <= 100000) {
            System.out.println("    [副总 " + name + "] 审批通过: " +
                    request.getDescription() + ", 金额: ¥" + request.getAmount());
        } else if (next != null) {
            System.out.println("    [副总 " + name + "] 无权审批，转交上级...");
            next.processRequest(request);
        }
    }
}

/**
 * =====================================================================================
 * 具体处理者：总裁（100万以下）
 * =====================================================================================
 */
class President extends Approver {
    public President(String name) {
        super(name);
    }

    @Override
    public void processRequest(PurchaseRequest request) {
        if (request.getAmount() <= 1000000) {
            System.out.println("    [总裁 " + name + "] 审批通过: " +
                    request.getDescription() + ", 金额: ¥" + request.getAmount());
        } else if (next != null) {
            System.out.println("    [总裁 " + name + "] 无权审批，转交董事会...");
            next.processRequest(request);
        }
    }
}

/**
 * =====================================================================================
 * 具体处理者：董事会（无上限）
 * =====================================================================================
 */
class Board extends Approver {
    public Board(String name) {
        super(name);
    }

    @Override
    public void processRequest(PurchaseRequest request) {
        System.out.println("    [董事会] 召开会议审批通过: " +
                request.getDescription() + ", 金额: ¥" + request.getAmount());
    }
}
