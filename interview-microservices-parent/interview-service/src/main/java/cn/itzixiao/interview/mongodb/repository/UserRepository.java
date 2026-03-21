package cn.itzixiao.interview.mongodb.repository;

import cn.itzixiao.interview.mongodb.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户 Repository 接口
 * 
 * <p>Spring Data MongoDB 自动实现基本 CRUD 操作</p>
 * <p>支持方法命名查询和 @Query 注解查询</p>
 * 
 * <h3>方法命名查询规则：</h3>
 * <ul>
 *   <li>findBy + 字段名：按字段查询</li>
 *   <li>findBy + 字段名 + Between：范围查询</li>
 *   <li>findBy + 字段名 + In：IN 查询</li>
 *   <li>findBy + 字段名 + Like：模糊查询</li>
 *   <li>countBy + 字段名：统计数量</li>
 *   <li>existsBy + 字段名：判断存在</li>
 * </ul>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // ==================== 方法命名查询 ====================

    /**
     * 按名称查找
     * 
     * <p>等价于: { "name": ? }</p>
     * 
     * @param name 用户名
     * @return 用户对象
     */
    Optional<User> findByName(String name);

    /**
     * 按邮箱查找
     * 
     * <p>等价于: { "email": ? }</p>
     * 
     * @param email 邮箱
     * @return 用户对象
     */
    Optional<User> findByEmail(String email);

    /**
     * 按状态查找
     * 
     * <p>等价于: { "status": ? }</p>
     * 
     * @param status 用户状态
     * @return 用户列表
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * 按年龄范围查找
     * 
     * <p>等价于: { "age": { $gt: min, $lt: max } }</p>
     * 
     * @param min 最小年龄
     * @param max 最大年龄
     * @return 用户列表
     */
    List<User> findByAgeBetween(Integer min, Integer max);

    /**
     * 按标签查找（数组元素匹配）
     * 
     * <p>等价于: { "tags": tag }</p>
     * <p>只要 tags 数组中包含指定标签即可匹配</p>
     * 
     * @param tag 标签
     * @return 用户列表
     */
    List<User> findByTags(String tag);

    /**
     * 按多个标签查找
     * 
     * <p>等价于: { "tags": { $all: [tag1, tag2] } }</p>
     * <p>tags 数组必须同时包含所有指定标签</p>
     * 
     * @param tag1 标签1
     * @param tag2 标签2
     * @return 用户列表
     */
    List<User> findByTagsContainingAndTagsContaining(String tag1, String tag2);

    /**
     * 按状态和创建时间查找
     * 
     * <p>等价于: { "status": status, "createdAt": { $gt: since } }</p>
     * 
     * @param status 用户状态
     * @param since  开始时间
     * @return 用户列表
     */
    List<User> findByStatusAndCreatedAtAfter(User.UserStatus status, LocalDateTime since);

    /**
     * 分页查询
     * 
     * @param status   用户状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    /**
     * 按名称模糊查询（忽略大小写）
     * 
     * <p>等价于: { "name": { $regex: name, $options: 'i' } }</p>
     * 
     * @param name 名称关键字
     * @return 用户列表
     */
    List<User> findByNameContainingIgnoreCase(String name);

    /**
     * 按城市查找
     * 
     * <p>查询嵌入式文档字段</p>
     * 
     * @param city 城市
     * @return 用户列表
     */
    List<User> findByAddressCity(String city);

    /**
     * 按省份查找
     * 
     * @param province 省份
     * @return 用户列表
     */
    List<User> findByAddressProvince(String province);

    /**
     * 统计某状态的用户数量
     * 
     * @param status 用户状态
     * @return 数量
     */
    long countByStatus(User.UserStatus status);

    /**
     * 检查邮箱是否存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 删除某状态的所有用户
     * 
     * @param status 用户状态
     * @return 删除数量
     */
    long deleteByStatus(User.UserStatus status);

    // ==================== @Query 注解查询 ====================

    /**
     * 自定义查询：按城市查找用户
     * 
     * <p>使用 @Query 注解指定查询条件</p>
     * 
     * @param city 城市
     * @return 用户列表
     */
    @Query("{ 'address.city': ?0 }")
    List<User> findByCityCustom(String city);

    /**
     * 投影查询：只返回指定字段
     * 
     * <p>fields 属性指定返回字段，1 表示返回</p>
     * 
     * @param status 用户状态
     * @return 用户列表（只包含 name 和 email）
     */
    @Query(value = "{ 'status': ?0 }", fields = "{ 'name': 1, 'email': 1 }")
    List<User> findNameAndEmailByStatus(User.UserStatus status);

    /**
     * 复杂条件查询
     * 
     * <p>使用 $and、$gte、$in 等操作符</p>
     * 
     * @param minAge 最小年龄
     * @param status 用户状态
     * @param tags   标签列表
     * @return 用户列表
     */
    @Query("{ $and: [ " +
           "  { 'age': { $gte: ?0 } }, " +
           "  { 'status': ?1 }, " +
           "  { 'tags': { $in: ?2 } } " +
           "] }")
    List<User> findComplex(Integer minAge, User.UserStatus status, List<String> tags);

    /**
     * 使用正则表达式查询
     * 
     * @param pattern 正则表达式
     * @return 用户列表
     */
    @Query("{ 'name': { $regex: ?0 } }")
    List<User> findByNameRegex(String pattern);

    /**
     * 查询余额大于指定值的用户
     * 
     * @param balance 余额
     * @return 用户列表
     */
    @Query("{ 'balance': { $gt: ?0 } }")
    List<User> findByBalanceGreaterThan(java.math.BigDecimal balance);

    /**
     * 统计某个时间段内创建的用户数量
     * 
     * @param start 开始时间
     * @param end   结束时间
     * @return 数量
     */
    @Query(value = "{ 'createdAt': { $gte: ?0, $lt: ?1 } }", count = true)
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
