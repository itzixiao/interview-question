# 设计模式知识点详解

## 📚 文档列表

#### 1. [01-设计模式详解.md](./01-%E8%AE%BE%E8%AE%A1%E6%A8%A1%E5%BC%8F%E8%AF%A6%E8%A7%A3.md)
- **内容：** 23 种设计模式详解（创建型、结构型、行为型）
- **面试题：** 30+ 道
- **重要程度：** ⭐⭐⭐⭐

---

## 📊 统计信息

- **文档数：** 1 个
- **面试题总数：** 30+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/designpattern/` 目录（24 个文件，~8,000 行代码）

---

## 🎯 学习建议

### 创建型模式（2 天）
1. **单例模式** - 饿汉、饱汉、双重检查锁
2. **工厂模式** - 简单工厂、工厂方法、抽象工厂
3. **建造者模式** - 复杂对象构建
4. **原型模式** - 深拷贝 vs 浅拷贝
5. **代理模式** - JDK动态代理、CGLIB

### 结构型模式（2 天）
1. **适配器模式** - 接口转换
2. **装饰器模式** - 动态增强功能
3. **外观模式** - 统一接口
4. **组合模式** - 树形结构处理
5. **享元模式** - 对象池复用

### 行为型模式（2 天）
1. **策略模式** - 算法自由切换
2. **观察者模式** - 事件驱动
3. **模板方法模式** - 骨架实现
4. **责任链模式** - 多节点处理
5. **状态模式** - 状态驱动

---

## 🔗 跨模块关联

### 前置知识
- ✅ **[Java基础](../01-Java基础/README.md)** - 面向对象、接口、抽象类
- ✅ **[Spring框架](../04-Spring框架/README.md)** - IOC、AOP、Bean

### 后续进阶
- 📚 **[MyBatis](../09-中间件/README.md)** - 使用了多种设计模式
- 📚 **[SpringBoot](../05-SpringBoot 与自动装配/README.md)** - 启动流程中的设计模式

### 知识点对应
| 设计模式 | Spring 中的应用 |
|---------|---------------|
| 代理模式 | AOP 实现 |
| 单例模式 | Bean 默认单例 |
| 工厂模式 | BeanFactory |
| 模板方法 | JdbcTemplate |
| 观察者模式 | ApplicationEvent |
| 适配器模式 | HandlerAdapter |

---

## 💡 高频面试题 Top 15

1. **单例模式有几种实现方式？哪种最安全？**
2. **双重检查锁定的单例为什么要加 volatile？**
3. **工厂模式和抽象工厂的区别？**
4. **代理模式和装饰器模式的区别？**
5. **Spring 中用到了哪些设计模式？**
6. **JDK动态代理和 CGLIB 的区别？**
7. **观察者模式和发布订阅模式的区别？**
8. **策略模式的优缺点？**
9. **模板方法模式的应用场景？**
10. **责任链模式的实现原理？**
11. **Builder 模式和 Abstract Factory 的区别？**
12. **什么是迪米特法则？**
13. **开闭原则的理解？**
14. **依赖倒置原则的应用？**
15. **如何选择合适的创建型模式？**

---

## 🛠️ 实战技巧

### 双重检查锁定单例
```java
public class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

### 策略模式应用
```java
public interface PaymentStrategy {
    void pay(BigDecimal amount);
}

@Component
public class AlipayStrategy implements PaymentStrategy {
    public void pay(BigDecimal amount) {
        // 支付宝支付逻辑
    }
}

@Component
public class WechatPayStrategy implements PaymentStrategy {
    public void pay(BigDecimal amount) {
        // 微信支付逻辑
    }
}
```

---

## 📖 推荐学习顺序

```
单例模式（必会）
   ↓
工厂模式
   ↓
代理模式（重点）
   ↓
装饰器模式
   ↓
观察者模式
   ↓
策略模式
   ↓
其他模式
   ↓
综合实战
```

---

## 📈 更新日志

### v2.0 - 2026-03-08
- ✅ 新增跨模块关联章节
- ✅ 补充 30+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本
- ✅ 基础设计模式文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-08  
**问题反馈：** 欢迎提 Issue 或 PR
