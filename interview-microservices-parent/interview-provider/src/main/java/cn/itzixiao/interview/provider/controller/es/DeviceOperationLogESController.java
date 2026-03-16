package cn.itzixiao.interview.provider.controller.es;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.provider.entity.es.DeviceOperationLogES;
import cn.itzixiao.interview.provider.service.sharding.DeviceOperationLogESService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch 搜索控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/es")
public class DeviceOperationLogESController {

    private final DeviceOperationLogESService esService;

    public DeviceOperationLogESController(DeviceOperationLogESService esService) {
        this.esService = esService;
    }

    /**
     * 加载 MySQL 数据到 ES
     */
    @PostMapping("/load")
    public Result<Map<String, Object>> loadAllDataToES() {
        log.info("【ES 接口】接收到加载全部数据请求");

        try {
            int count = esService.loadAllDataToES();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "数据加载成功");
            result.put("count", count);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】加载数据失败", e);
            return Result.error("加载数据失败：" + e.getMessage());
        }
    }

    /**
     * 创建索引
     */
    @PostMapping("/index/create")
    public Result<String> createIndex() {
        log.info("【ES 接口】接收到创建索引请求");

        try {
            esService.createIndex();
            return Result.success("索引创建成功");
        } catch (Exception e) {
            log.error("【ES 接口】创建索引失败", e);
            return Result.error("创建索引失败：" + e.getMessage());
        }
    }

    /**
     * 删除索引
     */
    @DeleteMapping("/index/delete")
    public Result<String> deleteIndex() {
        log.info("【ES 接口】接收到删除索引请求");

        try {
            esService.deleteIndex();
            return Result.success("索引删除成功");
        } catch (Exception e) {
            log.error("【ES 接口】删除索引失败", e);
            return Result.error("删除索引失败：" + e.getMessage());
        }
    }

    /**
     * 获取索引统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getIndexStats() {
        log.info("【ES 接口】接收到获取统计信息请求");

        try {
            Map<String, Object> stats = esService.getIndexStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("【ES 接口】获取统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 分页搜索 - 根据设备编号
     */
    @GetMapping("/search/device-code")
    public Result<Page<DeviceOperationLogES>> searchByDeviceCode(
            @RequestParam String deviceCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("【ES 接口】搜索设备编号：{}, 页码：{}, 大小：{}", deviceCode, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DeviceOperationLogES> result = esService.findByDeviceCode(deviceCode, pageable);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 分页搜索 - 根据操作类型
     */
    @GetMapping("/search/operation-type")
    public Result<Page<DeviceOperationLogES>> searchByOperationType(
            @RequestParam Integer operationType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("【ES 接口】搜索操作类型：{}, 页码：{}, 大小：{}", operationType, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DeviceOperationLogES> result = esService.findByOperationType(operationType, pageable);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 分页搜索 - 根据操作人
     */
    @GetMapping("/search/operator")
    public Result<Page<DeviceOperationLogES>> searchByOperator(
            @RequestParam String operator,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("【ES 接口】搜索操作人：{}, 页码：{}, 大小：{}", operator, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DeviceOperationLogES> result = esService.findByOperator(operator, pageable);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 全文检索 - 根据设备名称
     */
    @GetMapping("/search/device-name")
    public Result<Page<DeviceOperationLogES>> searchByDeviceName(
            @RequestParam String deviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("【ES 接口】全文检索设备名称：{}, 页码：{}, 大小：{}", deviceName, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DeviceOperationLogES> result = esService.searchByDeviceName(deviceName, pageable);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 组合查询 - 设备名称 + 操作类型
     */
    @GetMapping("/search/combined")
    public Result<Page<DeviceOperationLogES>> searchByDeviceNameAndOperationType(
            @RequestParam String deviceName,
            @RequestParam Integer operationType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("【ES 接口】组合查询 - 设备名称：{}, 操作类型：{}, 页码：{}, 大小：{}",
                deviceName, operationType, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DeviceOperationLogES> result = esService.searchByDeviceNameAndOperationType(
                    deviceName, operationType, pageable);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
}
