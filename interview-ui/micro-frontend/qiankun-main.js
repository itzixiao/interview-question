/**
 * 微前端主应用 - qiankun 集成示例
 *
 * 核心概念：
 * 1. 主应用（Main App）- 负责加载和管理子应用
 * 2. 子应用（Micro App）- 独立的业务应用
 * 3. 生命周期 - bootstrap、mount、unmount
 * 4. 通信机制 - initGlobalState、onGlobalStateChange
 *
 * 对应文档：docs/18-前端开发/08-微前端架构实战.md
 */

/**
 * 加载子应用的辅助函数
 * 用于手动加载子应用（非路由激活场景）
 */
import {initGlobalState, loadMicroApp, registerMicroApps, start} from 'qiankun';
/**
 * 主应用布局组件（React 示例）
 */
import React, {useEffect, useState} from 'react';
import {Link, useLocation} from 'react-router-dom';

// ==================== 1. 子应用配置 ====================

/**
 * 子应用注册配置
 *
 * 配置项说明：
 * - name: 子应用名称，必须唯一
 * - entry: 子应用入口（HTML 地址）
 * - container: 子应用挂载的 DOM 节点
 * - activeRule: 激活规则（路由匹配）
 * - props: 传递给子应用的数据
 */
const microApps = [
    {
        name: 'user-app',           // 用户管理子应用
        entry: '//localhost:8081',  // 开发环境地址
        container: '#micro-app-container',
        activeRule: '/user',        // 当路由匹配 /user 时激活
        props: {
            // 传递给子应用的数据
            baseApi: '/api/user',
            appName: '用户管理系统'
        }
    },
    {
        name: 'order-app',          // 订单管理子应用
        entry: '//localhost:8082',
        container: '#micro-app-container',
        activeRule: '/order',
        props: {
            baseApi: '/api/order',
            appName: '订单管理系统'
        }
    },
    {
        name: 'report-app',         // 报表子应用
        entry: '//localhost:8083',
        container: '#micro-app-container',
        activeRule: '/report',
        props: {
            baseApi: '/api/report',
            appName: '数据报表系统'
        }
    }
];

// ==================== 2. 全局状态管理 ====================

/**
 * 初始化全局状态
 * 用于主应用和子应用之间的通信
 */
const initialState = {
    // 用户信息
    user: {
        id: null,
        name: '',
        role: '',
        token: ''
    },
    // 全局配置
    config: {
        theme: 'light',
        language: 'zh-CN',
        sidebarCollapsed: false
    },
    // 当前激活的子应用
    activeApp: ''
};

// 创建全局状态管理实例
const actions = initGlobalState(initialState);

/**
 * 全局状态变更监听
 * 主应用可以监听所有子应用的状态变更
 */
actions.onGlobalStateChange((state, prev) => {
    console.log('[主应用] 全局状态变化：', {
        current: state,
        previous: prev
    });

    // 处理主题变更
    if (state.config?.theme !== prev.config?.theme) {
        document.body.setAttribute('data-theme', state.config.theme);
    }

    // 处理语言变更
    if (state.config?.language !== prev.config?.language) {
        // 更新主应用语言
        i18n.changeLanguage(state.config.language);
    }
});

// ==================== 3. 注册子应用 ====================

/**
 * 注册所有子应用
 */
registerMicroApps(microApps, {
    /**
     * 子应用加载前的生命周期钩子
     * @param {Object} app - 子应用信息
     */
    beforeLoad: (app) => {
        console.log(`[主应用] 开始加载子应用：${app.name}`);
        // 可以在这里显示 loading
        showLoading(app.name);
    },

    /**
     * 子应用挂载前的生命周期钩子
     * @param {Object} app - 子应用信息
     */
    beforeMount: (app) => {
        console.log(`[主应用] 开始挂载子应用：${app.name}`);
    },

    /**
     * 子应用挂载后的生命周期钩子
     * @param {Object} app - 子应用信息
     */
    afterMount: (app) => {
        console.log(`[主应用] 子应用挂载完成：${app.name}`);
        // 隐藏 loading
        hideLoading(app.name);
        // 更新当前激活的子应用
        actions.setGlobalState({activeApp: app.name});
    },

    /**
     * 子应用卸载前的生命周期钩子
     * @param {Object} app - 子应用信息
     */
    beforeUnmount: (app) => {
        console.log(`[主应用] 开始卸载子应用：${app.name}`);
    },

    /**
     * 子应用卸载后的生命周期钩子
     * @param {Object} app - 子应用信息
     */
    afterUnmount: (app) => {
        console.log(`[主应用] 子应用卸载完成：${app.name}`);
    }
});

// ==================== 4. 启动微前端 ====================

/**
 * 启动 qiankun
 *
 * 配置项说明：
 * - prefetch: 预加载策略（'all' | 'popstate' | boolean）
 * - sandbox: 沙箱配置（用于样式隔离和 JS 隔离）
 * - singular: 是否单实例模式（同时只能有一个子应用激活）
 * - fetch: 自定义 fetch 方法
 */
start({
    // 预加载策略：当第一个子应用挂载后，预加载其他子应用
    prefetch: 'all',

    // 沙箱配置
    sandbox: {
        // 开启严格样式隔离（Shadow DOM）
        strictStyleIsolation: true,
        // 开启实验性样式隔离（通过添加选择器前缀）
        experimentalStyleIsolation: true
    },

    // 单实例模式
    singular: true,

    // 自定义 fetch（可用于添加认证头等）
    fetch: (url, options) => {
        // 添加全局请求头
        const token = localStorage.getItem('token');
        if (token) {
            options = options || {};
            options.headers = {
                ...options.headers,
                'Authorization': `Bearer ${token}`
            };
        }
        return window.fetch(url, options);
    }
});

// ==================== 5. 主应用组件示例 ====================

function MainLayout() {
    const location = useLocation();
    const [globalState, setGlobalState] = useState(initialState);
    const [loading, setLoading] = useState({});

    // 监听全局状态变化
    useEffect(() => {
        actions.onGlobalStateChange((state) => {
            setGlobalState(state);
        });
    }, []);

    // 显示 loading
    const showLoading = (appName) => {
        setLoading(prev => ({...prev, [appName]: true}));
    };

    // 隐藏 loading
    const hideLoading = (appName) => {
        setLoading(prev => ({...prev, [appName]: false}));
    };

    // 切换主题
    const toggleTheme = () => {
        const newTheme = globalState.config.theme === 'light' ? 'dark' : 'light';
        actions.setGlobalState({
            config: {...globalState.config, theme: newTheme}
        });
    };

    // 切换语言
    const changeLanguage = (lang) => {
        actions.setGlobalState({
            config: {...globalState.config, language: lang}
        });
    };

    // 登出
    const logout = () => {
        // 清空全局状态
        actions.setGlobalState({
            user: {id: null, name: '', role: '', token: ''}
        });
        localStorage.removeItem('token');
        window.location.href = '/login';
    };

    return (
        <div className="main-layout">
            {/* 顶部导航 */}
            <header className="header">
                <div className="logo">微前端平台</div>
                <nav className="nav">
                    <Link to="/" className={location.pathname === '/' ? 'active' : ''}>
                        首页
                    </Link>
                    <Link to="/user" className={location.pathname.startsWith('/user') ? 'active' : ''}>
                        用户管理
                    </Link>
                    <Link to="/order" className={location.pathname.startsWith('/order') ? 'active' : ''}>
                        订单管理
                    </Link>
                    <Link to="/report" className={location.pathname.startsWith('/report') ? 'active' : ''}>
                        数据报表
                    </Link>
                </nav>
                <div className="actions">
                    <button onClick={toggleTheme}>
                        {globalState.config.theme === 'light' ? '🌙' : '☀️'}
                    </button>
                    <select
                        value={globalState.config.language}
                        onChange={(e) => changeLanguage(e.target.value)}
                    >
                        <option value="zh-CN">中文</option>
                        <option value="en-US">English</option>
                    </select>
                    <span>{globalState.user.name}</span>
                    <button onClick={logout}>登出</button>
                </div>
            </header>

            {/* 侧边栏（可选） */}
            <aside className="sidebar">
                {/* 根据当前激活的子应用显示不同的菜单 */}
                {globalState.activeApp === 'user-app' && (
                    <ul>
                        <li><Link to="/user/list">用户列表</Link></li>
                        <li><Link to="/user/roles">角色管理</Link></li>
                    </ul>
                )}
                {globalState.activeApp === 'order-app' && (
                    <ul>
                        <li><Link to="/order/list">订单列表</Link></li>
                        <li><Link to="/order/statistics">订单统计</Link></li>
                    </ul>
                )}
            </aside>

            {/* 主内容区 */}
            <main className="main-content">
                {/* 首页内容（非微前端） */}
                {location.pathname === '/' && (
                    <div className="home-page">
                        <h1>欢迎使用微前端平台</h1>
                        <p>当前登录用户：{globalState.user.name || '未登录'}</p>
                    </div>
                )}

                {/* 子应用挂载容器 */}
                <div id="micro-app-container" className="micro-app-container">
                    {/* 子应用将挂载在这里 */}
                    {Object.entries(loading).map(([appName, isLoading]) =>
                        isLoading ? (
                            <div key={appName} className="loading">
                                正在加载 {appName}...
                            </div>
                        ) : null
                    )}
                </div>
            </main>
        </div>
    );
}

// ==================== 6. 工具函数 ====================

/**
 * 手动加载子应用
 * @param {string} appName - 子应用名称
 * @param {string} entry - 子应用入口
 * @param {HTMLElement} container - 挂载容器
 */
function loadAppManually(appName, entry, container) {
    const microApp = loadMicroApp({
        name: appName,
        entry,
        container,
        props: {
            // 传递数据给子应用
            mode: 'modal'  // 标识是弹窗模式加载
        }
    });

    // 返回微应用实例，可用于手动卸载
    return microApp;
}

/**
 * 示例：在弹窗中加载子应用
 */
function openMicroAppModal(appName, entry) {
    // 创建弹窗容器
    const modal = document.createElement('div');
    modal.className = 'micro-app-modal';
    modal.innerHTML = `
        <div class="modal-overlay">
            <div class="modal-content">
                <div class="modal-header">
                    <h3>${appName}</h3>
                    <button class="close-btn">×</button>
                </div>
                <div class="modal-body" id="modal-micro-container"></div>
            </div>
        </div>
    `;
    document.body.appendChild(modal);

    // 加载子应用
    const container = modal.querySelector('#modal-micro-container');
    const microApp = loadAppManually(appName, entry, container);

    // 关闭弹窗时卸载子应用
    modal.querySelector('.close-btn').addEventListener('click', () => {
        microApp.unmount().then(() => {
            document.body.removeChild(modal);
        });
    });

    return microApp;
}

// ==================== 7. 路由守卫 ====================

/**
 * 路由守卫 - 检查子应用访问权限
 */
function checkMicroAppAccess(appName) {
    const {user} = actions.getGlobalState();

    // 权限映射
    const permissionMap = {
        'user-app': ['admin', 'user-manager'],
        'order-app': ['admin', 'order-manager'],
        'report-app': ['admin', 'analyst']
    };

    const requiredRoles = permissionMap[appName] || [];

    if (!requiredRoles.includes(user.role)) {
        console.warn(`[主应用] 用户 ${user.name} 没有权限访问 ${appName}`);
        return false;
    }

    return true;
}

// ==================== 8. 导出 ====================

export {
    actions,              // 全局状态操作
    microApps,            // 子应用配置
    MainLayout,           // 主应用布局组件
    loadAppManually,      // 手动加载子应用
    openMicroAppModal,    // 弹窗加载子应用
    checkMicroAppAccess   // 权限检查
};

export default MainLayout;
