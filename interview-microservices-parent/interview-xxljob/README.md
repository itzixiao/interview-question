# XXL-JOB 分布式任务调度示例模块

## 项目简介

本模块演示 XXL-JOB 分布式任务调度平台的集成和使用，包含完整的执行器配置和多种业务场景的任务示例。

## 项目结构

```
interview-xxljob/
├── src/main/java/cn/itzixiao/interview/xxljob/
│   ├── XxlJobApplication.java          # 应用启动类
│   ├── config/
│   │   └── XxlJobConfig.java           # XXL-JOB执行器配置
│   └── jobhandler/
│       ├── SampleJobHandler.java       # 示例任务
│       ├── OrderTimeoutJobHandler.java # 订单超时取消任务
│       └── DataSyncJobHandler.java     # 数据同步任务
├── src/main/resources/
│   └── application.yml                 # 配置文件
├── pom.xml                             # Maven配置
└── README.md                           # 本文件
```

## 快速开始

### 1. 部署调度中心

```bash
# 方式1：Docker部署
docker run -p 8080:8080 -v /tmp:/data/applogs \
  -e PARAMS="--spring.datasource.url=jdbc:mysql://host:3306/xxl_job" \
  --name xxl-job-admin \
  -d xuxueli/xxl-job-admin:2.4.0

# 方式2：源码部署
# 1. 下载源码：https://github.com/xuxueli/xxl-job
# 2. 执行数据库脚本：doc/db/tables_xxl_job.sql
# 3. 修改配置文件：xxl-job-admin/src/main/resources/application.properties
# 4. 启动项目：运行 XxlJobAdminApplication
```

访问调度中心：`http://localhost:8080/xxl-job-admin`

- 默认账号：admin / 123456

### 2. 启动执行器

```bash
# 编译项目
mvn clean package

# 启动应用
java -jar target/interview-xxljob-1.0.0-SNAPSHOT.jar

# 或在IDE中直接运行 XxlJobApplication
```

### 3. 配置任务

在调度中心界面配置任务：

1. **添加执行器**
    - 执行器管理 → 新增执行器
    - AppName: `interview-xxljob`（与配置文件中一致）
    - 名称: 面试项目执行器
    - 注册方式: 自动注册

2. **添加任务**
    - 任务管理 → 新增任务
    - 执行器: 选择上面添加的执行器
    - 任务描述: 示例任务
    - 负责人: itzixiao
    - Cron: `0/30 * * * * ?`（每30秒执行）
    - 运行模式: BEAN
    - JobHandler: `demoJobHandler`
    - 执行策略: 单机串行
    - 路由策略: 轮询

3. **启动任务**
    - 点击"操作" → 启动

## 任务示例说明

### 1. 示例任务 (demoJobHandler)

**功能**：简单的示例任务，演示基本用法

**配置**：

- JobHandler: `demoJobHandler`
- Cron: `0/30 * * * * ?`

### 2. 带参数任务 (paramJobHandler)

**功能**：演示如何接收和处理任务参数

**配置**：

- JobHandler: `paramJobHandler`
- 任务参数: `{"name":"test","count":10}`

### 3. 分片任务 (shardingJobHandler)

**功能**：演示大数据量分片处理

**配置**：

- JobHandler: `shardingJobHandler`
- 执行策略: 分片广播
- 路由策略: SHARDING_BROADCAST

### 4. 订单超时取消 (cancelTimeoutOrderJob)

**功能**：自动取消超时未支付的订单

**配置**：

- JobHandler: `cancelTimeoutOrderJob`
- Cron: `0 0/5 * * * ?`（每5分钟）

### 5. 数据同步 (syncUserToEsJob)

**功能**：用户数据同步到Elasticsearch

**配置**：

- JobHandler: `syncUserToEsJob`
- Cron: `0 0 2 * * ?`（每天凌晨2点）
- 执行策略: 分片广播

## 核心配置说明

### 执行器配置 (application.yml)

```yaml
xxl:
  job:
    admin:
      # 调度中心地址（集群时逗号分隔）
      addresses: http://127.0.0.1:8080/xxl-job-admin
    executor:
      # 执行器AppName（必须唯一）
      appname: interview-xxljob
      # 执行器端口
      port: 9999
      # 日志路径
      logpath: /data/applogs/xxl-job/jobhandler
      # 日志保留天数
      logretentiondays: 30
    # 通讯TOKEN（安全校验）
    accessToken: default_token
```

## 开发规范

### 1. JobHandler 开发规范

```java
@Component
@Slf4j
public class MyJobHandler {

    @XxlJob("myJobHandler")
    public void myJobHandler() {
        // 1. 记录开始日志
        XxlJobHelper.log("任务开始执行");
        
        try {
            // 2. 执行业务逻辑
            doBusiness();
            
            // 3. 记录成功日志
            XxlJobHelper.log("任务执行成功");
            
        } catch (Exception e) {
            // 4. 记录异常日志
            XxlJobHelper.log("任务执行失败: {}", e.getMessage());
            log.error("任务执行异常", e);
            
            // 5. 返回失败（触发重试）
            XxlJobHelper.handleFail("任务执行失败: " + e.getMessage());
        }
    }
}
```

### 2. 任务设计原则

1. **幂等性**：所有任务必须保证幂等性，支持重复执行
2. **异常处理**：捕获所有异常，不要抛出到框架层
3. **日志记录**：使用 XxlJobHelper.log() 记录执行日志
4. **超时控制**：设置合理的任务超时时间
5. **资源释放**：及时释放数据库连接、文件句柄等资源

## 常见问题

### Q1: 执行器无法注册到调度中心

**排查步骤**：

1. 检查调度中心地址配置是否正确
2. 检查网络是否连通
3. 检查 accessToken 是否一致
4. 查看执行器启动日志

### Q2: 任务不执行

**排查步骤**：

1. 检查 Cron 表达式是否正确
2. 检查任务状态是否为"运行中"
3. 检查执行器是否在线
4. 查看调度日志

### Q3: 如何查看执行日志

**方式1**：调度中心界面 → 任务管理 → 日志
**方式2**：执行器本地日志文件

## 参考资料

- [XXL-JOB 官方文档](https://www.xuxueli.com/xxl-job/)
- [XXL-JOB GitHub](https://github.com/xuxueli/xxl-job)
- [配套文档](../../docs/11-中间件/15-XXL-JOB分布式任务调度详解.md)

## 更新日志

### v1.0.0 (2026-03-19)

- 初始版本
- 集成 XXL-JOB 2.4.0
- 添加基础示例任务
- 添加业务场景示例（订单超时、数据同步）
