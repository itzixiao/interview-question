package cn.itzixiao.interview.provider.controller.es;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.provider.service.es.ElasticsearchAdvancedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Elasticsearch 高级功能控制器
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@RestController
@RequestMapping("/api/es/advanced")
public class ElasticsearchAdvancedController {

    private final ElasticsearchAdvancedService advancedService;

    public ElasticsearchAdvancedController(ElasticsearchAdvancedService advancedService) {
        this.advancedService = advancedService;
    }

    // ==================== 一、倒排索引原理演示 ====================

    /**
     * 倒排索引原理演示
     */
    @GetMapping("/inverted-index-demo")
    public Result<Map<String, Object>> invertedIndexDemo() {
        log.info("【倒排索引】演示请求");
        
        Map<String, Object> demo = new HashMap<>();
        demo.put("title", "倒排索引原理演示");
        
        // 示例文档
        List<Map<String, String>> documents = Arrays.asList(
                createDoc("1", "Elasticsearch 是一个分布式搜索引擎"),
                createDoc("2", "Elasticsearch 支持全文检索"),
                createDoc("3", "Hadoop 是大数据处理框架")
        );
        
        demo.put("documents", documents);
        
        // 构建倒排索引
        Map<String, List<String>> invertedIndex = buildInvertedIndex(documents);
        demo.put("inverted_index", invertedIndex);
        
        demo.put("explanation", Arrays.asList(
                "1. 倒排索引 = 词典（Term Dictionary）+ 倒排表（Posting List）",
                "2. 词典：按字母顺序存储所有词项",
                "3. 倒排表：记录包含该词项的文档 ID 列表",
                "4. 优势：快速根据内容查找文档，适合全文检索"
        ));
        
        return Result.success(demo);
    }

    // ==================== 二、分词器机制 ====================

    /**
     * 测试分词器效果
     */
    @PostMapping("/analyze")
    public Result<Map<String, Object>> analyzeText(
            @RequestParam(defaultValue = "ik_max_word") String analyzer,
            @RequestParam(defaultValue = "Elasticsearch 分布式搜索引擎") String text) {
        log.info("【分词测试】analyzer: {}, text: {}", analyzer, text);
        
        try {
            Map<String, Object> result = advancedService.analyzeText(analyzer, text);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【分词测试】失败", e);
            return Result.error("分词测试失败：" + e.getMessage());
        }
    }

    // ==================== 三、查询 DSL 高级用法 ====================

    /**
     * 布尔查询示例
     */
    @PostMapping("/search/bool-query")
    public Result<List<Map<String, Object>>> boolQuerySearch(
            @RequestParam(defaultValue = "device_operation_log") String index,
            @RequestBody Map<String, Object> queryParams) {
        log.info("【布尔查询】index: {}, params: {}", index, queryParams);
        
        List<Map<String, Object>> results = advancedService.boolQuerySearch(index, queryParams);
        return Result.success(results);
    }

    /**
     * 多字段匹配查询
     */
    @PostMapping("/search/multi-match")
    public Result<List<Map<String, Object>>> multiMatchQuery(
            @RequestParam(defaultValue = "device_operation_log") String index,
            @RequestParam String searchText,
            @RequestParam List<String> fields) {
        log.info("【多字段查询】index: {}, searchText: {}, fields: {}", index, searchText, fields);
        
        List<Map<String, Object>> results = advancedService.multiMatchQuery(index, searchText, fields);
        return Result.success(results);
    }

    /**
     * 高亮搜索
     */
    @GetMapping("/search/highlight")
    public Result<Map<String, Object>> highlightSearch(
            @RequestParam(defaultValue = "device_operation_log") String index,
            @RequestParam String searchText,
            @RequestParam(defaultValue = "device_name") String field) {
        log.info("【高亮搜索】index: {}, searchText: {}, field: {}", index, searchText, field);
        
        Map<String, Object> result = advancedService.highlightSearch(index, searchText, field);
        return Result.success(result);
    }

    // ==================== 四、聚合分析实战 ====================

    /**
     * Terms 聚合
     */
    @GetMapping("/aggregation/terms")
    public Result<Map<String, Object>> termsAggregation(
            @RequestParam(defaultValue = "device_operation_log") String index,
            @RequestParam(defaultValue = "operation_type") String fieldName) {
        log.info("【Terms 聚合】index: {}, field: {}", index, fieldName);
        
        Map<String, Object> result = advancedService.termsAggregation(index, fieldName);
        return Result.success(result);
    }

    /**
     * 日期直方图聚合
     */
    @GetMapping("/aggregation/histogram")
    public Result<Map<String, Object>> dateHistogramAggregation(
            @RequestParam(defaultValue = "device_operation_log") String index,
            @RequestParam(defaultValue = "operation_time") String dateField) {
        log.info("【日期直方图】index: {}, field: {}", index, dateField);
        
        Map<String, Object> result = advancedService.dateHistogramAggregation(index, dateField);
        return Result.success(result);
    }

    /**
     * 嵌套聚合
     */
    @GetMapping("/aggregation/nested")
    public Result<Map<String, Object>> nestedAggregation(
            @RequestParam(defaultValue = "device_operation_log") String index) {
        log.info("【嵌套聚合】index: {}", index);
        
        Map<String, String> aggregations = new HashMap<>();
        aggregations.put("level_agg", "terms(level)");
        aggregations.put("time_histogram", "date_histogram(@timestamp, day)");
        
        Map<String, Object> result = advancedService.nestedAggregation(index, aggregations);
        return Result.success(result);
    }

    // ==================== 五、性能优化 ====================

    /**
     * 批量写入优化示例
     */
    @PostMapping("/optimize/bulk-write")
    public Result<Map<String, Object>> bulkWriteOptimization(
            @RequestParam(defaultValue = "test_index") String index,
            @RequestBody List<Map<String, Object>> documents) {
        log.info("【批量写入】index: {}, count: {}", index, documents.size());
        
        Map<String, Object> result = advancedService.bulkWriteOptimization(index, documents);
        return Result.success(result);
    }

    /**
     * 查询缓存优化
     */
    @GetMapping("/optimize/query-cache")
    public Result<Map<String, Object>> queryCacheOptimization(
            @RequestParam(defaultValue = "device_operation_log") String index,
            @RequestParam(defaultValue = "status") String filterField,
            @RequestParam(defaultValue = "active") String filterValue) {
        log.info("【查询缓存】index: {}, filter: {}={}", index, filterField, filterValue);
        
        Map<String, Object> result = advancedService.queryCacheOptimization(index, filterField, filterValue);
        return Result.success(result);
    }

    /**
     * 索引设置优化
     */
    @GetMapping("/optimize/index-settings")
    public Result<Map<String, Object>> indexSettingsOptimization(
            @RequestParam(defaultValue = "device_operation_log") String index) {
        log.info("【索引优化】index: {}", index);
        
        Map<String, Object> result = advancedService.indexSettingsOptimization(index);
        return Result.success(result);
    }

    // ==================== 六、集群管理与扩容 ====================

    /**
     * 获取集群健康状态
     */
    @GetMapping("/cluster/health")
    public Result<Map<String, Object>> getClusterHealth() {
        log.info("【集群管理】获取集群健康状态");
        
        Map<String, Object> result = advancedService.getClusterHealth();
        return Result.success(result);
    }

    /**
     * 获取节点信息
     */
    @GetMapping("/cluster/nodes")
    public Result<Map<String, Object>> getClusterNodes() {
        log.info("【集群管理】获取节点信息");
        
        Map<String, Object> result = advancedService.getClusterNodes();
        return Result.success(result);
    }

    /**
     * 获取分片分布
     */
    @GetMapping("/cluster/shards")
    public Result<Map<String, Object>> getShardDistribution(
            @RequestParam(required = false) String index) {
        log.info("【集群管理】获取分片分布");
        
        Map<String, Object> result = advancedService.getShardDistribution(
                index != null ? index : "*");
        return Result.success(result);
    }

    // ==================== 七、ELK 日志分析实战 ====================

    /**
     * 采集日志
     */
    @PostMapping("/elk/ingest-log")
    public Result<Map<String, Object>> ingestLogEntry(@RequestBody Map<String, Object> logData) {
        log.info("【ELK 日志】采集日志数据");
        
        Map<String, Object> result = advancedService.ingestLogEntry(logData);
        return Result.success(result);
    }

    /**
     * 查询日志
     */
    @PostMapping("/elk/query")
    public Result<Map<String, Object>> queryLogs(
            @RequestParam(defaultValue = "logs-*") String indexPattern,
            @RequestBody Map<String, Object> queryParams) {
        log.info("【ELK 日志】查询：index={}, params={}", indexPattern, queryParams);
        
        Map<String, Object> result = advancedService.queryLogs(indexPattern, queryParams);
        return Result.success(result);
    }

    // ==================== 辅助方法 ====================

    private Map<String, String> createDoc(String id, String content) {
        Map<String, String> doc = new HashMap<>();
        doc.put("id", id);
        doc.put("content", content);
        return doc;
    }

    private Map<String, List<String>> buildInvertedIndex(List<Map<String, String>> documents) {
        Map<String, List<String>> index = new HashMap<>();
        
        for (Map<String, String> doc : documents) {
            String content = doc.get("content");
            String docId = doc.get("id");
            
            // 简单分词（按空格和标点）
            String[] words = content.split("[\\s，。、？！,.!?]+");
            for (String word : words) {
                if (!word.isEmpty()) {
                    index.computeIfAbsent(word.toLowerCase(), k -> new ArrayList<>()).add("文档" + docId);
                }
            }
        }
        
        return index;
    }
}
