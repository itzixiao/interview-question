package cn.itzixiao.interview.mongodb.service;

import cn.itzixiao.interview.mongodb.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB 聚合查询服务
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mongodb.enabled", havingValue = "true")
public class MongoAggregationService {

    private final MongoTemplate mongoTemplate;

    public List<Map<String, Object>> countByStatus() {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.group("status").count().as("count"),
            Aggregation.project("count").and("_id").as("status"),
            Aggregation.sort(Sort.Direction.DESC, "count")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, User.class, Map.class
        );

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map map : results.getMappedResults()) {
            resultList.add(new HashMap<>(map));
        }
        return resultList;
    }

    public Map<String, Object> multiDimensionAnalysis() {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.facet()
                .and(
                    Aggregation.group("status").count().as("count"),
                    Aggregation.project("count").and("_id").as("status")
                ).as("byStatus")
                .and(
                    Aggregation.group()
                        .count().as("totalUsers")
                        .avg("age").as("avgAge")
                ).as("overall")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, User.class, Map.class
        );

        Map uniqueResult = results.getUniqueMappedResult();
        return uniqueResult != null ? new HashMap<>(uniqueResult) : new HashMap<>();
    }

    public long batchUpdateStatus(List<String> userIds, User.UserStatus newStatus) {
        Query query = Query.query(Criteria.where("_id").in(userIds));
        Update update = new Update()
            .set("status", newStatus)
            .set("updatedAt", LocalDateTime.now());
        
        long count = mongoTemplate.updateMulti(query, update, User.class).getModifiedCount();
        log.info("批量更新状态完成，影响文档数：{}", count);
        return count;
    }

    public boolean incrementBalance(String userId, BigDecimal amount) {
        Query query = Query.query(Criteria.where("_id").is(userId));
        Update update = new Update()
            .inc("balance", amount)
            .set("updatedAt", LocalDateTime.now());
        
        return mongoTemplate.updateFirst(query, update, User.class).getModifiedCount() > 0;
    }

    public boolean addTag(String userId, String tag) {
        Query query = Query.query(Criteria.where("_id").is(userId));
        Update update = new Update()
            .addToSet("tags", tag)
            .set("updatedAt", LocalDateTime.now());
        
        return mongoTemplate.updateFirst(query, update, User.class).getModifiedCount() > 0;
    }

    public boolean removeTag(String userId, String tag) {
        Query query = Query.query(Criteria.where("_id").is(userId));
        Update update = new Update()
            .pull("tags", tag)
            .set("updatedAt", LocalDateTime.now());
        
        return mongoTemplate.updateFirst(query, update, User.class).getModifiedCount() > 0;
    }
}
