package cn.itzixiao.interview.provider.service.sharding;

import cn.itzixiao.interview.provider.entity.DeviceOperationLog;
import cn.itzixiao.interview.provider.entity.es.DeviceOperationLogES;
import cn.itzixiao.interview.provider.mapper.DeviceOperationLogMapper;
import cn.itzixiao.interview.provider.repository.DeviceOperationLogESRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 数据加载服务
 */
@Slf4j
@Service
public class DeviceOperationLogESService {

    private final DeviceOperationLogMapper deviceOperationLogMapper;
    private final DeviceOperationLogESRepository esRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public DeviceOperationLogESService(DeviceOperationLogMapper deviceOperationLogMapper,
                                       DeviceOperationLogESRepository esRepository,
                                       ElasticsearchOperations elasticsearchOperations) {
        this.deviceOperationLogMapper = deviceOperationLogMapper;
        this.esRepository = esRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * 创建索引（如果不存在）
     */
    public void createIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(DeviceOperationLogES.class);

        // 检查索引是否存在
        if (!indexOps.exists()) {
            log.info("索引不存在，正在创建索引：device_operation_log");
            // 创建索引并设置映射
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping());
            log.info("索引创建成功");
        } else {
            log.info("索引已存在：device_operation_log");
        }
    }

    /**
     * 删除索引
     */
    public void deleteIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(DeviceOperationLogES.class);
        if (indexOps.exists()) {
            log.info("正在删除索引：device_operation_log");
            indexOps.delete();
            log.info("索引删除成功");
        } else {
            log.warn("索引不存在：device_operation_log");
        }
    }

    /**
     * 从 MySQL 加载全部数据到 ES
     *
     * @return 加载的记录数
     */
    public int loadAllDataToES() {
        log.info("===========================================");
        log.info("【ES 数据加载】开始从 MySQL 加载全部数据到 ES");
        log.info("===========================================");

        long startTime = System.currentTimeMillis();

        // 先确保索引存在
        createIndex();

        // 分批查询 MySQL 数据，避免一次性加载过多数据
        // ⚠️ 注意：在 ShardingSphere 环境下，使用 selectAll() 代替 selectPage()
        // 原因：ShardingSphere 的分页查询在分表环境下可能行为异常
        List<DeviceOperationLog> allData = deviceOperationLogMapper.selectAll();

        if (allData == null || allData.isEmpty()) {
            log.warn("【ES 数据加载】MySQL 中没有数据");
            return 0;
        }

        // 转换为 ES 实体
        List<DeviceOperationLogES> esEntities = convertToESEntities(allData);

        // 批量保存到 ES
        List<IndexQuery> indexQueries = new ArrayList<>();
        for (DeviceOperationLogES entity : esEntities) {
            // ⚠️ 关键修复：使用 deviceCode + operationTime 作为 ES 的唯一 ID
            // 原因：ShardingSphere 分表的自增 ID 会重复，直接使用会导致 ES 文档被覆盖
            String uniqueId = entity.getDeviceCode() + "_" +
                    entity.getOperationTime().replace("-", "").replace(" ", "").replace(":", "");

            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(uniqueId)
                    .withObject(entity)
                    .build();
            indexQueries.add(indexQuery);
        }

        // 批量索引
        elasticsearchOperations.bulkIndex(indexQueries,
                IndexCoordinates.of("device_operation_log"));

        log.info("【ES 数据加载】批次完成：共 {} 条，耗时 {}ms",
                allData.size(), System.currentTimeMillis() - startTime);

        log.info("【ES 数据加载】全部完成，总计 {}ms，加载 {} 条记录",
                System.currentTimeMillis() - startTime, allData.size());

        // 手动刷新索引，确保数据立即可搜索
        try {
            elasticsearchOperations.indexOps(DeviceOperationLogES.class).refresh();
            log.info("【ES 数据加载】索引已刷新，数据立即可搜索");
        } catch (Exception e) {
            log.error("【ES 数据加载】刷新索引失败", e);
        }

        return allData.size();
    }

    /**
     * 增量更新单条数据到 ES
     */
    public void updateSingleDataToES(Long id) {
        log.info("【ES 数据更新】正在更新单条数据到 ES，ID: {}", id);

        // 从 MySQL 查询数据
        DeviceOperationLog mysqlData = deviceOperationLogMapper.selectById(id);
        if (mysqlData == null) {
            log.warn("【ES 数据更新】MySQL 中未找到数据，ID: {}", id);
            return;
        }

        // 转换为 ES 实体
        DeviceOperationLogES esEntity = convertToESEntity(mysqlData);

        // 保存到 ES
        esRepository.save(esEntity);

        log.info("【ES 数据更新】更新成功，ID: {}", id);
    }

    /**
     * 从 ES 中删除单条数据
     */
    public void deleteSingleDataFromES(Long id) {
        log.info("【ES 数据删除】正在从 ES 删除数据，ID: {}", id);

        esRepository.deleteById(id);

        log.info("【ES 数据删除】删除成功，ID: {}", id);
    }

    /**
     * 批量转换实体
     */
    private List<DeviceOperationLogES> convertToESEntities(List<DeviceOperationLog> mysqlDataList) {
        List<DeviceOperationLogES> esEntities = new ArrayList<>();
        for (DeviceOperationLog mysqlData : mysqlDataList) {
            esEntities.add(convertToESEntity(mysqlData));
        }
        return esEntities;
    }

    /**
     * 转换单个实体
     */
    private DeviceOperationLogES convertToESEntity(DeviceOperationLog mysqlData) {
        DeviceOperationLogES esEntity = new DeviceOperationLogES();
        BeanUtils.copyProperties(mysqlData, esEntity);
        return esEntity;
    }

    /**
     * 获取 ES 索引统计信息
     */
    public Map<String, Object> getIndexStats() {
        log.info("【ES 统计】正在获取索引统计信息");

        try {
            // 获取文档总数
            long count = esRepository.count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("indexName", "device_operation_log");
            stats.put("documentCount", count);
            stats.put("health", "green"); // 简化实现，实际应该查询集群健康状态

            return stats;
        } catch (Exception e) {
            log.error("【ES 统计】获取统计信息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取统计信息失败：" + e.getMessage());
            errorResult.put("indexName", "device_operation_log");
            return errorResult;
        }
    }

    /**
     * 根据设备编号查询
     */
    public Page<DeviceOperationLogES> findByDeviceCode(String deviceCode, Pageable pageable) {
        log.info("【ES 搜索】根据设备编号查询：{}", deviceCode);
        return esRepository.findByDeviceCode(deviceCode, pageable);
    }

    /**
     * 根据操作类型查询
     */
    public Page<DeviceOperationLogES> findByOperationType(Integer operationType, Pageable pageable) {
        log.info("【ES 搜索】根据操作类型查询：{}", operationType);
        return esRepository.findByOperationType(operationType, pageable);
    }

    /**
     * 根据操作人查询
     */
    public Page<DeviceOperationLogES> findByOperator(String operator, Pageable pageable) {
        log.info("【ES 搜索】根据操作人查询：{}", operator);
        return esRepository.findByOperator(operator, pageable);
    }

    /**
     * 根据设备名称模糊查询（全文检索）
     */
    public Page<DeviceOperationLogES> searchByDeviceName(String deviceName, Pageable pageable) {
        log.info("【ES 搜索】根据设备名称模糊查询：{}", deviceName);
        return esRepository.searchByDeviceName(deviceName, pageable);
    }

    /**
     * 多字段组合查询
     */
    public Page<DeviceOperationLogES> searchByDeviceNameAndOperationType(
            String deviceName, Integer operationType, Pageable pageable) {
        log.info("【ES 搜索】组合查询 - 设备名称：{}, 操作类型：{}", deviceName, operationType);
        return esRepository.searchByDeviceNameAndOperationType(deviceName, operationType, pageable);
    }
}
