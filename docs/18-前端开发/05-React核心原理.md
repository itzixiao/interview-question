# React 核心原理

## 一、React vs Vue 架构对比

| 特性             | React               | Vue            |
|----------------|---------------------|----------------|
| **开发团队**       | Meta (Facebook)     | 社区驱动 + 尤雨溪     |
| **核心思想**       | 函数式编程、不可变数据         | 渐进式、响应式数据      |
| **模板语法**       | JSX (JavaScript 扩展) | 模板语法 + JSX 可选  |
| **数据绑定**       | 单向数据流               | 双向绑定 (v-model) |
| **状态管理**       | useState/useReducer | ref/reactive   |
| **DOM更新**      | 虚拟 DOM + Diff       | 虚拟 DOM + Diff  |
| **生态规模**       | 更大，更分散              | 更集中，官方维护       |
| **学习曲线**       | 较陡（需了解 JSX、Hooks）   | 较平缓            |
| **TypeScript** | 原生支持好               | 支持良好           |

## 二、JSX 本质与编译

```javascript
// JSX 写法
const element = (
    <div className="container">
        <h1>Hello React</h1>
        <p>Count: {count}</p>
    </div>
);

// 编译后（Babel 转换）
const element = React.createElement(
    'div',
    { className: 'container' },
    React.createElement('h1', null, 'Hello React'),
    React.createElement('p', null, 'Count: ', count)
);

// React 17+ 自动运行时（无需引入 React）
import { jsx as _jsx } from 'react/jsx-runtime';

const element = _jsx('div', {
    className: 'container',
    children: [
        _jsx('h1', { children: 'Hello React' }),
        _jsx('p', { children: ['Count: ', count] })
    ]
});
```

```javascript
// React.createElement 实现原理
function createElement(type, config, ...children) {
    const props = {};
    let key = null;
    let ref = null;
    
    if (config != null) {
        if (config.key !== undefined) {
            key = '' + config.key;
        }
        if (config.ref !== undefined) {
            ref = config.ref;
        }
        
        for (const propName in config) {
            if (propName !== 'key' && propName !== 'ref') {
                props[propName] = config[propName];
            }
        }
    }
    
    props.children = children.length === 1 
        ? children[0] 
        : children;
    
    return {
        $$typeof: Symbol.for('react.element'),
        type,
        key,
        ref,
        props,
        _owner: null
    };
}
```

## 三、Hooks 原理深度解析

### 3.1 Hooks 调用规则

```javascript
// ✅ 正确：在组件顶层调用
function Counter() {
    const [count, setCount] = useState(0);     // Hook 1
    const [name, setName] = useState('');      // Hook 2
    const ref = useRef(null);                   // Hook 3
    
    useEffect(() => {                          // Hook 4
        console.log(count);
    }, [count]);
    
    return <div>{count}</div>;
}

// ❌ 错误：在条件语句中调用
function Wrong() {
    if (condition) {
        const [state, setState] = useState(0); // 错误！
    }
}

// ❌ 错误：在循环中调用
function Wrong2() {
    for (let i = 0; i < 3; i++) {
        useState(i); // 错误！
    }
}
```

### 3.2 Hooks 实现原理（简化版）

```javascript
// React Hooks 内部实现原理（简化版）

// Hook 链表节点
const Hook = {
    memoizedState: null,    // 当前状态
    baseState: null,        // 基础状态
    baseQueue: null,        // 基础更新队列
    queue: null,            // 更新队列
    next: null              // 下一个 Hook
};

// 当前正在渲染的组件
let currentlyRenderingFiber = null;
let workInProgressHook = null;  // 当前工作的 Hook
let firstWorkInProgressHook = null; // 第一个 Hook

// 初始化 Hook
function mountWorkInProgressHook() {
    const hook = {
        memoizedState: null,
        baseState: null,
        baseQueue: null,
        queue: null,
        next: null
    };
    
    if (workInProgressHook === null) {
        // 第一个 Hook
        firstWorkInProgressHook = workInProgressHook = hook;
    } else {
        // 链接到链表
        workInProgressHook.next = hook;
        workInProgressHook = hook;
    }
    
    return workInProgressHook;
}

// useState 实现
function useState(initialState) {
    return useReducer(
        basicStateReducer,
        initialState
    );
}

// useReducer 实现
function useReducer(reducer, initialArg, init) {
    // 获取当前 Hook
    const hook = mountWorkInProgressHook();
    
    // 初始化状态
    let initialState;
    if (init !== undefined) {
        initialState = init(initialArg);
    } else {
        initialState = initialArg;
    }
    
    hook.memoizedState = hook.baseState = initialState;
    
    // 创建更新队列
    const queue = {
        pending: null,
        dispatch: null,
        lastRenderedReducer: reducer,
        lastRenderedState: initialState
    };
    
    hook.queue = queue;
    
    // 绑定 dispatch
    const dispatch = queue.dispatch = dispatchAction.bind(
        null,
        currentlyRenderingFiber,
        queue
    );
    
    return [hook.memoizedState, dispatch];
}

// dispatch 函数（触发更新）
function dispatchAction(fiber, queue, action) {
    // 创建更新对象
    const update = {
        lane: currentLane,
        action: action,
        eagerReducer: null,
        eagerState: null,
        next: null
    };
    
    // 添加到更新队列
    const pending = queue.pending;
    if (pending === null) {
        update.next = update;
    } else {
        update.next = pending.next;
        pending.next = update;
    }
    queue.pending = update;
    
    // 调度更新
    scheduleUpdateOnFiber(fiber);
}
```

### 3.3 常用 Hooks 详解

```javascript
// ===== useState =====
const [state, setState] = useState(initialValue);
const [state, setState] = useState(() => computeInitialState()); // 懒初始化

// 函数式更新（避免闭包问题）
setState(prevState => prevState + 1);

// ===== useEffect =====
// 组件挂载 + 依赖更新时执行
useEffect(() => {
    console.log('effect runs');
    return () => {
        console.log('cleanup'); // 组件卸载或依赖变化前执行
    };
}, [dependency]);

// 只执行一次（挂载和卸载）
useEffect(() => {
    // 初始化逻辑
    return () => {
        // 清理逻辑
    };
}, []);

// ===== useLayoutEffect =====
// 同步执行，在浏览器绘制之前
// 用于需要同步执行、避免闪烁的场景
useLayoutEffect(() => {
    // 测量 DOM 并同步修改
    const { width } = element.getBoundingClientRect();
    setWidth(width);
}, []);

// ===== useRef =====
// 保存可变值，不触发重新渲染
const ref = useRef(initialValue);
console.log(ref.current); // 访问值
ref.current = newValue;   // 修改值

// DOM 引用
const inputRef = useRef(null);
inputRef.current.focus();

// 保存上一次的值
const prevCountRef = useRef();
useEffect(() => {
    prevCountRef.current = count;
});
const prevCount = prevCountRef.current;

// ===== useMemo =====
// 缓存计算结果
const memoizedValue = useMemo(() => {
    return computeExpensiveValue(a, b);
}, [a, b]);

// ===== useCallback =====
// 缓存函数引用
const memoizedCallback = useCallback(() => {
    doSomething(a, b);
}, [a, b]);

// 等价于
const memoizedCallback = useMemo(() => {
    return () => {
        doSomething(a, b);
    };
}, [a, b]);

// ===== useReducer =====
// 复杂状态逻辑
const initialState = { count: 0 };

function reducer(state, action) {
    switch (action.type) {
        case 'increment':
            return { count: state.count + 1 };
        case 'decrement':
            return { count: state.count - 1 };
        default:
            throw new Error();
    }
}

const [state, dispatch] = useReducer(reducer, initialState);

// 惰性初始化
const [state, dispatch] = useReducer(reducer, initialArg, init);

// ===== useContext =====
const ThemeContext = createContext('light');

function ThemedButton() {
    const theme = useContext(ThemeContext);
    return <button className={theme}>Click me</button>;
}

// ===== 自定义 Hook =====
function useLocalStorage(key, initialValue) {
    const [storedValue, setStoredValue] = useState(() => {
        try {
            const item = window.localStorage.getItem(key);
            return item ? JSON.parse(item) : initialValue;
        } catch (error) {
            return initialValue;
        }
    });

    const setValue = (value) => {
        try {
            const valueToStore = value instanceof Function 
                ? value(storedValue) 
                : value;
            setStoredValue(valueToStore);
            window.localStorage.setItem(key, JSON.stringify(valueToStore));
        } catch (error) {
            console.log(error);
        }
    };

    return [storedValue, setValue];
}

// ===== useImperativeHandle =====
// 暴露子组件方法给父组件
const FancyInput = forwardRef((props, ref) => {
    const inputRef = useRef();
    
    useImperativeHandle(ref, () => ({
        focus: () => {
            inputRef.current.focus();
        },
        clear: () => {
            inputRef.current.value = '';
        }
    }));
    
    return <input ref={inputRef} {...props} />;
});
```

## 四、React 渲染流程

```
触发更新 (setState/forceUpdate/useReducer)
    ↓
创建更新对象 (Update)
    ↓
调度更新 (Scheduler)
    ↓
进入 Reconciler（协调器）
    ↓
构建 Fiber 树（双缓冲：current / workInProgress）
    ↓
Diff 算法（对比新旧虚拟 DOM）
    ↓
生成 Effect List（副作用列表）
    ↓
进入 Renderer（渲染器）
    ↓
提交阶段（Commit Phase）
    - BeforeMutation
    - Mutation（DOM 操作）
    - Layout（useLayoutEffect）
    ↓
浏览器绘制
    ↓
Passive Effects（useEffect）
```

### 4.1 Fiber 架构

```javascript
// Fiber 节点结构
const fiber = {
    // 组件类型
    type: 'div', // 或 MyComponent
    
    // 唯一标识
    key: null,
    
    // 实例引用
    stateNode: null, // DOM 节点或组件实例
    
    // Fiber 链表结构
    return: parentFiber,  // 父 Fiber
    child: firstChild,    // 第一个子 Fiber
    sibling: nextSibling, // 下一个兄弟 Fiber
    index: 0,             // 在兄弟中的索引
    
    // 更新相关
    pendingProps: newProps,
    memoizedProps: currentProps,
    memoizedState: currentState,
    updateQueue: updateQueue,
    
    // 副作用
    flags: flags,         // 副作用标记（Placement/Update/Deletion）
    subtreeFlags: flags,  // 子树副作用
    deletions: null,      // 待删除的子节点
    
    // 双缓冲
    alternate: currentFiber, // 对应的 current/workInProgress
    
    // 其他
    lanes: lanes,         // 更新优先级
    childLanes: lanes     // 子树优先级
};
```

### 4.2 Diff 算法（协调算法）

```javascript
// React Diff 三大策略

// 1. 同级比较（不会跨层级移动）
// 2. 不同类型的元素直接替换
// 3. 通过 key 识别可复用元素

// Diff 过程（单链表 diff）
function reconcileChildrenArray(
    returnFiber,
    currentFirstChild,
    newChildren
) {
    let resultingFirstChild = null;
    let previousNewFiber = null;
    let oldFiber = currentFirstChild;
    let lastPlacedIndex = 0;
    let newIdx = 0;
    let nextOldFiber = null;
    
    // 第一轮：遍历相同位置，key 相同则复用
    for (; oldFiber !== null && newIdx < newChildren.length; newIdx++) {
        if (oldFiber.index > newIdx) {
            nextOldFiber = oldFiber;
            oldFiber = null;
        } else {
            nextOldFiber = oldFiber.sibling;
        }
        
        const newFiber = updateSlot(
            returnFiber,
            oldFiber,
            newChildren[newIdx]
        );
        
        if (newFiber === null) {
            if (oldFiber === null) {
                oldFiber = nextOldFiber;
            }
            break;
        }
        
        if (shouldTrackSideEffects) {
            if (oldFiber && newFiber.alternate === null) {
                // 删除旧节点
                deleteChild(returnFiber, oldFiber);
            }
        }
        
        lastPlacedIndex = placeChild(newFiber, lastPlacedIndex, newIdx);
        
        if (previousNewFiber === null) {
            resultingFirstChild = newFiber;
        } else {
            previousNewFiber.sibling = newFiber;
        }
        
        previousNewFiber = newFiber;
        oldFiber = nextOldFiber;
    }
    
    // 第二轮：新节点遍历完，删除剩余旧节点
    if (newIdx === newChildren.length) {
        deleteRemainingChildren(returnFiber, oldFiber);
        return resultingFirstChild;
    }
    
    // 第三轮：旧节点遍历完，创建剩余新节点
    if (oldFiber === null) {
        for (; newIdx < newChildren.length; newIdx++) {
            const newFiber = createChild(returnFiber, newChildren[newIdx]);
            if (newFiber === null) continue;
            
            lastPlacedIndex = placeChild(newFiber, lastPlacedIndex, newIdx);
            
            if (previousNewFiber === null) {
                resultingFirstChild = newFiber;
            } else {
                previousNewFiber.sibling = newFiber;
            }
            previousNewFiber = newFiber;
        }
        return resultingFirstChild;
    }
    
    // 第四轮：key 不同的情况，建立 map 查找
    const existingChildren = mapRemainingChildren(returnFiber, oldFiber);
    
    for (; newIdx < newChildren.length; newIdx++) {
        const newFiber = updateFromMap(
            existingChildren,
            returnFiber,
            newIdx,
            newChildren[newIdx]
        );
        
        if (newFiber !== null) {
            if (shouldTrackSideEffects) {
                if (newFiber.alternate !== null) {
                    existingChildren.delete(
                        newFiber.key === null ? newIdx : newFiber.key
                    );
                }
            }
            
            lastPlacedIndex = placeChild(newFiber, lastPlacedIndex, newIdx);
            
            if (previousNewFiber === null) {
                resultingFirstChild = newFiber;
            } else {
                previousNewFiber.sibling = newFiber;
            }
            previousNewFiber = newFiber;
        }
    }
    
    // 删除 map 中剩余的旧节点
    if (shouldTrackSideEffects) {
        existingChildren.forEach(child => deleteChild(returnFiber, child));
    }
    
    return resultingFirstChild;
}
```

## 五、状态管理方案

### 5.1 Context API

```javascript
// 创建 Context
const ThemeContext = createContext({
    theme: 'light',
    toggleTheme: () => {}
});

// Provider 组件
function ThemeProvider({ children }) {
    const [theme, setTheme] = useState('light');
    
    const toggleTheme = useCallback(() => {
        setTheme(t => t === 'light' ? 'dark' : 'light');
    }, []);
    
    const value = useMemo(() => ({
        theme,
        toggleTheme
    }), [theme, toggleTheme]);
    
    return (
        <ThemeContext.Provider value={value}>
            {children}
        </ThemeContext.Provider>
    );
}

// 使用 Context
function ThemedButton() {
    const { theme, toggleTheme } = useContext(ThemeContext);
    return (
        <button 
            style={{ background: theme === 'light' ? '#fff' : '#333' }}
            onClick={toggleTheme}
        >
            Toggle Theme
        </button>
    );
}

// 自定义 Hook 封装
function useTheme() {
    const context = useContext(ThemeContext);
    if (context === undefined) {
        throw new Error('useTheme must be used within ThemeProvider');
    }
    return context;
}
```

### 5.2 Redux 基础

```javascript
// Store 创建
import { createStore, combineReducers, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import { composeWithDevTools } from 'redux-devtools-extension';

// Action Types
const INCREMENT = 'counter/INCREMENT';
const DECREMENT = 'counter/DECREMENT';
const SET_USER = 'user/SET_USER';

// Action Creators
const increment = () => ({ type: INCREMENT });
const decrement = () => ({ type: DECREMENT });
const setUser = (user) => ({ type: SET_USER, payload: user });

// 异步 Action（Thunk）
const fetchUser = (userId) => {
    return async (dispatch, getState) => {
        try {
            const response = await fetch(`/api/users/${userId}`);
            const user = await response.json();
            dispatch(setUser(user));
        } catch (error) {
            console.error(error);
        }
    };
};

// Reducers
const counterReducer = (state = 0, action) => {
    switch (action.type) {
        case INCREMENT:
            return state + 1;
        case DECREMENT:
            return state - 1;
        default:
            return state;
    }
};

const userReducer = (state = null, action) => {
    switch (action.type) {
        case SET_USER:
            return action.payload;
        default:
            return state;
    }
};

// 合并 Reducers
const rootReducer = combineReducers({
    counter: counterReducer,
    user: userReducer
});

// 创建 Store
const store = createStore(
    rootReducer,
    composeWithDevTools(applyMiddleware(thunk))
);

// React-Redux Hooks
import { useSelector, useDispatch } from 'react-redux';

function Counter() {
    const count = useSelector(state => state.counter);
    const user = useSelector(state => state.user);
    const dispatch = useDispatch();
    
    return (
        <div>
            <p>Count: {count}</p>
            <button onClick={() => dispatch(increment())}>+</button>
            <button onClick={() => dispatch(decrement())}>-</button>
        </div>
    );
}
```

### 5.3 Zustand（轻量状态管理）

```javascript
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

const useStore = create(
    devtools(
        persist(
            (set, get) => ({
                // State
                count: 0,
                user: null,

                // Actions
                increment: () => set(state => ({count: state.count + 1})),
                decrement: () => set(state => ({count: state.count - 1})),
                setUser: (user) => set({user}),

                // 异步 Action
                fetchUser: async (userId) => {
                    const response = await fetch(`/api/users/${userId}`);
                    const user = await response.json();
                    set({user});
                },

                // 计算属性
                get doubleCount() {
                    return get().count * 2;
                }
            }),
            {
                name: 'my-app-storage', // localStorage key
                partialize: (state) => ({user: state.user}) // 只持久化 user
            }
        )
    )
);

// 使用
function Counter() {
    const {count, increment, doubleCount} = useStore();
    return (
        <div>
            <p>{count} / {doubleCount}</p>
            <button onClick={increment}>+</button>
        </div>
    );
}

// 选择器优化（避免不必要的重渲染）
const useCount = () => useStore(state => state.count);
```

---

## 六、高频面试题

**问题 1：React Hooks 为什么不能放在条件语句中？**

**答：**

React 通过 Hook 的**调用顺序**来区分不同的 Hook：

```javascript
// React 内部使用数组存储 Hook 状态
const hooks = [];
let idx = 0;

function useState(initialValue) {
    const state = hooks[idx] !== undefined ? hooks[idx] : initialValue;
    hooks[idx] = state;
    const _idx = idx;
    idx++;
    
    return [
        state,
        (newValue) => {
            hooks[_idx] = newValue;
            render();
        }
    ];
}

// 如果条件调用，顺序会错乱
function Wrong() {
    const [name, setName] = useState(''); // idx=0
    if (condition) {
        const [age, setAge] = useState(0); // 可能执行也可能不执行
    }
    const [count, setCount] = useState(0); // idx 不确定是 1 还是 2
}
```

---

**问题 2：useEffect 和 useLayoutEffect 的区别？**

**答：**

| 特性       | useEffect | useLayoutEffect   |
|----------|-----------|-------------------|
| **执行时机** | 浏览器绘制之后   | 浏览器绘制之前           |
| **阻塞**   | 不阻塞绘制     | 阻塞绘制              |
| **使用场景** | 大多数副作用    | DOM 测量、同步修改       |
| **SSR**  | 支持        | 不支持（需用 useEffect） |

```javascript
// useLayoutEffect 使用场景：避免闪烁
function Tooltip() {
    const [position, setPosition] = useState({ x: 0, y: 0 });
    const tooltipRef = useRef();
    
    useLayoutEffect(() => {
        // 同步计算位置，避免先渲染错误位置再修正
        const { width, height } = tooltipRef.current.getBoundingClientRect();
        setPosition(calculatePosition(width, height));
    }, []);
    
    return <div ref={tooltipRef} style={position}>...</div>;
}
```

---

**问题 3：React 的 Diff 算法和 Vue 有什么区别？**

**答：**

| 特性         | React              | Vue     |
|------------|--------------------|---------|
| **对比策略**   | 单链表 diff           | 双端比较    |
| **key 作用** | 识别可复用节点            | 识别可复用节点 |
| **移动检测**   | 基于 lastPlacedIndex | 基于双端指针  |
| **复杂度**    | O(n)               | O(n)    |

**React Diff 特点：**

1. 只比较同层级，不跨层级
2. 不同类型直接替换
3. key 相同则复用，通过 lastPlacedIndex 判断是否移动

**Vue Diff 特点：**

1. 双端比较（头头、尾尾、头尾、尾头）
2. 四种比较方式最大化复用
3. key 查找旧节点位置

---

**问题 4：useMemo 和 useCallback 的区别和使用场景？**

**答：**

| 特性       | useMemo | useCallback |
|----------|---------|-------------|
| **缓存内容** | 计算结果    | 函数引用        |
| **返回值**  | 任意类型    | 函数          |
| **使用场景** | 昂贵计算    | 子组件优化、依赖函数  |

```javascript
// useMemo：缓存计算结果
const expensiveValue = useMemo(() => {
    return data.filter(item => item.active).sort((a, b) => b.score - a.score);
}, [data]);

// useCallback：缓存函数引用
const handleSubmit = useCallback((values) => {
    submitForm(values);
}, []); // 空依赖，函数引用不变

// 配合 React.memo 使用
const ChildComponent = React.memo(({ onSubmit }) => {
    return <button onClick={onSubmit}>Submit</button>;
});

// 父组件
function Parent() {
    const [count, setCount] = useState(0);
    
    // 不使用 useCallback，每次渲染都是新函数，导致子组件重渲染
    // const handleClick = () => console.log('clicked');
    
    // 使用 useCallback，函数引用稳定
    const handleClick = useCallback(() => {
        console.log('clicked');
    }, []);
    
    return (
        <div>
            <p>{count}</p>
            <button onClick={() => setCount(c => c + 1)}>+</button>
            <ChildComponent onSubmit={handleClick} />
        </div>
    );
}
```

---

**问题 5：React 的 Fiber 架构解决了什么问题？**

**答：**

**解决的问题：**

1. **可中断的渲染**：之前的递归渲染无法中断，大组件会阻塞主线程
2. **优先级调度**：不同更新可以设置不同优先级
3. **增量渲染**：将渲染工作拆分成小块，利用空闲时间执行

**Fiber 特点：**

```javascript
// 链表结构支持暂停和恢复
const fiber = {
    child: Fiber | null,    // 第一个子节点
    sibling: Fiber | null,  // 下一个兄弟节点
    return: Fiber | null,   // 父节点
    // ...
};

// 工作循环（可中断）
function workLoop(hasTimeRemaining, initialTime) {
    let currentTime = initialTime;
    advanceTimers(currentTime);
    currentTask = peek(taskQueue);
    
    while (currentTask !== null) {
        if (currentTask.expirationTime > currentTime 
            && (!hasTimeRemaining || shouldYieldToHost())) {
            // 时间片用完，中断渲染
            break;
        }
        
        const callback = currentTask.callback;
        if (typeof callback === 'function') {
            callback();
        }
        currentTask = peek(taskQueue);
    }
}
```

---

**问题 6：Redux 和 Context API 应该如何选择？**

**答：**

| 场景            | 推荐方案                 |
|---------------|----------------------|
| 简单状态共享（主题、语言） | Context API          |
| 复杂状态逻辑、异步操作   | Redux / Zustand      |
| 频繁更新的状态       | 避免 Context（会导致大量重渲染） |
| 需要时间旅行调试      | Redux                |
| 轻量应用          | Zustand / Jotai      |

**Context 性能问题：**

```javascript
// 问题：任何状态变化都导致所有消费者重渲染
function Provider({ children }) {
    const [state, setState] = useState({ count: 0, name: '' });
    
    return (
        <Context.Provider value={{ state, setState }}>
            {children}
        </Context.Provider>
    );
}

// 优化：拆分 Context 或使用状态选择器
function CountProvider({ children }) {
    const [count, setCount] = useState(0);
    const value = useMemo(() => ({ count, setCount }), [count]);
    return <CountContext.Provider value={value}>{children}</CountContext.Provider>;
}
```

---

**问题 7：React.memo 和 useMemo 的区别？**

**答：**

| 特性       | React.memo | useMemo |
|----------|------------|---------|
| **作用对象** | 组件         | 任意值     |
| **比较方式** | 浅比较 props  | 依赖数组比较  |
| **返回值**  | 组件         | 缓存的值    |

```javascript
// React.memo：组件级别的缓存
const MemoizedComponent = React.memo(MyComponent, (prevProps, nextProps) => {
    // 自定义比较逻辑
    return prevProps.id === nextProps.id;
});

// useMemo：值级别的缓存
const memoizedValue = useMemo(() => {
    return computeExpensiveValue(a, b);
}, [a, b]);
```
