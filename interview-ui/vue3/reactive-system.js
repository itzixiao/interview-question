/**
 * Vue3 响应式系统实现
 *
 * 核心原理：
 * 1. 使用 ES6 Proxy 代理对象，拦截属性的读取和设置操作
 * 2. 使用 Reflect 进行属性操作，保证 this 指向正确
 * 3. 懒代理：只有在访问属性时才进行深层代理
 * 4. 支持 Map、Set、WeakMap、WeakSet 等集合类型
 *
 * 与 Vue2 的区别：
 * - Vue2 使用 Object.defineProperty，Vue3 使用 Proxy
 * - Vue2 初始化时递归遍历所有属性，Vue3 是懒代理
 * - Vue3 可以监听动态新增的属性
 * - Vue3 可以监听数组索引的变化和 length 修改
 *
 * 对应文档：docs/18-前端开发/02-Vue3核心原理详解.md
 */

// 用于存储原始对象到代理的映射
const reactiveMap = new WeakMap();

// 用于存储代理到原始对象的映射
const rawMap = new WeakMap();

/**
 * 当前活动的 effect（相当于 Vue2 的 Watcher）
 * 用于依赖收集
 */
let activeEffect = null;

/**
 * effect 栈，用于处理嵌套的 effect
 */
const effectStack = [];

/**
 * 创建响应式对象
 * 这是对外暴露的主要 API，相当于 Vue3 的 reactive()
 *
 * @param {Object} target - 要代理的目标对象
 * @returns {Proxy} 响应式代理对象
 */
function reactive(target) {
    // 如果不是对象，直接返回
    if (typeof target !== 'object' || target === null) {
        console.warn('reactive() 只能接受对象类型参数');
        return target;
    }

    // 如果已经是响应式对象，直接返回
    if (reactiveMap.has(target)) {
        return reactiveMap.get(target);
    }

    // 创建代理
    const proxy = new Proxy(target, baseHandlers);

    // 存储映射关系
    reactiveMap.set(target, proxy);
    rawMap.set(proxy, target);

    return proxy;
}

/**
 * 创建 ref 对象
 * 用于包装基本类型值，使其具有响应式能力
 *
 * @param {*} value - 初始值
 * @returns {Object} ref 对象
 */
function ref(value) {
    const refObject = {
        get value() {
            // 收集依赖
            track(refObject, 'value');
            return value;
        },
        set value(newVal) {
            if (value !== newVal) {
                value = newVal;
                // 触发更新
                trigger(refObject, 'value');
            }
        }
    };
    return refObject;
}

/**
 * Proxy 处理器对象
 * 定义了 get、set、has、deleteProperty 等拦截器
 */
const baseHandlers = {
    /**
     * 拦截属性读取操作
     * @param {Object} target - 目标对象
     * @param {string} key - 属性名
     * @param {Object} receiver - 代理对象（或继承代理的对象）
     * @returns {*} 属性值
     */
    get(target, key, receiver) {
        // 特殊处理：如果访问 __v_isReactive，返回 true（用于标识响应式对象）
        if (key === '__v_isReactive') {
            return true;
        }

        // 获取原始值
        const result = Reflect.get(target, key, receiver);

        // 收集依赖
        track(target, key);

        // 如果值是对象，进行懒代理（递归处理）
        if (typeof result === 'object' && result !== null) {
            return reactive(result);
        }

        return result;
    },

    /**
     * 拦截属性设置操作
     * @param {Object} target - 目标对象
     * @param {string} key - 属性名
     * @param {*} value - 新值
     * @param {Object} receiver - 代理对象
     * @returns {boolean} 是否设置成功
     */
    set(target, key, value, receiver) {
        // 获取旧值
        const oldValue = target[key];

        // 判断是新增属性还是修改属性
        const hadKey = Object.prototype.hasOwnProperty.call(target, key);

        // 设置新值
        const result = Reflect.set(target, key, value, receiver);

        if (!hadKey) {
            // 新增属性
            trigger(target, key, 'add');
        } else if (value !== oldValue) {
            // 修改属性
            trigger(target, key, 'set');
        }

        return result;
    },

    /**
     * 拦截 in 操作符
     * @param {Object} target - 目标对象
     * @param {string} key - 属性名
     * @returns {boolean}
     */
    has(target, key) {
        const result = Reflect.has(target, key);
        track(target, key);
        return result;
    },

    /**
     * 拦截 delete 操作
     * @param {Object} target - 目标对象
     * @param {string} key - 属性名
     * @returns {boolean}
     */
    deleteProperty(target, key) {
        const hadKey = Object.prototype.hasOwnProperty.call(target, key);
        const result = Reflect.deleteProperty(target, key);

        if (hadKey && result) {
            // 删除属性，触发更新
            trigger(target, key, 'delete');
        }

        return result;
    }
};

/**
 * 依赖收集函数
 * 在读取属性时调用，将当前 effect 添加到依赖集合中
 *
 * @param {Object} target - 目标对象
 * @param {string} key - 属性名
 */
function track(target, key) {
    // 如果没有活动的 effect，直接返回
    if (!activeEffect) {
        return;
    }

    // 获取 target 对应的依赖映射
    let depsMap = targetMap.get(target);
    if (!depsMap) {
        depsMap = new Map();
        targetMap.set(target, depsMap);
    }

    // 获取 key 对应的依赖集合
    let dep = depsMap.get(key);
    if (!dep) {
        dep = new Set();
        depsMap.set(key, dep);
    }

    // 将当前 effect 添加到依赖集合
    dep.add(activeEffect);

    // 建立双向关联，用于清理
    activeEffect.deps.push(dep);
}

/**
 * 触发更新函数
 * 在设置属性时调用，执行所有依赖该属性的 effect
 *
 * @param {Object} target - 目标对象
 * @param {string} key - 属性名
 * @param {string} type - 操作类型（add、set、delete）
 */
function trigger(target, key, type = 'set') {
    const depsMap = targetMap.get(target);
    if (!depsMap) {
        return;
    }

    // 收集需要执行的 effect
    const effects = new Set();

    // 添加 key 对应的 effect
    const dep = depsMap.get(key);
    if (dep) {
        dep.forEach(effect => effects.add(effect));
    }

    // 如果是数组的 length 变化，需要触发所有索引的更新
    if (Array.isArray(target) && key === 'length') {
        depsMap.forEach((dep, key) => {
            if (key >= target.length) {
                dep.forEach(effect => effects.add(effect));
            }
        });
    }

    // 执行所有 effect
    effects.forEach(effect => {
        if (effect.scheduler) {
            // 如果有调度器，使用调度器执行
            effect.scheduler();
        } else {
            // 直接执行
            effect.run();
        }
    });
}

// 存储所有依赖关系的 WeakMap
// target -> Map<key, Set<effect>>
const targetMap = new WeakMap();

/**
 * Effect 类
 * 封装副作用函数，支持调度器和清理
 */
class ReactiveEffect {
    constructor(fn, scheduler = null) {
        this.fn = fn;                    // 副作用函数
        this.scheduler = scheduler;      // 调度器
        this.deps = [];                  // 依赖的集合数组
        this.active = true;              // 是否激活
    }

    run() {
        if (!this.active) {
            return this.fn();
        }

        // 清理之前的依赖
        cleanupEffect(this);

        // 设置当前活动的 effect
        activeEffect = this;
        effectStack.push(this);

        try {
            return this.fn();
        } finally {
            // 恢复之前的 effect
            effectStack.pop();
            activeEffect = effectStack[effectStack.length - 1] || null;
        }
    }

    stop() {
        if (this.active) {
            cleanupEffect(this);
            this.active = false;
        }
    }
}

/**
 * 清理 effect 的所有依赖
 * @param {ReactiveEffect} effect
 */
function cleanupEffect(effect) {
    const {deps} = effect;
    for (let i = 0; i < deps.length; i++) {
        deps[i].delete(effect);
    }
    deps.length = 0;
}

/**
 * 创建 effect
 * 对外暴露的 API，相当于 Vue3 的 watchEffect
 *
 * @param {Function} fn - 副作用函数
 * @param {Object} options - 选项
 * @returns {Function} 停止 effect 的函数
 */
function watchEffect(fn, options = {}) {
    const effect = new ReactiveEffect(fn, options.scheduler);
    effect.run();

    // 返回停止函数
    return () => effect.stop();
}

/**
 * computed 计算属性
 * 基于 effect 实现，具有缓存功能
 *
 * @param {Function} getter - 计算函数
 * @returns {Object} ref 对象
 */
function computed(getter) {
    let value;
    let dirty = true;  // 标记是否需要重新计算

    const effect = new ReactiveEffect(getter, () => {
        // 依赖变化时，标记为脏数据
        dirty = true;
    });

    return {
        get value() {
            if (dirty) {
                value = effect.run();
                dirty = false;
            }
            return value;
        }
    };
}

// ==================== 使用示例 ====================

// 1. 使用 reactive 创建响应式对象
const state = reactive({
    count: 0,
    user: {
        name: '张三',
        age: 25
    }
});

// 2. 使用 watchEffect 监听变化
watchEffect(() => {
    console.log('count 变化了：', state.count);
});

watchEffect(() => {
    console.log('user.name 变化了：', state.user.name);
});

// 3. 修改数据，触发更新
console.log('=== 修改 count ===');
state.count++;  // 触发第一个 effect

console.log('=== 修改 user.name ===');
state.user.name = '李四';  // 触发第二个 effect

console.log('=== 新增属性 ===');
state.newProp = '新属性';  // Vue3 支持动态新增属性

// 4. 使用 ref
const num = ref(10);
watchEffect(() => {
    console.log('num 变化了：', num.value);
});
num.value = 20;  // 触发更新

// 5. 使用 computed
const doubleCount = computed(() => state.count * 2);
console.log('doubleCount:', doubleCount.value);
state.count++;
console.log('doubleCount after update:', doubleCount.value);

// 导出模块
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {reactive, ref, watchEffect, computed};
}
