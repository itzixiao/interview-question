# interview-ui 前端示例代码

本目录包含前端开发相关的示例代码，与 `docs/18-前端开发` 文档配套使用。

## 目录结构

```
interview-ui/
├── vue2/                       # Vue2 核心原理示例
│   ├── reactive-system.js      # 响应式系统实现（Object.defineProperty）
│   ├── array-reactive.js       # 数组响应式处理
│   └── diff-algorithm.js       # Diff 算法实现
│
├── vue3/                       # Vue3 核心原理示例
│   └── reactive-system.js      # 响应式系统实现（Proxy）
│
├── react/                      # React 核心原理示例
│   └── hooks-examples.jsx      # Hooks 详解与示例
│
├── typescript/                 # TypeScript 高级类型
│   └── advanced-types.ts       # 泛型、条件类型、映射类型等
│
├── engineering/                # 前端工程化
│   └── webpack-config.js       # Webpack 配置详解
│
├── api-performance/            # API 与性能优化
│   ├── axios-interceptor.js    # Axios 拦截器封装
│   └── virtual-list.jsx        # 虚拟列表实现
│
└── micro-frontend/             # 微前端架构
    ├── qiankun-main.js         # qiankun 主应用配置
    └── qiankun-child.js        # qiankun 子应用配置
```

## 使用说明

### Vue2 响应式系统

```javascript
const { reactive, watch } = require('./vue2/reactive-system');

const state = reactive({ count: 0 });
watch(() => state.count, (newVal, oldVal) => {
    console.log('count changed:', oldVal, '->', newVal);
});
state.count++;  // 触发更新
```

### Vue3 响应式系统

```javascript
const { reactive, watchEffect } = require('./vue3/reactive-system');

const state = reactive({ count: 0 });
watchEffect(() => {
    console.log('count:', state.count);
});
state.count++;  // 触发更新
```

### React Hooks

```jsx
import { Counter, UseRefDemo } from './react/hooks-examples';

function App() {
    return (
        <>
            <Counter />
            <UseRefDemo />
        </>
    );
}
```

### TypeScript 高级类型

```typescript
import type { ApiResponse, Person } from './typescript/advanced-types';

// 使用泛型接口
const response: ApiResponse<User> = {
    code: 0,
    data: { id: 1, name: '张三' },
    message: 'success'
};
```

### Axios 拦截器

```javascript
import { api } from './api-performance/axios-interceptor';

// GET 请求
const users = await api.get('/users', { page: 1 });

// 带缓存的请求
const user = await api.getCached('/users/1', {}, { ttl: 300000 });

// 上传文件
await api.upload('/upload', file, (percent) => {
    console.log(`上传进度: ${percent}%`);
});
```

### 虚拟列表

```jsx
import { FixedSizeVirtualList } from './api-performance/virtual-list';

function App() {
    const items = Array.from({ length: 10000 }, (_, i) => ({
        id: i,
        title: `Item ${i}`
    }));

    return (
        <FixedSizeVirtualList
            items={items}
            itemHeight={50}
            containerHeight={400}
            renderItem={(item) => <div>{item.title}</div>}
        />
    );
}
```

### qiankun 微前端

**主应用：**

```javascript
import { registerMicroApps, start } from './micro-frontend/qiankun-main';

registerMicroApps([
    {
        name: 'app1',
        entry: '//localhost:8081',
        container: '#container',
        activeRule: '/app1'
    }
]);

start();
```

**子应用：**

```javascript
export async function bootstrap() {
    console.log('app bootstraped');
}

export async function mount(props) {
    renderApp(props);
}

export async function unmount() {
    app.unmount();
}
```

## 对应文档

| 代码目录               | 对应文档                                                               |
|--------------------|--------------------------------------------------------------------|
| `vue2/`            | [01-Vue2核心原理详解.md](../docs/18-前端开发/01-Vue2核心原理详解.md)               |
| `vue3/`            | [02-Vue3核心原理详解.md](../docs/18-前端开发/02-Vue3核心原理详解.md)               |
| `engineering/`     | [03-前端工程化详解.md](../docs/18-前端开发/03-前端工程化详解.md)                     |
| `typescript/`      | [04-TypeScript高级类型与实战.md](../docs/18-前端开发/04-TypeScript高级类型与实战.md) |
| `react/`           | [05-React核心原理.md](../docs/18-前端开发/05-React核心原理.md)                 |
| `api-performance/` | [06-前后端联调最佳实践.md](../docs/18-前端开发/06-前后端联调最佳实践.md)                 |
| `api-performance/` | [07-前端性能优化全链路.md](../docs/18-前端开发/07-前端性能优化全链路.md)                 |
| `micro-frontend/`  | [08-微前端架构实战.md](../docs/18-前端开发/08-微前端架构实战.md)                     |

## 运行环境

- Node.js >= 14.0.0
- React >= 16.8.0
- Vue >= 2.6.0 / 3.0.0
- TypeScript >= 4.0.0

## 注意事项

1. 所有代码均包含详细注释，建议配合文档阅读
2. 示例代码为简化实现，用于理解核心原理
3. 生产环境建议使用官方库
