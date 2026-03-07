package cn.itzixiao.interview.designpattern.structural.composite;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================================
 * 组合模式（Composite Pattern）
 * =====================================================================================
 * 
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 将对象组合成树形结构以表示「部分-整体」的层次结构。组合模式使得用户对单个对象
 * 和组合对象的使用具有一致性。
 * 
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 组件（Component）：叶子节点和组合节点的共同接口
 * 2. 叶子（Leaf）：没有子节点的最底层对象
 * 3. 组合（Composite）：包含子节点的对象，提供添加/删除子节点的方法
 * 
 * 三、树形结构示例
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │                        文件系统                                     │
 * │  ┌──────────────────────────────────────────────────────────────┐   │
 * │  │  根目录（Composite）                                          │   │
 * │  │  ├── 文件夹A（Composite）                                     │   │
 * │  │  │   ├── 文件1.txt（Leaf）                                    │   │
 * │  │  │   └── 文件2.txt（Leaf）                                    │   │
 * │  │  ├── 文件夹B（Composite）                                     │   │
 * │  │  │   └── 子文件夹（Composite）                                │   │
 * │  │  │       └── 文件3.txt（Leaf）                                │   │
 * │  │  └── 文件4.txt（Leaf）                                        │   │
 * │  └──────────────────────────────────────────────────────────────┘   │
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * 四、透明式 vs 安全式
 * -------------------------------------------------------------------------------------
 * 1. 透明式：Component 定义所有方法（包括 add/remove），客户端统一处理
 *    - 优点：客户端代码一致
 *    - 缺点：Leaf 需要实现无意义的 add/remove
 * 
 * 2. 安全式：Component 只定义公共方法，Composite 额外定义 add/remove
 *    - 优点：类型安全
 *    - 缺点：客户端需要区分 Leaf 和 Composite
 * 
 * 本示例使用透明式
 * 
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - 文件系统：文件和文件夹
 * - 组织架构：员工和部门
 * - 菜单系统：菜单项和子菜单
 * - GUI 组件：容器和组件
 */
public class CompositeDemo {

    public static void main(String[] args) {
        System.out.println("========== 组合模式（Composite Pattern）==========\n");

        System.out.println("【场景：文件系统目录结构】\n");

        // 创建文件系统结构
        System.out.println("创建文件系统结构：");

        // 根目录
        FileSystemComponent root = new Folder("根目录");

        // 文件夹A
        FileSystemComponent folderA = new Folder("文件夹A");
        folderA.add(new File("文件A1.txt", 1024));
        folderA.add(new File("文件A2.txt", 2048));

        // 文件夹B（包含子文件夹）
        FileSystemComponent folderB = new Folder("文件夹B");
        FileSystemComponent subFolder = new Folder("子文件夹");
        subFolder.add(new File("文件B1.txt", 512));
        folderB.add(subFolder);

        // 根目录下添加内容
        root.add(folderA);
        root.add(folderB);
        root.add(new File("文件C.txt", 4096));

        // 显示结构
        System.out.println("\n【文件系统结构】：\n");
        root.display(0);

        // 计算总大小
        System.out.println("\n【计算总大小】：");
        System.out.println("  总大小: " + root.getSize() + " 字节");

        // 统一操作演示
        System.out.println("\n【统一操作演示】：");
        System.out.println("  无论是文件还是文件夹，都可以调用 display() 和 getSize()");
        System.out.println("  客户端无需区分叶子节点和组合节点");

        System.out.println("\n【模式分析】：");
        System.out.println("  - 用树形结构表示「部分-整体」层次");
        System.out.println("  - 单个对象和组合对象被一致对待");
        System.out.println("  - 递归处理树形结构非常自然");
        System.out.println("  - 符合开闭原则：新增组件类型无需修改现有代码");
    }
}

/**
 * =====================================================================================
 * 组件接口（Component）
 * =====================================================================================
 * 定义叶子节点和组合节点的共同接口
 */
interface FileSystemComponent {
    /**
     * 显示组件
     * @param indent 缩进级别
     */
    void display(int indent);

    /**
     * 获取大小
     * @return 大小（字节）
     */
    int getSize();

    /**
     * 添加子组件（透明式：叶子节点需实现此方法，但抛出异常）
     */
    void add(FileSystemComponent component);

    /**
     * 移除子组件
     */
    void remove(FileSystemComponent component);
}

/**
 * =====================================================================================
 * 叶子节点（Leaf）：文件
 * =====================================================================================
 * 没有子节点的最底层对象
 */
class File implements FileSystemComponent {
    private String name;
    private int size;  // 字节

    public File(String name, int size) {
        this.name = name;
        this.size = size;
    }

    @Override
    public void display(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("    ");
        }
        System.out.println(sb + "📄 " + name + " (" + size + " 字节)");
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void add(FileSystemComponent component) {
        throw new UnsupportedOperationException("文件不支持添加子组件");
    }

    @Override
    public void remove(FileSystemComponent component) {
        throw new UnsupportedOperationException("文件不支持移除子组件");
    }
}

/**
 * =====================================================================================
 * 组合节点（Composite）：文件夹
 * =====================================================================================
 * 包含子节点的对象
 */
class Folder implements FileSystemComponent {
    private String name;
    // 存储子组件
    private List<FileSystemComponent> children = new ArrayList<>();

    public Folder(String name) {
        this.name = name;
    }

    @Override
    public void display(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("    ");
        }
        System.out.println(sb + "📁 " + name + "/");

        // 递归显示子组件
        for (FileSystemComponent child : children) {
            child.display(indent + 1);
        }
    }

    @Override
    public int getSize() {
        // 递归计算所有子组件的大小
        int totalSize = 0;
        for (FileSystemComponent child : children) {
            totalSize += child.getSize();
        }
        return totalSize;
    }

    @Override
    public void add(FileSystemComponent component) {
        children.add(component);
    }

    @Override
    public void remove(FileSystemComponent component) {
        children.remove(component);
    }
}
