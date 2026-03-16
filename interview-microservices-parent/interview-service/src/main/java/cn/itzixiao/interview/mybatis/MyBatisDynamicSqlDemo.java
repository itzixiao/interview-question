package cn.itzixiao.interview.mybatis;

import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.jdbc.SQL;

import java.util.List;
import java.util.Map;

/**
 * MyBatis 动态 SQL 详解
 * <p>
 * 动态 SQL 是 MyBatis 的强大特性之一，可以基于不同条件动态生成 SQL 语句
 * <p>
 * 主要标签：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  <if>          - 条件判断                                    │
 * │  <choose>      - 多选一（类似 Java switch）                  │
 * │  <when>        - choose 的分支                               │
 * │  <otherwise>   - choose 的默认分支                           │
 * │  <where>       - 智能处理 WHERE 关键字和 AND/OR              │
 * │  <set>         - 智能处理 SET 关键字和逗号                   │
 * │  <trim>        - 自定义前缀/后缀处理                         │
 * │  <foreach>     - 遍历集合（用于 IN 语句）                    │
 * │  <bind>        - 创建变量绑定                                │
 * └─────────────────────────────────────────────────────────────┘
 */
public class MyBatisDynamicSqlDemo {

    /**
     * 1. 动态查询 - 使用 XML Mapper
     * <p>
     * <!-- UserMapper.xml -->
     * <select id="findByCondition" resultType="User">
     * SELECT * FROM user
     * <where>
     * <if test="username != null">
     * AND username LIKE CONCAT('%', #{username}, '%')
     * </if>
     * <if test="status != null">
     * AND status = #{status}
     * </if>
     * <if test="startTime != null and endTime != null">
     * AND create_time BETWEEN #{startTime} AND #{endTime}
     * </if>
     * </where>
     * </select>
     */
    public interface UserMapper {

        /**
         * 动态条件查询
         */
        @SelectProvider(type = UserSqlProvider.class, method = "findByConditionSql")
        List<User> findByCondition(@Param("username") String username,
                                   @Param("status") Integer status,
                                   @Param("startTime") String startTime,
                                   @Param("endTime") String endTime);

        /**
         * 动态更新
         */
        @UpdateProvider(type = UserSqlProvider.class, method = "updateUserSql")
        int updateUser(@Param("id") Long id,
                       @Param("username") String username,
                       @Param("email") String email,
                       @Param("status") Integer status);

        /**
         * 批量插入
         */
        @InsertProvider(type = UserSqlProvider.class, method = "batchInsertSql")
        int batchInsert(@Param("list") List<User> users);

        /**
         * 批量查询（IN 语句）
         */
        @SelectProvider(type = UserSqlProvider.class, method = "findByIdsSql")
        List<User> findByIds(@Param("ids") List<Long> ids);

        /**
         * 动态选择（choose/when/otherwise）
         */
        @SelectProvider(type = UserSqlProvider.class, method = "findByConditionSql")
        List<User> findBySearchType(@Param("searchType") String searchType,
                                    @Param("keyword") String keyword);

        /**
         * 动态排序（注意：需要防范 SQL 注入）
         */
        @SelectProvider(type = UserSqlProvider.class, method = "findUsersWithOrderSql")
        List<User> findUsersWithOrder(@Param("orderBy") String orderBy,
                                      @Param("orderDirection") String orderDirection);

        /**
         * 动态表名（注意：存在 SQL 注入风险）
         */
        @SelectProvider(type = UserSqlProvider.class, method = "findFromTableSql")
        List<Map<String, Object>> findFromTable(@Param("tableName") String tableName,
                                                @Param("id") Long id);
    }

    /**
     * SQL 提供器类
     */
    static class UserSqlProvider {

        /**
         * 动态条件查询 SQL
         */
        public String findByConditionSql(@Param("username") String username,
                                         @Param("status") Integer status,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime) {
            return new SQL() {{
                SELECT("*");
                FROM("user");
                if (username != null) {
                    WHERE("username LIKE CONCAT('%', #{username}, '%')");
                }
                if (status != null) {
                    WHERE("status = #{status}");
                }
                if (startTime != null && endTime != null) {
                    WHERE("create_time BETWEEN #{startTime} AND #{endTime}");
                }
            }}.toString();
        }

        /**
         * 动态更新 SQL
         */
        public String updateUserSql(@Param("id") Long id,
                                    @Param("username") String username,
                                    @Param("email") String email,
                                    @Param("status") Integer status) {
            return new SQL() {{
                UPDATE("user");
                if (username != null) {
                    SET("username = #{username}");
                }
                if (email != null) {
                    SET("email = #{email}");
                }
                if (status != null) {
                    SET("status = #{status}");
                }
                SET("update_time = NOW()");
                WHERE("id = #{id}");
            }}.toString();
        }

        /**
         * 批量插入 SQL
         */
        public String batchInsertSql(@Param("list") List<User> users) {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO user (username, email, status, create_time) VALUES ");
            for (int i = 0; i < users.size(); i++) {
                sql.append("(#{list[").append(i).append("].username}, ");
                sql.append("#{list[").append(i).append("].email}, ");
                sql.append("#{list[").append(i).append("].status}, ");
                sql.append("NOW())");
                if (i < users.size() - 1) {
                    sql.append(", ");
                }
            }
            return sql.toString();
        }

        /**
         * 批量查询 SQL（IN 语句）
         */
        public String findByIdsSql(@Param("ids") List<Long> ids) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT * FROM user WHERE id IN (");
            for (int i = 0; i < ids.size(); i++) {
                sql.append("#{ids[").append(i).append("]}");
                if (i < ids.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
            return sql.toString();
        }

        /**
         * 动态选择查询 SQL
         */
        public String findBySearchTypeSql(@Param("searchType") String searchType,
                                          @Param("keyword") String keyword) {
            return new SQL() {{
                SELECT("*");
                FROM("user");
                if ("username".equals(searchType)) {
                    WHERE("username LIKE CONCAT('%', #{keyword}, '%')");
                } else if ("email".equals(searchType)) {
                    WHERE("email LIKE CONCAT('%', #{keyword}, '%')");
                } else if ("phone".equals(searchType)) {
                    WHERE("phone LIKE CONCAT('%', #{keyword}, '%')");
                } else {
                    // 默认查询
                    WHERE("1=1");
                }
            }}.toString();
        }

        /**
         * 动态排序 SQL（白名单校验防止 SQL 注入）
         */
        public String findUsersWithOrderSql(@Param("orderBy") String orderBy,
                                            @Param("orderDirection") String orderDirection) {
            // 白名单校验，防止 SQL 注入
            String safeOrderBy = validateOrderBy(orderBy);
            String safeOrderDirection = validateOrderDirection(orderDirection);

            return new SQL() {{
                SELECT("*");
                FROM("user");
            }}.toString() + " ORDER BY " + safeOrderBy + " " + safeOrderDirection;
        }

        /**
         * 动态表名查询（存在 SQL 注入风险，仅作演示）
         */
        public String findFromTableSql(@Param("tableName") String tableName,
                                       @Param("id") Long id) {
            // 注意：实际应用中应该使用白名单校验表名
            return "SELECT * FROM " + tableName + " WHERE id = #{id}";
        }

        /**
         * 校验排序字段（白名单）
         */
        private String validateOrderBy(String orderBy) {
            // 允许的排序字段白名单
            String[] allowedFields = {"id", "username", "email", "create_time", "update_time"};
            for (String field : allowedFields) {
                if (field.equals(orderBy)) {
                    return field;
                }
            }
            return "id"; // 默认按 id 排序
        }

        /**
         * 校验排序方向
         */
        private String validateOrderDirection(String direction) {
            if ("DESC".equalsIgnoreCase(direction)) {
                return "DESC";
            }
            return "ASC";
        }
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
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    /**
     * 演示动态 SQL 的使用
     */
    public static void main(String[] args) {
        System.out.println("========== MyBatis 动态 SQL 详解 ==========\n");

        System.out.println("【动态 SQL 标签】");
        System.out.println("  <if>        - 条件判断");
        System.out.println("  <choose>    - 多选一");
        System.out.println("  <where>     - 智能 WHERE");
        System.out.println("  <set>       - 智能 SET");
        System.out.println("  <foreach>   - 遍历集合");
        System.out.println("  <trim>      - 自定义修剪\n");

        System.out.println("【SQL 注入防护要点】");
        System.out.println("  1. 排序字段使用白名单校验");
        System.out.println("  2. 表名使用白名单校验");
        System.out.println("  3. 避免直接拼接用户输入到 SQL");
        System.out.println("  4. 使用 #{} 而不是 ${} 进行参数绑定\n");

        System.out.println("【注解方式 vs XML 方式】");
        System.out.println("  注解：简单直观，适合简单 SQL");
        System.out.println("  XML： 功能更强大，适合复杂动态 SQL\n");
    }
}
