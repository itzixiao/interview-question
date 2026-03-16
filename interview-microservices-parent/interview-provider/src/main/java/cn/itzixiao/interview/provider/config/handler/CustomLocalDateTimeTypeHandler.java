package cn.itzixiao.interview.provider.config.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.LocalDateTimeTypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * 自定义 LocalDateTime 类型处理器
 * <p>
 * 用于解决 ShardingSphere 4.1.1 不支持 ResultSet.getObject() with type 的问题
 *
 * @author lovec
 * @date 2026-03-12
 */
public class CustomLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    private static final LocalDateTimeTypeHandler DELEGATE = new LocalDateTimeTypeHandler();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType) throws SQLException {
        DELEGATE.setNonNullParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 直接调用 getString() 然后解析，避免使用 getObject() with type
        String value = rs.getString(columnName);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value.replace(" ", "T"));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value.replace(" ", "T"));
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value.replace(" ", "T"));
    }
}
