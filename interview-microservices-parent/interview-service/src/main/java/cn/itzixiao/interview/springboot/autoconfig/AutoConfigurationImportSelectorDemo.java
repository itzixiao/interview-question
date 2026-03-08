package cn.itzixiao.interview.springboot.autoconfig;

import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.util.List;
import java.util.Set;

/**
 * AutoConfigurationImportSelector 详解
 *
 * 这是 Spring Boot 自动装配的核心类，实现了 ImportSelector 接口
 *
 * 核心方法执行流程：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  selectImports(AnnotationMetadata annotationMetadata)        │
 * │      │                                                      │
 * │      ▼                                                      │
 * │  getAutoConfigurationEntry(annotationMetadata)               │
 * │      │                                                      │
 * │      ├── getCandidateConfigurations(annotationMetadata)      │
 * │      │      └── SpringFactoriesLoader.loadFactoryNames()     │
 * │      │                                                      │
 * │      ├── removeDuplicates(configurations)                    │
 * │      ├── getExclusions(annotationMetadata)                   │
 * │      ├── checkExcludedClasses(configurations, exclusions)    │
 * │      └── filter(configurations, autoConfigurationMetadata)   │
 * │             └── 执行 @Conditional 条件过滤                   │
 * │                                                              │
 * │  返回：String[] 配置类全限定名数组                            │
 * └─────────────────────────────────────────────────────────────┘
 */
public class AutoConfigurationImportSelectorDemo {

    /**
     * 核心流程说明
     */
    public static void main(String[] args) {
        System.out.println("========== AutoConfigurationImportSelector 详解 ==========\n");

        System.out.println("【类继承关系】");
        System.out.println("  AutoConfigurationImportSelector");
        System.out.println("      implements DeferredImportSelector");
        System.out.println("          extends ImportSelector");
        System.out.println("              （用于动态导入配置类）\n");

        System.out.println("【核心方法：selectImports】");
        System.out.println("  1. 获取自动配置入口");
        System.out.println("  2. 返回需要导入的配置类数组\n");

        System.out.println("【关键方法：getAutoConfigurationEntry】");
        System.out.println("  1. 获取候选配置类（spring.factories）");
        System.out.println("  2. 去重");
        System.out.println("  3. 排除指定类");
        System.out.println("  4. 条件过滤");
        System.out.println("  5. 返回配置类集合\n");

        System.out.println("【条件过滤机制】");
        System.out.println("  - 读取 @ConditionalOnClass 等注解");
        System.out.println("  - 使用 ConditionEvaluator 评估条件");
        System.out.println("  - 不满足条件的配置类被过滤\n");
    }

    /**
     * 模拟 selectImports 方法
     */
    public String[] simulateSelectImports(AnnotationMetadata metadata) {
        System.out.println("\n【模拟】selectImports 执行流程\n");

        // 1. 获取自动配置入口
        AutoConfigurationEntry entry = getAutoConfigurationEntry(metadata);

        // 2. 返回配置类名称数组
        return entry.getConfigurations().toArray(new String[0]);
    }

    /**
     * 模拟 getAutoConfigurationEntry 方法
     */
    private AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata metadata) {
        System.out.println("步骤1: 获取候选配置类...");

        // 1. 从 spring.factories 加载
        List<String> candidates = SpringFactoriesLoader.loadFactoryNames(
                org.springframework.boot.autoconfigure.EnableAutoConfiguration.class,
                this.getClass().getClassLoader()
        );
        System.out.println("  加载到 " + candidates.size() + " 个候选配置类");

        // 2. 去重
        Set<String> configurations = removeDuplicates(candidates);
        System.out.println("  去重后剩余 " + configurations.size() + " 个");

        // 3. 获取排除的类
        Set<String> exclusions = getExclusions(metadata);
        System.out.println("  排除 " + exclusions.size() + " 个类");

        // 4. 检查排除类
        checkExcludedClasses(configurations, exclusions);
        configurations.removeAll(exclusions);

        // 5. 条件过滤
        configurations = filter(configurations, metadata);
        System.out.println("  条件过滤后剩余 " + configurations.size() + " 个");

        return new AutoConfigurationEntry(configurations, exclusions);
    }

    private Set<String> removeDuplicates(List<String> list) {
        return new java.util.LinkedHashSet<>(list);
    }

    private Set<String> getExclusions(AnnotationMetadata metadata) {
        // 从 @EnableAutoConfiguration.exclude 获取
        return new java.util.HashSet<>();
    }

    private void checkExcludedClasses(Set<String> configurations, Set<String> exclusions) {
        // 检查排除类是否有效
    }

    private Set<String> filter(Set<String> configurations, AnnotationMetadata metadata) {
        // 执行条件过滤
        // 实际使用 ConditionEvaluator
        return configurations;
    }

    /**
     * 自动配置入口内部类
     */
    private static class AutoConfigurationEntry {
        private final Set<String> configurations;
        private final Set<String> exclusions;

        public AutoConfigurationEntry(Set<String> configurations, Set<String> exclusions) {
            this.configurations = configurations;
            this.exclusions = exclusions;
        }

        public Set<String> getConfigurations() {
            return configurations;
        }
    }

    /**
     * 条件评估示例
     */
    public void conditionEvaluation() {
        System.out.println("\n========== 条件评估示例 ==========\n");

        System.out.println("【OnClassCondition】");
        System.out.println("  检查：@ConditionalOnClass / @ConditionalOnMissingClass");
        System.out.println("  逻辑：检查类路径是否存在指定类\n");

        System.out.println("【OnBeanCondition】");
        System.out.println("  检查：@ConditionalOnBean / @ConditionalOnMissingBean");
        System.out.println("  逻辑：检查 Spring 容器是否存在指定 Bean\n");

        System.out.println("【OnPropertyCondition】");
        System.out.println("  检查：@ConditionalOnProperty");
        System.out.println("  逻辑：检查配置属性是否满足条件\n");

        System.out.println("【OnWebApplicationCondition】");
        System.out.println("  检查：@ConditionalOnWebApplication");
        System.out.println("  逻辑：检查是否是 Web 应用\n");
    }
}
