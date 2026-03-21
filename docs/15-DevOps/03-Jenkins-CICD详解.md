# Jenkins CI/CD 详解

## 一、CI/CD 概念

### 1.1 CI（持续集成）

- 频繁合并代码到主干
- 自动构建、自动测试
- 快速发现问题

### 1.2 CD（持续交付/部署）

- **持续交付**：自动化发布到测试/预发环境，手动发布到生产
- **持续部署**：全自动发布到生产环境

---

## 二、Jenkins Pipeline

### 2.1 Jenkinsfile（声明式Pipeline）

```groovy
pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'registry.example.com'
        IMAGE_NAME = 'my-app'
    }
    
    stages {
        stage('拉取代码') {
            steps {
                git branch: 'main', 
                    url: 'https://github.com/xxx/xxx.git'
            }
        }
        
        stage('编译') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('单元测试') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('构建镜像') {
            steps {
                sh """
                    docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} .
                    docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}
                """
            }
        }
        
        stage('部署到测试环境') {
            steps {
                sh """
                    ssh user@test-server "docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}"
                    ssh user@test-server "docker-compose up -d"
                """
            }
        }
        
        stage('部署到生产环境') {
            when {
                branch 'main'
            }
            input {
                message '是否部署到生产环境？'
            }
            steps {
                sh 'deploy-to-production.sh'
            }
        }
    }
    
    post {
        success {
            echo '构建成功！'
        }
        failure {
            echo '构建失败！'
            // 发送通知
        }
    }
}
```

---

## 三、Jenkins 触发方式

### 3.1 触发方式列表

1. **手动触发**：在 Jenkins 界面点击 Build
2. **定时触发（Cron 表达式）**：
   ```groovy
   triggers {
       cron('H 2 * * *')  // 每天凌晨 2 点
   }
   ```
3. **Git Webhook 触发**：代码推送时自动触发构建
4. **轮询 SCM**：
   ```groovy
   triggers {
       pollSCM('H/5 * * * *')  // 每 5 分钟检查一次
   }
   ```

---

## 四、高频面试题汇总

### Jenkins 面试题

#### **问题 1：什么是 CI/CD？**

**答：**

- CI（持续集成）：频繁合并代码，自动构建、测试，快速发现问题
- CD（持续交付）：自动化发布到测试/预发环境
- CD（持续部署）：自动化发布到生产环境

#### **问题 2：Jenkins Pipeline 有哪两种语法？**

**答：**

1. 声明式 Pipeline（推荐）：结构化语法，更易读
   ```groovy
   pipeline { stages { stage { steps { } } } }
   ```
2. 脚本式 Pipeline：更灵活，用 Groovy 语法
   ```groovy
   node { stage { } }
   ```

#### **问题 3：Jenkins 如何触发构建？**

**答：**

1. 手动触发
2. 定时触发（Cron 表达式）
3. Git Webhook（代码推送时）
4. 轮询 SCM
5. 其他 Job 触发

---

## 总结

本文详细介绍了 Jenkins CI/CD 的核心知识点：

1. **CI/CD 概念**：持续集成、持续交付、持续部署
2. **Pipeline**：声明式语法、阶段定义、环境变量
3. **触发方式**：手动、定时、Webhook、轮询

每个部分都配有高频面试题及参考答案，帮助理解和应对面试。

---

**维护者：** itzixiao  
**最后更新：** 2026-03-21  
**问题反馈：** 欢迎提 Issue 或 PR
