# SPI 机制详解

## 一、什么是 SPI？

SPI（Service Provider Interface）是 Java 提供的一种**服务发现机制**，允许第三方实现接口并通过配置文件注册，实现动态扩展。

### 1.1 核心特点

✅ **解耦**：接口定义与实现分离  
✅ **可扩展**：无需修改源码即可添加新实现  
✅ **动态加载**：运行时通过 ServiceLoader 加载实现类

---

## 二、SPI 工作原理

### 2.1 工作流程

```
Step1: ServiceLoader.load(PaymentService.class)
Step2: 读取 META-INF/services/接口名 文件
Step3: 解析文件中的实现类全限定名
Step4: 通过反射实例化所有实现类
Step5: 返回迭代器遍历所有服务
```

### 2.2 示例结构

```
spi/
├── PaymentService.java              # SPI 接口定义
└── impl/
    ├── AlipayService.java           # 支付宝支付实现
    ├── WechatPayService.java        # 微信支付实现
    └── UnionPayService.java         # 银联支付实现

resources/META-INF/services/
└── cn.itzixiao.interview.transaction.spi.PaymentService  # SPI 配置文件
```

### 2.3 代码示例

#### （1）SPI 接口定义

```java
package cn.itzixiao.interview.transaction.spi;

public interface PaymentService {
    /**
     * 支付方法
     * @param orderId 订单号
     * @param amount 金额
     * @return 支付结果
     */
    String pay(String orderId, Double amount);
    
    /**
     * 获取支付方式名称
     */
    String getName();
}
```

#### （2）SPI 实现类

```java
package cn.itzixiao.interview.transaction.spi.impl;

import cn.itzixiao.interview.transaction.spi.PaymentService;

public class AlipayService implements PaymentService {
    @Override
    public String pay(String orderId, Double amount) {
        return "支付宝支付：[支付宝] 订单 " + orderId + " 支付成功，金额：￥" + amount;
    }
    
    @Override
    public String getName() {
        return "支付宝";
    }
}
```

#### （3）SPI 配置文件

```properties
# META-INF/services/cn.itzixiao.interview.transaction.spi.PaymentService
cn.itzixiao.interview.transaction.spi.impl.AlipayService
cn.itzixiao.interview.transaction.spi.impl.WechatPayService
cn.itzixiao.interview.transaction.spi.impl.UnionPayService
```

#### （4）使用 ServiceLoader 加载

```java
ServiceLoader<PaymentService> loader = ServiceLoader.load(PaymentService.class);
for (PaymentService service : loader) {
    System.out.println("支付方式：" + service.getName());
    String result = service.pay("ORDER001", 100.00);
    System.out.println(result);
}
```

---

## 三、SPI vs Spring IOC

| 特性 | SPI | Spring IOC |
|------|-----|-----------|
| 实现方式 | 配置文件 + ServiceLoader | 注解 + 容器管理 |
| 加载时机 | 延迟加载（使用时加载） | 启动时加载 |
| 依赖注入 | 无 | 支持 |
| 应用场景 | JDBC、日志框架 | Spring 应用 |

---

## 四、SPI 应用场景

- **JDBC 驱动**：DriverManager 加载各种数据库驱动
- **日志框架**：SLF4J 加载不同日志实现
- **Dubbo SPI**：扩展点加载
- **Spring Boot 自动装配**：spring.factories 文件

---

## 五、SPI 相关面试题

### 1. 什么是 SPI？工作原理是什么？

**参考答案：**

SPI（Service Provider Interface）是 Java 提供的一种服务发现机制。

**工作原理：**
1. 定义服务接口
2. 创建实现类
3. 在 `META-INF/services/接口全限定名` 文件中配置实现类
4. 通过 `ServiceLoader.load(接口.class)` 加载所有实现
5. ServiceLoader 读取配置文件，通过反射实例化所有实现类

### 2. SPI 的优缺点？

**优点：**
- ✅ 解耦：接口定义与实现分离
- ✅ 可扩展：无需修改源码即可添加新实现
- ✅ 动态加载：运行时加载实现类

**缺点：**
- ❌ 需要遍历所有实现，无法按需加载
- ❌ 延迟加载：第一次使用时才加载
- ❌ 无法注入依赖：实现类必须有无参构造

### 3. JDBC 是如何使用 SPI 的？

**参考答案：**

JDBC 通过 SPI 机制自动加载各种数据库驱动：

```java
// META-INF/services/java.sql.Driver 文件内容
com.mysql.cj.jdbc.Driver
org.postgresql.Driver
oracle.jdbc.driver.OracleDriver

// DriverManager 初始化时加载所有驱动
static {
    ServiceLoader<Driver> loadedDrivers = ServiceLoader.load(Driver.class);
    // 遍历并注册所有驱动
}
```

### 4. Spring Boot 自动装配与 SPI 的关系？

**参考答案：**

Spring Boot 的自动装配借鉴了 SPI 思想：

```properties
# spring.factories 文件（类似 SPI 配置）
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.example.MyAutoConfiguration
```

**区别：**
- SPI 使用 `META-INF/services/接口名`
- Spring Boot 使用 `META-INF/spring.factories`
- Spring Boot 支持条件装配（@Conditional）

---

## 六、API 接口测试

```bash
# SPI 机制演示
curl "http://localhost:8084/api/spi/demo?orderId=ORDER001&amount=100.00"

# SPI 机制详解
curl http://localhost:8084/api/spi/explain
```

**响应示例：**
```json
[
  "支付宝支付：[支付宝] 订单 ORDER001 支付成功，金额：￥100.00",
  "微信支付：[微信] 订单 ORDER001 支付成功，金额：￥100.00",
  "银联支付：[银联] 订单 ORDER001 支付成功，金额：￥100.00"
]
```

---

## 七、示例代码位置

```
interview-transaction-demo/
├── src/main/java/cn/itzixiao/interview/transaction/
│   ├── spi/                          # SPI 机制示例
│   │   ├── PaymentService.java       # SPI 接口
│   │   └── impl/
│   │       ├── AlipayService.java    # 支付宝实现
│   │       ├── WechatPayService.java # 微信实现
│   │       └── UnionPayService.java  # 银联实现
│   └── controller/
│       └── SpiController.java        # SPI 控制器
└── src/main/resources/
    └── META-INF/services/
        └── cn.itzixiao.interview.transaction.spi.PaymentService
```

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
