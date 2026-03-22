/**
 * React Hooks 示例与原理说明
 *
 * 核心 Hooks：
 * 1. useState - 状态管理
 * 2. useEffect - 副作用处理
 * 3. useContext - 上下文消费
 * 4. useReducer - 复杂状态逻辑
 * 5. useMemo - 记忆化计算
 * 6. useCallback - 记忆化回调
 * 7. useRef - 引用持久化
 *
 * 对应文档：docs/18-前端开发/05-React核心原理.md
 */

import React, {
    createContext,
    memo,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useReducer,
    useRef,
    useState
} from 'react';

// ==================== useState 示例 ====================

/**
 * useState 基础用法
 *
 * 原理说明：
 * - useState 返回一个数组 [state, setState]
 * - state 是状态值，setState 是更新函数
 * - 调用 setState 会触发组件重新渲染
 * - 更新函数可以接收新值或函数（函数式更新）
 */
function Counter() {
    // 基本用法
    const [count, setCount] = useState(0);

    // 函数式初始化（只在首次渲染执行）
    const [user, setUser] = useState(() => ({
        name: '张三',
        age: 25
    }));

    return (
        <div>
            <p>当前计数：{count}</p>
            {/* 直接传递新值 */}
            <button onClick={() => setCount(count + 1)}>
                直接+1
            </button>
            {/* 函数式更新（基于前一个状态） */}
            <button onClick={() => setCount(prev => prev + 1)}>
                函数式+1
            </button>
            <p>用户名：{user.name}</p>
        </div>
    );
}

// ==================== useEffect 示例 ====================

/**
 * useEffect 基础用法
 *
 * 原理说明：
 * - 用于处理副作用（数据获取、订阅、手动修改 DOM 等）
 * - 在组件渲染后异步执行
 * - 通过依赖数组控制执行时机
 * - 返回的清理函数在组件卸载或依赖变化前执行
 */
function UserInfo({userId}) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    /**
     * 依赖数组说明：
     * - []: 只在组件挂载和卸载时执行
     * - [userId]: 在挂载、卸载和 userId 变化时执行
     * - 无依赖数组：每次渲染后都执行
     */
    useEffect(() => {
        // 标记组件是否已卸载，避免内存泄漏
        let isMounted = true;

        async function fetchUser() {
            setLoading(true);
            try {
                const response = await fetch(`/api/users/${userId}`);
                const data = await response.json();

                // 只有在组件未卸载时才更新状态
                if (isMounted) {
                    setUser(data);
                    setLoading(false);
                }
            } catch (error) {
                if (isMounted) {
                    console.error('获取用户失败：', error);
                    setLoading(false);
                }
            }
        }

        fetchUser();

        // 清理函数：组件卸载或 userId 变化前执行
        return () => {
            isMounted = false;
        };
    }, [userId]);  // 依赖数组

    if (loading) return <div>加载中...</div>;
    if (!user) return <div>用户不存在</div>;

    return (
        <div>
            <h3>{user.name}</h3>
            <p>邮箱：{user.email}</p>
        </div>
    );
}

/**
 * useEffect 不同依赖的对比
 */
function EffectDemo() {
    const [count, setCount] = useState(0);

    // 1. 只在挂载时执行（空依赖数组）
    useEffect(() => {
        console.log('组件挂载');
        return () => {
            console.log('组件卸载');
        };
    }, []);

    // 2. 在挂载和 count 变化时执行
    useEffect(() => {
        console.log('count 变化：', count);
    }, [count]);

    // 3. 每次渲染都执行（不推荐，除非特殊需求）
    useEffect(() => {
        console.log('每次渲染');
    });

    return <button onClick={() => setCount(c => c + 1)}>点击 {count}</button>;
}

// ==================== useContext 示例 ====================

/**
 * 创建上下文
 * 用于跨组件传递数据，避免层层传递 props
 */
const ThemeContext = createContext({
    theme: 'light',
    toggleTheme: () => {
    }
});

/**
 * useContext 使用示例
 *
 * 原理说明：
 * - useContext 接收一个 context 对象（React.createContext 的返回值）
 * - 返回当前 context 的值
 * - 当 context 值变化时，组件会重新渲染
 */
function ThemedButton() {
    // 使用 useContext 获取上下文值
    const {theme, toggleTheme} = useContext(ThemeContext);

    const styles = {
        backgroundColor: theme === 'light' ? '#fff' : '#333',
        color: theme === 'light' ? '#333' : '#fff',
        padding: '10px 20px',
        border: '1px solid #ccc',
        borderRadius: '4px'
    };

    return (
        <button style={styles} onClick={toggleTheme}>
            当前主题：{theme}
        </button>
    );
}

// 提供上下文的组件
function App() {
    const [theme, setTheme] = useState('light');

    const contextValue = {
        theme,
        toggleTheme: () => setTheme(t => t === 'light' ? 'dark' : 'light')
    };

    return (
        <ThemeContext.Provider value={contextValue}>
            <ThemedButton/>
        </ThemeContext.Provider>
    );
}

// ==================== useReducer 示例 ====================

/**
 * useReducer 使用示例
 *
 * 适用场景：
 * - 状态逻辑复杂，包含多个子值
 * - 下一个状态依赖于之前的状态
 * - 需要复用状态逻辑
 *
 * 原理说明：
 * - 类似 Redux 的 reducer 模式
 * - dispatch 触发 action，reducer 根据 action 返回新状态
 */

// 初始状态
const initialState = {
    count: 0,
    step: 1,
    history: []
};

// reducer 函数
function counterReducer(state, action) {
    switch (action.type) {
        case 'increment':
            return {
                ...state,
                count: state.count + state.step,
                history: [...state.history, `+${state.step}`]
            };
        case 'decrement':
            return {
                ...state,
                count: state.count - state.step,
                history: [...state.history, `-${state.step}`]
            };
        case 'setStep':
            return {...state, step: action.payload};
        case 'reset':
            return initialState;
        default:
            throw new Error(`未知的 action 类型：${action.type}`);
    }
}

function CounterWithReducer() {
    const [state, dispatch] = useReducer(counterReducer, initialState);

    return (
        <div>
            <p>计数：{state.count}</p>
            <p>步长：{state.step}</p>
            <input
                type="number"
                value={state.step}
                onChange={e => dispatch({type: 'setStep', payload: Number(e.target.value)})}
            />
            <button onClick={() => dispatch({type: 'decrement'})}>-</button>
            <button onClick={() => dispatch({type: 'increment'})}>+</button>
            <button onClick={() => dispatch({type: 'reset'})}>重置</button>
            <p>历史：{state.history.join(', ')}</p>
        </div>
    );
}

// ==================== useMemo 和 useCallback 示例 ====================

/**
 * useMemo 使用示例
 *
 * 原理说明：
 * - 缓存计算结果，避免重复计算
 * - 只有依赖项变化时才重新计算
 * - 适用于昂贵的计算
 */
function ExpensiveComponent({data, filter}) {
    // 使用 useMemo 缓存过滤结果
    const filteredData = useMemo(() => {
        console.log('执行过滤计算...');
        return data.filter(item =>
            item.name.toLowerCase().includes(filter.toLowerCase())
        );
    }, [data, filter]);  // 只有 data 或 filter 变化时才重新计算

    return (
        <ul>
            {filteredData.map(item => (
                <li key={item.id}>{item.name}</li>
            ))}
        </ul>
    );
}

/**
 * useCallback 使用示例
 *
 * 原理说明：
 * - 缓存函数引用，避免每次渲染创建新函数
 * - 配合 React.memo 使用，优化子组件渲染
 * - 只有依赖项变化时才返回新函数
 */

// 使用 React.memo 优化的子组件
const ChildButton = memo(function ChildButton({onClick, label}) {
    console.log(`渲染按钮：${label}`);
    return <button onClick={onClick}>{label}</button>;
});

function ParentComponent() {
    const [count, setCount] = useState(0);
    const [text, setText] = useState('');

    // 不使用 useCallback：每次渲染都会创建新函数，导致 ChildButton 重新渲染
    const handleClickBad = () => {
        setCount(c => c + 1);
    };

    // 使用 useCallback：只有 count 变化时才创建新函数
    const handleClickGood = useCallback(() => {
        setCount(c => c + 1);
    }, []);  // 空依赖数组，函数引用保持不变

    return (
        <div>
            <p>计数：{count}</p>
            <input value={text} onChange={e => setText(e.target.value)}/>
            {/* 使用 useCallback 的函数，不会导致不必要的子组件渲染 */}
            <ChildButton onClick={handleClickGood} label="+1（优化）"/>
            <ChildButton onClick={handleClickBad} label="+1（未优化）"/>
        </div>
    );
}

// ==================== useRef 示例 ====================

/**
 * useRef 使用示例
 *
 * 原理说明：
 * - 返回一个可变的 ref 对象，其 .current 属性被初始化为传入的参数
 * - ref 对象在组件的整个生命周期内保持不变
 * - 修改 ref.current 不会触发重新渲染
 * - 常用于：访问 DOM、保存上一次的值、存储定时器 ID 等
 */
function UseRefDemo() {
    // 1. 访问 DOM 元素
    const inputRef = useRef(null);

    // 2. 保存上一次的值
    const [count, setCount] = useState(0);
    const prevCountRef = useRef();

    // 3. 存储定时器 ID
    const timerRef = useRef(null);

    useEffect(() => {
        // 保存上一次的值
        prevCountRef.current = count;
    });

    const startTimer = () => {
        // 使用 ref 存储定时器 ID，避免重新渲染
        timerRef.current = setInterval(() => {
            setCount(c => c + 1);
        }, 1000);
    };

    const stopTimer = () => {
        // 通过 ref 访问定时器 ID
        if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
        }
    };

    return (
        <div>
            {/* 访问 DOM */}
            <input ref={inputRef} type="text"/>
            <button onClick={() => inputRef.current.focus()}>
                聚焦输入框
            </button>

            {/* 显示当前值和上一次的值 */}
            <p>当前值：{count}</p>
            <p>上一次值：{prevCountRef.current}</p>

            {/* 定时器控制 */}
            <button onClick={startTimer}>开始计时</button>
            <button onClick={stopTimer}>停止计时</button>
        </div>
    );
}

// ==================== 自定义 Hook 示例 ====================

/**
 * 自定义 Hook：useLocalStorage
 *
 * 自定义 Hook 规则：
 * - 必须以 use 开头
 * - 内部可以调用其他 Hook
 * - 实现逻辑复用
 *
 * @param {string} key - localStorage 的 key
 * @param {*} initialValue - 初始值
 */
function useLocalStorage(key, initialValue) {
    // 使用函数式初始化，避免每次渲染都读取 localStorage
    const [storedValue, setStoredValue] = useState(() => {
        try {
            const item = window.localStorage.getItem(key);
            return item ? JSON.parse(item) : initialValue;
        } catch (error) {
            console.error('读取 localStorage 失败：', error);
            return initialValue;
        }
    });

    // 使用 useEffect 监听值的变化，同步到 localStorage
    useEffect(() => {
        try {
            window.localStorage.setItem(key, JSON.stringify(storedValue));
        } catch (error) {
            console.error('写入 localStorage 失败：', error);
        }
    }, [key, storedValue]);

    return [storedValue, setStoredValue];
}

// 使用自定义 Hook
function UserSettings() {
    const [name, setName] = useLocalStorage('userName', '');
    const [theme, setTheme] = useLocalStorage('theme', 'light');

    return (
        <div>
            <input
                value={name}
                onChange={e => setName(e.target.value)}
                placeholder="输入用户名"
            />
            <select value={theme} onChange={e => setTheme(e.target.value)}>
                <option value="light">浅色</option>
                <option value="dark">深色</option>
            </select>
        </div>
    );
}

// 导出所有示例组件
export {
    Counter,
    UserInfo,
    EffectDemo,
    ThemedButton,
    CounterWithReducer,
    ExpensiveComponent,
    ParentComponent,
    UseRefDemo,
    UserSettings
};
