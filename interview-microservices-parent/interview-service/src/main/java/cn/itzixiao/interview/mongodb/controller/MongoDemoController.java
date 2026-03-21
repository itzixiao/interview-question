package cn.itzixiao.interview.mongodb.controller;

import cn.itzixiao.interview.mongodb.entity.User;
import cn.itzixiao.interview.mongodb.repository.UserRepository;
import cn.itzixiao.interview.mongodb.service.MongoAggregationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * MongoDB 演示控制器
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Slf4j
@RestController
@RequestMapping("/api/mongodb")
@Api(tags = "MongoDB 演示接口")
@ConditionalOnProperty(name = "mongodb.enabled", havingValue = "true")
public class MongoDemoController {

    private final UserRepository userRepository;
    private final MongoAggregationService aggregationService;

    public MongoDemoController(UserRepository userRepository,
            MongoAggregationService aggregationService) {
        this.userRepository = userRepository;
        this.aggregationService = aggregationService;
    }

    @PostMapping("/users")
    @ApiOperation("创建用户")
    public Map<String, Object> createUser(@RequestBody User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        if (user.getStatus() == null) {
            user.setStatus(User.UserStatus.ACTIVE);
        }
        if (user.getBalance() == null) {
            user.setBalance(BigDecimal.ZERO);
        }
        
        User saved = userRepository.save(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "用户创建成功");
        result.put("data", saved);
        return result;
    }

    @GetMapping("/users/{id}")
    @ApiOperation("根据 ID 查询用户")
    public Map<String, Object> getUser(@PathVariable String id) {
        Optional<User> user = userRepository.findById(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", user.isPresent());
        result.put("data", user.orElse(null));
        result.put("message", user.isPresent() ? "查询成功" : "用户不存在");
        return result;
    }

    @GetMapping("/users")
    @ApiOperation("查询所有用户")
    public Map<String, Object> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", users);
        result.put("count", users.size());
        return result;
    }

    @GetMapping("/users/page")
    @ApiOperation("分页查询用户")
    public Map<String, Object> getUsersByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) User.UserStatus status) {
        
        PageRequest pageRequest = PageRequest.of(page, size, 
            Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<User> userPage;
        if (status != null) {
            userPage = userRepository.findByStatus(status, pageRequest);
        } else {
            userPage = userRepository.findAll(pageRequest);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", userPage.getContent());
        result.put("totalElements", userPage.getTotalElements());
        result.put("totalPages", userPage.getTotalPages());
        result.put("currentPage", page);
        return result;
    }

    @PutMapping("/users/{id}")
    @ApiOperation("更新用户")
    public Map<String, Object> updateUser(@PathVariable String id, @RequestBody User user) {
        Optional<User> existing = userRepository.findById(id);
        
        Map<String, Object> result = new HashMap<>();
        if (!existing.isPresent()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        
        User toUpdate = existing.get();
        if (user.getName() != null) toUpdate.setName(user.getName());
        if (user.getEmail() != null) toUpdate.setEmail(user.getEmail());
        if (user.getAge() != null) toUpdate.setAge(user.getAge());
        if (user.getStatus() != null) toUpdate.setStatus(user.getStatus());
        if (user.getAddress() != null) toUpdate.setAddress(user.getAddress());
        if (user.getTags() != null) toUpdate.setTags(user.getTags());
        if (user.getBalance() != null) toUpdate.setBalance(user.getBalance());
        
        toUpdate.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(toUpdate);
        
        result.put("success", true);
        result.put("message", "更新成功");
        result.put("data", saved);
        return result;
    }

    @DeleteMapping("/users/{id}")
    @ApiOperation("删除用户")
    public Map<String, Object> deleteUser(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        if (!userRepository.existsById(id)) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        
        userRepository.deleteById(id);
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    @GetMapping("/users/status/{status}")
    @ApiOperation("按状态查询用户")
    public Map<String, Object> getUsersByStatus(@PathVariable User.UserStatus status) {
        List<User> users = userRepository.findByStatus(status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", users);
        result.put("count", users.size());
        return result;
    }

    @GetMapping("/users/tag/{tag}")
    @ApiOperation("按标签查询用户")
    public Map<String, Object> getUsersByTag(@PathVariable String tag) {
        List<User> users = userRepository.findByTags(tag);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", users);
        result.put("count", users.size());
        return result;
    }

    @GetMapping("/stats/by-status")
    @ApiOperation("按状态分组统计用户数量")
    public Map<String, Object> countByStatus() {
        List<Map<String, Object>> stats = aggregationService.countByStatus();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", stats);
        return result;
    }

    @GetMapping("/stats/multi-dimension")
    @ApiOperation("多维度统计分析")
    public Map<String, Object> multiDimensionAnalysis() {
        Map<String, Object> stats = aggregationService.multiDimensionAnalysis();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", stats);
        return result;
    }

    @PostMapping("/users/batch-update-status")
    @ApiOperation("批量更新用户状态")
    public Map<String, Object> batchUpdateStatus(
            @RequestBody List<String> userIds,
            @RequestParam User.UserStatus status) {
        
        long count = aggregationService.batchUpdateStatus(userIds, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "批量更新成功");
        result.put("modifiedCount", count);
        return result;
    }

    @PostMapping("/users/{id}/balance/increment")
    @ApiOperation("增加用户余额")
    public Map<String, Object> incrementBalance(
            @PathVariable String id,
            @RequestParam BigDecimal amount) {
        
        boolean success = aggregationService.incrementBalance(id, amount);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "余额增加成功" : "用户不存在");
        return result;
    }

    @PostMapping("/users/{id}/tags")
    @ApiOperation("为用户添加标签")
    public Map<String, Object> addTag(
            @PathVariable String id,
            @RequestParam String tag) {
        
        boolean success = aggregationService.addTag(id, tag);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "标签添加成功" : "用户不存在");
        return result;
    }

    @DeleteMapping("/users/{id}/tags")
    @ApiOperation("移除用户标签")
    public Map<String, Object> removeTag(
            @PathVariable String id,
            @RequestParam String tag) {
        
        boolean success = aggregationService.removeTag(id, tag);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "标签移除成功" : "用户不存在或标签不存在");
        return result;
    }

    @GetMapping("/stats/overview")
    @ApiOperation("获取用户统计概览")
    public Map<String, Object> getStatsOverview() {
        long total = userRepository.count();
        long active = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long inactive = userRepository.countByStatus(User.UserStatus.INACTIVE);
        long suspended = userRepository.countByStatus(User.UserStatus.SUSPENDED);
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("active", active);
        data.put("inactive", inactive);
        data.put("suspended", suspended);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }
}
