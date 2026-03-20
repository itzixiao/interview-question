package cn.itzixiao.interview.provider.hbase;

import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * HBase 操作工具类
 *
 * <p>HBase 是一个分布式、面向列的 NoSQL 数据库，适用于海量数据存储</p>
 *
 * <h3>核心概念：</h3>
 * <ul>
 *     <li><b>Table（表）</b>: HBase 中的表，由行和列组成</li>
 *     <li><b>Row Key（行键）</b>: 行的唯一标识，按字典序排序</li>
 *     <li><b>Column Family（列族）</b>: 列的集合，是权限控制和存储的基本单位</li>
 *     <li><b>Column Qualifier（列限定符）</b>: 具体的列名，属于某个列族</li>
 *     <li><b>Cell（单元格）</b>: 行和列的交点，包含值和时间戳</li>
 *     <li><b>Time Stamp（时间戳）</b>: 数据版本标识，支持多版本</li>
 * </ul>
 *
 * <h3>HBase vs MySQL:</h3>
 * <pre>
 * | 特性        | HBase                    | MySQL            |
 * |-------------|--------------------------|------------------|
 * | 数据模型    | 面向列                   | 关系型           |
 * | 扩展性      | 水平扩展（强）           | 垂直扩展（弱）   |
 * | 事务        | 单行事务                 | 支持跨行事务     |
 * | 查询        | 简单查询                 | 复杂 SQL 查询     |
 * | 适用场景    | 海量数据、稀疏数据       | 事务处理、复杂查询 |
 * </pre>
 */
@Component
public class HBaseDemo {

    @Autowired(required = false)
    private Connection connection;

    /**
     * 1. 创建表
     *
     * @param tableName      表名
     * @param columnFamilies 列族列表
     */
    public void createTable(String tableName, String... columnFamilies) throws IOException {
        Admin admin = connection.getAdmin();

        try {
            // 检查表是否已存在
            if (admin.tableExists(TableName.valueOf(tableName))) {
                System.out.println("表 " + tableName + " 已存在");
                return;
            }

            // 创建表描述器
            TableDescriptorBuilder tableDescriptor = TableDescriptorBuilder.newBuilder(TableName.valueOf(tableName));

            // 添加列族
            for (String family : columnFamilies) {
                ColumnFamilyDescriptorBuilder familyDescriptor =
                        ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(family));

                // 设置列族属性
                familyDescriptor.setMaxVersions(3);  // 最多保存 3 个版本
                familyDescriptor.setMinVersions(0);  // 最少保留 0 个版本
                familyDescriptor.setTimeToLive(2592000);  // TTL: 30 天

                tableDescriptor.setColumnFamily(familyDescriptor.build());
            }

            // 创建表
            admin.createTable(tableDescriptor.build());
            System.out.println("表 " + tableName + " 创建成功，列族：" + String.join(", ", columnFamilies));

        } finally {
            admin.close();
        }
    }

    /**
     * 2. 插入数据（单行）
     *
     * @param tableName 表名
     * @param rowKey    行键
     * @param family    列族
     * @param qualifier 列限定符
     * @param value     值
     */
    public void putRow(String tableName, String rowKey, String family,
                       String qualifier, String value) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));

        try {
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(value));

            table.put(put);
            System.out.println("插入数据：" + rowKey + " -> " + family + ":" + qualifier + "=" + value);

        } finally {
            table.close();
        }
    }

    /**
     * 3. 批量插入数据
     *
     * @param tableName 表名
     * @param puts      数据列表
     */
    public void putRows(String tableName, List<Put> puts) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));

        try {
            table.put(puts);
            System.out.println("批量插入 " + puts.size() + " 条数据");

        } finally {
            table.close();
        }
    }

    /**
     * 4. 查询单行数据
     *
     * @param tableName 表名
     * @param rowKey    行键
     */
    public void getRow(String tableName, String rowKey) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));

        try {
            Get get = new Get(Bytes.toBytes(rowKey));
            Result result = table.get(get);

            if (!result.isEmpty()) {
                System.out.println("\n查询行：" + rowKey);
                printResult(result);
            } else {
                System.out.println("未找到行：" + rowKey);
            }

        } finally {
            table.close();
        }
    }

    /**
     * 5. 扫描表数据（全表扫描）
     *
     * @param tableName 表名
     */
    public void scanTable(String tableName) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));

        try {
            Scan scan = new Scan();
            ResultScanner scanner = table.getScanner(scan);

            System.out.println("\n=== 全表扫描 ===");
            for (Result result : scanner) {
                printResult(result);
            }

        } finally {
            table.close();
        }
    }

    /**
     * 6. 带过滤器的扫描
     *
     * @param tableName 表名
     * @param family    列族
     * @param qualifier 列限定符
     * @param value     过滤值
     */
    public void scanWithFilter(String tableName, String family, String qualifier, String value) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));

        try {
            Scan scan = new Scan();

            // 单列值过滤器
            SingleColumnValueFilter filter = new SingleColumnValueFilter(
                    Bytes.toBytes(family),
                    Bytes.toBytes(qualifier),
                    CompareOperator.EQUAL,
                    Bytes.toBytes(value)
            );
            filter.setFilterIfMissing(true);  // 如果列不存在则过滤

            scan.setFilter(filter);

            ResultScanner scanner = table.getScanner(scan);

            System.out.println("\n=== 过滤扫描：" + family + ":" + qualifier + "=" + value + " ===");
            for (Result result : scanner) {
                printResult(result);
            }

        } finally {
            table.close();
        }
    }

    /**
     * 7. 删除数据
     *
     * @param tableName 表名
     * @param rowKey    行键
     */
    public void deleteRow(String tableName, String rowKey) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));

        try {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
            System.out.println("删除行：" + rowKey);

        } finally {
            table.close();
        }
    }

    /**
     * 8. 删除表
     *
     * @param tableName 表名
     */
    public void deleteTable(String tableName) throws IOException {
        Admin admin = connection.getAdmin();

        try {
            TableName tn = TableName.valueOf(tableName);

            if (!admin.tableExists(tn)) {
                System.out.println("表 " + tableName + " 不存在");
                return;
            }

            // 先禁用表
            admin.disableTable(tn);
            // 再删除表
            admin.deleteTable(tn);
            System.out.println("表 " + tableName + " 删除成功");

        } finally {
            admin.close();
        }
    }

    /**
     * 9. 追加数据到列
     *
     * @param tableName   表名
     * @param rowKey      行键
     * @param family      列族
     * @param qualifier   列限定符
     * @param appendValue 追加的值
     */
    public void appendToColumn(String tableName, String rowKey, String family,
                               String qualifier, String appendValue) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));

        try {
            Append append = new Append(Bytes.toBytes(rowKey));
            append.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(appendValue));

            Result result = table.append(append);
            System.out.println("追加后的值：" + Bytes.toString(result.getValue(
                    Bytes.toBytes(family), Bytes.toBytes(qualifier))));

        } finally {
            table.close();
        }
    }

    /**
     * 10. 原子递增/递减
     *
     * @param tableName 表名
     * @param rowKey    行键
     * @param family    列族
     * @param qualifier 列限定符
     * @param amount    增减量
     */
    public long incrementColumn(String tableName, String rowKey, String family,
                                String qualifier, long amount) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));

        try {
            Increment increment = new Increment(Bytes.toBytes(rowKey));
            increment.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier), amount);

            Result result = table.increment(increment);
            long newValue = Bytes.toLong(result.getValue(
                    Bytes.toBytes(family), Bytes.toBytes(qualifier)));

            System.out.println("递增后的值：" + newValue);
            return newValue;

        } finally {
            table.close();
        }
    }

    /**
     * 打印 Result 结果
     */
    private void printResult(Result result) {
        System.out.print("RowKey: " + Bytes.toString(result.getRow()));

        for (org.apache.hadoop.hbase.Cell cell : result.rawCells()) {
            String family = Bytes.toString(cell.getFamilyArray(),
                    cell.getFamilyOffset(), cell.getFamilyLength());
            String qualifier = Bytes.toString(cell.getQualifierArray(),
                    cell.getQualifierOffset(), cell.getQualifierLength());
            String value = Bytes.toString(cell.getValueArray(),
                    cell.getValueOffset(), cell.getValueLength());
            long timestamp = cell.getTimestamp();

            System.out.print(" [" + family + ":" + qualifier + "=" + value + "@" + timestamp + "]");
        }
        System.out.println();
    }

    /**
     * 使用示例
     */
    public void demonstrateHBaseOperations() {
        System.out.println("========== HBase 操作演示 ==========\n");

        try {
            // 1. 创建表
            System.out.println("【1. 创建表】");
            createTable("user_info", "basic", "education", "work");

            // 2. 插入单行数据
            System.out.println("\n【2. 插入数据】");
            putRow("user_info", "user001", "basic", "name", "张三");
            putRow("user_info", "user001", "basic", "age", "25");
            putRow("user_info", "user001", "basic", "email", "zhangsan@example.com");
            putRow("user_info", "user001", "education", "university", "北京大学");
            putRow("user_info", "user001", "education", "degree", "本科");
            putRow("user_info", "user001", "work", "company", "某互联网公司");
            putRow("user_info", "user001", "work", "position", "工程师");

            // 插入更多用户
            putRow("user_info", "user002", "basic", "name", "李四");
            putRow("user_info", "user002", "basic", "age", "28");
            putRow("user_info", "user002", "basic", "email", "lisi@example.com");
            putRow("user_info", "user002", "education", "university", "清华大学");
            putRow("user_info", "user002", "work", "company", "某科技公司");

            putRow("user_info", "user003", "basic", "name", "王五");
            putRow("user_info", "user003", "basic", "age", "30");
            putRow("user_info", "user003", "work", "company", "某大型企业");
            putRow("user_info", "user003", "work", "position", "经理");

            // 3. 查询单行
            System.out.println("\n【3. 查询单行】");
            getRow("user_info", "user001");

            // 4. 全表扫描
            System.out.println("\n【4. 全表扫描】");
            scanTable("user_info");

            // 5. 带过滤器的扫描
            System.out.println("\n【5. 过滤扫描】");
            scanWithFilter("user_info", "basic", "name", "李四");

            // 6. 追加数据
            System.out.println("\n【6. 追加数据】");
            appendToColumn("user_info", "user001", "work", "position", " - 高级");

            // 7. 原子递增
            System.out.println("\n【7. 原子递增】");
            putRow("user_info", "user001", "basic", "view_count", "10");
            incrementColumn("user_info", "user001", "basic", "view_count", 1);
            incrementColumn("user_info", "user001", "basic", "view_count", 5);

            // 8. 删除数据
            System.out.println("\n【8. 删除数据】");
            deleteRow("user_info", "user003");

            // 9. 删除表（演示用，实际生产环境谨慎使用）
            // deleteTable("user_info");

            System.out.println("\n========== 演示完成 ==========");

        } catch (IOException e) {
            System.err.println("HBase 操作失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
