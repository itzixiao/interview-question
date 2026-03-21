package cn.itzixiao.interview.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体类
 * 
 * <p>注解说明：</p>
 * <ul>
 *   <li>@Document: 指定集合名称</li>
 *   <li>@CompoundIndex: 定义复合索引</li>
 *   <li>@Indexed: 定义单字段索引</li>
 *   <li>@Field: 指定字段名映射</li>
 * </ul>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
@CompoundIndexes({
    // 复合索引：名称 + 邮箱
    @CompoundIndex(name = "idx_name_email", def = "{'name': 1, 'email': 1}"),
    // 复合索引：状态 + 创建时间（降序）
    @CompoundIndex(name = "idx_status_created", def = "{'status': 1, 'createdAt': -1}")
})
public class User implements Persistable<String>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     * 
     * <p>MongoDB 自动生成 ObjectId</p>
     * <p>也可以使用 @GeneratedValue 生成</p>
     */
    @Id
    private String id;

    /**
     * 用户名
     * 
     * <p>单字段索引，支持快速按名称查询</p>
     */
    @Indexed(name = "idx_name")
    @Field("name")
    private String name;

    /**
     * 邮箱
     * 
     * <p>唯一索引，保证邮箱不重复</p>
     */
    @Indexed(name = "idx_email", unique = true)
    @Field("email")
    private String email;

    /**
     * 年龄
     */
    @Field("age")
    private Integer age;

    /**
     * 用户状态
     */
    @Field("status")
    private UserStatus status;

    /**
     * 用户地址
     * 
     * <p>嵌入式文档，与用户文档一起存储</p>
     * <p>适合一对一或一对少的场景</p>
     */
    @Field("address")
    private Address address;

    /**
     * 用户标签
     * 
     * <p>数组字段，支持多键索引</p>
     * <p>可以按标签查询用户</p>
     */
    @Field("tags")
    private List<String> tags;

    /**
     * 用户余额
     * 
     * <p>使用 BigDecimal 保证精度</p>
     * <p>MongoDB 存储为 Decimal128 类型</p>
     */
    @Field("balance")
    private BigDecimal balance;

    /**
     * 创建时间
     * 
     * <p>索引字段，支持按时间范围查询</p>
     */
    @Indexed(name = "idx_created_at")
    @Field("createdAt")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * 
     * <p>使用 @LastModifiedDate 自动更新</p>
     */
    @LastModifiedDate
    @Field("updatedAt")
    private LocalDateTime updatedAt;

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        /** 活跃状态 */
        ACTIVE,
        /** 未激活状态 */
        INACTIVE,
        /** 已封禁状态 */
        SUSPENDED
    }

    /**
     * 地址嵌入式文档
     * 
     * <p>不需要单独的 @Document 注解</p>
     * <p>作为 User 文档的一部分存储</p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /** 省份 */
        private String province;
        
        /** 城市 */
        private String city;
        
        /** 街道 */
        private String street;
        
        /** 邮编 */
        private String zipCode;
    }

    /**
     * 判断是否为新实体
     * 
     * <p>用于 Spring Data 判断是 insert 还是 update</p>
     */
    @Override
    public boolean isNew() {
        return id == null;
    }
}
