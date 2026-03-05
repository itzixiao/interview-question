# JDK 动态代理与 CGLIB 对比

## 概述

代理模式是一种结构型设计模式，允许通过代理对象控制对目标对象的访问。Java 中主要有两种动态代理实现：JDK 动态代理和 CGLIB。

## JDK 动态代理

### 原理

- 基于**接口**实现
- 在运行时生成代理类的字节码
- 代理类继承 `java.lang.reflect.Proxy`，实现目标接口

### 核心组件

| 组件 | 说明 |
|------|------|
| `Proxy` | 代理类的基类 |
| `InvocationHandler` | 调用处理器接口 |
| `newProxyInstance()` | 创建代理对象的方法 |

### 使用步骤

1. 定义接口
2. 实现 InvocationHandler
3. 使用 Proxy.newProxyInstance() 创建代理

### 代码示例

```java
InvocationHandler handler = (proxy, method, args) -> {
    System.out.println("前置处理");
    Object result = method.invoke(target, args);
    System.out.println("后置处理");
    return result;
};

MyInterface proxy = (MyInterface) Proxy.newProxyInstance(
    target.getClass().getClassLoader(),
    target.getClass().getInterfaces(),
    handler
);
```

## CGLIB

### 原理

- 基于**继承**实现
- 在运行时生成目标类的子类
- 通过字节码技术生成代理类

### 核心组件

| 组件 | 说明 |
|------|------|
| `Enhancer` | 增强器，用于创建代理 |
| `MethodInterceptor` | 方法拦截器接口 |
| `MethodProxy` | 方法代理，用于调用父类方法 |

### 使用步骤

1. 创建 Enhancer
2. 设置父类和回调
3. 创建代理对象

### 代码示例

```java
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(TargetClass.class);
enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
    System.out.println("前置处理");
    Object result = proxy.invokeSuper(obj, args);
    System.out.println("后置处理");
    return result;
});

TargetClass proxy = (TargetClass) enhancer.create();
```

## 对比总结

| 特性 | JDK 动态代理 | CGLIB |
|------|-------------|-------|
| 实现方式 | 实现接口 | 继承目标类 |
| 目标要求 | 必须实现接口 | 不能是 final 类/方法 |
| 调用方式 | 反射 Method.invoke() | FastClass 索引调用 |
| 性能 | 较慢 | 更快（首次生成慢）|
| 依赖 | JDK 内置 | 需引入 CGLIB 库 |
| 包访问 | 支持 | 不支持（继承限制）|

## Spring AOP 代理选择

### 选择规则

1. **目标类实现接口**：默认使用 JDK 动态代理
2. **目标类无接口**：使用 CGLIB 代理
3. **强制 CGLIB**：设置 `proxyTargetClass = true`

### 配置方式

```java
@EnableAspectJAutoProxy(proxyTargetClass = true)  // 强制 CGLIB
```

## 性能对比

| 场景 | 推荐方案 |
|------|----------|
| 有接口，性能要求不高 | JDK 动态代理 |
| 无接口 | CGLIB |
| 高性能要求 | CGLIB |
| 频繁创建代理 | CGLIB（缓存 FastClass）|

## 常见问题

### 1. JDK 代理类型转换问题

```java
// 错误：不能直接转为实现类
TargetImpl proxy = (TargetImpl) Proxy.newProxyInstance(...);  // ClassCastException

// 正确：只能转为接口
TargetInterface proxy = (TargetInterface) Proxy.newProxyInstance(...);
```

### 2. CGLIB 无法代理 final 方法

```java
public class Target {
    public final void finalMethod() {}  // 无法被代理
}
```

### 3. 自调用问题

```java
public class Target {
    public void methodA() {
        this.methodB();  // 自调用，不经过代理
    }
    
    public void methodB() {}
}
```

**解决方案**：
- 注入自身代理
- 使用 AspectJ 编译时织入

## 最佳实践

1. **优先使用接口**：面向接口编程
2. **合理选择代理方式**：根据场景选择 JDK 或 CGLIB
3. **注意 final 限制**：CGLIB 不能代理 final 类/方法
4. **避免自调用**：自调用不经过代理
