package cn.itzixiao.interview.bigdata.spark;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.util.Arrays;
import java.util.List;

/**
 * Apache Spark 离线分析示例
 * <p>
 * Spark 特点：
 * - 基于内存计算
 * - 批处理能力强大
 * - SQL 查询支持
 * - 机器学习库（MLlib）
 * - 图计算（GraphX）
 * <p>
 * 应用场景：
 * - 离线报表
 * - 数据仓库
 * - 机器学习
 * - 数据挖掘
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
public class SparkOfflineDemo {

    /**
     * Spark RDD 基础操作
     */
    public void rddBasicOperations() {
        log.info("========== Spark RDD 基础操作 ==========");

        // 创建 SparkContext
        JavaSparkContext sc = new JavaSparkContext("local[*]", "SparkRDDDemo");

        // 创建 RDD
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        JavaRDD<Integer> rdd = sc.parallelize(data);

        // Transformation（转换）
        JavaRDD<Integer> squared = rdd.map(x -> x * x);
        JavaRDD<Integer> filtered = squared.filter(x -> x > 10);

        // Action（行动）
        long count = filtered.count();
        int sum = filtered.reduce((a, b) -> a + b);

        log.info("原始数据：{}", data);
        log.info("平方后过滤 (>10): {}", filtered.collect());
        log.info("数量：{}, 总和：{}", count, sum);

        // 词频统计示例
        List<String> lines = Arrays.asList(
                "hello spark",
                "hello world",
                "spark streaming"
        );

        JavaRDD<String> linesRDD = sc.parallelize(lines);

        JavaPairRDD<String, Integer> wordCounts = linesRDD
                .flatMap(line -> Arrays.asList(line.split("\\s+")).iterator())
                .mapToPair(word -> new Tuple2<>(word, 1))
                .reduceByKey((a, b) -> a + b);

        log.info("词频统计：{}", wordCounts.collect());

        sc.close();
        log.info("==============================\n");
    }

    /**
     * Spark SQL DataFrame 操作
     */
    public void sparkSqlExample() {
        log.info("========== Spark SQL DataFrame ==========");

        // 创建 SparkSession
        SparkSession spark = SparkSession.builder()
                .appName("SparkSQLDemo")
                .master("local[*]")
                .getOrCreate();

        // 创建 DataFrame
        List<User> users = Arrays.asList(
                new User(1L, "张三", 25, "北京"),
                new User(2L, "李四", 30, "上海"),
                new User(3L, "王五", 28, "深圳"),
                new User(4L, "赵六", 35, "北京")
        );

        Dataset<Row> df = spark.createDataFrame(users, User.class);

        // DataFrame 操作
        log.info("原始数据：");
        df.show();

        // 筛选年龄>28 的用户
        log.info("年龄>28 的用户：");
        df.filter(df.col("age").gt(28)).show();

        // 按城市分组统计
        log.info("按城市分组统计：");
        df.groupBy("city").count().show();

        // SQL 查询
        df.createOrReplaceTempView("users");
        Dataset<Row> result = spark.sql("SELECT city, AVG(age) as avg_age FROM users GROUP BY city");
        log.info("SQL 查询 - 各城市平均年龄：");
        result.show();

        spark.stop();
        log.info("==============================\n");
    }

    /**
     * Spark 数据分析实战
     */
    public void dataAnalysisExample() {
        log.info("========== Spark 数据分析实战 ==========");

        SparkSession spark = SparkSession.builder()
                .appName("DataAnalysis")
                .master("local[*]")
                .getOrCreate();

        // 模拟销售数据
        List<SaleRecord> sales = Arrays.asList(
                new SaleRecord("2024-01-01", "手机", 5999.0, 1),
                new SaleRecord("2024-01-01", "电脑", 8999.0, 2),
                new SaleRecord("2024-01-02", "手机", 5999.0, 3),
                new SaleRecord("2024-01-02", "耳机", 299.0, 2),
                new SaleRecord("2024-01-03", "键盘", 599.0, 1)
        );

        Dataset<Row> df = spark.createDataFrame(sales, SaleRecord.class);

        // 1. 总销售额
        df.agg(org.apache.spark.sql.functions.sum("amount")).show();

        // 2. 各商品销售额
        df.groupBy("product").sum("amount").show();

        // 3. 每日销售额
        df.groupBy("date").sum("amount").orderBy("date").show();

        // 4. 销量最好的商品
        df.groupBy("product").sum("quantity").orderBy("sum(quantity)").show();

        spark.stop();
        log.info("==============================\n");
    }
}

/**
 * 用户实体类
 */
class User {
    private Long id;
    private String name;
    private Integer age;
    private String city;

    public User() {
    }

    public User(Long id, String name, Integer age, String city) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.city = city;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}

/**
 * 销售记录实体类
 */
class SaleRecord {
    private String date;
    private String product;
    private Double amount;
    private Integer quantity;

    public SaleRecord() {
    }

    public SaleRecord(String date, String product, Double amount, Integer quantity) {
        this.date = date;
        this.product = product;
        this.amount = amount;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
