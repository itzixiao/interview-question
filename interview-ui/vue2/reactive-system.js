/**
 * Vue2 响应式系统实现
 *
 * 核心原理：
 * 1. 使用 Object.defineProperty() 对数据属性进行劫持
 * 2. 在 getter 中收集依赖（Watcher）
 * 3. 在 setter 中触发更新，通知所有依赖的 Watcher 执行回调
 *
 * 对应文档：docs/18-前端开发/01-Vue2核心原理详解.md
 */

/**
 * 依赖收集器类
 * 负责管理所有订阅者（Watcher），在数据变化时通知它们更新
 */
class Dep {
    constructor() {
        // subs 数组存储所有订阅了该数据的 Watcher
        this.subs = [];
    }

    /**
     * 添加订阅者
     * 当 Watcher 读取数据时，会将自身添加到 Dep 中
     */
    depend() {
        // Dep.target 是当前正在执行的 Watcher
        if (Dep.target) {
            this.subs.push(Dep.target);
        }
    }

    /**
     * 通知所有订阅者更新
     * 当数据发生变化时，调用此方法通知所有 Watcher
     */
    notify() {
        // 遍历所有订阅者，调用它们的 update 方法
        this.subs.forEach(watcher => watcher.update());
    }
}

// 全局变量，用于存储当前正在执行的 Watcher
Dep.target = null;

/**
 * Watcher 观察者类
 * 负责监听数据变化并执行回调函数
 */
class Watcher {
    /**
     * @param {Object} vm - Vue 实例（上下文）
     * @param {Function} expOrFn - 获取数据的函数
     * @param {Function} cb - 数据变化时的回调函数
     */
    constructor(vm, expOrFn, cb) {
        this.vm = vm;           // 保存 Vue 实例上下文
        this.getter = expOrFn;  // 获取数据的函数
        this.cb = cb;           // 回调函数
        // 初始化时立即执行一次 get，触发依赖收集
        this.value = this.get();
    }

    /**
     * 获取数据并触发依赖收集
     */
    get() {
        // 将当前 Watcher 设置为全局 target
        Dep.target = this;
        // 执行 getter，触发数据的 getter，从而收集依赖
        const value = this.getter.call(this.vm);
        // 收集完成后清空 target
        Dep.target = null;
        return value;
    }

    /**
     * 数据变化时调用此方法更新
     */
    update() {
        const oldValue = this.value;
        // 重新获取新值
        this.value = this.get();
        // 执行回调，通知外部数据已变化
        this.cb.call(this.vm, this.value, oldValue);
    }
}

/**
 * 将对象的属性转换为响应式
 * 使用 Object.defineProperty 进行数据劫持
 *
 * @param {Object} obj - 目标对象
 * @param {string} key - 属性名
 * @param {*} val - 属性值
 */
function defineReactive(obj, key, val) {
    // 递归处理嵌套对象，确保深层对象也是响应式的
    observe(val);

    // 为每个属性创建一个依赖收集器
    const dep = new Dep();

    // 使用 Object.defineProperty 定义属性的 getter 和 setter
    Object.defineProperty(obj, key, {
        enumerable: true,    // 可枚举
        configurable: true,  // 可配置

        /**
         * getter：读取属性时触发
         * 作用：收集依赖（Watcher）
         */
        get() {
            // 如果有正在执行的 Watcher，将其添加到依赖列表
            if (Dep.target) {
                dep.depend();
            }
            return val;
        },

        /**
         * setter：设置属性时触发
         * 作用：触发更新
         */
        set(newVal) {
            // 如果新值与旧值相同，不触发更新
            if (newVal === val) return;

            // 更新值
            val = newVal;

            // 如果新值是对象，也要将其转换为响应式
            observe(newVal);

            // 通知所有依赖该属性的 Watcher 更新
            dep.notify();
        }
    });
}

/**
 * 观察对象，将其所有属性转换为响应式
 *
 * @param {Object} obj - 要观察的对象
 */
function observe(obj) {
    // 如果不是对象或为 null，直接返回
    if (typeof obj !== 'object' || obj === null) {
        return;
    }

    // 遍历对象的所有属性，逐个转换为响应式
    Object.keys(obj).forEach(key => {
        defineReactive(obj, key, obj[key]);
    });
}

// ==================== 使用示例 ====================

// 创建响应式数据
const data = {name: 'Vue', count: 0};

// 将数据转换为响应式
observe(data);

// 创建 Watcher 监听数据变化
new Watcher({name: 'Vue', count: 0}, function () {
    // 这个函数会在初始化时执行，触发 getter 收集依赖
    return this.name + ': ' + this.count;
}, function (newVal, oldVal) {
    // 数据变化时的回调
    console.log('数据更新：', oldVal, '→', newVal);
});

// 修改数据，触发更新
console.log('初始值：', data.count);
data.count++; // 触发 setter，进而触发 notify，Watcher 收到通知执行 update
console.log('修改后：', data.count);

// 导出模块，供其他文件使用
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {Dep, Watcher, defineReactive, observe};
}
