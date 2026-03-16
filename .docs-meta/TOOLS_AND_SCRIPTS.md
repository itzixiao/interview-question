# 文档维护工具脚本

## 1. 面试题目格式检查与修复

### check_interview_format.py

**功能：** 检查并修复所有面试题目格式

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

**使用方法：**

```bash
python check_interview_format.py
```

---

## 2. docs 目录命名规范检查

### check_docs_naming.py

**功能：**

- 检查所有目录名是否包含空格
- 检查所有.md 文件名是否包含空格
- 检查每个分类目录下是否有 README.md
- 生成详细的检查报告

**使用方法：**

```bash
python check_docs_naming.py
```

**输出示例：**

```
======================================================================
docs 目录命名规范检查
======================================================================

检查结果汇总
======================================================================
问题目录数：0
问题文件数：0
总问题数：0

✅ 恭喜！所有目录和文件名都符合规范（无空格）

======================================================================
README.md 存在性检查
======================================================================
✅ 已有 README.md: 01-Java基础/
✅ 已有 README.md: 02-Java并发编程/
...
缺少 README.md 的目录数：0

🎉 所有检查通过！目录结构完全符合规范！
```

---

## 3. docs 目录空格修复

### fix_docs_spaces.py

**功能：**

- 批量移除目录名中的空格
- 批量移除文件名中的空格
- 智能处理目标已存在的情况
- 自动备份和清理

**使用方法：**

```bash
python fix_docs_spaces.py
```

**注意事项：**

- ⚠️ 运行前建议先备份 docs 目录
- ✅ 脚本会自动处理冲突情况
- ✅ 会显示详细的处理日志

---

## 4. 综合检查清单

### 日常检查项

```bash
# 1. 检查面试题目格式
grep -r "### Q[0-9]:" docs/

# 2. 检查目录和文件名空格
find docs -name "* *"

# 3. 检查 README 完整性
find docs -type d -not -path '*/\.*' | while read dir; do
    if [ ! -f "$dir/README.md" ]; then
        echo "❌ 缺少 README.md: $dir"
    fi
done
```

---

## 5. 自动化工作流

### Git Hooks 示例

**.git/hooks/pre-commit**

```bash
#!/bin/bash

echo "🔍 检查文档规范性..."

# 检查文件名是否包含空格
if git diff --cached --name-only | grep -q ' '; then
    echo "❌ 检测到文件名包含空格，请修正后再提交"
    exit 1
fi

# 检查 Markdown 文件格式
if git diff --cached --name-only | grep -q '\.md$'; then
    echo "✅ Markdown 文件将进行检查..."
    python check_interview_format.py
fi

echo "🎉 所有检查通过！"
exit 0
```

**启用方法：**

```bash
chmod +x .git/hooks/pre-commit
```

---

## 6. CI/CD集成示例

### GitHub Actions

**.github/workflows/docs-check.yml**

```yaml
name: Docs Check

on:
  push:
    paths:
      - 'docs/**'
  pull_request:
    paths:
      - 'docs/**'

jobs:
  docs-check:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up Python
      uses: actions/setup-python@v2
      with:
        python-version: '3.x'
    
    - name: Install dependencies
      run: |
        pip install -r requirements.txt
    
    - name: Check interview format
      run: python check_interview_format.py
    
    - name: Check docs naming
      run: python check_docs_naming.py
    
    - name: Build documentation
      run: |
        # 添加构建命令
        echo "Building docs..."
```

---

## 7. 定期维护计划

### 每周检查

```bash
# 每周一执行
python check_docs_naming.py > docs_check_$(date +%Y%m%d).log
```

### 每月清理

```bash
# 每月 1 号执行
python fix_docs_spaces.py
git add docs/
git commit -m "chore(docs): 月度文档清理和维护"
```

---

## 8. 文档统计脚本

### docs_statistics.py

**功能：** 统计文档数量、面试题数量等

```python
import os
import re
import glob

def count_interview_questions(filepath):
    """统计文件中的面试题目数量"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 匹配新格式
    new_format = len(re.findall(r'\*\*问题 \d+:', content))
    # 匹配旧格式
    old_format = len(re.findall(r'### Q\d+:', content))
    
    return new_format + old_format

def docs_statistics():
    """统计文档信息"""
    total_files = 0
    total_questions = 0
    
    for md_file in glob.glob('docs/**/*.md', recursive=True):
        if 'README' not in md_file:
            total_files += 1
            questions = count_interview_questions(md_file)
            total_questions += questions
    
    print(f'总文档数：{total_files}')
    print(f'总面试题数：{total_questions}')
    print(f'平均每个文档：{total_questions/total_files:.1f}道题')

if __name__ == '__main__':
    docs_statistics()
```

---

## 9. 快速参考卡片

### 常用命令速查

```bash
# 检查面试题目格式
python check_interview_format.py

# 检查目录命名规范
python check_docs_naming.py

# 修复空格问题
python fix_docs_spaces.py

# 统计文档信息
python docs_statistics.py

# 查看帮助
python -m pydoc check_interview_format
```

### 编辑器配置

**.vscode/settings.json**

```json
{
    "files.exclude": {
        "**/.docs-meta": true
    },
    "editor.formatOnSave": true,
    "markdownlint.config": {
        "MD001": false,
        "MD013": false
    }
}
```

---

**版本**: 1.0  
**最后更新**: 2026-03-08  
**维护者**: itzixiao
