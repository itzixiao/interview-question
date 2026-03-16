package cn.itzixiao.interview.lowcode.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 代码生成器 - 简化的演示版本
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class CodeGenerator {

    /**
     * 演示代码生成
     */
    public void demoGeneration() {
        log.info("\n============================================================");
        log.info("低代码平台 - 代码生成器演示");
        log.info("============================================================\n");

        log.info("代码生成器核心功能：");
        log.info("1. 数据库表结构解析");
        log.info("2. Velocity 模板渲染");
        log.info("3. 多文件批量生成");
        log.info("4. 自定义模板支持\n");

        log.info("生成的文件类型：");
        log.info("- Entity 实体类");
        log.info("- Mapper 接口");
        log.info("- Service 接口和实现");
        log.info("- Controller 控制器");
        log.info("- Vue 前端页面\n");

        log.info("Velocity 模板示例：");
        log.info("@Data");
        log.info("@TableName(\"$tableName\")");
        log.info("public class ${className} implements Serializable {");
        log.info("    #foreach($field in $fields)");
        log.info("    private $field.type $field.name;");
        log.info("    #end");
        log.info("}\n");

        log.info("字段类型映射：");
        log.info("INT/BIGINT → Long/Integer");
        log.info("VARCHAR → String");
        log.info("DATETIME → Date/LocalDateTime");
        log.info("DECIMAL → BigDecimal");
        log.info("TINYINT → Integer/Boolean\n");

        log.info("========== 代码生成完成 ==========\n");
    }
}
