package cn.itzixiao.interview.xxljob.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 数据同步任务处理器
 * 演示业务场景：大数据量分片同步
 *
 * @author itzixiao
 * @date 2026-03-19
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSyncJobHandler {

    // 模拟服务依赖
    // private final UserService userService;
    // private final ElasticsearchService esService;

    /**
     * 用户数据同步到ES（分片执行）
     * 调度策略：每天凌晨2点执行
     * Cron表达式：0 0 2 * * ?
     * 执行策略：分片广播
     */
    @XxlJob("syncUserToEsJob")
    public void syncUserToEsJob() {
        // 获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("开始执行用户数据同步任务, 分片: {}/{}", shardIndex, shardTotal);
        log.info("执行用户数据同步任务, shardIndex={}, shardTotal={}", shardIndex, shardTotal);

        try {
            // 模拟总数据量
            long totalCount = 10000;
            long perShard = totalCount / shardTotal;
            long startOffset = shardIndex * perShard;
            long endOffset = (shardIndex == shardTotal - 1) ? totalCount : (shardIndex + 1) * perShard;

            XxlJobHelper.log("本分片处理数据范围: {} - {}", startOffset, endOffset);

            // 分页处理数据
            int pageSize = 1000;
            int pageNum = 0;
            int totalSync = 0;

            while (true) {
                // 模拟分页查询本分片数据
                int currentBatchSize = (int) Math.min(pageSize, endOffset - startOffset - totalSync);
                if (currentBatchSize <= 0) {
                    break;
                }

                // 模拟查询数据
                List<String> userIds = IntStream.range(0, currentBatchSize)
                        .mapToObj(i -> "USER_" + (startOffset + totalSync + i))
                        .collect(Collectors.toList());

                // 模拟批量同步到ES
                // esService.batchIndex(documents);

                totalSync += userIds.size();
                pageNum++;

                XxlJobHelper.log("已同步 {} 条数据", totalSync);

                // 避免内存溢出，每批处理后休眠
                if (pageNum % 10 == 0) {
                    Thread.sleep(1000);
                }

                // 模拟结束条件
                if (totalSync >= perShard) {
                    break;
                }
            }

            XxlJobHelper.log("数据同步完成, 共同步 {} 条", totalSync);
            log.info("用户数据同步完成, shardIndex={}, 同步数量={}", shardIndex, totalSync);

        } catch (Exception e) {
            XxlJobHelper.log("数据同步失败: {}", e.getMessage());
            log.error("数据同步任务异常", e);
            XxlJobHelper.handleFail("数据同步失败: " + e.getMessage());
        }
    }

    /**
     * 全量数据同步（不分片）
     * 调度策略：每周日凌晨3点执行
     * Cron表达式：0 0 3 ? * 1
     */
    @XxlJob("fullDataSyncJob")
    public void fullDataSyncJob() {
        XxlJobHelper.log("开始执行全量数据同步任务");
        log.info("开始执行全量数据同步任务");

        try {
            // 模拟全量数据同步
            int totalCount = 50000;
            int pageSize = 2000;
            int totalSync = 0;

            while (totalSync < totalCount) {
                int currentBatch = Math.min(pageSize, totalCount - totalSync);

                // 模拟查询和同步
                XxlJobHelper.log("正在同步第 {} 批数据, 本批 {} 条", (totalSync / pageSize) + 1, currentBatch);

                totalSync += currentBatch;

                // 每10批休眠一次
                if ((totalSync / pageSize) % 10 == 0) {
                    Thread.sleep(500);
                }
            }

            XxlJobHelper.log("全量数据同步完成, 共同步 {} 条", totalSync);
            log.info("全量数据同步完成, 共同步 {} 条", totalSync);

        } catch (Exception e) {
            XxlJobHelper.log("全量数据同步失败: {}", e.getMessage());
            log.error("全量数据同步任务异常", e);
            XxlJobHelper.handleFail("全量数据同步失败: " + e.getMessage());
        }
    }
}
