# 前端开发技术文档

## 文档列表

| 序号 | 文档标题                                         | 核心内容                                       | 面试题数 | 状态 |
|----|----------------------------------------------|--------------------------------------------|------|----|
| 01 | [Vue2 核心原理详解](01-Vue2核心原理详解.md)              | 响应式原理、Diff 算法、组件通信、生命周期、Vuex、Vue Router    | 10   | ✅  |
| 02 | [Vue3 核心原理详解](02-Vue3核心原理详解.md)              | Composition API、Proxy 响应式、Pinia、新特性组件、性能优化 | 10   | ✅  |
| 03 | [前端工程化详解](03-前端工程化详解.md)                     | Webpack/Vite构建优化、monorepo实践、CI/CD          | 6    | ✅  |
| 04 | [TypeScript高级类型与实战](04-TypeScript高级类型与实战.md) | 类型体操、与Java类型系统对比、泛型实战                      | 6    | ✅  |
| 05 | [React核心原理](05-React核心原理.md)                 | Hooks原理、Fiber架构、与Vue架构对比                   | 7    | ✅  |
| 06 | [前后端联调最佳实践](06-前后端联调最佳实践.md)                 | API规范、错误处理、Mock方案、JWT认证                    | 5    | ✅  |
| 07 | [前端性能优化全链路](07-前端性能优化全链路.md)                 | Core Web Vitals、网络优化、渲染优化、内存优化             | 5    | ✅  |
| 08 | [微前端架构实战](08-微前端架构实战.md)                     | qiankun、Module Federation、样式隔离、通信方案        | 5    | ✅  |
| 09 | [前端实战指南](09-前端实战指南.md)                       | 技术选型、实战场景、避坑指南                             | -    | ✅  |

**总计：9 篇文档，54+ 道面试题（面试题分布在各模块文档中）**

## 知识体系

### Vue2 核心知识点

- **响应式原理**：Object.defineProperty() 数据劫持、依赖收集、派发更新
- **虚拟 DOM**：VNode 结构、Diff 算法（双端比较）、Patch 过程
- **组件系统**：生命周期、组件通信（props/emit、Event Bus、Provide/Inject、Vuex）
- **指令系统**：内置指令、自定义指令
- **状态管理**：Vuex（state/getters/mutations/actions/modules）
- **路由管理**：Vue Router（导航守卫、动态路由、路由懒加载）

### Vue3 核心知识点

- **Composition API**：setup 函数、响应式 API（ref/reactive/computed/watch）
- **响应式原理**：Proxy 代理、Reflect、依赖追踪
- **Script Setup**：语法糖、defineProps/defineEmits/defineModel
- **状态管理**：Pinia（更简洁的 API、更好的 TS 支持）
- **新特性**：Teleport、Suspense、Fragments（多根节点）
- **性能优化**：静态提升、Patch Flags、Tree-shaking

### 前端工程化

- **构建工具**：Webpack 配置优化、Vite 快速开发、Loader/Plugin 原理
- **Monorepo**：pnpm workspaces、Lerna、Turborepo、依赖管理策略
- **代码规范**：ESLint、Prettier、Git Hooks、Commitlint
- **CI/CD**：GitHub Actions、自动化测试、持续部署

### TypeScript 进阶

- **高级类型**：条件类型、映射类型、模板字面量类型、递归类型
- **类型体操**：infer 关键字、类型守卫、类型收窄
- **与 Java 对比**：泛型差异、类型系统、类型擦除
- **实战模式**：工厂模式、混入模式、类型安全的事件总线

### React 核心

- **Hooks 原理**：闭包陷阱、依赖数组、Hooks 调用规则
- **Fiber 架构**：可中断渲染、优先级调度、双缓冲机制
- **Diff 算法**：单链表 diff、key 的作用、复用策略
- **状态管理**：Redux、Zustand、Context API 性能优化

### 前后端联调

- **API 规范**：RESTful 设计、统一响应格式、版本控制
- **请求封装**：Axios 拦截器、错误处理、Token 刷新
- **Mock 方案**：MSW、Vite 插件 Mock、数据模拟
- **认证授权**：JWT 流程、无感刷新、权限控制

### 性能优化

- **Core Web Vitals**：LCP、FID、CLS、FCP、TTFB 指标优化
- **网络优化**：资源预加载、缓存策略、CDN、HTTP/2
- **渲染优化**：虚拟列表、懒加载、代码分割、防抖节流
- **内存优化**：内存泄漏排查、WeakMap/WeakSet、大数据处理

### 微前端架构

- **方案对比**：qiankun、Module Federation、single-spa、Micro App
- **qiankun 实战**：主应用配置、子应用改造、生命周期
- **样式隔离**：Shadow DOM、CSS Modules、BEM 规范
- **应用通信**：全局状态、事件总线、Props 传递

## 技术对比

### Vue2 vs Vue3 对比

| 特性           | Vue2                    | Vue3                          |
|--------------|-------------------------|-------------------------------|
| 响应式原理        | Object.defineProperty() | Proxy                         |
| API 风格       | Options API             | Options API + Composition API |
| TypeScript   | 支持较弱                    | 原生支持                          |
| 性能           | 良好                      | 提升 1.3~2 倍                    |
| 包体积          | ~23KB                   | ~10KB（运行时）                    |
| Tree-shaking | 不支持                     | 支持                            |
| 多根节点         | 不支持                     | 支持（Fragments）                 |
| 状态管理         | Vuex                    | Pinia（推荐）                     |

### Webpack vs Vite 对比

| 特性    | Webpack | Vite        |
|-------|---------|-------------|
| 启动速度  | 慢（需要打包） | 快（原生 ESM）   |
| 热更新   | 较慢      | 极快（HMR 毫秒级） |
| 配置复杂度 | 复杂      | 简单          |
| 生态成熟度 | 非常成熟    | 快速成熟中       |
| 生产构建  | 高度优化    | 基于 Rollup   |
| 适用场景  | 大型复杂项目  | 中小型项目、快速开发  |

### React vs Vue 对比

| 特性   | React              | Vue         |
|------|--------------------|-------------|
| 学习曲线 | 较陡（JSX、Hooks 规则）   | 平缓（模板语法直观）  |
| 灵活性  | 极高（纯 JS 生态）        | 高（渐进式框架）    |
| 模板语法 | JSX                | 模板 + JSX 可选 |
| 状态管理 | Redux/Zustand/MobX | Vuex/Pinia  |
| 社区生态 | 极其丰富               | 丰富          |
| 适用场景 | 超大型应用、跨平台          | 中大型应用、快速开发  |

### qiankun vs Module Federation 对比

| 特性    | qiankun                   | Module Federation |
|-------|---------------------------|-------------------|
| 实现方式  | 运行时沙箱                     | 构建时共享             |
| 技术栈限制 | 无限制                       | 需统一构建工具           |
| 样式隔离  | 支持（Shadow DOM/Scoped CSS） | 需自行处理             |
| JS 沙箱 | 支持（快照/Proxy）              | 依赖构建隔离            |
| 加载性能  | 需加载完整子应用                  | 按需加载共享模块          |
| 适用场景  | 异构技术栈、存量改造                | 同构技术栈、新架构         |

## 技术选型指南

### 前端框架选型

**选择 Vue 的场景：**

- 团队前端经验相对较少
- 需要快速开发和迭代
- 项目以数据展示和管理后台为主
- 需要渐进式升级存量项目

**选择 React 的场景：**

- 团队有丰富的前端经验
- 需要高度定制化和灵活性
- 项目复杂度极高（如大型 SaaS 平台）
- 需要 React Native 跨平台能力

### 构建工具选型

**选择 Webpack 的场景：**

- 超大型项目需要精细优化
- 需要复杂的 Loader/Plugin 定制
- 存量项目维护

**选择 Vite 的场景：**

- 新项目启动
- 需要极快的开发体验
- 中小型项目
- 库开发

### 微前端方案选型

**选择 qiankun 的场景：**

- 存量系统整合（技术栈混杂）
- 需要严格的样式和 JS 隔离
- 独立部署需求强烈

**选择 Module Federation 的场景：**

- 新架构设计
- 统一技术栈（如全 React 生态）
- 需要极致的模块共享能力

## 实战场景与解决方案

### 场景一：大型表单性能优化

**问题**：表单字段超过 100 个，输入卡顿

**解决方案**：

1. 使用 `shallowRef` 替代 `ref` 减少响应式开销
2. 表单字段分组，使用 `markRaw` 标记静态配置
3. 虚拟滚动展示（如 `vue-virtual-scroller`）
4. 防抖处理输入事件（300ms）
5. 使用 `v-memo` 或 `React.memo` 缓存不相关组件

### 场景二：首屏加载优化

**问题**：首屏加载时间超过 3 秒

**解决方案**：

1. **资源优化**：
    - 路由懒加载 + 组件异步加载
    - 图片懒加载（`loading="lazy"`）
    - 使用 WebP 格式图片

2. **构建优化**：
    - 代码分割（Code Splitting）
    - Tree-shaking 移除未使用代码
    - Gzip/Brotli 压缩

3. **网络优化**：
    - CDN 部署静态资源
    - HTTP/2 多路复用
    - 资源预加载（`<link rel="preload">`）

4. **渲染优化**：
    - SSR/SSG 首屏直出
    - 骨架屏提升感知性能
    - 关键 CSS 内联

### 场景三：跨团队组件库共享

**问题**：多个团队维护各自组件库，重复开发

**解决方案**：

1. **Monorepo 架构**：
    - pnpm workspaces 管理多包
    - Turborepo 优化构建流水线
    - 统一的设计系统和规范

2. **组件库设计**：
    - 原子化设计（Atomic Design）
    - 主题定制能力（CSS Variables）
    - 完善的文档和示例（Storybook）

3. **发布流程**：
    - 自动化版本管理（Changesets）
    - CI/CD 自动发布
    - 语义化版本控制

### 场景四：微前端存量系统整合

**问题**：5 个存量系统（Vue2/React/Angular）需要整合

**解决方案**：

1. **基座应用设计**：
    - 统一登录和权限
    - 公共布局组件（Header/Sidebar）
    - 应用注册和路由管理

2. **子应用改造**：
    - 生命周期导出（bootstrap/mount/unmount）
    - 样式隔离（CSS Modules + 命名空间）
    - 通信机制（全局状态 + 事件总线）

3. **部署策略**：
    - 独立部署（子应用单独发版）
    - 灰度发布（按用户/地区）
    - 回滚机制

### 场景五：TypeScript 类型安全架构

**问题**：大型项目类型混乱，`any` 泛滥

**解决方案**：

1. **类型规范**：
    - 禁用 `any`（ESLint 规则）
    - 统一 API 响应类型
    - 领域模型类型定义

2. **类型工具**：
    - 泛型工具函数
    - 类型守卫（Type Guards）
    - branded types 防止 ID 混淆

3. **代码生成**：
    - OpenAPI 自动生成类型
    - GraphQL Code Generator
    - 数据库 Schema 生成类型

## 常见陷阱与避坑指南

### Vue 开发陷阱

1. **响应式丢失**
    - ❌ 直接修改数组索引：`arr[0] = newVal`
    - ✅ 使用 `Vue.set` 或 `splice`

2. **计算属性副作用**
    - ❌ 在 computed 中修改外部状态
    - ✅ computed 只用于派生数据

3. **生命周期误用**
    - ❌ 在 `created` 中操作 DOM
    - ✅ DOM 操作放在 `mounted`

### React 开发陷阱

1. **Hooks 调用顺序**
    - ❌ 在条件语句中调用 Hooks
    - ✅ 始终在组件顶层调用

2. **闭包陷阱**
    - ❌ useEffect 依赖数组遗漏
    - ✅ 使用 eslint-plugin-react-hooks

3. **状态更新异步**
    - ❌ 依赖更新后的 state 立即执行
    - ✅ 使用 useEffect 监听或使用函数式更新

### 性能优化陷阱

1. **过度优化**
    - ❌ 所有组件都使用 memo
    - ✅ 只在性能瓶颈处优化

2. **错误的 key 使用**
    - ❌ 使用数组索引作为 key
    - ✅ 使用唯一标识符

3. **内存泄漏**
    - ❌ 未清理的事件监听和定时器
    - ✅ 在 unmount 时清理

## 学习路径

### 第一阶段：Vue2 基础（1-2周）

1. 响应式原理（Object.defineProperty）
2. 组件生命周期
3. 组件通信方式
4. 计算属性与侦听器
5. 指令系统

### 第二阶段：Vue2 进阶（1-2周）

1. 虚拟 DOM 与 Diff 算法
2. Vue Router 路由管理
3. Vuex 状态管理
4. 自定义指令与插件
5. 性能优化技巧

### 第三阶段：Vue3 基础（1-2周）

1. Composition API 核心概念
2. setup 函数与生命周期
3. 响应式 API（ref/reactive/computed/watch）
4. Script Setup 语法糖
5. 依赖注入（Provide/Inject）

### 第四阶段：Vue3 进阶（1-2周）

1. Proxy 响应式原理
2. Pinia 状态管理
3. Vue Router 4
4. 新特性组件（Teleport、Suspense）
5. 组合式函数（Composables）
6. 性能优化与 Tree-shaking

### 第五阶段：前端工程化（1-2周）

1. Webpack/Vite 构建工具配置
2. Monorepo 项目管理
3. ESLint/Prettier 代码规范
4. CI/CD 自动化流程

### 第六阶段：TypeScript 进阶（1-2周）

1. 高级类型与类型体操
2. 泛型实战模式
3. 与 Java 类型系统对比
4. 类型安全架构设计

### 第七阶段：React 核心（1-2周）

1. Hooks 原理与使用
2. Fiber 架构理解
3. 状态管理方案
4. 与 Vue 架构对比

### 第八阶段：全栈能力（2-3周）

1. 前后端联调最佳实践
2. 前端性能优化全链路
3. 微前端架构实战
4. 大型项目工程化实践

## 高频面试题速览

> 以下面试题简要列出，详细答案请查看各模块文档末尾的「高频面试题」章节

### Vue2 高频题（10道）- 详见 [01-Vue2核心原理详解.md](01-Vue2核心原理详解.md)

1. **Vue2 的响应式原理是什么？**  
   使用 `Object.defineProperty()` 对数据属性进行劫持，通过 getter 收集依赖（Watcher），setter 触发更新，实现数据变化驱动视图更新。

2. **Vue2 的 Diff 算法原理是什么？**  
   采用双端比较策略，设置新旧节点列表的头尾指针，通过四种比较策略（旧头新头、旧尾新尾、旧头新尾、旧尾新头）快速找到可复用节点，时间复杂度
   O(n)。

3. **computed 和 watch 的区别是什么？**  
   `computed` 有缓存，用于计算派生数据；`watch` 无缓存，用于监听数据变化执行副作用。`computed` 必须有返回值，`watch` 不需要。

4. **v-if 和 v-show 的区别是什么？**  
   `v-if` 条件为 false 时不渲染组件，切换开销大；`v-show` 始终渲染，通过 `display: none` 切换，适合频繁切换场景。

5. **Vue 组件的通信方式有哪些？**  
   props/$emit（父子）、Event Bus（任意）、Provide/Inject（跨级）、Vuex（全局状态）、$refs（父调子）、$attrs/$listeners（隔代）。

6. **Vue Router 的导航守卫有哪些？执行顺序是什么？**  
   全局：beforeEach → beforeResolve → afterEach；路由独享：beforeEnter；组件内：beforeRouteEnter → beforeRouteUpdate →
   beforeRouteLeave。

7. **Vuex 的 mutation 和 action 有什么区别？**  
   `mutation` 必须是同步，直接修改 state；`action` 可以异步，通过提交 mutation 修改 state。调用方式：commit vs dispatch。

8. **Vue2 的生命周期有哪些？created 和 mounted 的区别？**  
   创建(beforeCreate/created)、挂载(beforeMount/mounted)、更新(beforeUpdate/updated)、销毁(beforeDestroy/destroyed)。created
   时 DOM 未生成，mounted 时 DOM 已可用。

9. **为什么 Vue2 中 data 必须是函数？**  
   保证组件实例间数据隔离。如果 data 是对象，所有实例共享同一引用；使用函数返回新对象，每个实例拥有独立数据副本。

10. **Vue2 中的 key 有什么作用？**  
    作为 VNode 唯一标识，帮助 Diff 算法识别节点，实现节点复用，提高渲染性能。避免使用数组索引作为 key。

### Vue3 高频题（10道）- 详见 [02-Vue3核心原理详解.md](02-Vue3核心原理详解.md)

1. **Vue3 的响应式原理与 Vue2 有什么区别？**  
   Vue2 使用 `Object.defineProperty()`，Vue3 使用 `Proxy`。Proxy 可监听动态新增/删除属性、数组索引变化，且是懒代理，初始化性能更好。

2. **Composition API 与 Options API 的区别？**  
   Options API 按选项类型组织代码，Composition API 按功能逻辑组合。Composition API 更好的逻辑复用（Composables）、TypeScript
   支持、tree-shaking。

3. **ref 和 reactive 的区别？**  
   `ref` 用于基本类型和对象，访问需 `.value`；`reactive` 仅用于对象/数组，直接访问属性。`ref` 可替换整个对象，`reactive`
   替换后失去响应式。

4. **Vue3 的生命周期钩子有哪些变化？**  
   选项式：beforeDestroy → beforeUnmount，destroyed → unmounted。组合式：使用 onXxx 函数（onMounted、onUpdated 等），setup
   执行时机相当于 beforeCreate 和 created 之间。

5. **Pinia 与 Vuex 的区别？**  
   Pinia 更简洁（无 mutations）、更好的 TypeScript 支持、更轻量（~1KB）、模块化更简单。Vuex 4 仍支持，但 Pinia 是 Vue3 官方推荐。

6. **Vue Router 4 有哪些变化？**  
   创建方式改为 `createRouter`，模式使用 `createWebHistory()`/`createWebHashHistory()`，新增组合式
   API（useRoute、useRouter），支持动态路由添加。

7. **defineModel 是什么？解决了什么问题？**  
   Vue 3.3+ 新特性，简化双向绑定的 props + emits 定义。使用 `defineModel()` 直接获得可修改的响应式值，自动 emit 更新事件。

8. **Vue3 的性能优化有哪些？**  
   编译时：静态提升、Patch Flags 标记动态节点、更好的 Tree-shaking。运行时：Proxy 懒代理、事件缓存、Slot 优化。包体积从 ~23KB
   降到 ~10KB。

9. **Teleport 和 Suspense 的使用场景？**  
   `Teleport`：将组件渲染到 DOM 其他位置，如 Modal、Toast。`Suspense`：处理异步组件加载状态，显示 fallback 内容。

10. **Vue3 如何兼容 Vue2 的写法？**  
    Options API 完全兼容，可混用 Composition API。使用 `@vue/compat` 迁移构建版本可逐步迁移。移除 filter、$on/$off/$once
    等特性。

### 前端工程化高频题（6道）- 详见 [03-前端工程化详解.md](03-前端工程化详解.md)

1. **Vite 为什么比 Webpack 快？**  
   开发阶段使用原生 ESM 按需编译，无需打包；使用 esbuild（Go 编写）预构建依赖，比 Babel 快 10-100 倍；HMR 热更新精确的模块替换，毫秒级响应。

2. **Webpack 的 Tree-shaking 是如何工作的？**  
   利用 ESM 静态结构特性，标记未使用的导出代码，通过 Terser 等压缩工具删除死代码。需要配置 `sideEffects` 告知哪些文件有副作用。

3. **Monorepo 的优势和挑战是什么？**  
   优势：代码复用、统一规范、原子提交、依赖统一管理。挑战：构建性能（需 Turborepo）、权限管理、CI/CD 复杂度、学习成本。

4. **如何优化 Webpack 构建速度？**  
   使用 thread-loader 多线程、cache-loader 缓存、include/exclude 缩小范围、resolve.alias 减少解析、DllPlugin 预编译第三方库、Webpack
   5 持久化缓存。

5. **pnpm 相比 npm/yarn 的优势？**  
   全局存储 + 硬链接共享节省磁盘空间；严格的依赖树结构解决幽灵依赖；安装速度更快；原生 Monorepo workspaces 支持更强大。

6. **如何设计一个组件库的构建流程？**  
   输出 ES Module（es/）、CommonJS（lib/）、UMD（dist/）和类型定义（types/）。流程：清理 → 编译 ES/CJS → 打包 UMD → 生成类型 →
   复制样式。使用 Changesets 管理版本发布。

### TypeScript 高频题（6道）- 详见 [04-TypeScript高级类型与实战.md](04-TypeScript高级类型与实战.md)

1. **TypeScript 中的 any、unknown、never 有什么区别？**  
   `any` 关闭类型检查，尽量避免；`unknown` 未知类型，需类型检查后才能使用，比 any 更安全；`never` 永不存在的类型，用于穷尽检查或抛出错误的函数。

2. **什么是协变、逆变、双变和抗变？**  
   描述类型子类型关系在复合类型中的保持方式。协变：子类型关系保持（数组、对象属性）；逆变：子类型关系反转（函数参数）；双变：双向兼容；抗变：无关。

3. **如何提取 Promise 的返回值类型？**  
   使用 `infer` 关键字：`type PromiseReturn<T> = T extends Promise<infer R> ? R : T`。TypeScript 4.5+ 内置 `Awaited<T>`
   类型。

4. **TypeScript 装饰器原理是什么？**  
   装饰器是高阶函数，在类声明、方法、属性等被声明时执行，可以修改或替换被装饰的目标。编译后转换为 `__decorate` 函数调用。

5. **如何实现一个类型安全的深度 Partial？**  
   使用递归类型定义：`type DeepPartial<T> = { [P in keyof T]?: T[P] extends object ? DeepPartial<T[P]> : T[P] }`
   ，对嵌套对象递归应用 Partial。

6. **TypeScript 与 Java 泛型的主要区别？**  
   TypeScript 类型擦除更彻底，编译后完全消失；Java 保留原始类型信息到字节码。Java 有 `?` 通配符（extends/super），TS
   使用类型参数直接约束。TS 泛型推断更强大。

### React 高频题（7道）- 详见 [05-React核心原理.md](05-React核心原理.md)

1. **React Hooks 为什么不能放在条件语句中？**  
   React 使用数组/链表按调用顺序存储 Hooks 状态，条件语句会导致调用顺序不一致，状态匹配错误。Hooks 必须在组件顶层按相同顺序每次渲染都调用。

2. **useEffect 和 useLayoutEffect 的区别？**  
   `useEffect` 在渲染后异步执行，不阻塞浏览器绘制；`useLayoutEffect` 在渲染后同步执行，阻塞绘制。DOM 操作、测量布局用
   useLayoutEffect，其他用 useEffect。

3. **React 的 Diff 算法和 Vue 有什么区别？**  
   React 使用单链表逐位比较，Vue 使用双端比较（头尾指针）。两者都是 O(n) 时间复杂度，Vue 双端比较能更快找到可复用节点，对列表反转等操作优化更好。

4. **useMemo 和 useCallback 的区别和使用场景？**  
   `useMemo` 缓存计算结果，`useCallback` 缓存函数引用。useMemo 用于 expensive 计算，useCallback 用于缓存回调函数配合
   React.memo 使用。避免过度优化。

5. **React 的 Fiber 架构解决了什么问题？**  
   解决可中断渲染问题。旧架构递归渲染无法中断，长任务阻塞主线程。Fiber 将渲染工作拆分成小单元，支持优先级调度、时间切片，利用空闲时间执行。

6. **Redux 和 Context API 应该如何选择？**  
   Context API 适合简单全局数据（主题、用户信息），Redux 适合复杂状态管理。Context 任意值变化导致所有消费者重渲染，Redux
   精细化订阅。Redux 提供 DevTools 时间旅行调试。

7. **React.memo 和 useMemo 的区别？**  
   `React.memo` 是 HOC，用于避免组件不必要的重渲染；`useMemo` 是 Hook，用于缓存计算结果。两者可以配合使用，React.memo 比较
   props，useMemo 缓存值。

### 前后端联调高频题（5道）- 详见 [06-前后端联调最佳实践.md](06-前后端联调最佳实践.md)

1. **如何处理前端请求并发和竞态问题？**  
   竞态问题：先发出的请求后返回导致数据被覆盖。解决方案：使用 CancelToken 取消旧请求；或使用唯一标识丢弃过期响应；或使用防抖/节流控制请求频率。

2. **如何设计一个可扩展的 API 层？**  
   分层设计：axios 实例配置、请求/响应拦截器（统一错误处理、Token 刷新）、按模块组织 API（user.ts、order.ts）、类型定义。支持请求缓存、重试、取消。

3. **前后端如何约定接口版本？**  
   URL 路径版本（推荐）：`/api/v1/users`；请求头版本：`Accept: application/vnd.api.v1+json`；Query 参数版本：`?version=1`
   。主版本号变更表示不兼容修改，保持旧版本兼容期 3-6 个月。

4. **如何处理大文件上传？**  
   分片上传：将文件切分为小块（如 5MB）分别上传，支持断点续传和进度监控。使用 SparkMD5 计算文件指纹实现秒传。最后发送合并请求组装文件。

5. **如何防止重复提交？**  
   前端：按钮 loading 状态、请求防抖。后端：幂等 token（先获取 token，提交时携带，后端校验并删除）。组合使用前端防重+后端幂等保证数据安全。

### 性能优化高频题（5道）- 详见 [07-前端性能优化全链路.md](07-前端性能优化全链路.md)

1. **如何优化首屏加载时间？**  
   资源优化：代码分割、图片懒加载/WebP、Gzip 压缩。网络优化：CDN、HTTP/2、资源预加载。渲染优化：SSR/SSG、骨架屏、关键 CSS
   内联。减少主线程阻塞：异步加载第三方脚本。

2. **虚拟列表的实现原理是什么？**  
   只渲染可视区域内的元素，通过计算起始索引、结束索引和偏移量，动态更新渲染列表。容器高度为总数据量 × 单项高度，利用
   transform 或 padding 实现滚动效果。

3. **如何检测和解决内存泄漏？**  
   检测：Chrome DevTools Performance/Memory 面板观察堆内存增长。常见原因：未清理的事件监听、定时器、闭包引用大对象。解决：在组件卸载时清理事件监听和定时器，避免闭包持有大对象引用。

4. **Service Worker 的缓存策略有哪些？**  
   Cache First（缓存优先）、Network First（网络优先）、Stale While Revalidate（先返回缓存，后台更新）、Network Only、Cache
   Only。根据资源类型选择不同策略，静态资源用 Cache First，API 请求用 Network First 或 Stale While Revalidate。

5. **什么是 Time to Interactive (TTI)？如何优化？**  
   TTI 是页面从加载到完全可交互的时间，要求主线程空闲且能响应用户输入。优化：减少 JavaScript（代码分割、Tree-shaking）、优化长任务（Web
   Worker、任务切片）、预加载关键资源、第三方脚本异步加载。

### 微前端高频题（5道）- 详见 [08-微前端架构实战.md](08-微前端架构实战.md)

1. **微前端解决了什么问题？带来了什么挑战？**  
   解决：巨石应用拆分、技术栈无关、独立部署、团队自治。挑战：样式隔离、JS 隔离（全局变量污染）、性能（多框架运行）、跨应用通信、调试困难、公共依赖管理。

2. **qiankun 的 JS 沙箱是如何实现的？**  
   SnapshotSandbox（快照沙箱）：记录 window 状态快照，切换时恢复。ProxySandbox（代理沙箱）：创建 fakeWindow 对象，使用 Proxy
   拦截读写操作，多实例隔离更好。推荐 ProxySandbox。

3. **Module Federation 与 qiankun 有什么区别？**  
   qiankun 是运行时沙箱，技术栈无限制，加载完整子应用，适合异构技术栈存量改造。Module Federation
   是构建时模块共享，需统一构建工具，按需加载共享模块，适合同构技术栈新架构。

4. **微前端如何做到子应用独立部署？**  
   子应用部署到独立域名或 CDN，主应用通过 entry URL 动态加载。使用版本化 URL（如 `//app-a.com/v1.2.3/index.html`
   ）控制版本。支持灰度发布（按用户/地区路由到不同版本）和快速回滚。

5. **微前端中的公共依赖如何管理？**  
   qiankun：使用 externals 将依赖指向主应用加载的全局变量。Module Federation：使用 shared
   配置自动共享依赖，支持版本匹配和单例模式。推荐核心框架（Vue/React）使用 singleton，工具库可重复加载。

## 最佳实践

### Vue2 最佳实践

1. **组件通信**：优先使用 props/emit，复杂场景使用 Vuex
2. **性能优化**：v-for 必须加 key、合理使用 computed 缓存
3. **代码组织**：按功能拆分组件，避免过大组件
4. **状态管理**：全局状态放 Vuex，局部状态放组件 data

### Vue3 最佳实践

1. **API 选择**：复杂逻辑使用 Composition API，简单组件可用 Options API
2. **逻辑复用**：使用 Composables 替代 mixins
3. **响应式数据**：基本类型用 ref，对象用 reactive
4. **TypeScript**：充分利用 Vue3 的 TS 支持

### 前端工程化最佳实践

1. **构建工具**：新项目优先使用 Vite，老项目逐步迁移
2. **Monorepo**：中大型项目使用 pnpm workspaces + Turborepo
3. **代码规范**：强制 ESLint + Prettier + Git Hooks
4. **依赖管理**：锁定版本，定期审计安全漏洞

### TypeScript 最佳实践

1. **严格模式**：开启 strict 选项，充分利用类型检查
2. **避免 any**：使用 unknown + 类型守卫替代
3. **泛型约束**：合理使用 extends 约束泛型参数
4. **类型导出**：公共 API 必须导出类型定义

### React 最佳实践

1. **Hooks 规则**：只在顶层调用，只在 React 函数中调用
2. **性能优化**：useMemo/useCallback 适度使用，避免过度优化
3. **状态管理**：简单状态 useState，复杂状态 Zustand/Redux
4. **组件拆分**：单一职责，容器组件 + 展示组件

### 前后端联调最佳实践

1. **接口文档**：使用 Swagger/OpenAPI 自动生成文档
2. **Mock 数据**：开发环境使用 MSW 模拟接口
3. **错误处理**：统一错误码，分类处理不同错误
4. **安全防护**：XSS/CSRF 防护，请求防重放

### 性能优化最佳实践

1. **指标监控**：接入 Web Vitals 监控核心指标
2. **资源优化**：图片懒加载、代码分割、Tree-shaking
3. **缓存策略**：合理配置 HTTP 缓存、Service Worker
4. **渲染优化**：虚拟列表、防抖节流、Web Worker

### 微前端最佳实践

1. **技术选型**：技术栈统一用 Module Federation，异构用 qiankun
2. **样式隔离**：优先 CSS Modules，必要时 Shadow DOM
3. **公共依赖**：通过 externals 或共享配置避免重复加载
4. **通信机制**：简单场景 props，复杂场景全局状态

## 参考资料

- [Vue2 官方文档](https://v2.cn.vuejs.org/)
- [Vue3 官方文档](https://cn.vuejs.org/)
- [Vue Router 文档](https://router.vuejs.org/zh/)
- [Pinia 文档](https://pinia.vuejs.org/zh/)
- [VueUse](https://vueuse.org/) - Vue 组合式工具库
- [React 官方文档](https://react.dev/)
- [TypeScript 官方文档](https://www.typescriptlang.org/)
- [qiankun 文档](https://qiankun.umijs.org/)
- [Module Federation 文档](https://module-federation.io/)
- [Web Vitals](https://web.dev/vitals/)

---

**维护者**: itzixiao  
**最后更新**: 2026-03-22

