# Java 反射与类加载机制

## 反射（Reflection）

### 概述

反射允许在运行时获取类的完整信息（构造器、方法、字段等），并动态创建对象、调用方法、修改字段。

### 获取 Class 对象

```java
// 方式1：类名.class
Class<String> clazz1 = String.class;

// 方式2：对象.getClass()
Class<?> clazz2 = "Hello".getClass();

// 方式3：Class.forName()
Class<?> clazz3 = Class.forName("java.lang.String");

// 三种方式获取的是同一个 Class 对象
System.out.println(clazz1 == clazz2);  // true
System.out.println(clazz2 == clazz3);  // true
```

### Class 对象方法

```java
// 类信息
String name = clazz.getName();           // 全限定名
String simpleName = clazz.getSimpleName(); // 简单名
Package pkg = clazz.getPackage();        // 包信息
Class<?> superClass = clazz.getSuperclass(); // 父类
Class<?>[] interfaces = clazz.getInterfaces(); // 接口

// 类型判断
boolean isInterface = clazz.isInterface();
boolean isArray = clazz.isArray();
boolean isPrimitive = clazz.isPrimitive();

// 修饰符
int modifiers = clazz.getModifiers();
boolean isPublic = Modifier.isPublic(modifiers);
```

### 构造器反射

```java
// 获取所有构造器
Constructor<?>[] constructors = clazz.getDeclaredConstructors();

// 获取指定构造器
Constructor<Person> constructor = clazz.getDeclaredConstructor(String.class, int.class);

// 创建对象
Person person = constructor.newInstance("张三", 25);

// 访问私有构造器
constructor.setAccessible(true);
```

### 方法反射

```java
// 获取所有方法
Method[] methods = clazz.getDeclaredMethods();

// 获取指定方法
Method method = clazz.getDeclaredMethod("setName", String.class);

// 调用方法
method.invoke(person, "李四");

// 访问私有方法
method.setAccessible(true);

// 调用静态方法
method.invoke(null, args);
```

### 字段反射

```java
// 获取所有字段
Field[] fields = clazz.getDeclaredFields();

// 获取指定字段
Field field = clazz.getDeclaredField("name");

// 读取字段值
field.setAccessible(true);
String name = (String) field.get(person);

// 修改字段值
field.set(person, "王五");

// 修改静态字段
field.set(null, value);
```

### 注解反射

```java
// 获取类上的注解
MyAnnotation annotation = clazz.getAnnotation(MyAnnotation.class);

// 获取方法上的注解
Method method = clazz.getMethod("methodName");
MyAnnotation methodAnno = method.getAnnotation(MyAnnotation.class);

// 判断是否有指定注解
boolean hasAnnotation = clazz.isAnnotationPresent(MyAnnotation.class);
```

### 动态代理

```java
// JDK 动态代理
Object proxy = Proxy.newProxyInstance(
    target.getClass().getClassLoader(),
    target.getClass().getInterfaces(),
    (proxyObj, method, args) -> {
        System.out.println("前置处理");
        Object result = method.invoke(target, args);
        System.out.println("后置处理");
        return result;
    }
);
```

### 反射性能

| 调用方式 | 相对性能 |
|----------|----------|
| 直接调用 | 1x |
| 反射调用 | 10-20x |
| 反射调用（setAccessible）| 5-10x |

**优化建议**：
- 使用 `setAccessible(true)` 关闭安全检查
- 缓存 Method/Field 对象
- 使用 MethodHandle（Java 7+）
- 使用 LambdaMetafactory（Java 8+）

## 类加载机制

### 类加载过程

```
加载 → 验证 → 准备 → 解析 → 初始化 → 使用 → 卸载
```

#### 1. 加载（Loading）

- 通过全限定名获取二进制字节流
- 将字节流转化为方法区的运行时数据结构
- 在堆中生成 Class 对象

#### 2. 验证（Verification）

- 文件格式验证
- 元数据验证
- 字节码验证
- 符号引用验证

#### 3. 准备（Preparation）

- 为类变量分配内存
- 设置零值（0、null、false）

```java
static int a = 123;  // 准备阶段 a = 0
```

#### 4. 解析（Resolution）

- 符号引用 → 直接引用

#### 5. 初始化（Initialization）

- 执行 `<clinit>()` 方法
- 类变量赋值
- 静态代码块执行

```java
static int a = 123;  // 初始化阶段 a = 123

static {
    // 静态代码块执行
}
```

### 类加载器

```
Bootstrap ClassLoader（启动类加载器）
    ↑ 委托
Extension ClassLoader（扩展类加载器）
    ↑ 委托
Application ClassLoader（应用程序类加载器）
    ↑ 委托
Custom ClassLoader（自定义类加载器）
```

| 类加载器 | 加载路径 | 实现语言 |
|----------|----------|----------|
| Bootstrap | $JAVA_HOME/lib | C++ |
| Extension | $JAVA_HOME/lib/ext | Java |
| Application | classpath | Java |
| Custom | 自定义 | Java |

### 双亲委派模型

**流程**：
1. 收到类加载请求
2. 委托给父类加载器
3. 父类加载器继续向上委托
4. 到达顶层（Bootstrap）
5. 从顶层开始尝试加载
6. 父类无法加载，子类才尝试加载

**优点**：
- 避免类的重复加载
- 保证核心 API 不被篡改（安全性）

**源码**：
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

### 打破双亲委派

**场景**：
- JDBC：SPI 机制
- Tomcat：Web 应用隔离
- OSGi：模块化热部署

**Tomcat 类加载器结构**：
```
Bootstrap
    ↑
Common
   /    \
Catalina  Shared
            ↑
        WebApp1  WebApp2
```

### 线程上下文类加载器

```java
// 获取
ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

// 设置
Thread.currentThread().setContextClassLoader(customLoader);
```

**用途**：
- 父类加载器请求子类加载器完成类加载
- 解决 SPI 问题

### 类卸载

**条件**：
1. 该类所有实例已被回收
2. 加载该类的 ClassLoader 已被回收
3. 该类对应的 Class 对象没有被引用

**场景**：
- Tomcat 热部署
- OSGi 模块卸载

## 最佳实践

1. **反射性能**：使用 setAccessible(true)，缓存反射对象
2. **类加载**：理解双亲委派，避免重复加载
3. **类卸载**：自定义类加载器实现热部署
4. **安全**：反射可访问私有成员，注意安全风险
