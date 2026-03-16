# 面试题目格式规范

## 标准格式

所有技术文档中的面试题目必须遵循以下统一格式：

### 基本结构

```markdown
## X、高频面试题

**问题 1：问题内容？**

**答：**

答案内容...

**问题 2：问题内容？**

**答：**

答案内容...
```

### 格式要求

#### 1. 章节标题

- 使用 `## X、高频面试题` 格式
- X 为中文数字或阿拉伯数字
- 例如：`## 五、高频面试题` 或 `## 7、高频面试题`

#### 2. 问题格式

- **正确格式：** `**问题 X：问题内容？**`
- **错误格式：** `### Q1: 问题内容？` 或 `**Q1: 问题内容？**`
- 问题编号使用阿拉伯数字（1, 2, 3...）
- 问题必须以问号结尾

#### 3. 答案格式

- **正确格式：** `**答：**`
- **错误格式：** `**答案：**` 或 `答:` 或 `Answer:`
- 答案后空一行再开始答案内容

#### 4. 答案内容组织

答案内容可以使用以下方式组织：

**列表式：**

```markdown
**答：**

1. **要点 1** - 说明文字
2. **要点 2** - 说明文字
3. **要点 3** - 说明文字
```

**表格式：**

```markdown
**答：**

| 对比项 | 选项 A | 选项 B |
|--------|--------|--------|
| 特性 1 | 描述   | 描述   |
| 特性 2 | 描述   | 描述   |
```

**代码式：**

```markdown
**答：**

```java
public class Example {
    // 示例代码
}
```

```

**混合式：**
```markdown
**答：**

核心观点...

**关键点：**
- 要点 1
- 要点 2

**示例：**
```java
// 代码示例
```

**注意事项：**

- 注意点 1
- 注意点 2

```

## 格式对比

### ✅ 正确示例

```markdown
## 五、高频面试题

**问题 1：什么是 RPC？它的核心优势是什么？**

**答：**

RPC（Remote Procedure Call）远程过程调用，是一种通过网络从远程计算机程序上请求服务的软件协议。

**核心优势：**

1. **透明性** - 对调用者屏蔽了底层网络通信细节
2. **高效性** - 相比 RESTful API，RPC 通常使用二进制协议
3. **强类型** - 接口定义明确，编译时就能检查类型错误
4. **支持复杂数据结构** - 可以直接传递对象、集合等
5. **双向通信** - 支持同步/异步调用、流式传输等多种模式

**常见 RPC 框架：** Dubbo、gRPC、Thrift、Spring Cloud OpenFeign
```

### ❌ 错误示例

```markdown
## Q&A

### Q1: 什么是 RPC？它的核心优势是什么？

**答案：**

RPC（Remote Procedure Call）...

### Q2: RPC 的完整工作流程是什么？

**答案：**

1. 服务暴露
2. 服务发现
...
```

## 自动化检查脚本

可以使用以下 Python 脚本检查和修复格式：

```python
import re
import glob

def fix_interview_format(filepath):
    """修改面试题目格式为统一格式"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 替换 Q1-Q8 格式为**问题 X**格式
    def replace_question(match):
        question_num = match.group(1)
        question_text = match.group(2)
        return f'**问题 {question_num}：{question_text}**\n\n**答：**'
    
    pattern = r'### Q(\d+): ([^\n]+)'
    content = re.sub(pattern, replace_question, content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f'已更新：{filepath}')

# 批量处理所有 markdown 文件
files = glob.glob('docs/**/*.md', recursive=True)
for file in files:
    fix_interview_format(file)
```

## 格式迁移步骤

1. **备份原文件**
2. **运行自动化脚本** 批量替换格式
3. **手动检查** 确保特殊题目格式正确
4. **验证输出** 查看渲染后的 Markdown 效果

## 格式优势

1. **统一性** - 所有文档保持一致的风格
2. **可读性** - 问题和答案清晰区分
3. **易维护** - 便于后续编辑和扩展
4. **自动化** - 可以通过脚本批量处理和检查

## 适用范围

本格式规范适用于所有技术文档中的面试题目部分，包括但不限于：

- Java基础
- Spring框架
- MySQL数据库
- Redis 缓存
- 中间件（MyBatis、Nacos、Sentinel 等）
- 微服务（Spring Cloud、Dubbo 等）
- 分布式系统
- 算法与数据结构
- 设计模式

---

**版本：** 1.0  
**最后更新：** 2026-03-08  
**维护者：** itzixiao
