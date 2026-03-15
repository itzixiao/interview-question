package cn.itzixiao.interview.provider.hbase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * HBase 示例接口
 * 
 * <p>用于演示 HBase 的基本操作</p>
 */
@RestController
@RequestMapping("/api/hbase")
public class HBaseController {

    @Autowired
    private HBaseDemo hBaseDemo;

    /**
     * 运行 HBase 操作演示
     * 
     * @return 执行结果
     */
    @PostMapping("/demo")
    public String runDemo() {
        try {
            hBaseDemo.demonstrateHBaseOperations();
            return "HBase 演示执行成功！查看控制台输出";
        } catch (Exception e) {
            return "HBase 演示失败：" + e.getMessage();
        }
    }

    /**
     * 创建表
     * 
     * @param tableName 表名
     * @param columnFamilies 列族（逗号分隔）
     * @return 结果
     */
    @PostMapping("/table/create")
    public String createTable(
            @RequestParam String tableName,
            @RequestParam String columnFamilies) {
        try {
            String[] families = columnFamilies.split(",");
            hBaseDemo.createTable(tableName, families);
            return "表 " + tableName + " 创建成功，列族：" + columnFamilies;
        } catch (Exception e) {
            return "创建表失败：" + e.getMessage();
        }
    }

    /**
     * 插入数据
     * 
     * @param tableName 表名
     * @param rowKey 行键
     * @param family 列族
     * @param qualifier 列限定符
     * @param value 值
     * @return 结果
     */
    @PostMapping("/put")
    public String putData(
            @RequestParam String tableName,
            @RequestParam String rowKey,
            @RequestParam String family,
            @RequestParam String qualifier,
            @RequestParam String value) {
        try {
            hBaseDemo.putRow(tableName, rowKey, family, qualifier, value);
            return "数据插入成功";
        } catch (Exception e) {
            return "插入数据失败：" + e.getMessage();
        }
    }

    /**
     * 查询数据
     * 
     * @param tableName 表名
     * @param rowKey 行键
     * @return 结果
     */
    @GetMapping("/get")
    public String getData(
            @RequestParam String tableName,
            @RequestParam String rowKey) {
        try {
            hBaseDemo.getRow(tableName, rowKey);
            return "查询完成，请查看控制台输出";
        } catch (Exception e) {
            return "查询失败：" + e.getMessage();
        }
    }

    /**
     * 扫描表
     * 
     * @param tableName 表名
     * @return 结果
     */
    @GetMapping("/scan")
    public String scanTable(@RequestParam String tableName) {
        try {
            hBaseDemo.scanTable(tableName);
            return "扫描完成，请查看控制台输出";
        } catch (Exception e) {
            return "扫描失败：" + e.getMessage();
        }
    }

    /**
     * 删除行
     * 
     * @param tableName 表名
     * @param rowKey 行键
     * @return 结果
     */
    @DeleteMapping("/delete")
    public String deleteRow(
            @RequestParam String tableName,
            @RequestParam String rowKey) {
        try {
            hBaseDemo.deleteRow(tableName, rowKey);
            return "删除成功";
        } catch (Exception e) {
            return "删除失败：" + e.getMessage();
        }
    }
}
