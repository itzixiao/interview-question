# Warm-Flow 前端 UI 使用说明

## 📱 快速开始

### 1. 安装依赖

```bash
cd ui
pnpm install
# 或 npm install
```

### 2. 启动开发服务器

```bash
pnpm dev
# 或 npm run dev
```

访问：http://localhost:5174

### 3. 构建生产版本

```bash
pnpm build
# 或 npm run build
```

构建产物在 `ui/dist` 目录

## 🎯 功能模块

### 1. 主页 (/)

美观的卡片式导航界面，包含：
- 请假审批入口
- 报销审批入口
- 流程设计器入口
- 系统说明信息

### 2. 请假审批 (/leave)

#### 功能说明

**提交请假申请：**
- 申请人ID（默认：1001）
- 申请人姓名
- 请假类型（事假/病假/年假/婚假/产假）
- 开始时间
- 结束时间
- 请假天数
- 请假原因

**审批操作：**
- 输入流程实例ID
- 审批人ID（默认：2001）
- 审批结果（通过/驳回）
- 审批意见

**撤销申请：**
- 输入流程实例ID
- 申请人ID

**查询详情：**
- 输入流程实例ID
- 查看完整申请信息和审批状态

#### 测试流程

```
1. 填写请假表单 → 点击"提交申请"
   ↓
2. 系统返回流程实例ID → 自动填充到审批表单
   ↓
3. 选择审批结果 → 填写审批意见 → 点击"提交审批"
   ↓
4. 在查询区域输入流程实例ID → 查看审批结果
```

### 3. 报销审批 (/reimbursement)

功能与请假审批类似，区别在于：
- 报销类型（差旅费/交通费/餐饮费/办公用品/其他）
- 报销金额
- 报销事由
- 发票附件URL

### 4. 流程设计器 (/designer)

#### 功能特性

**🎨 可视化设计：**
- 拖拽方式绘制 BPMN 2.0 流程图
- 支持多种 BPMN 元素：
  - 开始事件（Start Event）
  - 结束事件（End Event）
  - 用户任务（User Task）
  - 服务任务（Service Task）
  - 排他网关（Exclusive Gateway）
  - 并行网关（Parallel Gateway）
  - 顺序流（Sequence Flow）

**💾 保存与导出：**
- **保存** - 保存流程图并预览 BPMN XML
- **导出 SVG** - 导出为 SVG 图片（可用于文档、演示等）
- **导出 XML** - 导出为 BPMN 标准 XML 文件
- **复制 XML** - 一键复制到剪贴板

**📥 导入：**
- 支持导入 `.bpmn` 或 `.xml` 文件
- 自动渲染流程图

#### 默认流程图

系统预置了"请假审批流程"示例：

```
[开始] → [部门经理审批] → [HR审批] → [结束]
```

#### 使用技巧

1. **添加节点：** 从左侧工具栏拖拽到画布
2. **连接节点：** 点击节点的连接点，拖拽到目标节点
3. **删除节点：** 选中节点后按 Delete 键
4. **配置属性：** 选中节点后在右侧面板配置
5. **缩放画布：** 鼠标滚轮或点击右下角缩放按钮
6. **平移画布：** 按住鼠标左键拖动

## 🔌 API 配置

前端通过 Vite 代理转发请求到后端：

```javascript
// vite.config.js
server: {
    port: 5174,
    proxy: {
        '/api': {
            target: 'http://127.0.0.1:8095',  // 后端地址
            changeOrigin: true
        }
    }
}
```

如果后端端口不是 8095，需要修改 `vite.config.js` 中的代理配置。

## 📁 目录结构

```
ui/
├── src/
│   ├── api/                    # API 接口
│   │   ├── request.js         # Axios 封装
│   │   ├── leave.js           # 请假审批 API
│   │   └── reimbursement.js   # 报销审批 API
│   ├── router/
│   │   └── index.js           # 路由配置
│   ├── views/                  # 页面组件
│   │   ├── Home.vue           # 主页
│   │   ├── LeaveApproval.vue  # 请假审批
│   │   ├── ReimbursementApproval.vue  # 报销审批
│   │   └── FlowDesigner.vue   # 流程设计器
│   ├── App.vue                # 根组件
│   └── main.js                # 入口文件
├── index.html                 # HTML 模板
├── package.json               # 依赖配置
└── vite.config.js             # Vite 配置
```

## 🎨 技术亮点

### 1. bpmn-js 集成

```javascript
import BpmnModeler from 'bpmn-js/lib/Modeler'

// 初始化设计器
const bpmnModeler = new BpmnModeler({
    container: canvasRef.value
})

// 导入 XML
await bpmnModeler.importXML(bpmnXML)

// 导出 XML
const {xml} = await bpmnModeler.saveXML({format: true})

// 导出 SVG
const {svg} = await bpmnModeler.saveSVG()
```

### 2. Element Plus 组件库

- 表单组件（el-form, el-input, el-select）
- 按钮组件（el-button）
- 卡片组件（el-card）
- 对话框组件（el-dialog）
- 消息提示（ElMessage）
- 描述列表（el-descriptions）

### 3. Vue 3 Composition API

```javascript
import {ref, reactive, onMounted} from 'vue'

const form = reactive({
    userId: 1001,
    userName: ''
})

const loading = ref(false)

onMounted(() => {
    // 初始化逻辑
})
```

### 4. Axios 请求封装

```javascript
// 请求拦截器
request.interceptors.request.use(config => {
    return config
})

// 响应拦截器
request.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code !== 200) {
            ElMessage.error(res.message)
            return Promise.reject(new Error(res.message))
        }
        return res
    }
)
```

## 🐛 常见问题

### 1. 启动失败：端口被占用

**错误信息：** `Port 5174 is already in use`

**解决方案：**
- 修改 `vite.config.js` 中的 `port` 配置
- 或关闭占用端口的进程

### 2. 后端接口请求失败

**错误信息：** `Network Error` 或 `ECONNREFUSED`

**解决方案：**
- 确保后端服务已启动在 8095 端口
- 检查 `vite.config.js` 中的代理配置
- 确保使用 `http://127.0.0.1` 而不是 `http://localhost`（避免 IPv6 问题）

### 3. bpmn-js 导入失败

**错误信息：** `Cannot find module 'bpmn-js'`

**解决方案：**
```bash
# 重新安装依赖
rm -rf node_modules
pnpm install
```

### 4. 样式显示异常

**解决方案：**
- 确保导入了 bpmn-js 的 CSS 文件
- 检查 Element Plus 是否正确安装

## 📊 性能优化

### 1. 路由懒加载

```javascript
const routes = [
    {
        path: '/designer',
        name: 'FlowDesigner',
        component: () => import('@/views/FlowDesigner.vue')  // 懒加载
    }
]
```

### 2. 组件按需导入

使用 `unplugin-vue-components` 自动按需导入 Element Plus 组件：

```javascript
// vite.config.js
Components({
    resolvers: [ElementPlusResolver()],
    dts: false
})
```

### 3. 自动导入 API

使用 `unplugin-auto-import` 自动导入 Vue API：

```javascript
// vite.config.js
AutoImport({
    resolvers: [ElementPlusResolver()],
    imports: ['vue', 'vue-router', 'pinia'],
    dts: false
})
```

## 🚀 部署

### 1. 构建生产版本

```bash
pnpm build
```

### 2. 预览构建结果

```bash
pnpm preview
```

### 3. 部署到 Nginx

```nginx
server {
    listen 80;
    server_name warm-flow.example.com;

    root /path/to/ui/dist;
    index index.html;

    # SPA 路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8095;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 📚 扩展开发

### 添加新的审批流程

1. **后端：** 创建新的 Controller、Service、Entity
2. **前端 API：** 在 `src/api/` 下创建新的 API 文件
3. **前端页面：** 在 `src/views/` 下创建新的页面组件
4. **路由配置：** 在 `src/router/index.js` 中添加路由

### 自定义流程设计器

```javascript
// 添加自定义属性面板
import customPropertiesProvider from './customPropertiesProvider'

const bpmnModeler = new BpmnModeler({
    container: canvasRef.value,
    additionalModules: [
        customPropertiesProvider
    ]
})
```

## 💡 最佳实践

1. **错误处理：** 所有 API 调用都使用 try-catch
2. **加载状态：** 使用 loading 状态提升用户体验
3. **消息提示：** 操作成功/失败都给出明确提示
4. **表单验证：** 提交前验证必填字段
5. **数据默认值：** 表单字段设置合理的默认值
6. **响应式布局：** 使用 Element Plus 的栅格系统

## 🔗 相关链接

- [Vue 3 文档](https://cn.vuejs.org/)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
- [bpmn-js 文档](https://bpmn.io/toolkit/bpmn-js/)
- [Vite 文档](https://cn.vitejs.dev/)
- [Warm-Flow 文档](https://warm-flow.my-doc.net/)

---

**维护者：** itzixiao  
**最后更新：** 2026-04-08
