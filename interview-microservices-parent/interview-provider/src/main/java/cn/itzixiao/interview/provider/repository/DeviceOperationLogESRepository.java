package cn.itzixiao.interview.provider.repository;

import cn.itzixiao.interview.provider.entity.es.DeviceOperationLogES;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备运行日志 ES 仓库接口
 */
@Repository
public interface DeviceOperationLogESRepository extends ElasticsearchRepository<DeviceOperationLogES, Long> {

    /**
     * 根据设备编号查询
     */
    Page<DeviceOperationLogES> findByDeviceCode(String deviceCode, Pageable pageable);

    /**
     * 根据操作类型查询
     */
    Page<DeviceOperationLogES> findByOperationType(Integer operationType, Pageable pageable);

    /**
     * 根据操作人查询
     */
    Page<DeviceOperationLogES> findByOperator(String operator, Pageable pageable);

    /**
     * 根据设备名称模糊查询（全文检索）
     * 使用方法命名查询，Spring Data 会自动生成 match 查询
     */
    Page<DeviceOperationLogES> searchByDeviceName(String deviceName, Pageable pageable);

    /**
     * 多字段组合查询
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"deviceName\": \"?0\"}}, {\"term\": {\"operationType\": ?1}}]}}")
    Page<DeviceOperationLogES> searchByDeviceNameAndOperationType(String deviceName, Integer operationType, Pageable pageable);
}
