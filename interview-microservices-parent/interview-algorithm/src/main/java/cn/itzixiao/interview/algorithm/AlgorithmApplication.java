package cn.itzixiao.interview.algorithm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 算法面试题库微服务启动类
 * 
 * 本模块提供常见算法面试题的完整实现和详细注释
 * 包含以下算法分类：
 * - 排序算法：快速排序、归并排序、堆排序等
 * - 查找算法：二分查找、DFS、BFS 等
 * - 链表操作：反转、合并、环检测等
 * - 树相关：遍历、深度、最近公共祖先等
 * - 动态规划：背包问题、最长子序列等
 * - 字符串：KMP、正则匹配等
 * - 数据结构：栈、队列、哈希表、堆等
 * 
 * @author itzixiao
 */
@SpringBootApplication
public class AlgorithmApplication {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    算法面试题库微服务                                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝\n");
        
        SpringApplication.run(AlgorithmApplication.class, args);
        
        System.out.println("\n✅ 算法面试题库微服务启动成功！");
        System.out.println("📚 本模块包含以下算法分类：");
        System.out.println("   1. 排序算法 - 快速排序、归并排序、堆排序等");
        System.out.println("   2. 查找算法 - 二分查找、DFS、BFS 等");
        System.out.println("   3. 链表操作 - 反转、合并、环检测等");
        System.out.println("   4. 树相关 - 遍历、深度、最近公共祖先等");
        System.out.println("   5. 动态规划 - 背包问题、最长子序列等");
        System.out.println("   6. 字符串 - KMP、正则匹配等");
        System.out.println("   7. 数据结构 - 栈、队列、哈希表、堆等");
        System.out.println("\n💡 详细题解请参考 docs/算法面试题详解.md 文档");
        System.out.println("🔗 API 端点请访问：http://localhost:8083/actuator/health");
    }
}
