package cn.itzixiao.interview.transaction.circular;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
@Api(tags = "11-Spring 循环依赖演示 API", value = "循环依赖解决方案测试接口")
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
    @ApiOperation(value = "01-Setter 注入解决循环依赖", notes = "ServiceC 和 ServiceD 通过 Setter 注入互相依赖")
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
    @ApiOperation(value = "02-@PostConstruct 解决循环依赖", notes = "ServiceE 和 ServiceF 通过@Autowired + @PostConstruct 互相依赖")
    public void testPostConstruct() {
        log.info("===== 测试@PostConstruct 解决循环依赖 =====");
        log.info("调用 ServiceE.doSomething()");
        serviceE.doSomething();
        
        log.info("\n调用 ServiceF.doSomethingElse()");
        serviceF.doSomethingElse();
    }
}
