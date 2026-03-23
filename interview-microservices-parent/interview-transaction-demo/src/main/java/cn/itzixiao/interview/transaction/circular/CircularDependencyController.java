package cn.itzixiao.interview.transaction.circular;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 循环依赖演示控制器
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Slf4j
@RestController
@RequestMapping("/api/circular")
public class CircularDependencyController {

    @Resource
    private ServiceC serviceC;

    @Resource
    private ServiceD serviceD;

    @Resource
    private ServiceE serviceE;

    @Resource
    private ServiceF serviceF;

    /**
     * 测试 Setter 注入解决循环依赖
     */
    @GetMapping("/setter")
    public void testSetterInjection() {
        log.info("===== 测试 Setter 注入解决循环依赖 =====");
        log.info("调用 ServiceC.doSomething()");
        serviceC.doSomething();

        log.info("\n调用 ServiceD.doSomethingElse()");
        serviceD.doSomethingElse();
    }

    /**
     * 测试 @PostConstruct 解决循环依赖
     */
    @GetMapping("/postconstruct")
    public void testPostConstruct() {
        log.info("===== 测试@PostConstruct 解决循环依赖 =====");
        log.info("调用 ServiceE.doSomething()");
        serviceE.doSomething();

        log.info("\n调用 ServiceF.doSomethingElse()");
        serviceF.doSomethingElse();
    }
}
