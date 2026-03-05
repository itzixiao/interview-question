package cn.itzixiao.interview.mybatis;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.StatementType;

import java.util.List;

/**
 * SQL 注入防护详解
 *
 * SQL 注入是最常见的 Web 安全漏洞之一，攻击者通过构造特殊的输入，
 * 改变原本 SQL 语句的结构，从而执行非授权的操作。
 *
 * 危害：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 数据泄露：获取敏感信息（用户密码、银行卡号等）            │
 * │  2. 数据篡改：修改或删除数据                                  │
 * │  3. 权限提升：获取管理员权限                                  │
 * │  4. 服务器控制：执行系统命令（极端情况）                      │
 * └─────────────────────────────────────────────────────────────┘
 */
public class SqlInjectionPreventionDemo {

    /**
     * ============================================
     * 1. 安全的参数绑定（#{}）
     * ============================================
     *
     * #{} 使用预编译语句，参数会被当作字符串处理，自动转义特殊字符
     */
    public interface SafeUserMapper {

        /**
         * 安全查询 - 使用 #{}
         *
         * 输入：username = "admin' OR '1'='1"
         * 实际 SQL：SELECT * FROM user WHERE username = 'admin'' OR ''1''=''1' AND status = 1
         * 结果：查询不到数据（参数被转义）
         */
        @Select("SELECT * FROM user WHERE username = #{username} AND status = #{status}")
        User findByUsernameSafe(@Param("username") String username,
                                @Param("status") Integer status);

        /**
         * 安全模糊查询
         *
         * 注意：CONCAT 中使用 #{}
         */
        @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%')")
        List<User> searchByKeywordSafe(@Param("keyword") String keyword);
    }

    /**
     * ============================================
     * 2. 危险的字符串替换（${}）
     * ============================================
     *
     * ${} 直接进行字符串替换，不做任何转义，存在 SQL 注入风险
     */
    public interface DangerousUserMapper {

        /**
         * 危险查询 - 使用 ${}
         *
         * 输入：username = "admin' OR '1'='1"
         * 实际 SQL：SELECT * FROM user WHERE username = 'admin' OR '1'='1'
         * 结果：查询到所有数据（SQL 注入成功！）
         */
        @Select("SELECT * FROM user WHERE username = '${username}'")
        User findByUsernameDangerous(@Param("username") String username);

        /**
         * 危险排序 - 使用 ${}
         *
         * 输入：orderBy = "id; DROP TABLE user; --"
         * 实际 SQL：SELECT * FROM user ORDER BY id; DROP TABLE user; --
         * 结果：用户表被删除！
         */
        @Select("SELECT * FROM user ORDER BY ${orderBy}")
        List<User> findAllWithOrderDangerous(@Param("orderBy") String orderBy);

        /**
         * 危险表名 - 使用 ${}
         *
         * 输入：tableName = "user; DELETE FROM user; --"
         * 实际 SQL：SELECT * FROM user; DELETE FROM user; -- WHERE id = 1
         * 结果：所有用户数据被删除！
         */
        @Select("SELECT * FROM ${tableName} WHERE id = #{id}")
        User findByTableDangerous(@Param("tableName") String tableName,
                                  @Param("id") Long id);
    }

    /**
     * ============================================
     * 3. 必须使用 ${} 时的防护方案
     * ============================================
     *
     * 某些场景必须使用 ${}，如动态表名、列名、排序字段
     * 此时需要使用白名单机制进行校验
     */
    public interface SecureUserMapper {

        @Select("SELECT * FROM user ORDER BY ${orderBy} ${orderDirection}")
        List<User> findAllWithOrderInternal(@Param("orderBy") String orderBy,
                                            @Param("orderDirection") String orderDirection);

        @Select("SELECT * FROM ${tableName} WHERE id = #{id}")
        User findByTableInternal(@Param("tableName") String tableName,
                                 @Param("id") Long id);
    }

    /**
     * 安全校验工具类
     */
    static class SecurityValidator {

        /**
         * 校验排序字段（白名单）
         */
        static String validateOrderBy(String orderBy) {
            // 允许的排序字段白名单
            String[] allowedFields = {"id", "username", "email", "create_time", "update_time"};
            for (String field : allowedFields) {
                if (field.equals(orderBy)) {
                    return field;
                }
            }
            // 不在白名单中，使用默认值
            return "id";
        }

        /**
         * 校验排序方向
         */
        static String validateOrderDirection(String direction) {
            if ("DESC".equalsIgnoreCase(direction)) {
                return "DESC";
            }
            return "ASC";
        }

        /**
         * 校验表名（白名单）
         */
        static String validateTableName(String tableName) {
            // 允许的表名白名单
            String[] allowedTables = {"user", "order", "product", "category"};
            for (String table : allowedTables) {
                if (table.equals(tableName)) {
                    return table;
                }
            }
            // 不在白名单中，抛出异常或使用默认值
            throw new IllegalArgumentException("非法的表名: " + tableName);
        }
    }

    /**
     * ============================================
     * 4. 使用存储过程（另一种防护方式）
     * ============================================
     */
    public interface StoredProcedureMapper {

        /**
         * 使用存储过程查询
         *
         * 存储过程内部使用参数化查询，可以防止 SQL 注入
         */
        @Select("{CALL sp_get_user_by_username(#{username, mode=IN})}")
        @Options(statementType = StatementType.CALLABLE)
        User findByUsernameWithProcedure(@Param("username") String username);
        
        // 引入 StatementType 枚举
        // StatementType.CALLABLE 表示调用存储过程
    }

    /**
     * 用户实体类
     */
    static class User {
        private Long id;
        private String username;
        private String email;
        private Integer status;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    /**
     * 演示 SQL 注入防护
     */
    public static void main(String[] args) {
        System.out.println("========== SQL 注入防护详解 ==========\n");

        System.out.println("【SQL 注入示例】");
        System.out.println("  正常输入: username = 'admin'");
        System.out.println("  恶意输入: username = 'admin\\' OR \\'1\\'=\\'1'");
        System.out.println("  结果: 绕过认证，获取所有用户数据\n");

        System.out.println("【防护方案】");
        System.out.println("  1. 使用 #{} 参数绑定（预编译语句）");
        System.out.println("  2. 必须使用 ${} 时，使用白名单校验");
        System.out.println("  3. 使用存储过程");
        System.out.println("  4. 使用 MyBatis 拦截器全局防护");
        System.out.println("  5. 数据库账号最小权限原则\n");

        System.out.println("【白名单校验示例】");
        System.out.println("  排序字段: 只允许 id, username, create_time");
        System.out.println("  表名: 只允许 user, order, product");
        System.out.println("  其他值一律拒绝或使用默认值\n");

        System.out.println("【#{ } vs ${ }】");
        System.out.println("  #{}: 预编译，安全，自动转义");
        System.out.println("  ${}: 直接替换，危险，需白名单校验\n");
    }
}
