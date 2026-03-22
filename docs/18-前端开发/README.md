# 前端开发技术文档

## 文档列表

| 序号 | 文档标题 | 核心内容 | 面试题数 | 状态 |
|------|---------|---------|---------|------|
| 01 | [Vue2 核心原理详解](01-Vue2核心原理详解.md) | 响应式原理、Diff 算法、组件通信、生命周期、Vuex、Vue Router | 10 | ✅ |
| 02 | [Vue3 核心原理详解](02-Vue3核心原理详解.md) | Composition API、Proxy 响应式、Pinia、新特性组件、性能优化 | 10 | ✅ |

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

## Vue2 vs Vue3 对比

| 特性 | Vue2 | Vue3 |
|------|------|------|
| 响应式原理 | Object.defineProperty() | Proxy |
| API 风格 | Options API | Options API + Composition API |
| TypeScript | 支持较弱 | 原生支持 |
| 性能 | 良好 | 提升 1.3~2 倍 |
| 包体积 | ~23KB | ~10KB（运行时） |
| Tree-shaking | 不支持 | 支持 |
| 多根节点 | 不支持 | 支持（Fragments） |
| 状态管理 | Vuex | Pinia（推荐） |

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

## 高频面试题速览

### Vue2 高频题

1. Vue2 的响应式原理是什么？
2. Vue2 的 Diff 算法原理是什么？
3. computed 和 watch 的区别是什么？
4. v-if 和 v-show 的区别是什么？
5. Vue 组件的通信方式有哪些？
6. Vue Router 的导航守卫有哪些？执行顺序是什么？
7. Vuex 的 mutation 和 action 有什么区别？
8. Vue2 的生命周期有哪些？created 和 mounted 的区别？
9. 为什么 Vue2 中 data 必须是函数？
10. Vue2 中的 key 有什么作用？

### Vue3 高频题

1. Vue3 的响应式原理与 Vue2 有什么区别？
2. Composition API 与 Options API 的区别？
3. ref 和 reactive 的区别？
4. Vue3 的生命周期钩子有哪些变化？
5. Pinia 与 Vuex 的区别？
6. Vue Router 4 有哪些变化？
7. defineModel 是什么？解决了什么问题？
8. Vue3 的性能优化有哪些？
9. Teleport 和 Suspense 的使用场景？
10. Vue3 如何兼容 Vue2 的写法？

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

## 参考资料

- [Vue2 官方文档](https://v2.cn.vuejs.org/)
- [Vue3 官方文档](https://cn.vuejs.org/)
- [Vue Router 文档](https://router.vuejs.org/zh/)
- [Pinia 文档](https://pinia.vuejs.org/zh/)
- [VueUse](https://vueuse.org/) - Vue 组合式工具库

---

**维护者**: itzixiao  
**最后更新**: 2026-03-22
