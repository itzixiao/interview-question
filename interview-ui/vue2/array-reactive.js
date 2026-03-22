/**
 * Vue2 数组响应式处理
 *
 * 核心原理：
 * Vue2 无法直接监听数组索引的变化，因此采用重写数组方法的方式实现响应式
 * 重写的数组方法：push、pop、shift、unshift、splice、sort、reverse
 *
 * 对应文档：docs/18-前端开发/01-Vue2核心原理详解.md
 */

// 保存原始数组原型
const arrayProto = Array.prototype;

// 创建一个新的对象，其原型是 Array.prototype
// 这样可以不污染原生数组原型
const arrayMethods = Object.create(arrayProto);

/**
 * 需要重写的数组方法列表
 * 这些方法会改变数组本身，需要触发更新
 */
const methodsToPatch = [
    'push',      // 在末尾添加元素
    'pop',       // 移除末尾元素
    'shift',     // 移除开头元素
    'unshift',   // 在开头添加元素
    'splice',    // 删除/添加元素
    'sort',      // 排序
    'reverse'    // 反转
];

/**
 * 重写数组方法
 * 拦截数组变异方法，在调用原始方法后触发更新
 */
methodsToPatch.forEach(method => {
    // 保存原始方法
    const original = arrayProto[method];

    // 定义新方法
    Object.defineProperty(arrayMethods, method, {
        /**
         * 变异方法实现
         * @param {...*} args - 原始方法的参数
         * @returns {*} - 原始方法的返回值
         */
        value: function mutator(...args) {
            // 1. 调用原始方法，获取结果
            const result = original.apply(this, args);

            // 2. 获取观察器对象（__ob__ 是在 observe 时添加的）
            const ob = this.__ob__;

            // 3. 处理新增的元素，将它们也转换为响应式
            let inserted;
            switch (method) {
                case 'push':
                case 'unshift':
                    // push 和 unshift 会将参数作为新元素添加到数组
                    inserted = args;
                    break;
                case 'splice':
                    // splice(start, deleteCount, ...items)
                    // 第三个参数及以后是要插入的新元素
                    inserted = args.slice(2);
                    break;
            }

            // 如果有新增元素，观察这些新元素
            if (inserted) {
                ob.observeArray(inserted);
            }

            // 4. 通知依赖更新
            // dep 是在 observe 时为数组创建的依赖收集器
            ob.dep.notify();

            return result;
        },
        enumerable: false,   // 不可枚举
        writable: true,      // 可写
        configurable: true   // 可配置
    });
});

/**
 * 观察数组类
 * 负责将数组转换为响应式
 */
class Observer {
    constructor(value) {
        this.value = value;
        // 为数组创建一个 Dep 实例，用于收集依赖
        this.dep = new Dep();

        // 在数组上添加 __ob__ 属性，指向当前 Observer 实例
        // 这样数组方法可以访问到 dep 和 observeArray
        Object.defineProperty(value, '__ob__', {
            value: this,
            enumerable: false,  // 不可枚举，避免被遍历到
            writable: true,
            configurable: true
        });

        // 如果值是数组，重写其原型方法
        if (Array.isArray(value)) {
            // 将数组的原型指向我们重写后的 arrayMethods
            Object.setPrototypeOf(value, arrayMethods);
            // 观察数组中的每一项
            this.observeArray(value);
        } else {
            // 如果是对象，遍历属性进行响应式处理
            this.walk(value);
        }
    }

    /**
     * 观察数组中的每一项
     * @param {Array} items - 数组
     */
    observeArray(items) {
        for (let i = 0, l = items.length; i < l; i++) {
            observe(items[i]);
        }
    }

    /**
     * 遍历对象的所有属性，转换为响应式
     * @param {Object} obj - 对象
     */
    walk(obj) {
        const keys = Object.keys(obj);
        for (let i = 0; i < keys.length; i++) {
            defineReactive(obj, keys[i]);
        }
    }
}

/**
 * 简化的 Dep 类（依赖收集器）
 */
class Dep {
    constructor() {
        this.subs = [];
    }

    depend() {
        if (Dep.target) {
            this.subs.push(Dep.target);
        }
    }

    notify() {
        this.subs.forEach(sub => sub.update());
    }
}

Dep.target = null;

/**
 * 观察函数 - 将值转换为响应式
 * @param {*} value - 要观察的值
 * @returns {Observer} - Observer 实例
 */
function observe(value) {
    if (typeof value !== 'object' || value === null) {
        return;
    }
    let ob;
    if (value.__ob__ instanceof Observer) {
        ob = value.__ob__;
    } else {
        ob = new Observer(value);
    }
    return ob;
}

/**
 * 简化的 defineReactive 函数
 */
function defineReactive(obj, key, val) {
    const dep = new Dep();
    const property = Object.getOwnPropertyDescriptor(obj, key);
    if (property && property.configurable === false) {
        return;
    }

    // 获取属性的 getter 和 setter（如果已有）
    const getter = property && property.get;
    const setter = property && property.set;

    // 递归观察值
    let childOb = observe(val);

    Object.defineProperty(obj, key, {
        enumerable: true,
        configurable: true,
        get() {
            const value = getter ? getter.call(obj) : val;
            if (Dep.target) {
                dep.depend();
                if (childOb) {
                    childOb.dep.depend();
                }
            }
            return value;
        },
        set(newVal) {
            const value = getter ? getter.call(obj) : val;
            if (newVal === value) {
                return;
            }
            if (setter) {
                setter.call(obj, newVal);
            } else {
                val = newVal;
            }
            childOb = observe(newVal);
            dep.notify();
        }
    });
}

// ==================== 使用示例 ====================

// 创建一个包含数组的响应式对象
const data = {
    items: [1, 2, 3],
    user: {
        name: '张三',
        hobbies: ['读书', '游泳']
    }
};

// 观察数据
observe(data);

// 模拟 Watcher
class Watcher {
    constructor(vm, exp, cb) {
        this.vm = vm;
        this.exp = exp;
        this.cb = cb;
        this.value = this.get();
    }

    get() {
        Dep.target = this;
        const value = this.vm[this.exp];
        Dep.target = null;
        return value;
    }

    update() {
        const oldValue = this.value;
        this.value = this.get();
        this.cb.call(this.vm, this.value, oldValue);
    }
}

// 监听数组变化
new Watcher(data, 'items', function (newVal, oldVal) {
    console.log('数组变化：', oldVal, '→', newVal);
});

console.log('初始数组：', data.items);

// 使用重写后的方法，会触发更新
data.items.push(4);  // 触发更新
console.log('push后：', data.items);

data.items.splice(0, 1, 10);  // 触发更新
console.log('splice后：', data.items);

// 导出模块
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {arrayMethods, Observer, observe};
}
