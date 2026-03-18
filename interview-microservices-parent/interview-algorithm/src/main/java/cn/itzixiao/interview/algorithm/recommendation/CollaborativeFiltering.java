package cn.itzixiao.interview.algorithm.recommendation;

import java.util.*;

/**
 * 协同过滤推荐算法详解与实现
 *
 * <p>协同过滤（Collaborative Filtering）是推荐系统中最经典、应用最广泛的算法。
 * 核心思想：利用用户群体的行为数据，发现用户或物品之间的相似性，进行推荐。
 *
 * <p>主要类型：
 * 1. 基于用户的协同过滤（User-Based CF）：找到相似用户，推荐他们喜欢的物品
 * 2. 基于物品的协同过滤（Item-Based CF）：找到相似物品，推荐与用户喜欢物品相似的物品
 * 3. 基于模型的协同过滤：使用矩阵分解等机器学习方法
 *
 * <p>相似度计算方法：
 * - 余弦相似度（Cosine Similarity）
 * - 皮尔逊相关系数（Pearson Correlation）
 * - 杰卡德相似度（Jaccard Similarity）
 * - 欧几里得距离（Euclidean Distance）
 *
 * <p>应用场景：
 * - 电商推荐（亚马逊、淘宝）
 * - 视频推荐（Netflix、YouTube）
 * - 音乐推荐（Spotify、网易云音乐）
 * - 社交推荐（Facebook、微博）
 *
 * @author itzixiao
 * @since 2024-01-01
 */
public class CollaborativeFiltering {

    /**
     * 用户-物品评分矩阵
     * Map<用户ID, Map<物品ID, 评分>>
     */
    private Map<String, Map<String, Double>> userItemRatings;

    public CollaborativeFiltering() {
        this.userItemRatings = new HashMap<>();
    }

    /**
     * 添加用户评分数据
     */
    public void addRating(String userId, String itemId, double rating) {
        userItemRatings.computeIfAbsent(userId, k -> new HashMap<>())
                .put(itemId, rating);
    }

    /**
     * 1. 余弦相似度计算
     *
     * <p>公式：cos(A,B) = (A·B) / (||A|| × ||B||)
     * 范围：[-1, 1]，1表示完全相似，0表示无关，-1表示相反
     *
     * @param ratingsA 用户A的评分向量
     * @param ratingsB 用户B的评分向量
     * @return 相似度
     */
    public double cosineSimilarity(Map<String, Double> ratingsA, Map<String, Double> ratingsB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        // 计算共同评分的物品
        Set<String> commonItems = new HashSet<>(ratingsA.keySet());
        commonItems.retainAll(ratingsB.keySet());

        if (commonItems.isEmpty()) {
            return 0.0;
        }

        for (String item : commonItems) {
            double ratingA = ratingsA.get(item);
            double ratingB = ratingsB.get(item);
            dotProduct += ratingA * ratingB;
        }

        for (double rating : ratingsA.values()) {
            normA += rating * rating;
        }

        for (double rating : ratingsB.values()) {
            normB += rating * rating;
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 2. 皮尔逊相关系数
     *
     * <p>公式：r = Σ((x-x̄)(y-ȳ)) / √(Σ(x-x̄)² × Σ(y-ȳ)²)
     * 特点：考虑了评分的平均值，减少用户评分偏置的影响
     *
     * @param ratingsA 用户A的评分
     * @param ratingsB 用户B的评分
     * @return 相关系数
     */
    public double pearsonCorrelation(Map<String, Double> ratingsA, Map<String, Double> ratingsB) {
        Set<String> commonItems = new HashSet<>(ratingsA.keySet());
        commonItems.retainAll(ratingsB.keySet());

        int n = commonItems.size();
        if (n == 0) {
            return 0.0;
        }

        double sumA = 0.0, sumB = 0.0;
        for (String item : commonItems) {
            sumA += ratingsA.get(item);
            sumB += ratingsB.get(item);
        }

        double avgA = sumA / n;
        double avgB = sumB / n;

        double numerator = 0.0;
        double sumSqA = 0.0;
        double sumSqB = 0.0;

        for (String item : commonItems) {
            double diffA = ratingsA.get(item) - avgA;
            double diffB = ratingsB.get(item) - avgB;
            numerator += diffA * diffB;
            sumSqA += diffA * diffA;
            sumSqB += diffB * diffB;
        }

        double denominator = Math.sqrt(sumSqA * sumSqB);
        return denominator == 0 ? 0.0 : numerator / denominator;
    }

    /**
     * 3. 杰卡德相似度
     *
     * <p>公式：J(A,B) = |A∩B| / |A∪B|
     * 适用场景：仅考虑是否交互（0/1），不考虑具体评分
     *
     * @param itemsA 用户A的物品集合
     * @param itemsB 用户B的物品集合
     * @return 相似度
     */
    public double jaccardSimilarity(Set<String> itemsA, Set<String> itemsB) {
        Set<String> intersection = new HashSet<>(itemsA);
        intersection.retainAll(itemsB);

        Set<String> union = new HashSet<>(itemsA);
        union.addAll(itemsB);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * 4. 基于用户的协同过滤推荐
     *
     * <p>算法步骤：
     * 1. 计算目标用户与其他用户的相似度
     * 2. 选择最相似的K个邻居
     * 3. 预测目标用户对未评分物品的评分
     * 4. 返回评分最高的N个物品
     *
     * @param targetUser 目标用户
     * @param k          邻居数量
     * @param n          推荐数量
     * @return 推荐列表（物品ID和预测评分）
     */
    public List<Map.Entry<String, Double>> userBasedRecommend(
            String targetUser, int k, int n) {

        Map<String, Double> targetRatings = userItemRatings.get(targetUser);
        if (targetRatings == null) {
            return new ArrayList<>();
        }

        // 计算与所有其他用户的相似度
        Map<String, Double> similarities = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : userItemRatings.entrySet()) {
            String otherUser = entry.getKey();
            if (!otherUser.equals(targetUser)) {
                double sim = pearsonCorrelation(targetRatings, entry.getValue());
                if (sim > 0) {  // 只考虑正相关用户
                    similarities.put(otherUser, sim);
                }
            }
        }

        // 选择最相似的K个邻居
        List<Map.Entry<String, Double>> neighbors = similarities.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(k)
                .collect(java.util.stream.Collectors.toList());

        // 预测评分
        Map<String, Double> predictions = new HashMap<>();
        Map<String, Double> simSums = new HashMap<>();

        for (Map.Entry<String, Double> neighbor : neighbors) {
            String neighborId = neighbor.getKey();
            double similarity = neighbor.getValue();
            Map<String, Double> neighborRatings = userItemRatings.get(neighborId);

            for (Map.Entry<String, Double> itemRating : neighborRatings.entrySet()) {
                String item = itemRating.getKey();
                if (!targetRatings.containsKey(item)) {  // 只预测未评分的物品
                    predictions.merge(item, similarity * itemRating.getValue(), Double::sum);
                    simSums.merge(item, similarity, Double::sum);
                }
            }
        }

        // 归一化并排序
        Map<String, Double> finalPredictions = new HashMap<>();
        for (String item : predictions.keySet()) {
            double score = predictions.get(item) / simSums.get(item);
            finalPredictions.put(item, score);
        }

        return finalPredictions.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(n)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 5. 基于物品的协同过滤推荐
     *
     * <p>优点：
     * - 物品数量通常少于用户数量，计算量小
     * - 物品相似度比用户相似度更稳定
     * - 可以离线计算物品相似度
     *
     * @param targetUser 目标用户
     * @param n          推荐数量
     * @return 推荐列表
     */
    public List<Map.Entry<String, Double>> itemBasedRecommend(String targetUser, int n) {
        Map<String, Double> targetRatings = userItemRatings.get(targetUser);
        if (targetRatings == null) {
            return new ArrayList<>();
        }

        // 构建物品-用户倒排表
        Map<String, Map<String, Double>> itemUserRatings = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : userItemRatings.entrySet()) {
            String user = entry.getKey();
            for (Map.Entry<String, Double> rating : entry.getValue().entrySet()) {
                itemUserRatings.computeIfAbsent(rating.getKey(), k -> new HashMap<>())
                        .put(user, rating.getValue());
            }
        }

        // 计算物品相似度矩阵
        Map<String, Map<String, Double>> itemSimilarities = new HashMap<>();
        for (String item1 : itemUserRatings.keySet()) {
            itemSimilarities.put(item1, new HashMap<>());
            for (String item2 : itemUserRatings.keySet()) {
                if (!item1.equals(item2)) {
                    double sim = cosineSimilarity(
                            itemUserRatings.get(item1),
                            itemUserRatings.get(item2)
                    );
                    if (sim > 0) {
                        itemSimilarities.get(item1).put(item2, sim);
                    }
                }
            }
        }

        // 预测评分
        Map<String, Double> predictions = new HashMap<>();
        Map<String, Double> simSums = new HashMap<>();

        for (String ratedItem : targetRatings.keySet()) {
            double rating = targetRatings.get(ratedItem);
            Map<String, Double> similarItems = itemSimilarities.get(ratedItem);

            if (similarItems != null) {
                for (Map.Entry<String, Double> entry : similarItems.entrySet()) {
                    String candidateItem = entry.getKey();
                    if (!targetRatings.containsKey(candidateItem)) {
                        double sim = entry.getValue();
                        predictions.merge(candidateItem, sim * rating, Double::sum);
                        simSums.merge(candidateItem, sim, Double::sum);
                    }
                }
            }
        }

        // 归一化并排序
        Map<String, Double> finalPredictions = new HashMap<>();
        for (String item : predictions.keySet()) {
            double score = predictions.get(item) / simSums.get(item);
            finalPredictions.put(item, score);
        }

        return finalPredictions.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(n)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 6. SVD矩阵分解（简化版）
     *
     * <p>核心思想：将高维的用户-物品矩阵分解为低维的潜在因子矩阵
     * R ≈ P × Q^T，其中P是用户特征矩阵，Q是物品特征矩阵
     *
     * @param numFactors     潜在因子数量
     * @param iterations     迭代次数
     * @param learningRate   学习率
     * @param regularization 正则化参数
     */
    public void matrixFactorization(int numFactors, int iterations,
                                    double learningRate, double regularization) {
        // 获取所有用户和物品
        Set<String> users = new HashSet<>(userItemRatings.keySet());
        Set<String> items = new HashSet<>();
        for (Map<String, Double> ratings : userItemRatings.values()) {
            items.addAll(ratings.keySet());
        }

        List<String> userList = new ArrayList<>(users);
        List<String> itemList = new ArrayList<>(items);

        int numUsers = userList.size();
        int numItems = itemList.size();

        // 初始化用户和物品特征矩阵
        Random random = new Random(42);
        double[][] P = new double[numUsers][numFactors];  // 用户特征
        double[][] Q = new double[numItems][numFactors];  // 物品特征

        for (int i = 0; i < numUsers; i++) {
            for (int f = 0; f < numFactors; f++) {
                P[i][f] = random.nextGaussian() * 0.01;
            }
        }

        for (int j = 0; j < numItems; j++) {
            for (int f = 0; f < numFactors; f++) {
                Q[j][f] = random.nextGaussian() * 0.01;
            }
        }

        // 随机梯度下降训练
        for (int iter = 0; iter < iterations; iter++) {
            for (int u = 0; u < numUsers; u++) {
                String userId = userList.get(u);
                Map<String, Double> ratings = userItemRatings.get(userId);

                for (Map.Entry<String, Double> entry : ratings.entrySet()) {
                    String itemId = entry.getKey();
                    double rating = entry.getValue();
                    int i = itemList.indexOf(itemId);

                    // 预测评分
                    double pred = 0;
                    for (int f = 0; f < numFactors; f++) {
                        pred += P[u][f] * Q[i][f];
                    }

                    // 计算误差
                    double error = rating - pred;

                    // 更新特征向量
                    for (int f = 0; f < numFactors; f++) {
                        double puf = P[u][f];
                        double qif = Q[i][f];

                        P[u][f] += learningRate * (error * qif - regularization * puf);
                        Q[i][f] += learningRate * (error * puf - regularization * qif);
                    }
                }
            }

            // 衰减学习率
            learningRate *= 0.9;
        }

        // 输出训练结果
        System.out.println("矩阵分解完成！");
        System.out.println("用户特征矩阵维度: " + numUsers + " × " + numFactors);
        System.out.println("物品特征矩阵维度: " + numItems + " × " + numFactors);
    }

    /**
     * 7. 冷启动处理 - 基于热门度的推荐
     *
     * <p>新用户没有历史行为时，推荐最热门的物品
     */
    public List<Map.Entry<String, Double>> popularityBasedRecommend(int n) {
        Map<String, Double> itemPopularity = new HashMap<>();

        for (Map<String, Double> ratings : userItemRatings.values()) {
            for (Map.Entry<String, Double> entry : ratings.entrySet()) {
                itemPopularity.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        return itemPopularity.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(n)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 8. 计算推荐系统的评估指标
     *
     * @param testData 测试数据
     * @return 评估指标
     */
    public Map<String, Double> evaluate(Map<String, Map<String, Double>> testData) {
        double mae = 0.0;  // 平均绝对误差
        double rmse = 0.0; // 均方根误差
        int count = 0;

        for (Map.Entry<String, Map<String, Double>> entry : testData.entrySet()) {
            String user = entry.getKey();
            Map<String, Double> actualRatings = entry.getValue();

            List<Map.Entry<String, Double>> recommendations = userBasedRecommend(user, 5, 10);
            Map<String, Double> predMap = new HashMap<>();
            for (Map.Entry<String, Double> rec : recommendations) {
                predMap.put(rec.getKey(), rec.getValue());
            }

            for (Map.Entry<String, Double> actual : actualRatings.entrySet()) {
                Double pred = predMap.get(actual.getKey());
                if (pred != null) {
                    double error = actual.getValue() - pred;
                    mae += Math.abs(error);
                    rmse += error * error;
                    count++;
                }
            }
        }

        Map<String, Double> metrics = new HashMap<>();
        if (count > 0) {
            metrics.put("MAE", mae / count);
            metrics.put("RMSE", Math.sqrt(rmse / count));
        }
        return metrics;
    }

    // ==================== 演示与测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 协同过滤推荐算法演示 ==========\n");

        CollaborativeFiltering cf = new CollaborativeFiltering();

        // 构建示例数据集（电影评分）
        System.out.println("1. 构建用户-电影评分数据集:");
        // 用户A：喜欢动作片
        cf.addRating("UserA", "Action1", 5.0);
        cf.addRating("UserA", "Action2", 4.5);
        cf.addRating("UserA", "Action3", 4.0);
        cf.addRating("UserA", "Comedy1", 2.0);

        // 用户B：也喜欢动作片
        cf.addRating("UserB", "Action1", 4.5);
        cf.addRating("UserB", "Action2", 5.0);
        cf.addRating("UserB", "Action3", 4.5);
        cf.addRating("UserB", "Drama1", 3.0);

        // 用户C：喜欢喜剧
        cf.addRating("UserC", "Comedy1", 5.0);
        cf.addRating("UserC", "Comedy2", 4.5);
        cf.addRating("UserC", "Action1", 3.0);

        // 用户D：混合口味
        cf.addRating("UserD", "Action1", 4.0);
        cf.addRating("UserD", "Comedy1", 4.0);
        cf.addRating("UserD", "Drama1", 5.0);

        // 目标用户
        cf.addRating("Target", "Action1", 5.0);
        cf.addRating("Target", "Action2", 4.5);
        cf.addRating("Target", "Comedy1", 2.5);

        System.out.println("   用户数: 5");
        System.out.println("   电影数: 6");

        // 2. 相似度计算演示
        System.out.println("\n2. 用户相似度计算:");
        Map<String, Double> targetRatings = cf.userItemRatings.get("Target");

        for (String user : new String[]{"UserA", "UserB", "UserC", "UserD"}) {
            Map<String, Double> userRatings = cf.userItemRatings.get(user);
            double cosSim = cf.cosineSimilarity(targetRatings, userRatings);
            double pearsonSim = cf.pearsonCorrelation(targetRatings, userRatings);
            System.out.printf("   Target vs %s: 余弦相似度=%.3f, 皮尔逊=%.3f%n",
                    user, cosSim, pearsonSim);
        }

        // 3. 基于用户的推荐
        System.out.println("\n3. 基于用户的协同过滤推荐 (K=2, N=3):");
        List<Map.Entry<String, Double>> userRecs = cf.userBasedRecommend("Target", 2, 3);
        for (Map.Entry<String, Double> rec : userRecs) {
            System.out.printf("   推荐电影: %s, 预测评分: %.2f%n", rec.getKey(), rec.getValue());
        }

        // 4. 基于物品的推荐
        System.out.println("\n4. 基于物品的协同过滤推荐 (N=3):");
        List<Map.Entry<String, Double>> itemRecs = cf.itemBasedRecommend("Target", 3);
        for (Map.Entry<String, Double> rec : itemRecs) {
            System.out.printf("   推荐电影: %s, 预测评分: %.2f%n", rec.getKey(), rec.getValue());
        }

        // 5. 热门推荐（冷启动）
        System.out.println("\n5. 热门电影推荐（冷启动方案）:");
        List<Map.Entry<String, Double>> popular = cf.popularityBasedRecommend(3);
        for (Map.Entry<String, Double> rec : popular) {
            System.out.printf("   电影: %s, 总评分: %.1f%n", rec.getKey(), rec.getValue());
        }

        // 6. 矩阵分解演示
        System.out.println("\n6. SVD矩阵分解训练:");
        cf.matrixFactorization(3, 100, 0.01, 0.02);

        // 7. 相似度方法对比
        System.out.println("\n7. 不同相似度方法对比:");
        Set<String> itemsA = new HashSet<>(Arrays.asList("A", "B", "C", "D"));
        Set<String> itemsB = new HashSet<>(Arrays.asList("B", "C", "D", "E"));
        double jaccard = cf.jaccardSimilarity(itemsA, itemsB);
        System.out.printf("   集合A=%s, 集合B=%s%n", itemsA, itemsB);
        System.out.printf("   杰卡德相似度: %.3f%n", jaccard);

        // 8. 实际应用场景
        System.out.println("\n8. 电商推荐场景模拟:");
        CollaborativeFiltering ecommerce = new CollaborativeFiltering();

        // 模拟购买数据（0/1表示是否购买）
        String[] users = {"User1", "User2", "User3", "User4", "User5"};
        String[] products = {"iPhone", "AirPods", "MacBook", "iPad", "AppleWatch"};

        // 用户购买记录
        ecommerce.addRating("User1", "iPhone", 1);
        ecommerce.addRating("User1", "AirPods", 1);
        ecommerce.addRating("User2", "iPhone", 1);
        ecommerce.addRating("User2", "AirPods", 1);
        ecommerce.addRating("User2", "MacBook", 1);
        ecommerce.addRating("User3", "MacBook", 1);
        ecommerce.addRating("User3", "iPad", 1);
        ecommerce.addRating("User4", "iPhone", 1);
        ecommerce.addRating("User4", "iPad", 1);
        ecommerce.addRating("User5", "AirPods", 1);
        ecommerce.addRating("User5", "AppleWatch", 1);

        // 新用户购买了iPhone，推荐什么？
        ecommerce.addRating("NewUser", "iPhone", 1);
        System.out.println("   新用户购买了 iPhone");
        List<Map.Entry<String, Double>> recs = ecommerce.itemBasedRecommend("NewUser", 3);
        System.out.println("   推荐商品:");
        for (Map.Entry<String, Double> rec : recs) {
            System.out.printf("     - %s (相关度: %.3f)%n", rec.getKey(), rec.getValue());
        }

        System.out.println("\n========== 演示结束 ==========");
    }
}
