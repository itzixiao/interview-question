package cn.itzixiao.interview.designpattern.behavioral.memento;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================================
 * 备忘录模式（Memento Pattern）
 * =====================================================================================
 * 
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 在不破坏封装性的前提下，捕获一个对象的内部状态，并在该对象之外保存这个状态。
 * 这样以后就可将该对象恢复到原先保存的状态。
 * 
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 备忘录（Memento）：存储原发器的内部状态
 * 2. 原发器（Originator）：创建备忘录，使用备忘录恢复状态
 * 3. 负责人（Caretaker）：负责保存备忘录
 * 
 * 三、备忘录模式结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Originator（原发器）                                                │
 * │  ├── state                   // 内部状态                            │
 * │  ├── createMemento()         // 创建备忘录                          │
 * │  └── restoreMemento()        // 从备忘录恢复                        │
 * │                                                                      │
 * │  Memento（备忘录）                                                   │
 * │  └── state                   // 存储的状态                          │
 * │                                                                      │
 * │  Caretaker（负责人）                                                 │
 * │  └── mementoList             // 保存备忘录列表                      │
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * 四、备忘录的安全性
 * -------------------------------------------------------------------------------------
 * 1. 白箱实现：备忘录对所有类公开，不安全
 * 2. 黑箱实现：使用内部类或包级私有，只有原发器可访问
 * 
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - 文本编辑器：撤销/重做
 * - 游戏存档：保存游戏进度
 * - 事务回滚：数据库事务
 * - 浏览器历史：前进/后退
 */
public class MementoDemo {

    public static void main(String[] args) {
        System.out.println("========== 备忘录模式（Memento Pattern）==========\n");

        System.out.println("【场景：文本编辑器撤销功能】\n");

        // 创建编辑器和历史管理器
        TextEditor editor = new TextEditor();
        EditorHistory history = new EditorHistory();

        // 编辑文本
        System.out.println("【编辑文本】：");
        editor.setContent("Hello");
        System.out.println("    当前内容: " + editor.getContent());
        history.save(editor.save());

        editor.setContent("Hello World");
        System.out.println("    当前内容: " + editor.getContent());
        history.save(editor.save());

        editor.setContent("Hello World!");
        System.out.println("    当前内容: " + editor.getContent());
        history.save(editor.save());

        editor.setContent("Hello Java!");
        System.out.println("    当前内容: " + editor.getContent());

        // 撤销操作
        System.out.println("\n【撤销操作】：");
        
        System.out.println("撤销1次：");
        editor.restore(history.undo());
        System.out.println("    当前内容: " + editor.getContent());

        System.out.println("撤销2次：");
        editor.restore(history.undo());
        System.out.println("    当前内容: " + editor.getContent());

        // 重做操作
        System.out.println("\n【重做操作】：");
        editor.restore(history.redo());
        System.out.println("    当前内容: " + editor.getContent());

        System.out.println("\n【模式分析】：");
        System.out.println("  - 捕获对象内部状态，实现撤销/恢复");
        System.out.println("  - 不破坏封装性，状态存储在备忘录中");
        System.out.println("  - 原发器负责创建和恢复备忘录");
        System.out.println("  - 负责人负责管理备忘录，但不修改备忘录");
        System.out.println("  - 符合单一职责原则：状态管理分离");
    }
}

/**
 * =====================================================================================
 * 备忘录：存储文本编辑器状态
 * =====================================================================================
 */
class TextMemento {
    private String content;
    private int cursorPos;

    public TextMemento(String content, int cursorPos) {
        this.content = content;
        this.cursorPos = cursorPos;
    }

    public String getContent() {
        return content;
    }

    public int getCursorPos() {
        return cursorPos;
    }
}

/**
 * =====================================================================================
 * 原发器：文本编辑器
 * =====================================================================================
 */
class TextEditor {
    private String content = "";
    private int cursorPos = 0;

    public void setContent(String content) {
        this.content = content;
        this.cursorPos = content.length();
    }

    public String getContent() {
        return content;
    }

    /**
     * 创建备忘录
     */
    public TextMemento save() {
        System.out.println("    [保存] 创建快照");
        return new TextMemento(content, cursorPos);
    }

    /**
     * 从备忘录恢复
     */
    public void restore(TextMemento memento) {
        if (memento != null) {
            this.content = memento.getContent();
            this.cursorPos = memento.getCursorPos();
            System.out.println("    [恢复] 从快照恢复");
        }
    }
}

/**
 * =====================================================================================
 * 负责人：编辑器历史管理器
 * =====================================================================================
 */
class EditorHistory {
    private List<TextMemento> history = new ArrayList<>();
    private int currentIndex = -1;

    /**
     * 保存备忘录
     */
    public void save(TextMemento memento) {
        // 如果当前不在历史末尾，删除后面的历史
        if (currentIndex < history.size() - 1) {
            history.subList(currentIndex + 1, history.size()).clear();
        }
        history.add(memento);
        currentIndex++;
    }

    /**
     * 撤销
     */
    public TextMemento undo() {
        if (currentIndex > 0) {
            currentIndex--;
            return history.get(currentIndex);
        }
        return null;
    }

    /**
     * 重做
     */
    public TextMemento redo() {
        if (currentIndex < history.size() - 1) {
            currentIndex++;
            return history.get(currentIndex);
        }
        return null;
    }
}
