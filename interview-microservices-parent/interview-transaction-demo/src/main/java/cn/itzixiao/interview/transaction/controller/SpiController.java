package cn.itzixiao.interview.transaction.controller;

import cn.itzixiao.interview.transaction.spi.PaymentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * SPI 机制演示控制器
 * 
 * SPI（Service Provider Interface）是 Java 提供的服务发现机制
 * 
 * 核心原理：
 * 1. 定义接口：PaymentService
 * 2. 创建实现类：AlipayService、WechatPayService、UnionPayService
 * 3. 配置文件：META-INF/services/接口全限定名
 * 4. 动态加载：通过 ServiceLoader 加载所有实现
 * 
 * 工作流程：
 * - ServiceLoader 读取 META-INF/services 下的配置文件
 * - 解析文件中配置的实现类全限定名
 * - 通过反射实例化所有实现类
 * - 提供迭代器遍历所有服务实现
 * 
 * @author itzixiao
 * @since 2026-03-14
 */
@Slf4j
@RestController
@RequestMapping("/api/spi")
@Api(tags = "SPI 机制演示")
public class SpiController {
    
    @GetMapping("/demo")
    @ApiOperation("SPI 机制演示")
    public List<String> spiDemo(@RequestParam(defaultValue = "ORDER001") String orderId,
                                @RequestParam(defaultValue = "100.00") double amount) {
        log.info("========== SPI 机制演示开始 ==========");
        
        List<String> results = new ArrayList<>();
        
        // 使用 ServiceLoader 加载所有 PaymentService 实现
        ServiceLoader<PaymentService> serviceLoader = ServiceLoader.load(PaymentService.class);
        
        // 遍历所有实现并调用
        for (PaymentService paymentService : serviceLoader) {
            String paymentName = paymentService.getPaymentName();
            String payResult = paymentService.pay(orderId, amount);
            
            log.info("支付方式：{}", paymentName);
            log.info("支付结果：{}", payResult);
            
            results.add(paymentName + ": " + payResult);
        }
        
        log.info("========== SPI 机制演示结束 ==========");
        return results;
    }
    
    @GetMapping("/explain")
    @ApiOperation("SPI 机制详解")
    public String spiExplain() {
        StringBuilder explanation = new StringBuilder();
        explanation.append("=== SPI 机制详解 ===\n\n");
        
        explanation.append("1. 什么是 SPI？\n");
        explanation.append("   SPI（Service Provider Interface）是一种服务发现机制\n");
        explanation.append("   允许第三方实现接口并通过配置文件注册，实现动态扩展\n\n");
        
        explanation.append("2. 核心组件：\n");
        explanation.append("   - 接口定义：PaymentService\n");
        explanation.append("   - 实现类：AlipayService、WechatPayService、UnionPayService\n");
        explanation.append("   - 配置文件：META-INF/services/cn.itzixiao.interview.transaction.spi.PaymentService\n");
        explanation.append("   - 加载工具：java.util.ServiceLoader\n\n");
        
        explanation.append("3. 工作原理：\n");
        explanation.append("   Step1: ServiceLoader.load(PaymentService.class)\n");
        explanation.append("   Step2: 读取 META-INF/services/接口名 文件\n");
        explanation.append("   Step3: 解析文件中的实现类全限定名\n");
        explanation.append("   Step4: 通过反射实例化所有实现类\n");
        explanation.append("   Step5: 返回迭代器遍历所有服务\n\n");
        
        explanation.append("4. 应用场景：\n");
        explanation.append("   - JDBC 驱动：DriverManager 加载各种数据库驱动\n");
        explanation.append("   - 日志框架：SLF4J 加载不同日志实现\n");
        explanation.append("   - Dubbo SPI：扩展点加载\n");
        explanation.append("   - Spring Boot 自动装配：spring.factories 文件\n\n");
        
        explanation.append("5. 优缺点：\n");
        explanation.append("   优点：解耦、可扩展、动态加载\n");
        explanation.append("   缺点：需要遍历所有实现、延迟加载、无法按需加载\n\n");
        
        explanation.append("6. 访问示例：\n");
        explanation.append("   GET /api/spi/demo?orderId=ORDER001&amount=100.00\n");
        
        return explanation.toString();
    }
}
