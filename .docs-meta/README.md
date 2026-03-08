# 文档规范索引

这是技术文档项目的规范和维护指南集合。

## 📚 规范文档列表

### 1. [面试题目格式规范](./INTERVIEW_QUESTION_FORMAT.md)

**内容：**
- 面试题目统一格式标准
- 正确/错误示例对比
- 自动化检查脚本
- 格式迁移步骤

**适用场景：** 编写或修改任何包含面试题目的技术文档

---

### 2. [目录命名与 README 维护规范](./DOC_NAMING_AND_README_STANDARD.md)

**内容：**
- 目录和文件命名规则（禁止空格）
- README.md 同步更新要求
- 模板示例和自动化脚本
- 历史问题处理方案

**适用场景：** 新增文档、创建目录、维护 README

---

### 3. [工具脚本说明](./TOOLS_AND_SCRIPTS.md)

**内容：**
- 面试题目格式检查脚本
- 目录命名规范检查脚本
- 空格批量修复脚本
- Git Hooks 和 CI/CD集成

**适用场景：** 日常检查、批量修复、自动化集成

---

### 4. [维护工作手册](./MAINTENANCE_GUIDE.md)

**内容：**
- 快速入门指南
- 常用工具脚本
- 检查清单
- 最佳实践
- 故障排查

**适用场景：** 新加入的维护者、日常文档管理

---

## 🚀 快速开始

### 新增文档时

```bash
# 1. 检查命名（无空格）
# 2. 创建文档（使用标准格式）
# 3. 更新分类 README.md
# 4. 更新根 README.md
# 5. git 提交

git add docs/
git commit -m "docs: 新增 XXX 文档"
```

### 日常检查

```bash
# 每周执行一次
python check_docs_naming.py
python check_interview_format.py
```

### 批量修复

```bash
# 发现问题后执行
python fix_docs_spaces.py
python fix_interview_format.py
```

---

## 📋 核心规范速查

### 命名规范

✅ **正确：**
- `06-RPC核心原理与实战指南.md`
- `04-Spring框架/`

❌ **错误：**
- `06-RPC核心原理与实战指南.md` （包含空格）
- `04-Spring框架 /` （包含空格）

### 面试题目格式

✅ **正确：**
```markdown
**问题 1：什么是 RPC？**

**答：**

RPC...
```

❌ **错误：**
```markdown
### Q1: 什么是 RPC？

**答案：**

RPC...
```

### README 更新

**必须做：**
- ✅ 每新增文档必更新对应 README.md
- ✅ 分类 README 包含文档列表和说明
- ✅ 根 README 包含导航和统计

---

## 🛠️ 工具脚本汇总

| 脚本 | 功能 | 使用频率 |
|------|------|---------|
| `check_interview_format.py` | 检查面试题目格式 | 每周 |
| `fix_interview_format.py` | 修复面试题目格式 | 需要时 |
| `check_docs_naming.py` | 检查目录命名规范 | 每周 |
| `fix_docs_spaces.py` | 修复空格问题 | 需要时 |
| `docs_statistics.py` | 统计文档信息 | 每月 |

---

## 📊 质量标准

| 指标 | 目标值 | 检查方式 |
|------|--------|---------|
| 命名规范符合率 | 100% | `check_docs_naming.py` |
| README 完整率 | 100% | 手动检查 |
| 面试题目格式统一率 | 100% | `check_interview_format.py` |
| 死链数量 | 0 | 定期审查 |

---

## 💾 记忆保存

已将以下规范保存到项目记忆中：

1. **面试题目格式规范** - development_code_specification
2. **目录命名与 README 维护规范** - development_code_specification
3. **工具脚本使用方法** - common_pitfalls_experience
4. **维护工作流程** - common_pitfalls_experience

---

## 📖 相关资源

- [docs/README.md](../README.md) - 技术文档总导航
- [.docs-meta/](./) - 规范文档目录
- [docs/](../) - 正式技术文档目录

---

**维护者**: itzixiao  
**最后更新**: 2026-03-08  
**问题反馈**: 欢迎提 Issue 或 PR
