package cn.itzixiao.interview.bigdata.flink;

import lombok.extern.slf4j.Slf4j;

/**
 * Apache Flink 实时计算示例
 * <p>
 * Flink 特点：
 * - 真正的流处理（非微批）
 * - 低延迟（毫秒级）
 * - 高吞吐
 * - Exactly-Once 语义
 * - 事件时间处理
 * - 状态管理
 * <p>
 * 应用场景：
 * - 实时数据大屏
 * - 实时风控
 * - 实时推荐
 * - 实时 ETL
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
public class FlinkRealTimeDemo {

    /**
     * Flink 实时词频统计
     * <p>
     * 需求：
     * 1. 从 Socket 读取数据流
     * 2. 按空格拆分单词
     * 3. 每 5 秒窗口统计一次词频
     * 4. 输出结果
     */
    public void wordCountExample() {
        log.info("========== Flink 实时词频统计 ==========");

        log.info("\nFlink 代码示例:");
        log.info("// 1. 创建执行环境");
        log.info("StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();");

        log.info("\n// 2. 读取数据源");
        log.info("DataStream<String> textStream = env.socketTextStream(\"localhost\", 9999);");

        log.info("\n// 3. 转换处理");
        log.info("DataStream<Tuple2<String, Integer>> wordCounts = textStream");
        log.info("    .flatMap(new Tokenizer())  // 拆分单词");
        log.info("    .keyBy(value -> value.f0)  // 按单词分组");
        log.info("    .window(TumblingEventTimeWindows.of(Time.seconds(5)))  // 滚动窗口");
        log.info("    .sum(1);  // 聚合");

        log.info("\n// 4. 打印结果");
        log.info("wordCounts.print();");

        log.info("\n// 5. 执行任务");
        log.info("env.execute(\"Flink WordCount\");");

        log.info("\n注意：实际生产环境需要连接 Kafka、HDFS 等真实数据源");
        log.info("==============================\n");
    }

    /**
     * Flink 实时数据过滤和转换
     */
    public void dataFilterAndTransform() {
        log.info("========== Flink 数据处理 ==========");

        log.info("\nFlink 代码示例:");
        log.info("DataStream<Order> orders = env.addSource(kafkaSource);");
        log.info("");
        log.info("// 过滤高价商品（价格>1000）");
        log.info("DataStream<Order> expensiveOrders = orders.filter(order -> order.getPrice() > 1000);");
        log.info("");
        log.info("// 转换为 (用户 ID, 金额) 元组");
        log.info("DataStream<Tuple2<Long, Double>> userSpending = expensiveOrders");
        log.info("    .map(order -> new Tuple2<>(order.getUserId(), order.getPrice()));");
        log.info("");
        log.info("// 按用户分组统计消费总额");
        log.info("DataStream<Tuple2<Long, Double>> totalSpending = userSpending");
        log.info("    .keyBy(value -> value.f0)");
        log.info("    .reduce((v1, v2) -> new Tuple2<>(v1.f0, v1.f1 + v2.f1));");

        log.info("\n==============================\n");
    }

    /**
     * Flink 窗口操作示例
     * <p>
     * 窗口类型：
     * 1. 滚动窗口（Tumbling Window）
     * 2. 滑动窗口（Sliding Window）
     * 3. 会话窗口（Session Window）
     */
    public void windowOperations() {
        log.info("========== Flink 窗口操作 ==========");

        log.info("\n窗口类型说明:");
        log.info("1. 滚动窗口：每 5 秒统计一次");
        log.info("   .window(TumblingEventTimeWindows.of(Time.seconds(5)))");
        log.info("");
        log.info("2. 滑动窗口：每 2 秒统计最近 5 秒的数据");
        log.info("   .window(SlidingEventTimeWindows.of(Time.seconds(5), Time.seconds(2)))");
        log.info("");
        log.info("3. 会话窗口：活跃度间隔 3 秒");
        log.info("   .window(EventTimeSessionWindows.withGap(Time.seconds(3)))");

        log.info("\n==============================\n");
    }
}

/**
 * 订单实体类
 */
class Order {
    private Long orderId;
    private String productName;
    private Double price;
    private Long userId;

    public Order() {
    }

    public Order(Long orderId, String productName, Double price, Long userId) {
        this.orderId = orderId;
        this.productName = productName;
        this.price = price;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}

/**
 * 事件实体类
 */
class Event {
    public String userId;
    public String actionType;
    public Long timestamp;

    public Event() {
    }

    public Event(String userId, String actionType, Long timestamp) {
        this.userId = userId;
        this.actionType = actionType;
        this.timestamp = timestamp;
    }
}
