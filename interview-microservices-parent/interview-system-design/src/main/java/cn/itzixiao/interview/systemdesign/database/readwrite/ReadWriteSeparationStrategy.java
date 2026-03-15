package cn.itzixiao.interview.systemdesign.database.readwrite;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 读写分离策略
 * 
 * 读写分离场景：
 * 1. 读多写少（如：查询订单、浏览商品）
 * 2. 主库压力大（写操作影响读性能）
 * 3. 需要水平扩展读能力
 * 
 * 实现方案：
 * 1. 主从复制 - 主库写，从库读
 * 2. 路由策略 - 根据 SQL 类型自动路由
 * 3. 数据一致性 - 主从延迟处理
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class ReadWriteSeparationStrategy {
    
    /**
     * 数据源类型枚举
     */
    public enum DataSourceType {
        MASTER,   // 主库（写）
        SLAVE_1,  // 从库 1（读）
        SLAVE_2,  // 从库 2（读）
        SLAVE_3   // 从库 3（读）
    }
    
    /**
     * ThreadLocal 保存当前线程使用的数据源
     */
    private static final ThreadLocal<DataSourceType> CONTEXT_HOLDER = 
            new ThreadLocal<>();
    
    /**
     * 设置为主库（写操作前调用）
     */
    public void useMaster() {
        CONTEXT_HOLDER.set(DataSourceType.MASTER);
        log.debug("切换到主库（写模式）");
    }
    
    /**
     * 设置为从库（读操作前调用）
     */
    public void useSlave() {
        // 简单的轮询策略
        int index = (int) (System.currentTimeMillis() % 3);
        DataSourceType slaveType = DataSourceType.values()[index + 1];
        CONTEXT_HOLDER.set(slaveType);
        log.debug("切换到从库：{}（读模式）", slaveType);
    }
    
    /**
     * 获取当前数据源
     */
    public DataSourceType getCurrentDataSource() {
        DataSourceType type = CONTEXT_HOLDER.get();
        return type != null ? type : DataSourceType.MASTER;
    }
    
    /**
     * 清除上下文
     */
    public void clearContext() {
        CONTEXT_HOLDER.remove();
    }
    
    /**
     * 演示读写分离
     */
    public void demonstrateReadWriteSeparation() {
        log.info("=== 读写分离演示 ===");
        
        // 模拟写操作
        useMaster();
        executeWrite("INSERT INTO orders ...");
        
        // 模拟读操作
        useSlave();
        executeRead("SELECT * FROM orders WHERE id = ?");
        
        // 清理
        clearContext();
    }
    
    private void executeWrite(String sql) {
        log.info("执行写操作 [{}]: {}", getCurrentDataSource(), sql);
    }
    
    private void executeRead(String sql) {
        log.info("执行读操作 [{}]: {}", getCurrentDataSource(), sql);
    }
}
