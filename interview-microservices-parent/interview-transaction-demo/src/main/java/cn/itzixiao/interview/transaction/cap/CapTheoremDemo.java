package cn.itzixiao.interview.transaction.cap;

/**
 * CAP 定理演示类
 * 
 * CAP 定理：分布式系统无法同时满足以下三点，最多只能同时满足两点
 * 
 * C（Consistency）一致性：
 * - 所有节点在同一时间具有相同的数据
 * - 操作后能立即读取到最新数据
 * 
 * A（Availability）可用性：
 * - 保证每个请求不管成功或者失败都有响应
 * - 系统一直可用，不会无限期等待
 * 
 * P（Partition tolerance）分区容错性：
 * - 系统中任意信息的丢失或失败不会影响系统的继续运作
 * - 网络分区发生时系统仍能运行
 * 
 * 实际选择：
 * - CP：强一致性 + 分区容错（放弃可用性）- Zookeeper、HBase
 * - AP：高可用 + 分区容错（放弃强一致性）- Eureka、Cassandra、Redis
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
public class CapTheoremDemo {
    
    /**
     * CP 系统示例 - Zookeeper
     * 
     * 场景：当 Leader 节点宕机时，系统会进入选举阶段（不可用），
     * 直到选出新的 Leader 才恢复服务，但保证了数据一致性
     */
    public void cpSystemExample() {
        System.out.println("=== CP 系统（Zookeeper）===");
        System.out.println("1. Leader 节点宕机");
        System.out.println("2. 系统进入选举阶段（暂停服务）");
        System.out.println("3. 选出新 Leader");
        System.out.println("4. 恢复服务，数据保持一致");
        System.out.println("特点：保证强一致性，牺牲可用性\n");
    }
    
    /**
     * AP 系统示例 - Eureka
     * 
     * 场景：当某个节点宕机时，其他节点仍然可用，
     * 但可能存在短暂的数据不一致（最终会一致）
     */
    public void apSystemExample() {
        System.out.println("=== AP 系统（Eureka）===");
        System.out.println("1. 节点 A 宕机");
        System.out.println("2. 节点 B、C 继续提供服务");
        System.out.println("3. 可能存在短暂数据不一致");
        System.out.println("4. 通过心跳机制最终达到一致");
        System.out.println("特点：保证高可用，接受最终一致性\n");
    }
    
    /**
     * BASE 理论
     * 
     * Basically Available（基本可用）
     * - 分布式系统出现故障时，允许损失部分可用性
     * - 如：降级、熔断、限流
     * 
     * Soft state（软状态）
     * - 允许系统存在中间状态
     * - 该状态不影响系统整体可用性
     * 
     * Eventually consistent（最终一致性）
     * - 系统中的所有数据副本经过一段时间后
     * - 最终能够达到一致的状态
     */
    public void baseTheoryExample() {
        System.out.println("=== BASE 理论 ===");
        System.out.println("1. 基本可用：系统故障时允许降级处理");
        System.out.println("   - 电商大促：非核心业务降级");
        System.out.println("   - 秒杀系统：排队等待机制");
        
        System.out.println("\n2. 软状态：允许中间状态存在");
        System.out.println("   - 订单支付中状态");
        System.out.println("   - 数据同步延迟");
        
        System.out.println("\n3. 最终一致性：数据最终会一致");
        System.out.println("   - 主从复制延迟");
        System.out.println("   - 缓存与数据库一致性");
        System.out.println("   - 消息队列异步处理");
    }
}
