package cn.itzixiao.interview.designpattern.behavioral.interpreter;

import java.util.Map;
import java.util.HashMap;

/**
 * =====================================================================================
 * 解释器模式（Interpreter Pattern）
 * =====================================================================================
 * 
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 给定一个语言，定义它的文法的一种表示，并定义一个解释器，这个解释器使用该表示
 * 来解释语言中的句子。
 * 
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 抽象表达式（Abstract Expression）：声明解释操作
 * 2. 终结符表达式（Terminal Expression）：实现终结符的解释
 * 3. 非终结符表达式（Non-terminal Expression）：实现非终结符的解释
 * 4. 上下文（Context）：存储解释器需要的全局信息
 * 
 * 三、解释器模式结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  AbstractExpression                                                  │
 * │  └── interpret(Context)                                              │
 * │                                                                      │
 * │  TerminalExpression                                                  │
 * │  └── interpret(Context)      // 解释终结符                           │
 * │                                                                      │
 * │  NonTerminalExpression                                               │
 * │  ├── expression: AbstractExpression                                  │
 * │  └── interpret(Context)      // 解释非终结符                         │
 * │                                                                      │
 * │  Context                                                             │
 * │  └── variables: Map<String, Object>                                  │
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * 四、文法规则示例
 * -------------------------------------------------------------------------------------
 * 简单的算术表达式文法：
 *   expression ::= term (('+' | '-') term)*
 *   term ::= factor (('*' | '/') factor)*
 *   factor ::= NUMBER | '(' expression ')'
 * 
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - 正则表达式
 * - SQL 解析器
 * - 表达式计算
 * - 配置文件解析
 * - 编译器
 */
public class InterpreterDemo {

    public static void main(String[] args) {
        System.out.println("========== 解释器模式（Interpreter Pattern）==========\n");

        System.out.println("【场景：简单数学表达式解释器】\n");

        // 创建上下文（变量表）
        Context context = new Context();
        context.setVariable("x", 10);
        context.setVariable("y", 20);
        context.setVariable("z", 5);

        System.out.println("变量表: x=10, y=20, z=5\n");

        // 解释表达式：x + y
        System.out.println("【表达式1：x + y】");
        Expression expr1 = new AddExpression(
                new VariableExpression("x"),
                new VariableExpression("y"));
        System.out.println("    结果: " + expr1.interpret(context));

        // 解释表达式：x + y - z
        System.out.println("\n【表达式2：x + y - z】");
        Expression expr2 = new SubtractExpression(
                new AddExpression(
                        new VariableExpression("x"),
                        new VariableExpression("y")),
                new VariableExpression("z"));
        System.out.println("    结果: " + expr2.interpret(context));

        // 解释表达式：x * y
        System.out.println("\n【表达式3：x * y】");
        Expression expr3 = new MultiplyExpression(
                new VariableExpression("x"),
                new VariableExpression("y"));
        System.out.println("    结果: " + expr3.interpret(context));

        // 解释表达式：(x + y) * z
        System.out.println("\n【表达式4：(x + y) * z】");
        Expression expr4 = new MultiplyExpression(
                new AddExpression(
                        new VariableExpression("x"),
                        new VariableExpression("y")),
                new VariableExpression("z"));
        System.out.println("    结果: " + expr4.interpret(context));

        System.out.println("\n【模式分析】：");
        System.out.println("  - 将文法规则映射为类");
        System.out.println("  - 易于扩展新的表达式类型");
        System.out.println("  - 每个规则一个类，清晰明了");
        System.out.println("  - 符合开闭原则：新增表达式无需修改现有代码");
        System.out.println("  - 缺点：文法规则复杂时类数量爆炸");
    }
}

/**
 * 上下文：存储变量
 */
class Context {
    private Map<String, Integer> variables = new HashMap<>();

    public void setVariable(String name, int value) {
        variables.put(name, value);
    }

    public int getVariable(String name) {
        Integer value = variables.get(name);
        if (value == null) {
            return 0;
        }
        return value;
    }
}

/**
 * 抽象表达式
 */
interface Expression {
    int interpret(Context context);
}

/**
 * 终结符表达式：变量
 */
class VariableExpression implements Expression {
    private String name;

    public VariableExpression(String name) {
        this.name = name;
    }

    @Override
    public int interpret(Context context) {
        return context.getVariable(name);
    }
}

/**
 * 终结符表达式：常量
 */
class NumberExpression implements Expression {
    private int number;

    public NumberExpression(int number) {
        this.number = number;
    }

    @Override
    public int interpret(Context context) {
        return number;
    }
}

/**
 * 非终结符表达式：加法
 */
class AddExpression implements Expression {
    private Expression left;
    private Expression right;

    public AddExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public int interpret(Context context) {
        return left.interpret(context) + right.interpret(context);
    }
}

/**
 * 非终结符表达式：减法
 */
class SubtractExpression implements Expression {
    private Expression left;
    private Expression right;

    public SubtractExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public int interpret(Context context) {
        return left.interpret(context) - right.interpret(context);
    }
}

/**
 * 非终结符表达式：乘法
 */
class MultiplyExpression implements Expression {
    private Expression left;
    private Expression right;

    public MultiplyExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public int interpret(Context context) {
        return left.interpret(context) * right.interpret(context);
    }
}
