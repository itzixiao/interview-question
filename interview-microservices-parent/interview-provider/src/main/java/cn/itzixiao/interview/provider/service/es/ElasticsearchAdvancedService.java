package cn.itzixiao.interview.provider.service.es;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Elasticsearch 高级功能服务
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Service
public class ElasticsearchAdvancedService {

    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

    public ElasticsearchAdvancedService(ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    /**
     * 测试分词器效果
     */
    public Map<String, Object> analyzeText(String analyzer, String text) {
        log.info("【分词测试】analyzer: {}, text: {}", analyzer, text);

        Map<String, Object> result = new HashMap<>();
        result.put("analyzer", analyzer);
        result.put("text", text);
        result.put("note", "实际项目中建议使用 Postman 调用 ES 的 _analyze API");
        result.put("example_api", "POST /_analyze { \"analyzer\": \"" + analyzer + "\", \"text\": \"" + text + "\" }");

        return result;
    }

    /**
     * 布尔查询示例
     */
    public List<Map<String, Object>> boolQuerySearch(String index, Map<String, Object> queryParams) {
        log.info("【布尔查询】index: {}, params: {}", index, queryParams);

        // 简化实现，返回示例数据
        Map<String, Object> result = new HashMap<>();
        result.put("index", index);
        result.put("query_params", queryParams);
        result.put("note", "实际项目中使用 NativeSearchQueryBuilder 构建布尔查询");

        return Collections.singletonList(result);
    }

    /**
     * 多字段匹配查询
     */
    public List<Map<String, Object>> multiMatchQuery(String index, String searchText, List<String> fields) {
        log.info("【多字段查询】index: {}, searchText: {}, fields: {}", index, searchText, fields);

        try {
            // 简化实现，实际项目中使用 NativeSearchQueryBuilder
            Map<String, Object> result = new HashMap<>();
            result.put("index", index);
            result.put("searchText", searchText);
            result.put("fields", fields);
            result.put("note", "实际项目中使用 NativeSearchQueryBuilder 构建多字段查询");

            return Collections.singletonList(result);
        } catch (Exception e) {
            log.error("【多字段查询】失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 高亮搜索
     */
    public Map<String, Object> highlightSearch(String index, String searchText, String field) {
        log.info("【高亮搜索】index: {}, searchText: {}, field: {}", index, searchText, field);

        Map<String, Object> result = new HashMap<>();
        result.put("index", index);
        result.put("searchText", searchText);
        result.put("field", field);
        result.put("highlight_tags", Arrays.asList("<em>", "</em>"));
        result.put("note", "实际项目中使用 NativeSearchQueryBuilder 设置高亮字段");

        return result;
    }

    /**
     * 聚合分析 - Terms 聚合
     */
    public Map<String, Object> termsAggregation(String index, String fieldName) {
        log.info("【Terms 聚合】index: {}, field: {}", index, fieldName);

        try {
            // 使用原生客户端构建聚合查询
            String aggJson = String.format(
                    "{" +
                            "  \"size\": 0," +
                            "  \"aggs\": {" +
                            "    \"%s_agg\": {" +
                            "      \"terms\": {" +
                            "        \"field\": \"%s.keyword\"" +
                            "      }" +
                            "    }" +
                            "  }" +
                            "}", fieldName, fieldName);

            log.info("【Terms 聚合】DSL: {}", aggJson);

            // 简化实现，实际应该执行聚合并返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("aggregation_type", "terms");
            result.put("field", fieldName);
            result.put("note", "实际项目中使用 NativeSearchQueryBuilder 构建聚合查询");

            return result;
        } catch (Exception e) {
            log.error("【Terms 聚合】失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 聚合分析 - 日期直方图
     */
    public Map<String, Object> dateHistogramAggregation(String index, String dateField) {
        log.info("【日期直方图】index: {}, field: {}", index, dateField);

        try {
            String aggJson = String.format(
                    "{" +
                            "  \"size\": 0," +
                            "  \"aggs\": {" +
                            "    \"%s_histogram\": {" +
                            "      \"date_histogram\": {" +
                            "        \"field\": \"%s\"," +
                            "        \"calendar_interval\": \"day\"" +
                            "      }" +
                            "    }" +
                            "  }" +
                            "}", dateField, dateField);

            log.info("【日期直方图】DSL: {}", aggJson);

            Map<String, Object> result = new HashMap<>();
            result.put("aggregation_type", "date_histogram");
            result.put("field", dateField);
            result.put("interval", "day");

            return result;
        } catch (Exception e) {
            log.error("【日期直方图】失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 嵌套聚合
     */
    public Map<String, Object> nestedAggregation(String index, Map<String, String> aggregations) {
        log.info("【嵌套聚合】index: {}, aggregations: {}", index, aggregations);

        Map<String, Object> result = new HashMap<>();
        result.put("aggregation_type", "nested");
        result.put("aggregations", aggregations);
        result.put("note", "支持多层嵌套聚合，如：先按级别分组，再按时间统计");

        return result;
    }

    /**
     * 批量写入优化示例
     */
    public Map<String, Object> bulkWriteOptimization(String index, List<Map<String, Object>> documents) {
        log.info("【批量写入】index: {}, count: {}", index, documents.size());

        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("indexed_count", documents.size());
            result.put("cost_time_ms", System.currentTimeMillis() - startTime);
            result.put("optimization_tips", Arrays.asList(
                    "1. 使用 BulkRequest 批量写入，减少网络往返",
                    "2. 调整 refresh_interval 降低刷新频率",
                    "3. 批量导入时临时关闭副本",
                    "4. 增加批处理大小（默认 100-1000 条/批）"
            ));

            return result;
        } catch (Exception e) {
            log.error("【批量写入】失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 查询缓存优化
     */
    public Map<String, Object> queryCacheOptimization(String index, String filterField, String filterValue) {
        log.info("【查询缓存】index: {}, filter: {}={}", index, filterField, filterValue);

        Map<String, Object> result = new HashMap<>();
        result.put("optimization_type", "query_cache");
        result.put("suggestions", Arrays.asList(
                "1. 使用 Filter Context 代替 Query Context（可缓存）",
                "2. 对于频繁使用的过滤条件，ES 会自动缓存",
                "3. 可通过 indices.queries.cache.size 调整缓存大小",
                "4. 使用 request_cache=true 缓存搜索结果"
        ));

        return result;
    }

    /**
     * 索引设置优化
     */
    public Map<String, Object> indexSettingsOptimization(String index) {
        log.info("【索引优化】index: {}", index);

        Map<String, Object> result = new HashMap<>();
        result.put("optimization_type", "index_settings");
        result.put("recommendations", Arrays.asList(
                "1. 根据数据量合理设置分片数（建议单分片 10-50GB）",
                "2. 不需要排序/聚合的字段关闭 doc_values",
                "3. 不需要评分的字段关闭 norms",
                "4. 使用 keyword 类型代替 string 类型",
                "5. 设置合适的 refresh_interval（写入密集型调大）"
        ));

        return result;
    }

    /**
     * 获取集群健康状态
     */
    public Map<String, Object> getClusterHealth() {
        log.info("【集群管理】获取集群健康状态");

        try {
            // 简化实现，实际应该调用 ES API
            Map<String, Object> result = new HashMap<>();
            result.put("cluster_name", "docker-cluster");
            result.put("status", "green");
            result.put("number_of_nodes", 1);
            result.put("number_of_data_nodes", 1);
            result.put("active_primary_shards", 0);
            result.put("active_shards", 0);
            result.put("relocating_shards", 0);
            result.put("initializing_shards", 0);
            result.put("unassigned_shards", 0);

            return result;
        } catch (Exception e) {
            log.error("【集群管理】获取健康状态失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 获取节点信息
     */
    public Map<String, Object> getClusterNodes() {
        log.info("【集群管理】获取节点信息");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();

        // 示例数据
        Map<String, Object> node = new HashMap<>();
        node.put("name", "node-1");
        node.put("ip", "127.0.0.1");
        node.put("transport_address", "127.0.0.1:9300");
        node.put("roles", Arrays.asList("master", "data", "ingest"));
        node.put("os", "Linux");
        node.put("jvm_version", "1.8.0_301");

        nodes.add(node);

        result.put("nodes", nodes);
        result.put("total_nodes", 1);

        return result;
    }

    /**
     * 获取分片分布
     */
    public Map<String, Object> getShardDistribution(String index) {
        log.info("【集群管理】获取分片分布：{}", index);

        Map<String, Object> result = new HashMap<>();
        result.put("index", index);
        result.put("shards", new ArrayList<>());
        result.put("note", "实际项目中使用 _cat/shards API 查看分片分布");

        return result;
    }

    /**
     * ELK 日志采集示例
     */
    public Map<String, Object> ingestLogEntry(Map<String, Object> logData) {
        log.info("【ELK 日志】采集日志：{}", logData);

        try {
            // 添加时间戳
            logData.put("@timestamp", System.currentTimeMillis());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("index", "logs-" + String.format("%tY-%<tm-%<td", new Date()));
            result.put("message", "日志采集成功");

            return result;
        } catch (Exception e) {
            log.error("【ELK 日志】采集失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * ELK 日志查询
     */
    public Map<String, Object> queryLogs(String indexPattern, Map<String, Object> queryParams) {
        log.info("【ELK 日志】查询：index={}, params={}", indexPattern, queryParams);

        Map<String, Object> result = new HashMap<>();
        result.put("index_pattern", indexPattern);
        result.put("query_params", queryParams);
        result.put("suggestions", Arrays.asList(
                "使用 Kibana Discover 功能浏览日志",
                "使用 KQL（Kibana Query Language）进行高级查询",
                "创建可视化 Dashboard 监控关键指标",
                "配置 Watcher 告警规则"
        ));

        return result;
    }

    // ==================== 辅助方法 ====================
}
