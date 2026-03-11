package cn.itzixiao.interview.provider.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义GC监控端点，替代原生不存在的/actuator/gc
 */
@Component
@Endpoint(id = "gc") // 端点ID为gc，对应访问路径/actuator/gc
public class CustomGcEndpoint {

    @ReadOperation // GET请求访问
    public Map<String, Object> gcInfo() {
        Map<String, Object> result = new HashMap<>();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        
        // 遍历所有GC收集器（PS Scavenge/PS MarkSweep等）
        for (GarbageCollectorMXBean bean : gcBeans) {
            Map<String, Object> gcData = new HashMap<>();
            gcData.put("collectionCount", bean.getCollectionCount()); // GC次数
            gcData.put("collectionTime", bean.getCollectionTime()); // GC总耗时（毫秒）
            gcData.put("memoryPools", bean.getMemoryPoolNames()); // 关联的内存池
            
            result.put(bean.getName(), gcData);
        }
        return result;
    }
}