package cn.itzixiao.interview.designpattern.behavioral.iterator;

import java.util.NoSuchElementException;

/**
 * =====================================================================================
 * 迭代器模式（Iterator Pattern）
 * =====================================================================================
 * 
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 提供一种方法顺序访问一个聚合对象中各个元素，而又不需暴露该对象的内部表示。
 * 
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 迭代器接口（Iterator）：定义遍历和访问元素的接口
 * 2. 具体迭代器（Concrete Iterator）：实现迭代器接口
 * 3. 聚合接口（Aggregate）：定义创建迭代器对象的接口
 * 4. 具体聚合（Concrete Aggregate）：实现聚合接口，返回迭代器
 * 
 * 三、迭代器模式结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Iterator（迭代器接口）                                              │
 * │  ├── hasNext()               // 是否有下一个元素                   │
 * │  └── next()                  // 获取下一个元素                     │
 * │                                                                      │
 * │  Aggregate（聚合接口）                                               │
 * │  └── createIterator()        // 创建迭代器                         │
 * │                                                                      │
 * │  ConcreteAggregate（具体聚合）                                       │
 * │  └── implements Aggregate                                           │
 * │      └── createIterator() → new ConcreteIterator(this)             │
 * │                                                                      │
 * │  ConcreteIterator（具体迭代器）                                      │
 * │  └── implements Iterator                                            │
 * │      ├── aggregate           // 持有聚合对象引用                    │
 * │      └── index               // 当前位置索引                        │
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * 四、Java 中的迭代器
 * -------------------------------------------------------------------------------------
 * - java.util.Iterator<E>
 *   - boolean hasNext()
 *   - E next()
 *   - default void remove()
 * - java.lang.Iterable<E>
 *   - Iterator<E> iterator()
 *   - 支持 for-each 循环
 * 
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - Java 集合框架：List、Set、Map
 * - MyBatis：DefaultCursor 游标
 * - 数据库查询结果遍历
 */
public class IteratorDemo {

    public static void main(String[] args) {
        System.out.println("========== 迭代器模式（Iterator Pattern）==========\n");

        System.out.println("【场景：自定义集合的遍历】\n");

        // 创建自定义列表
        BookShelf bookShelf = new BookShelf(5);
        bookShelf.appendBook(new Book("《Java编程思想》"));
        bookShelf.appendBook(new Book("《设计模式》"));
        bookShelf.appendBook(new Book("《Effective Java》"));
        bookShelf.appendBook(new Book("《深入理解Java虚拟机》"));

        // 使用迭代器遍历
        System.out.println("【使用迭代器遍历】：");
        Iterator<Book> iterator = bookShelf.createIterator();
        while (iterator.hasNext()) {
            Book book = iterator.next();
            System.out.println("    " + book.getName());
        }

        // 使用 for-each 遍历（实现了 Iterable 接口）
        System.out.println("\n【使用 for-each 遍历】：");
        for (Book book : bookShelf) {
            System.out.println("    " + book.getName());
        }

        System.out.println("\n【Java 集合框架中的迭代器】：");
        System.out.println("  - List.iterator() 返回 ListIterator");
        System.out.println("  - Set.iterator() 返回迭代器");
        System.out.println("  - Map.entrySet().iterator() 返回 Entry 迭代器");

        System.out.println("\n【模式分析】：");
        System.out.println("  - 提供统一的遍历接口，无需了解集合内部结构");
        System.out.println("  - 将遍历职责分离到迭代器，符合单一职责原则");
        System.out.println("  - 支持不同遍历方式（正向、反向、跳过等）");
        System.out.println("  - 符合开闭原则：新增迭代器无需修改集合类");
        System.out.println("  - 可以同时进行多个遍历（每个迭代器独立）");
    }
}

/**
 * =====================================================================================
 * 书籍类
 * =====================================================================================
 */
class Book {
    private String name;

    public Book(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

/**
 * =====================================================================================
 * 迭代器接口（模拟 java.util.Iterator）
 * =====================================================================================
 */
interface Iterator<E> {
    boolean hasNext();
    E next();
}

/**
 * =====================================================================================
 * 聚合接口（模拟 java.lang.Iterable）
 * =====================================================================================
 */
interface Aggregate<E> extends Iterable<E> {
    Iterator<E> createIterator();
}

/**
 * =====================================================================================
 * 具体迭代器：书架迭代器
 * =====================================================================================
 */
class BookShelfIterator implements Iterator<Book> {
    private BookShelf bookShelf;
    private int index = 0;

    public BookShelfIterator(BookShelf bookShelf) {
        this.bookShelf = bookShelf;
    }

    @Override
    public boolean hasNext() {
        return index < bookShelf.getLength();
    }

    @Override
    public Book next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Book book = bookShelf.getBookAt(index);
        index++;
        return book;
    }
}

/**
 * =====================================================================================
 * 具体聚合：书架
 * =====================================================================================
 */
class BookShelf implements Aggregate<Book> {
    private Book[] books;
    private int last = 0;

    public BookShelf(int maxSize) {
        this.books = new Book[maxSize];
    }

    public Book getBookAt(int index) {
        return books[index];
    }

    public void appendBook(Book book) {
        this.books[last] = book;
        last++;
    }

    public int getLength() {
        return last;
    }

    @Override
    public Iterator<Book> createIterator() {
        return new BookShelfIterator(this);
    }

    /**
     * 实现 Iterable 接口的 iterator 方法
     * 使书架支持 for-each 循环
     */
    @Override
    public java.util.Iterator<Book> iterator() {
        return new java.util.Iterator<Book>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < last;
            }

            @Override
            public Book next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return books[index++];
            }
        };
    }
}
