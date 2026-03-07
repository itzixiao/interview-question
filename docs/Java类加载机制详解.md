# Java 类加载机制详解

## 一、类加载概述

**类加载**是将 .class 文件加载到 JVM 内存中，转换为运行时数据结构，并在堆中生成 Class 对象的过程。

## 二、类加载过程

```
加载 → 验证 → 准备 → 解析 → 初始化 → 使用 → 卸载
      |_______________|
          链接阶段
```

### 1. 加载（Loading）

- 通过全限定名获取二进制字节流
- 将字节流转化为方法区的运行时数据结构
- 在堆中生成 Class 对象，作为方法区数据的访问入口

**字节流来源**：
- 本地文件系统（.class 文件）
- 网络（Applet）
- 运行时计算生成（动态代理）
- ZIP/JAR 包
- 数据库

### 2. 验证（Verification）

确保 Class 文件的字节流符合规范，不危害 JVM：

| 验证类型 | 内容 |
|----------|------|
| 文件格式验证 | 魔数(0xCAFEBABE)、版本号、常量池 |
| 元数据验证 | 语义分析，是否有父类、是否实现接口 |
| 字节码验证 | 控制流、数据流分析，确保程序逻辑正确 |
| 符号引用验证 | 能否找到对应的类、方法、字段 |

### 3. 准备（Preparation）

为**类变量（static）**分配内存并设置**零值**：

```java
public static int value = 123;
// 准备阶段：value = 0
// 初始化阶段：value = 123
```

**零值表**：
| 类型 | 零值 |
|------|------|
| int | 0 |
| long | 0L |
| short | 0 |
| byte | 0 |
| char | '\u0000' |
| boolean | false |
| float | 0.0f |
| double | 0.0d |
| 引用类型 | null |

**特例**：final static 的编译期常量，在准备阶段就赋值
```java
public static final int CONST = 100; // 准备阶段 CONST = 100
```

### 4. 解析（Resolution）

将常量池中的**符号引用**替换为**直接引用**：

- **符号引用**：用一组符号描述引用目标（如类的全限定名）
- **直接引用**：内存中的实际地址（指针）

解析对象：
- 类或接口
- 字段
- 方法
- 接口方法

### 5. 初始化（Initialization）

执行类构造器 `<clinit>()` 方法：

- 收集类变量赋值动作和静态代码块
- 编译器自动生成
- JVM 保证多线程安全（只执行一次）
- 父类先于子类初始化

```java
public class Test {
    static int a = 1;      // 收集
    static {
        a = 2;             // 收集
    }
    static int b = a;      // 收集
}
// <clinit>() 执行顺序：a=1 → a=2 → b=a(2)
```

## 三、类加载器

### 1. 类加载器层次

```
Bootstrap ClassLoader（启动类加载器）
    ↑ 委托
Extension ClassLoader（扩展类加载器）
    ↑ 委托
Application ClassLoader（应用程序类加载器）
    ↑ 委托
Custom ClassLoader（自定义类加载器）
```

| 类加载器 | 加载路径 | 实现语言 | Java 中的表现 |
|----------|----------|----------|---------------|
| Bootstrap | $JAVA_HOME/lib | C++ | null |
| Extension | $JAVA_HOME/lib/ext | Java | ExtClassLoader |
| Application | classpath | Java | AppClassLoader |
| Custom | 自定义 | Java | 用户自定义 |

### 2. 验证类加载器

```java
// String 由 Bootstrap 加载
System.out.println(String.class.getClassLoader());  // null

// 自定义类由 Application 加载
System.out.println(MyClass.class.getClassLoader());
// sun.misc.Launcher$AppClassLoader@xxx
```

## 四、双亲委派模型

### 1. 工作流程

1. 收到类加载请求
2. 检查该类是否已加载
3. 委托给父类加载器
4. 父类加载器继续向上委托，直到 Bootstrap
5. 从顶层开始尝试加载
6. 父类无法加载，子类才尝试加载

### 2. 核心源码

```java
protected Class<?> loadClass(String name, boolean resolve) {
    // 1. 检查是否已加载
    Class<?> c = findLoadedClass(name);
    
    if (c == null) {
        // 2. 委托父类加载
        if (parent != null) {
            c = parent.loadClass(name, false);
        } else {
            c = findBootstrapClassOrNull(name);
        }
        
        // 3. 父类无法加载，自己加载
        if (c == null) {
            c = findClass(name);
        }
    }
    
    return c;
}
```

### 3. 优点

1. **避免重复加载**：父加载器加载过的类，子加载器不会再加载
2. **保证安全性**：核心 API 不会被篡改

**安全性示例**：
即使自定义 java.lang.String，也不会被加载，因为 Bootstrap 会先加载 rt.jar 中的 String。

## 五、打破双亲委派

### 1. 打破方式

重写 `loadClass()` 方法，改变委派逻辑：

```java
public Class<?> loadClass(String name) {
    // 先尝试自己加载
    Class<?> clazz = findClass(name);
    if (clazz == null) {
        // 自己加载失败，再委托父类
        clazz = parent.loadClass(name);
    }
    return clazz;
}
```

### 2. 打破场景

| 场景 | 原因 | 解决方案 |
|------|------|----------|
| JDBC SPI | 接口在 rt.jar，实现在 classpath | 线程上下文类加载器 |
| Tomcat | Web 应用隔离 | WebAppClassLoader |
| OSGi | 模块化热部署 | 每个 Bundle 独立类加载器 |
| Spring Boot DevTools | 热重载 | RestartClassLoader |

### 3. Tomcat 类加载器结构

```
Bootstrap
    ↑
System
    ↑
Common（Tomcat 和所有应用共享）
   / \
Catalina  Shared
            ↑
       WebApp1  WebApp2  WebApp3
```

**特点**：
- 每个 Web 应用有独立的 WebAppClassLoader
- 同一个类可以加载不同版本
- 实现应用间隔离

## 六、线程上下文类加载器

### 1. 什么是 TCCL？

Thread Context ClassLoader，绑定在线程上的类加载器。

### 2. 为什么需要？

解决 SPI 问题：
- JDBC 接口在 rt.jar（Bootstrap 加载）
- MySQL 驱动在 classpath（Application 加载）
- Bootstrap 无法加载 Application 下的类
- 通过 TCCL 获取 Application ClassLoader

### 3. 使用方式

```java
// 获取
ClassLoader tccl = Thread.currentThread().getContextClassLoader();

// 设置
Thread.currentThread().setContextClassLoader(customLoader);
```

### 4. ServiceLoader 示例

```java
// JDBC 通过 ServiceLoader 加载驱动
ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class);
// 内部使用 Thread.currentThread().getContextClassLoader()
```

## 七、类的初始化时机

### 1. 主动引用（会触发初始化）

1. **new 实例化对象**
2. **访问静态变量**（final 常量除外）
3. **调用静态方法**
4. **反射调用**（Class.forName）
5. **初始化子类时**，父类先初始化
6. **main() 方法所在类**

### 2. 被动引用（不会触发初始化）

1. 通过子类引用父类的静态字段
2. 定义数组类型（如 SuperClass[]）
3. 引用常量（编译期存入常量池）
4. ClassLoader.loadClass() 只加载不初始化

**示例**：
```java
class Parent {
    static { System.out.println("Parent 初始化"); }
    public static int value = 123;
}

class Child extends Parent {
    static { System.out.println("Child 初始化"); }
}

// 被动引用：通过子类访问父类静态字段
System.out.println(Child.value);
// 输出：Parent 初始化
//       123
// 不会输出：Child 初始化
```

## 八、类的卸载

### 1. 卸载条件

三个条件**同时满足**：
1. 该类的所有实例已被 GC 回收
2. 加载该类的 ClassLoader 已被 GC 回收
3. 该类的 Class 对象没有被引用

### 2. 注意事项

- **Bootstrap 加载的类不会被卸载**
- 自定义类加载器加载的类可以卸载
- JVM 规范不强制要求实现类卸载

### 3. 应用场景

- Tomcat 热部署
- OSGi 模块卸载
- 动态代理类清理

---

## 高频面试题

### Q1：什么是类加载器？有哪几种？

**类加载器**：负责将 .class 文件加载到 JVM 内存中，生成 Class 对象。

**四种类加载器**：
| 类加载器 | 加载路径 | 实现 |
|----------|----------|------|
| Bootstrap | $JAVA_HOME/lib | C++ |
| Extension | $JAVA_HOME/lib/ext | Java |
| Application | classpath | Java |
| Custom | 自定义 | Java |

### Q2：双亲委派模型是什么？为什么要用？

**双亲委派**：收到加载请求时，先委托父加载器加载，父加载器无法加载时才自己加载。

**好处**：
1. **避免重复加载**：父加载器加载过的类不会重复加载
2. **保证安全性**：防止核心 API 被篡改

### Q3：如何打破双亲委派模型？

重写 `loadClass()` 方法，先自己加载，失败再委托：

**典型场景**：
- JDBC：SPI 机制，使用线程上下文类加载器
- Tomcat：Web 应用隔离，每个应用独立类加载器
- OSGi：模块化热部署

### Q4：Class.forName() 与 ClassLoader.loadClass() 的区别？

| 方式 | 是否初始化 | 用途 |
|------|-----------|------|
| Class.forName(name) | ✔ | JDBC 加载驱动 |
| Class.forName(name, false, loader) | ✖ | 延迟初始化 |
| ClassLoader.loadClass() | ✖ | Spring 懒加载 |

### Q5：线程上下文类加载器是什么？为什么需要？

**TCCL**：绑定在线程上的类加载器。

**用途**：解决 SPI 问题
- 接口由 Bootstrap 加载
- 实现由 Application 加载
- Bootstrap 无法加载 Application 下的类
- 通过 TCCL 获取 Application ClassLoader

### Q6：类的卸载条件是什么？

三个条件**同时满足**：
1. 该类的所有实例已被 GC 回收
2. 加载该类的 ClassLoader 已被 GC 回收
3. 该类的 Class 对象没有被引用

**注意**：Bootstrap 加载的类不会被卸载。

### Q7：准备阶段和初始化阶段有什么区别？

| 阶段 | 操作 | 示例 |
|------|------|------|
| 准备 | 分配内存 + 零值 | static int a = 0 |
| 初始化 | 真正赋值 | static int a = 123 |

**特例**：final static 的编译期常量，在准备阶段就赋值。

### Q8：什么情况下会触发类的初始化？

**主动引用（会触发）**：
1. new 实例化对象
2. 访问静态变量（final 常量除外）
3. 调用静态方法
4. 反射调用
5. 初始化子类时，父类先初始化
6. main() 方法所在类

**被动引用（不会触发）**：
1. 通过子类引用父类静态字段
2. 定义数组类型
3. 引用常量

### Q9：Tomcat 为什么要打破双亲委派？

**目的**：Web 应用隔离

**实现**：每个 Web 应用有独立的 WebAppClassLoader

**好处**：
- 同一个类可以加载不同版本
- 不同应用互不影响
- 支持热部署

### Q10：自定义类加载器怎么实现？

继承 ClassLoader，重写 findClass() 方法：

```java
public class MyClassLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] classData = loadClassData(name);  // 从文件/网络加载字节码
        if (classData == null) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, classData, 0, classData.length);
    }
    
    private byte[] loadClassData(String name) {
        // 自定义加载逻辑
    }
}
```

**注意**：重写 findClass() 保持双亲委派；重写 loadClass() 可以打破双亲委派。

---

## 最佳实践

1. **理解双亲委派**：知道类是被哪个加载器加载的
2. **谨慎打破双亲委派**：除非有明确需求
3. **使用线程上下文类加载器**：解决 SPI 问题
4. **自定义类加载器**：实现热部署、加密类、隔离
5. **注意类卸载条件**：及时释放资源
