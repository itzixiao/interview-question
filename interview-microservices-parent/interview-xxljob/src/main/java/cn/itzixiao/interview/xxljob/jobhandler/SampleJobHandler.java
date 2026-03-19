package cn.itzixiao.interview.xxljob.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * XXL-JOB 示例任务处理器
 *
 * @author itzixiao
 * @date 2026-03-19
 */
@Component
@Slf4j
public class SampleJobHandler {

    /**
     * 简单示例任务
     * 在调度中心配置：执行器选择 interview-xxljob，JobHandler 填写 demoJobHandler
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        XxlJobHelper.log("XXL-JOB, Hello World.");
        log.info("执行示例任务: demoJobHandler");

        for (int i = 0; i < 5; i++) {
            XxlJobHelper.log("任务执行中，第 {} 次心跳", i);
            TimeUnit.SECONDS.sleep(2);
        }

        XxlJobHelper.log("示例任务执行完成");
    }

    /**
     * 带参数的任务
     * 在调度中心配置任务参数，如：{"name":"test","count":10}
     */
    @XxlJob("paramJobHandler")
    public void paramJobHandler() throws Exception {
        // 获取任务参数
        String param = XxlJobHelper.getJobParam();
        XxlJobHelper.log("任务参数: {}", param);
        log.info("执行任务: paramJobHandler, 参数: {}", param);

        // 业务逻辑处理
        if (param != null && !param.trim().isEmpty()) {
            // 解析参数并执行业务逻辑
            XxlJobHelper.log("解析参数并执行...");
        }

        XxlJobHelper.log("带参数任务执行完成");
    }

    /**
     * 分片任务示例
     * 执行策略选择：分片广播
     * 路由策略选择：SHARDING_BROADCAST
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("分片参数: 当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);
        log.info("执行分片任务: shardIndex={}, shardTotal={}", shardIndex, shardTotal);

        // 模拟业务处理：按分片处理数据
        // 实际场景中，可以处理 ID % shardTotal == shardIndex 的数据
        for (int i = 0; i < 10; i++) {
            if (i % shardTotal == shardIndex) {
                XxlJobHelper.log("分片 {} 处理数据: {}", shardIndex, i);
            }
        }

        XxlJobHelper.log("分片任务执行完成");
    }

    /**
     * 生命周期任务示例
     * init: 任务初始化方法
     * destroy: 任务销毁方法
     */
    @XxlJob(value = "lifecycleJobHandler", init = "init", destroy = "destroy")
    public void lifecycleJobHandler() throws Exception {
        XxlJobHelper.log("执行生命周期任务...");
        log.info("执行生命周期任务");

        // 执行业务逻辑
        TimeUnit.SECONDS.sleep(3);

        XxlJobHelper.log("生命周期任务执行完成");
    }

    public void init() {
        log.info("任务初始化: lifecycleJobHandler");
    }

    public void destroy() {
        log.info("任务销毁: lifecycleJobHandler");
    }
}
