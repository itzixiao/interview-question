package cn.itzixiao.interview.mybatis;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * MyBatis-Plus 快速 CRUD 详解
 *
 * MyBatis-Plus 是 MyBatis 的增强工具，提供了：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 通用 CRUD：BaseMapper 提供基础增删改查                    │
 * │  2. 代码生成器：自动生成 Entity、Mapper、Service、Controller  │
 * │  3. 条件构造器：Wrapper 链式编程，无需手写 SQL                │
 * │  4. 分页插件：物理分页，支持多种数据库                        │
 * │  5. 性能分析：SQL 执行性能分析                                │
 * │  6. 乐观锁：@Version 注解实现                                 │
 * │  7. 逻辑删除：@TableLogic 注解实现                            │
 * │  8. 自动填充：@TableField 实现创建/更新时间自动填充           │
 * └─────────────────────────────────────────────────────────────┘
 */
public class MyBatisPlusDemo {

    /**
     * 1. 实体类定义
     *
     * 常用注解：
     * - @TableName：指定表名
     * - @TableId：主键字段
     * - @TableField：普通字段
     * - @TableLogic：逻辑删除字段
     * - @Version：乐观锁版本号
     * - @EnumValue：枚举映射
     */
    @TableName("sys_user")
    public static class User {

        /**
         * 主键 ID
         * type = IdType.AUTO：数据库自增
         * type = IdType.ASSIGN_ID：雪花算法（Long）
         * type = IdType.ASSIGN_UUID：UUID（String）
         */
        @TableId(type = IdType.ASSIGN_ID)
        private Long id;

        /**
         * 用户名
         * condition = SqlCondition.LIKE：生成 LIKE 条件
         */
        @TableField(value = "username", condition = SqlCondition.LIKE)
        private String username;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 年龄
         */
        private Integer age;

        /**
         * 状态（0-禁用，1-启用）
         */
        private Integer status;

        /**
         * 乐观锁版本号
         */
        @Version
        private Integer version;

        /**
         * 逻辑删除字段（0-未删除，1-已删除）
         */
        @TableLogic
        @TableField(fill = FieldFill.INSERT)
        private Integer deleted;

        /**
         * 创建时间
         * fill = FieldFill.INSERT：插入时自动填充
         */
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;

        /**
         * 更新时间
         * fill = FieldFill.INSERT_UPDATE：插入和更新时自动填充
         */
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }
        public Integer getDeleted() { return deleted; }
        public void setDeleted(Integer deleted) { this.deleted = deleted; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        public LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    }

    /**
     * 2. Mapper 接口
     *
     * 继承 BaseMapper<T> 即可获得通用 CRUD 方法
     */
    public interface UserMapper extends BaseMapper<User> {
        // 自定义方法可以在这里定义
        List<User> selectByAgeRange(@Param("minAge") Integer minAge,
                                    @Param("maxAge") Integer maxAge);
    }

    /**
     * 3. Service 接口
     *
     * 继承 IService<T> 获得更丰富的 Service 层方法
     */
    public interface UserService extends IService<User> {
        // 自定义业务方法
        boolean updateStatus(Long userId, Integer status);
    }

    /**
     * 4. Service 实现
     */
    public class UserServiceImpl extends ServiceImpl<UserMapper, User>
            implements UserService {

        @Override
        public boolean updateStatus(Long userId, Integer status) {
            LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(User::getId, userId)
                   .set(User::getStatus, status);
            return update(wrapper);
        }
    }

    /**
     * 5. 条件构造器使用示例
     */
    public void wrapperDemo(UserMapper userMapper) {
        System.out.println("========== 条件构造器示例 ==========\n");

        // 5.1 QueryWrapper - 传统方式（字段名硬编码）
        QueryWrapper<User> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("status", 1)
                     .like("username", "admin")
                     .ge("age", 18)
                     .orderByDesc("create_time");
        List<User> list1 = userMapper.selectList(queryWrapper1);

        // 5.2 LambdaQueryWrapper - Lambda 方式（类型安全，推荐）
        LambdaQueryWrapper<User> lambdaQuery = new LambdaQueryWrapper<>();
        lambdaQuery.eq(User::getStatus, 1)
                   .like(User::getUsername, "admin")
                   .ge(User::getAge, 18)
                   .orderByDesc(User::getCreateTime);
        List<User> list2 = userMapper.selectList(lambdaQuery);

        // 5.3 复杂条件组合
        LambdaQueryWrapper<User> complexQuery = new LambdaQueryWrapper<>();
        complexQuery.eq(User::getStatus, 1)
                    .and(wrapper -> wrapper
                        .like(User::getUsername, "admin")
                        .or()
                        .like(User::getEmail, "@gmail.com"))
                    .between(User::getAge, 18, 60)
                    .isNotNull(User::getEmail);
        List<User> list3 = userMapper.selectList(complexQuery);

        // 5.4 UpdateWrapper - 更新条件
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", 1L)
                     .set("status", 0)
                     .set("update_time", LocalDateTime.now());
        userMapper.update(null, updateWrapper);

        // 5.5 LambdaUpdateWrapper - Lambda 更新（推荐）
        LambdaUpdateWrapper<User> lambdaUpdate = new LambdaUpdateWrapper<>();
        lambdaUpdate.eq(User::getId, 1L)
                    .set(User::getStatus, 0)
                    .set(User::getUpdateTime, LocalDateTime.now());
        userMapper.update(null, lambdaUpdate);
    }

    /**
     * 6. 分页查询
     */
    public void paginationDemo(UserMapper userMapper) {
        System.out.println("========== 分页查询示例 ==========\n");

        // 6.1 基本分页
        Page<User> page = new Page<>(1, 10);  // 第1页，每页10条
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);

        IPage<User> userPage = userMapper.selectPage(page, wrapper);

        System.out.println("总记录数: " + userPage.getTotal());
        System.out.println("总页数: " + userPage.getPages());
        System.out.println("当前页: " + userPage.getCurrent());
        System.out.println("每页大小: " + userPage.getSize());
        System.out.println("记录: " + userPage.getRecords());

        // 6.2 自定义分页（关联查询）
        // 在 Mapper 中定义：
        // IPage<User> selectUserWithDept(Page<User> page, @Param("deptId") Long deptId);
    }

    /**
     * 7. 通用 CRUD 操作
     */
    public void crudDemo(UserService userService) {
        System.out.println("========== 通用 CRUD 示例 ==========\n");

        // 7.1 新增
        User user = new User();
        user.setUsername("zhangsan");
        user.setEmail("zhangsan@example.com");
        user.setAge(25);
        user.setStatus(1);
        boolean saveResult = userService.save(user);
        System.out.println("保存结果: " + saveResult + ", ID: " + user.getId());

        // 7.2 批量新增
        List<User> userList = Arrays.asList(
            createUser("lisi", "lisi@example.com", 30),
            createUser("wangwu", "wangwu@example.com", 28)
        );
        boolean batchSaveResult = userService.saveBatch(userList);
        System.out.println("批量保存结果: " + batchSaveResult);

        // 7.3 根据 ID 查询
        User userById = userService.getById(1L);
        System.out.println("根据ID查询: " + userById);

        // 7.4 条件查询（查一条）
        LambdaQueryWrapper<User> oneWrapper = new LambdaQueryWrapper<>();
        oneWrapper.eq(User::getUsername, "zhangsan");
        User one = userService.getOne(oneWrapper);
        System.out.println("条件查询一条: " + one);

        // 7.5 条件查询（查列表）
        LambdaQueryWrapper<User> listWrapper = new LambdaQueryWrapper<>();
        listWrapper.eq(User::getStatus, 1)
                   .orderByDesc(User::getCreateTime);
        List<User> list = userService.list(listWrapper);
        System.out.println("条件查询列表: " + list.size() + " 条");

        // 7.6 查询数量
        long count = userService.count();
        System.out.println("总数量: " + count);

        // 7.7 根据 ID 更新
        User updateUser = new User();
        updateUser.setId(1L);
        updateUser.setEmail("newemail@example.com");
        boolean updateResult = userService.updateById(updateUser);
        System.out.println("更新结果: " + updateResult);

        // 7.8 条件更新
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getStatus, 0)
                     .set(User::getStatus, 1);
        boolean updateByWrapper = userService.update(updateWrapper);
        System.out.println("条件更新结果: " + updateByWrapper);

        // 7.9 根据 ID 删除
        boolean removeResult = userService.removeById(1L);
        System.out.println("删除结果: " + removeResult);

        // 7.10 条件删除
        LambdaQueryWrapper<User> removeWrapper = new LambdaQueryWrapper<>();
        removeWrapper.eq(User::getStatus, 0);
        boolean removeByWrapper = userService.remove(removeWrapper);
        System.out.println("条件删除结果: " + removeByWrapper);

        // 7.11 批量删除
        boolean batchRemove = userService.removeByIds(Arrays.asList(2L, 3L, 4L));
        System.out.println("批量删除结果: " + batchRemove);

        // 7.12 保存或更新（根据 ID 判断）
        User saveOrUpdateUser = new User();
        saveOrUpdateUser.setId(1L);
        saveOrUpdateUser.setUsername("updated");
        boolean saveOrUpdateResult = userService.saveOrUpdate(saveOrUpdateUser);
        System.out.println("保存或更新结果: " + saveOrUpdateResult);
    }

    /**
     * 8. 链式查询（QueryChainWrapper）
     */
    public void chainQueryDemo(UserService userService) {
        System.out.println("========== 链式查询示例 ==========\n");

        // 8.1 链式查询（返回对象）
        User user = userService.lambdaQuery()
                .eq(User::getStatus, 1)
                .like(User::getUsername, "admin")
                .one();  // 查询一条

        // 8.2 链式查询（返回列表）
        List<User> list = userService.lambdaQuery()
                .eq(User::getStatus, 1)
                .list();

        // 8.3 链式查询（返回数量）
        long count = userService.lambdaQuery()
                .eq(User::getStatus, 1)
                .count();

        // 8.4 链式查询（判断是否存在）
        boolean exists = userService.lambdaQuery()
                .eq(User::getUsername, "admin")
                .exists();

        // 8.5 链式更新
        boolean update = userService.lambdaUpdate()
                .eq(User::getId, 1L)
                .set(User::getStatus, 0)
                .update();

        // 8.6 链式删除
        boolean remove = userService.lambdaUpdate()
                .eq(User::getStatus, 0)
                .remove();
    }

    private User createUser(String username, String email, Integer age) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setAge(age);
        user.setStatus(1);
        return user;
    }

    /**
     * 9. 常用 Wrapper 条件方法
     */
    public void wrapperMethodsDemo() {
        System.out.println("========== Wrapper 条件方法 ==========\n");

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 比较
        wrapper.eq(User::getId, 1L);           // 等于 =
        wrapper.ne(User::getStatus, 0);        // 不等于 <>
        wrapper.gt(User::getAge, 18);          // 大于 >
        wrapper.ge(User::getAge, 18);          // 大于等于 >=
        wrapper.lt(User::getAge, 60);          // 小于 <
        wrapper.le(User::getAge, 60);          // 小于等于 <=
        wrapper.between(User::getAge, 18, 60); // BETWEEN
        wrapper.notBetween(User::getAge, 0, 17); // NOT BETWEEN

        // 模糊查询
        wrapper.like(User::getUsername, "admin");      // LIKE '%admin%'
        wrapper.notLike(User::getUsername, "test");    // NOT LIKE '%test%'
        wrapper.likeLeft(User::getUsername, "admin");  // LIKE '%admin'
        wrapper.likeRight(User::getUsername, "admin"); // LIKE 'admin%'

        // 空值判断
        wrapper.isNull(User::getEmail);      // IS NULL
        wrapper.isNotNull(User::getEmail);   // IS NOT NULL

        // IN 查询
        wrapper.in(User::getId, Arrays.asList(1L, 2L, 3L));     // IN (...)
        wrapper.notIn(User::getId, Arrays.asList(4L, 5L, 6L));  // NOT IN (...)

        // 分组和排序
        wrapper.groupBy(User::getStatus);           // GROUP BY
        wrapper.orderByAsc(User::getCreateTime);    // ORDER BY ASC
        wrapper.orderByDesc(User::getCreateTime);   // ORDER BY DESC

        // 逻辑条件
        wrapper.and(w -> w.eq(User::getStatus, 1).like(User::getUsername, "a"));  // AND
        wrapper.or(w -> w.eq(User::getStatus, 0).like(User::getUsername, "b"));   // OR
        wrapper.nested(w -> w.eq(User::getStatus, 1).like(User::getUsername, "c")); // 嵌套

        // 其他
        wrapper.having("count(*) > 1");  // HAVING
        wrapper.last("LIMIT 10");        // 拼接在最后（有 SQL 注入风险，慎用）
    }
}
