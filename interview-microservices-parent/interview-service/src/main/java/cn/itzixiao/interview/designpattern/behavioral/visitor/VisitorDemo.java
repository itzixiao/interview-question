package cn.itzixiao.interview.designpattern.behavioral.visitor;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================================
 * 访问者模式（Visitor Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 表示一个作用于某对象结构中的各元素的操作。它使你可以在不改变各元素的类的前提下
 * 定义作用于这些元素的新操作。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 访问者接口（Visitor）：为每种元素定义访问方法
 * 2. 具体访问者（Concrete Visitor）：实现具体的访问操作
 * 3. 元素接口（Element）：定义 accept 方法，接受访问者
 * 4. 具体元素（Concrete Element）：实现 accept 方法
 * 5. 对象结构（Object Structure）：存储元素集合
 * <p>
 * 三、双分派机制
 * -------------------------------------------------------------------------------------
 * 访问者模式使用了「双分派」机制：
 * 1. 第一分派：element.accept(visitor) - 根据 element 类型选择方法
 * 2. 第二分派：visitor.visit(this) - 根据 visitor 类型选择方法
 * <p>
 * 四、应用场景
 * -------------------------------------------------------------------------------------
 * - 编译器：语法树访问（类型检查、代码生成）
 * - 文档处理：导出不同格式（PDF、HTML、Word）
 */
public class VisitorDemo {

    public static void main(String[] args) {
        System.out.println("========== 访问者模式（Visitor Pattern）==========\n");

        System.out.println("【场景：文件系统导出】\n");

        // 创建文件结构
        ObjectStructure fileSystem = new ObjectStructure();
        fileSystem.addElement(new TextFile("readme.txt", 1024, "UTF-8"));
        fileSystem.addElement(new ImageFile("logo.png", 51200, "1920x1080"));
        fileSystem.addElement(new VideoFile("demo.mp4", 104857600, "00:05:30"));

        // 使用不同访问者导出
        System.out.println("【1. 文本格式导出】：");
        fileSystem.accept(new TextExportVisitor());

        System.out.println("\n【模式分析】：");
        System.out.println("  - 将数据结构与数据操作分离");
        System.out.println("  - 使用双分派机制实现类型匹配");
        System.out.println("  - 符合开闭原则：新增操作无需修改元素类");
        System.out.println("  - 缺点：新增元素类型需修改所有访问者");
    }
}

/**
 * 访问者接口
 */
interface FileVisitor {
    void visit(TextFile textFile);

    void visit(ImageFile imageFile);

    void visit(VideoFile videoFile);
}

/**
 * 元素接口
 */
interface FileElement {
    String getName();

    long getSize();

    void accept(FileVisitor visitor);
}

/**
 * 具体元素：文本文件
 */
class TextFile implements FileElement {
    private String name;
    private long size;
    private String encoding;

    public TextFile(String name, long size, String encoding) {
        this.name = name;
        this.size = size;
        this.encoding = encoding;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getEncoding() {
        return encoding;
    }

    @Override
    public void accept(FileVisitor visitor) {
        visitor.visit(this);
    }
}

/**
 * 具体元素：图片文件
 */
class ImageFile implements FileElement {
    private String name;
    private long size;
    private String resolution;

    public ImageFile(String name, long size, String resolution) {
        this.name = name;
        this.size = size;
        this.resolution = resolution;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getResolution() {
        return resolution;
    }

    @Override
    public void accept(FileVisitor visitor) {
        visitor.visit(this);
    }
}

/**
 * 具体元素：视频文件
 */
class VideoFile implements FileElement {
    private String name;
    private long size;
    private String duration;

    public VideoFile(String name, long size, String duration) {
        this.name = name;
        this.size = size;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getDuration() {
        return duration;
    }

    @Override
    public void accept(FileVisitor visitor) {
        visitor.visit(this);
    }
}

/**
 * 对象结构
 */
class ObjectStructure {
    private List<FileElement> elements = new ArrayList<>();

    public void addElement(FileElement element) {
        elements.add(element);
    }

    public void accept(FileVisitor visitor) {
        for (FileElement element : elements) {
            element.accept(visitor);
        }
    }
}

/**
 * 具体访问者：文本导出
 */
class TextExportVisitor implements FileVisitor {
    @Override
    public void visit(TextFile textFile) {
        System.out.println("    [文本] " + textFile.getName() +
                " | " + textFile.getSize() + "B | 编码:" + textFile.getEncoding());
    }

    @Override
    public void visit(ImageFile imageFile) {
        System.out.println("    [图片] " + imageFile.getName() +
                " | " + imageFile.getSize() + "B | " + imageFile.getResolution());
    }

    @Override
    public void visit(VideoFile videoFile) {
        System.out.println("    [视频] " + videoFile.getName() +
                " | " + videoFile.getSize() + "B | " + videoFile.getDuration());
    }
}
