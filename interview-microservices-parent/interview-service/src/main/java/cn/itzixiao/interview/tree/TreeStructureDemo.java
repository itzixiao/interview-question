package cn.itzixiao.interview.tree;

/**
 * 树形数据结构核心知识点详解 - 面试高频考点
 *
 * <pre>
 * 涵盖内容：
 * 1. 二叉树（Binary Tree）
 * 2. 平衡二叉树（AVL Tree）
 * 3. 红黑树（Red-Black Tree）
 * 4. B 树（B-Tree）
 * 5. B+树（B+ Tree）
 * </pre>
 *
 * @author itzixiao
 */
public class TreeStructureDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    树形数据结构核心知识点详解                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝\n");

        // 第一部分：二叉树基础
        demonstrateBinaryTree();

        // 第二部分：平衡二叉树（AVL）
        demonstrateAVLTree();

        // 第三部分：红黑树
        demonstrateRedBlackTree();

        // 第四部分：B 树
        demonstrateBTree();

        // 第五部分：B+树
        demonstrateBPlusTree();

        // 第六部分：各种树的对比
        printComparison();

        // 第七部分：高频面试题
        printInterviewQuestions();
    }

    // ==================== 第一部分：二叉树基础 ====================

    private static void demonstrateBinaryTree() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第一部分：二叉树（Binary Tree）                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【1.1 什么是二叉树？】\n");
        System.out.println("定义：每个节点最多有两个子节点的树结构");
        System.out.println("特点：");
        System.out.println("- 左子节点 < 父节点 < 右子节点（二叉搜索树）");
        System.out.println("- 查找、插入、删除的时间复杂度：O(h)，h 为树高");
        System.out.println("- 最坏情况退化为链表（O(n)）\n");

        System.out.println("【1.2 二叉树结构定义】\n");
        System.out.println("class TreeNode {");
        System.out.println("    int val;");
        System.out.println("    TreeNode left;   // 左子节点");
        System.out.println("    TreeNode right;  // 右子节点");
        System.out.println("    TreeNode(int x) { val = x; }");
        System.out.println("}\n");

        System.out.println("【1.3 二叉树遍历方式】\n");

        System.out.println("前序遍历（Pre-order）：根 → 左 → 右");
        System.out.println("中序遍历（In-order）：左 → 根 → 右（得到有序序列）");
        System.out.println("后序遍历（Post-order）：左 → 右 → 根");
        System.out.println("层序遍历（Level-order）：按层从上到下遍历\n");

        System.out.println("【1.4 二叉树示例】\n");
        System.out.println("      8");
        System.out.println("     / \\");
        System.out.println("    3   10");
        System.out.println("   / \\    \\");
        System.out.println("  1   6    14");
        System.out.println(" ");
        System.out.println("前序遍历：8, 3, 1, 6, 10, 14");
        System.out.println("中序遍历：1, 3, 6, 8, 10, 14（有序！）");
        System.out.println("后序遍历：1, 6, 3, 14, 10, 8");
        System.out.println("层序遍历：8, 3, 10, 1, 6, 14\n");

        System.out.println("【1.5 二叉搜索树操作】\n");

        System.out.println("插入操作：");
        System.out.println("1. 从根节点开始");
        System.out.println("2. 比当前节点小 → 往左子树");
        System.out.println("3. 比当前节点大 → 往右子树");
        System.out.println("4. 找到空位置，插入新节点\n");

        System.out.println("查找操作：");
        System.out.println("1. 从根节点开始");
        System.out.println("2. 等于当前节点 → 找到");
        System.out.println("3. 小于当前节点 → 往左子树");
        System.out.println("4. 大于当前节点 → 往右子树");
        System.out.println("5. 到叶子节点还没找到 → 不存在\n");

        System.out.println("删除操作（最复杂）：");
        System.out.println("情况 1：叶子节点 → 直接删除");
        System.out.println("情况 2：只有一个子节点 → 子承父业");
        System.out.println("情况 3：有两个子节点 → 找右子树最小值（或左子树最大值）替换\n");
    }

    // ==================== 第二部分：平衡二叉树（AVL） ====================

    private static void demonstrateAVLTree() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║            第二部分：平衡二叉树（AVL Tree）                             ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【2.1 为什么需要平衡二叉树？】\n");
        System.out.println("问题：普通二叉搜索树可能退化为链表");
        System.out.println("例如：插入 1,2,3,4,5,6 → 所有节点都在右边");
        System.out.println("解决：AVL 树通过旋转保持平衡\n");

        System.out.println("【2.2 AVL 树的定义】\n");
        System.out.println("特点：");
        System.out.println("1. 是二叉搜索树");
        System.out.println("2. 任意节点的左右子树高度差 ≤ 1（平衡因子 ∈ [-1, 0, 1]）");
        System.out.println("3. 每次插入/删除后自动调整平衡\n");

        System.out.println("【2.3 平衡因子】\n");
        System.out.println("平衡因子 = 左子树高度 - 右子树高度");
        System.out.println("平衡因子 ∈ {-1, 0, 1} → 平衡");
        System.out.println("平衡因子 > 1 或 < -1 → 不平衡，需要旋转\n");

        System.out.println("【2.4 四种不平衡情况及旋转】\n");

        System.out.println("LL 型（右单旋）：");
        System.out.println("    5           3");
        System.out.println("   /           / \\");
        System.out.println("  3    →      2   5");
        System.out.println(" /");
        System.out.println("2\n");

        System.out.println("RR 型（左单旋）：");
        System.out.println("  5               7");
        System.out.println("   \\             / \\");
        System.out.println("    7    →      5   9");
        System.out.println("     \\");
        System.out.println("      9\n");

        System.out.println("LR 型（先左后右双旋）：");
        System.out.println("    5           5         4");
        System.out.println("   /           /         / \\");
        System.out.println("  2    →      4    →    2   5");
        System.out.println("   \\         /");
        System.out.println("    4       2\n");

        System.out.println("RL 型（先右后左双旋）：");
        System.out.println("  2             2           4");
        System.out.println("   \\           / \\         / \\");
        System.out.println("    5    →    4   5  →    2   5");
        System.out.println("   /           \\");
        System.out.println("  4             4\n");

        System.out.println("【2.5 AVL 树的优缺点】\n");
        System.out.println("优点：");
        System.out.println("- 查询效率极高（严格平衡，O(log n)）");
        System.out.println("- 适合查询密集型场景\n");

        System.out.println("缺点：");
        System.out.println("- 插入/删除时需要多次旋转");
        System.out.println("- 维护平衡的开销大");
        System.out.println("- 不适合频繁插入删除的场景\n");
    }

    // ==================== 第三部分：红黑树 ====================

    private static void demonstrateRedBlackTree() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第三部分：红黑树（Red-Black Tree）                      ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【3.1 红黑树的定义】\n");
        System.out.println("红黑树是一种自平衡的二叉搜索树，通过节点颜色保证近似平衡");
        System.out.println("每个节点要么是红色，要么是黑色\n");

        System.out.println("【3.2 红黑树的五大性质】\n");
        System.out.println("1. 每个节点是红色或黑色");
        System.out.println("2. 根节点是黑色");
        System.out.println("3. 叶子节点（NIL）是黑色");
        System.out.println("4. 红色节点的子节点必须是黑色（不能有两个连续红色节点）");
        System.out.println("5. 从任一节点到其叶子的所有路径包含相同数量的黑色节点\n");

        System.out.println("【3.3 红黑树示例】\n");
        System.out.println("        8(B)");
        System.out.println("       /    \\");
        System.out.println("     4(R)   12(R)");
        System.out.println("    /  \\    /   \\");
        System.out.println("  2(B) 6(B) 10(B) 14(B)");
        System.out.println("注：(B)=黑色，(R)=红色\n");

        System.out.println("【3.4 红黑树的平衡操作】\n");

        System.out.println("变色：");
        System.out.println("- 改变节点颜色（红↔黑）");
        System.out.println("- 用于调整黑色节点数量\n");

        System.out.println("左旋：");
        System.out.println("  5              7");
        System.out.println("   \\            /");
        System.out.println("    7    →     5");
        System.out.println("     \\          \\");
        System.out.println("      9          9\n");

        System.out.println("右旋：");
        System.out.println("      7          5");
        System.out.println("     /            \\");
        System.out.println("    5      →      7");
        System.out.println("   /                \\");
        System.out.println("  3                  3\n");

        System.out.println("【3.5 红黑树 vs AVL 树】\n");
        System.out.println("┌──────────────┬───────────────┬───────────────┐");
        System.out.println("│ 特性         │ 红黑树        │ AVL 树        │");
        System.out.println("├──────────────┼───────────────┼───────────────┤");
        System.out.println("│ 平衡性       │ 弱平衡        │ 强平衡        │");
        System.out.println("│ 树高         │ ≤ 2log₂(n)    │ ≤ log₂(n)     │");
        System.out.println("│ 查询效率     │ O(log n)      │ O(log n)      │");
        System.out.println("│ 插入效率     │ 高（最多 3 次旋转）│ 低（可能多次旋转）│");
        System.out.println("│ 删除效率     │ 高            │ 低            │");
        System.out.println("│ 适用场景     │ 增删改查频繁  │ 查询密集型    │");
        System.out.println("└──────────────┴───────────────┴───────────────┘\n");

        System.out.println("【3.6 红黑树的应用】\n");
        System.out.println("✓ Java TreeMap：基于红黑树实现");
        System.out.println("✓ Java HashMap（JDK8+）：桶数组 + 链表 + 红黑树");
        System.out.println("✓ C++ STL map：基于红黑树实现");
        System.out.println("✓ Linux 内核进程调度：使用红黑树管理进程\n");
    }

    // ==================== 第四部分：B 树 ====================

    private static void demonstrateBTree() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第四部分：B 树（B-Tree）                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【4.1 为什么需要 B 树？】\n");
        System.out.println("问题：数据量巨大时，树很高，磁盘 IO 次数多");
        System.out.println("解决：增加每个节点的子节点数，降低树高");
        System.out.println("目标：减少磁盘 IO 次数（数据库索引的核心）\n");

        System.out.println("【4.2 B 树的定义】\n");
        System.out.println("m 阶 B 树的特点：");
        System.out.println("1. 每个节点最多有 m 个子节点");
        System.out.println("2. 除根节点外，每个节点至少有 ⌈m/2⌉ 个子节点");
        System.out.println("3. 根节点至少有 2 个子节点（除非是叶子）");
        System.out.println("4. 所有叶子节点在同一层");
        System.out.println("5. 节点包含 k 个关键字和 k+1 个指针\n");

        System.out.println("【4.3 3 阶 B 树示例】\n");
        System.out.println("         [17, 35]");
        System.out.println("        /    |    \\");
        System.out.println("   [5,10] [20,30] [40,50]");
        System.out.println("   / | \\   / | \\   / | \\");
        System.out.println("  1 3 12 18 25 33 38 45 55");
        System.out.println(" ");
        System.out.println("特点：");
        System.out.println("- 每个节点最多 3 个子节点");
        System.out.println("- 每个节点最多 2 个关键字");
        System.out.println("- 所有叶子在同一层\n");

        System.out.println("【4.4 B 树的优势】\n");
        System.out.println("矮胖结构：");
        System.out.println("- 相比二叉树，B 树更\"矮胖\"");
        System.out.println("- 例如：100 万个节点");
        System.out.println("  - 二叉树：树高约 20 层");
        System.out.println("  - B 树（100 叉）：树高约 3 层");
        System.out.println("- 减少磁盘 IO 次数\n");

        System.out.println("【4.5 B 树的查找过程】\n");
        System.out.println("1. 从根节点开始");
        System.out.println("2. 在节点内二分查找关键字");
        System.out.println("3. 找到 → 返回");
        System.out.println("4. 没找到 → 根据范围选择子节点");
        System.out.println("5. 重复步骤 2-4，直到叶子节点\n");

        System.out.println("【4.6 B 树的插入与删除】\n");

        System.out.println("插入：");
        System.out.println("1. 找到合适的叶子节点");
        System.out.println("2. 插入关键字");
        System.out.println("3. 如果节点关键字数超过上限 → 分裂");
        System.out.println("4. 中间关键字上移到父节点\n");

        System.out.println("删除：");
        System.out.println("1. 找到要删除的关键字");
        System.out.println("2. 如果是叶子节点 → 直接删除");
        System.out.println("3. 如果不是叶子 → 用子节点替代");
        System.out.println("4. 如果节点关键字数低于下限 → 合并或借位\n");
    }

    // ==================== 第五部分：B+树 ====================

    private static void demonstrateBPlusTree() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第五部分：B+树（B+ Tree）                               ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【4.1 B+树的定义】\n");
        System.out.println("B+树是 B 树的变体，更适合文件系统和数据库索引");
        System.out.println("核心改进：数据只存储在叶子节点，非叶子节点只存索引\n");

        System.out.println("【4.2 B+树 vs B 树】\n");
        System.out.println("┌──────────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│ 特性         │ B 树                │ B+树                │");
        System.out.println("├──────────────┼─────────────────────┼─────────────────────┤");
        System.out.println("│ 数据存储位置 │ 所有节点            │ 只在叶子节点        │");
        System.out.println("│ 非叶子节点   │ 存数据 + 索引        │ 只存索引            │");
        System.out.println("│ 叶子节点     │ 不连接              │ 用链表连接          │");
        System.out.println("│ 查询稳定性   │ 不稳定（可能在非叶子）│ 稳定（都在叶子）    │");
        System.out.println("│ 范围查询     │ 需中序遍历          │ 直接遍历链表        │");
        System.out.println("└──────────────┴─────────────────────┴─────────────────────┘\n");

        System.out.println("【4.3 B+树示例】\n");
        System.out.println("非叶子节点（索引层）：");
        System.out.println("         [17, 35]");
        System.out.println("        /    |    \\");
        System.out.println("   [5,10] [20,30] [40,50]");
        System.out.println(" ");
        System.out.println("叶子节点（数据层，用链表连接）：");
        System.out.println("→ [1,3] → [12,18] → [25,33] → [38,45] → [55,60] →");
        System.out.println(" ");
        System.out.println("特点：");
        System.out.println("- 非叶子节点只存索引（容纳更多）");
        System.out.println("- 叶子节点存数据 + 指向下一条记录的指针");
        System.out.println("- 叶子节点形成有序链表\n");

        System.out.println("【4.4 B+树的优势】\n");

        System.out.println("1. 磁盘读写代价更低：");
        System.out.println("   - 非叶子节点不存数据，更小");
        System.out.println("   - 同样大小的磁盘页可以容纳更多索引项");
        System.out.println("   - 树更矮，IO 次数更少\n");

        System.out.println("2. 查询效率稳定：");
        System.out.println("   - 所有查询都要走到叶子节点");
        System.out.println("   - 查询路径长度相同\n");

        System.out.println("3. 范围查询极快：");
        System.out.println("   - 叶子节点用链表连接");
        System.out.println("   - 只需找到起点，然后遍历链表");
        System.out.println("   - 例如：SELECT * WHERE id > 100\n");

        System.out.println("4. 全表扫描简单：");
        System.out.println("   - 直接遍历叶子节点链表即可");
        System.out.println("   - 不需要中序遍历整棵树\n");

        System.out.println("【4.5 B+树在数据库中的应用】\n");
        System.out.println("MySQL InnoDB 引擎：");
        System.out.println("- 聚簇索引：使用 B+树");
        System.out.println("- 二级索引：使用 B+树");
        System.out.println("- 叶子节点存储完整数据行\n");

        System.out.println("文件系统：");
        System.out.println("- NTFS 文件系统使用 B+树");
        System.out.println("- HFS+ 文件系统使用 B+树");
        System.out.println("- 数据库索引普遍使用 B+树\n");
    }

    // ==================== 第六部分：各种树的对比 ====================

    private static void printComparison() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第六部分：各种树结构对比                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【6.1 时间复杂度对比】\n");
        System.out.println("┌──────────────┬──────────────┬──────────────┬──────────────┐");
        System.out.println("│ 数据结构     │ 平均查找     │ 平均插入     │ 平均删除     │");
        System.out.println("├──────────────┼──────────────┼──────────────┼──────────────┤");
        System.out.println("│ 二叉搜索树   │ O(log n)     │ O(log n)     │ O(log n)     │");
        System.out.println("│ AVL 树       │ O(log n)     │ O(log n)     │ O(log n)     │");
        System.out.println("│ 红黑树       │ O(log n)     │ O(log n)     │ O(log n)     │");
        System.out.println("│ B 树（m 阶）   │ O(logₘ n)    │ O(logₘ n)    │ O(logₘ n)    │");
        System.out.println("│ B+树（m 阶）  │ O(logₘ n)    │ O(logₘ n)    │ O(logₘ n)    │");
        System.out.println("└──────────────┴──────────────┴──────────────┴──────────────┘\n");

        System.out.println("【6.2 空间复杂度对比】\n");
        System.out.println("┌──────────────┬──────────────────────────────────────────┐");
        System.out.println("│ 数据结构     │ 空间复杂度                               │");
        System.out.println("├──────────────┼──────────────────────────────────────────┤");
        System.out.println("│ 二叉搜索树   │ O(n)                                     │");
        System.out.println("│ AVL 树       │ O(n)（需要存储平衡因子）                  │");
        System.out.println("│ 红黑树       │ O(n)（需要存储颜色）                      │");
        System.out.println("│ B 树         │ O(n)                                     │");
        System.out.println("│ B+树         │ O(n)                                     │");
        System.out.println("└──────────────┴──────────────────────────────────────────┘\n");

        System.out.println("【6.3 应用场景对比】\n");
        System.out.println("┌──────────────┬──────────────────────────────────────────┐");
        System.out.println("│ 数据结构     │ 典型应用场景                             │");
        System.out.println("├──────────────┼──────────────────────────────────────────┤");
        System.out.println("│ 二叉搜索树   │ 内存中的有序数据                         │");
        System.out.println("│ AVL 树       │ 查询密集型（如字典、词典）                │");
        System.out.println("│ 红黑树       │ Java TreeMap/HashMap,C++ STL map         │");
        System.out.println("│ B 树         │ 文件系统、数据库索引（逐渐被 B+树取代）   │");
        System.out.println("│ B+树         │ MySQL InnoDB、文件系统（NTFS、HFS+）     │");
        System.out.println("└──────────────┴──────────────────────────────────────────┘\n");

        System.out.println("【6.4 选择建议】\n");
        System.out.println("内存中的数据：");
        System.out.println("- 查询多、插入少 → AVL 树");
        System.out.println("- 增删改查频繁 → 红黑树");
        System.out.println("- 简单实现 → 二叉搜索树\n");

        System.out.println("磁盘上的数据（数据库、文件系统）：");
        System.out.println("- 首选 B+树（范围查询、全表扫描优势明显）");
        System.out.println("- 特殊场景可考虑 B 树\n");
    }

    // ==================== 第七部分：高频面试题 ====================

    private static void printInterviewQuestions() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第七部分：高频面试题                                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("==================== 二叉树基础 ====================\n");

        System.out.println("【问题 1】二叉树和二叉搜索树的区别？");
        System.out.println("答：");
        System.out.println("二叉树：每个节点最多有两个子节点的树");
        System.out.println("二叉搜索树：在二叉树的基础上，满足：");
        System.out.println("- 左子节点 < 父节点 < 右子节点");
        System.out.println("- 中序遍历得到有序序列\n");

        System.out.println("【问题 2】二叉树的遍历方式有哪些？");
        System.out.println("答：");
        System.out.println("1. 前序遍历：根 → 左 → 右");
        System.out.println("2. 中序遍历：左 → 根 → 右（得到有序序列）");
        System.out.println("3. 后序遍历：左 → 右 → 根");
        System.out.println("4. 层序遍历：按层从上到下遍历\n");

        System.out.println("==================== 平衡二叉树 ====================\n");

        System.out.println("【问题 3】为什么需要平衡二叉树？");
        System.out.println("答：");
        System.out.println("普通二叉搜索树可能退化为链表");
        System.out.println("例如：插入 1,2,3,4,5,6 → 所有节点都在右边");
        System.out.println("查找时间复杂度从 O(log n) 退化为 O(n)");
        System.out.println("平衡二叉树通过旋转保持平衡，保证 O(log n)\n");

        System.out.println("【问题 4】AVL 树的四种不平衡情况及旋转？");
        System.out.println("答：");
        System.out.println("1. LL 型（右单旋）：左子树的左子树导致不平衡");
        System.out.println("2. RR 型（左单旋）：右子树的右子树导致不平衡");
        System.out.println("3. LR 型（先左后右双旋）：左子树的右子树导致不平衡");
        System.out.println("4. RL 型（先右后左双旋）：右子树的左子树导致不平衡\n");

        System.out.println("==================== 红黑树 ====================\n");

        System.out.println("【问题 5】红黑树的五大性质？");
        System.out.println("答：");
        System.out.println("1. 每个节点是红色或黑色");
        System.out.println("2. 根节点是黑色");
        System.out.println("3. 叶子节点（NIL）是黑色");
        System.out.println("4. 红色节点的子节点必须是黑色");
        System.out.println("5. 从任一节点到叶子的路径包含相同数量的黑色节点\n");

        System.out.println("【问题 6】红黑树如何保证平衡？");
        System.out.println("答：");
        System.out.println("通过以下操作保证近似平衡：");
        System.out.println("1. 变色：改变节点颜色");
        System.out.println("2. 左旋：顺时针旋转");
        System.out.println("3. 右旋：逆时针旋转");
        System.out.println("树高 ≤ 2log₂(n)，查询效率 O(log n)\n");

        System.out.println("【问题 7】红黑树 vs AVL 树的区别？");
        System.out.println("答：");
        System.out.println("平衡性：");
        System.out.println("- AVL 树：严格平衡（左右子树高度差 ≤ 1）");
        System.out.println("- 红黑树：弱平衡（最长路径 ≤ 最短路径的 2 倍）\n");
        System.out.println("性能：");
        System.out.println("- AVL 树：查询更快，插入删除慢（旋转多）");
        System.out.println("- 红黑树：查询稍慢，插入删除快（旋转少）\n");
        System.out.println("应用：");
        System.out.println("- AVL 树：字典、词典等查询密集型场景");
        System.out.println("- 红黑树：Java TreeMap/HashMap、C++ STL map\n");

        System.out.println("==================== B 树与 B+树 ====================\n");

        System.out.println("【问题 8】为什么数据库索引使用 B+树而不是二叉树？");
        System.out.println("答：");
        System.out.println("1. 磁盘 IO 优化：");
        System.out.println("   - 数据量大时，二叉树很高，IO 次数多");
        System.out.println("   - B+树是\"矮胖\"结构，IO 次数少\n");
        System.out.println("2. 范围查询优势：");
        System.out.println("   - B+树叶节点用链表连接");
        System.out.println("   - 范围查询只需遍历链表\n");
        System.out.println("3. 全表扫描简单：");
        System.out.println("   - 直接遍历叶子节点链表");
        System.out.println("   - 不需要中序遍历整棵树\n");

        System.out.println("【问题 9】B 树和 B+树的区别？");
        System.out.println("答：");
        System.out.println("1. 数据存储位置：");
        System.out.println("   - B 树：所有节点都存数据");
        System.out.println("   - B+树：只有叶子节点存数据\n");
        System.out.println("2. 非叶子节点：");
        System.out.println("   - B 树：存数据 + 索引");
        System.out.println("   - B+树：只存索引\n");
        System.out.println("3. 叶子节点：");
        System.out.println("   - B 树：不连接");
        System.out.println("   - B+树：用链表连接\n");
        System.out.println("4. 查询稳定性：");
        System.out.println("   - B 树：不稳定（可能在非叶子节点找到）");
        System.out.println("   - B+树：稳定（都要到叶子节点）\n");

        System.out.println("【问题 10】为什么 B+树比 B 树更适合数据库索引？");
        System.out.println("答：");
        System.out.println("1. 磁盘读写代价更低：");
        System.out.println("   - 非叶子节点不存数据，更小");
        System.out.println("   - 同样大小的磁盘页容纳更多索引项");
        System.out.println("   - 树更矮，IO 次数更少\n");
        System.out.println("2. 范围查询极快：");
        System.out.println("   - 叶子节点用链表连接");
        System.out.println("   - 只需找到起点，然后遍历链表\n");
        System.out.println("3. 全表扫描简单：");
        System.out.println("   - 直接遍历叶子节点链表即可\n");

        System.out.println("【问题 11】B 树的阶数 m 如何选择？");
        System.out.println("答：");
        System.out.println("原则：让一个节点刚好占满一个磁盘页");
        System.out.println("例如：");
        System.out.println("- 磁盘页大小：4KB");
        System.out.println("- 关键字：8 字节，指针：4 字节");
        System.out.println("- m 阶 B 树：m*4 + (m-1)*8 ≤ 4096");
        System.out.println("- 解得：m ≈ 170\n");

        System.out.println("==================== 综合应用 ====================\n");

        System.out.println("【问题 12】Java 的 TreeMap 和 HashMap 底层实现？");
        System.out.println("答：");
        System.out.println("TreeMap：");
        System.out.println("- 基于红黑树实现");
        System.out.println("- 元素有序");
        System.out.println("- 操作时间复杂度：O(log n)\n");
        System.out.println("HashMap（JDK8+）：");
        System.out.println("- 数组 + 链表 + 红黑树");
        System.out.println("- 链表长度 > 8 且数组长度 ≥ 64 → 转红黑树");
        System.out.println("- 链表长度 < 6 → 转回链表");
        System.out.println("- 操作时间复杂度：O(1) ~ O(log n)\n");

        System.out.println("【问题 13】MySQL 索引为什么不用红黑树？");
        System.out.println("答：");
        System.out.println("1. 树高问题：");
        System.out.println("   - 红黑树是二叉树，数据量大时树很高");
        System.out.println("   - 每次查询需要多次磁盘 IO\n");
        System.out.println("2. 范围查询劣势：");
        System.out.println("   - 红黑树需要中序遍历");
        System.out.println("   - B+树只需遍历叶子节点链表\n");
        System.out.println("3. B+树优势：");
        System.out.println("   - 矮胖结构，IO 次数少");
        System.out.println("   - 范围查询快");
        System.out.println("   - 全表扫描快\n");

        System.out.println("【问题 14】什么场景下使用 AVL 树？");
        System.out.println("答：");
        System.out.println("适合查询密集型场景：");
        System.out.println("- 字典、词典（频繁查询，很少修改）");
        System.out.println("- 内存数据库的索引");
        System.out.println("- 对查询性能要求极高的场景\n");
        System.out.println("不适合：");
        System.out.println("- 频繁插入删除的场景");
        System.out.println("- 数据量巨大的场景（应该用 B+树）\n");

        System.out.println("【问题 15】设计一个海量数据的索引系统，你会选择什么数据结构？");
        System.out.println("答：");
        System.out.println("选择 B+树，原因：");
        System.out.println("1. 磁盘 IO 优化：矮胖结构，减少 IO 次数");
        System.out.println("2. 范围查询高效：叶子节点链表连接");
        System.out.println("3. 全表扫描简单：遍历叶子节点即可");
        System.out.println("4. 成熟稳定：MySQL、PostgreSQL 都在用");
        System.out.println("5. 可扩展：支持动态扩容、分区等\n");

        System.out.println("==========================================================================\n");
    }
}
