/**
 * 微前端子应用 - qiankun 子应用配置示例
 *
 * 核心要点：
 * 1. 导出生命周期函数：bootstrap、mount、unmount
 * 2. 配置 public-path 以支持动态加载
 * 3. 处理路由 base 配置
 * 4. 与主应用通信
 *
 * 对应文档：docs/18-前端开发/08-微前端架构实战.md
 */

// ==================== 1. 入口文件配置 ====================

/**
 * 子应用入口文件（main.js / index.js）
 *
 * 重要：需要设置 publicPath 以支持动态加载资源
 */

// 判断是否在 qiankun 环境中运行
const isQiankun = window.__POWERED_BY_QIANKUN__;

// 设置 publicPath，确保资源正确加载
if (isQiankun) {
    // eslint-disable-next-line no-undef
    __webpack_public_path__ = window.__INJECTED_PUBLIC_PATH_BY_QIANKUN__;
}

// ==================== 2. 生命周期函数 ====================

/**
 * 应用实例引用
 * 用于在 unmount 时销毁应用
 */
let appInstance = null;
let router = null;
let store = null;

/**
 * 引导函数 - 应用初始化时调用
 * 只会在应用第一次加载时执行
 */
export async function bootstrap(props) {
    console.log('[子应用] bootstrap 执行', props);

    // 可以在这里做一些全局初始化
    // 如：初始化日志、埋点等
}

/**
 * 挂载函数 - 应用挂载时调用
 * 每次激活应用时都会执行
 *
 * @param {Object} props - 主应用传递的属性
 *   - container: 挂载容器 DOM 元素
 *   - name: 子应用名称
 *   - props: 主应用传递的自定义数据
 *   - onGlobalStateChange: 监听全局状态变化
 *   - setGlobalState: 设置全局状态
 */
export async function mount(props) {
    console.log('[子应用] mount 执行', props);

    // 1. 保存主应用传递的通信方法
    const {container, onGlobalStateChange, setGlobalState} = props;

    // 2. 监听全局状态变化
    onGlobalStateChange((value, prev) => {
        console.log('[子应用] 全局状态变化：', value, prev);

        // 处理主题变化
        if (value.config?.theme !== prev.config?.theme) {
            applyTheme(value.config.theme);
        }

        // 处理语言变化
        if (value.config?.language !== prev.config?.language) {
            changeLanguage(value.config.language);
        }

        // 处理用户信息变化
        if (value.user?.id !== prev.user?.id) {
            updateUserInfo(value.user);
        }
    });

    // 3. 渲染应用
    appInstance = renderApp({
        container: container || '#app',  // 兼容独立运行
        props
    });
}

/**
 * 卸载函数 - 应用卸载时调用
 * 每次失活应用时都会执行
 */
export async function unmount(props) {
    console.log('[子应用] unmount 执行', props);

    // 1. 销毁应用实例
    if (appInstance) {
        appInstance.unmount();
        appInstance = null;
    }

    // 2. 清理路由
    if (router) {
        router = null;
    }

    // 3. 清理状态管理
    if (store) {
        store = null;
    }

    // 4. 清理 DOM
    const {container} = props;
    const mountNode = container ? container.querySelector('#app') : document.getElementById('app');
    if (mountNode) {
        mountNode.innerHTML = '';
    }
}

/**
 * 可选：更新函数 - 主应用手动更新 props 时调用
 */
export async function update(props) {
    console.log('[子应用] update 执行', props);
    // 处理主应用传递的新 props
}

// ==================== 3. 应用渲染函数 ====================

/**
 * 渲染应用
 * @param {Object} options
 */
function renderApp({container, props}) {
    // Vue 3 示例
    const {createApp} = require('vue');
    const App = require('./App.vue').default;
    const router = createRouter(props);
    const store = createStore(props);

    const app = createApp(App);

    // 使用路由和状态管理
    app.use(router);
    app.use(store);

    // 将主应用传递的 props 注入到全局属性
    app.config.globalProperties.$mainProps = props;

    // 挂载应用
    app.mount(container);

    return app;
}

// ==================== 4. 路由配置 ====================

/**
 * 创建路由
 * 需要处理 base 路径，确保路由正确匹配
 */
function createRouter(props) {
    const {createRouter, createWebHistory, createWebHashHistory} = require('vue-router');
    const routes = require('./routes').default;

    // 从 props 中获取 base 路径
    const base = props.base || (isQiankun ? '/user' : '/');

    const router = createRouter({
        // qiankun 环境下使用 history 模式，非 qiankun 环境根据配置选择
        history: isQiankun ? createWebHistory(base) : createWebHashHistory(),
        routes,
        base
    });

    // 路由守卫
    router.beforeEach((to, from, next) => {
        // 可以在这里做权限验证
        console.log('[子应用] 路由切换：', from.path, '->', to.path);
        next();
    });

    return router;
}

// ==================== 5. 状态管理 ====================

/**
 * 创建状态管理
 * 与主应用状态同步
 */
function createStore(props) {
    const {createStore} = require('vuex');

    const store = createStore({
        state: {
            // 从主应用获取初始用户信息
            user: props.user || {},
            // 从主应用获取初始配置
            config: props.config || {},
            // 子应用自己的状态
            appData: {}
        },
        mutations: {
            SET_USER(state, user) {
                state.user = user;
            },
            SET_CONFIG(state, config) {
                state.config = config;
            },
            SET_APP_DATA(state, data) {
                state.appData = data;
            }
        },
        actions: {
            // 同步用户信息到主应用
            syncUserToMain({state}, props) {
                if (props && props.setGlobalState) {
                    props.setGlobalState({user: state.user});
                }
            }
        }
    });

    return store;
}

// ==================== 6. 工具函数 ====================

/**
 * 应用主题
 * @param {string} theme - 'light' | 'dark'
 */
function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);

    // 可以在这里动态加载主题样式
    const themeLink = document.getElementById('theme-style');
    if (themeLink) {
        themeLink.href = `/themes/${theme}.css`;
    }
}

/**
 * 切换语言
 * @param {string} language - 语言代码
 */
function changeLanguage(language) {
    // 使用 i18n 切换语言
    const i18n = appInstance?.config?.globalProperties?.$i18n;
    if (i18n) {
        i18n.locale = language;
    }
}

/**
 * 更新用户信息
 * @param {Object} user
 */
function updateUserInfo(user) {
    if (store) {
        store.commit('SET_USER', user);
    }
}

// ==================== 7. Vue 组件中使用示例 ====================

/**
 * 在 Vue 组件中访问主应用传递的 props
 */
const ExampleComponent = {
    template: `
        <div class="example-component">
            <h2>{{ appName }}</h2>
            <p>当前用户：{{ userInfo.name }}</p>
            <p>当前主题：{{ theme }}</p>
            <button @click="changeTheme">切换主题</button>
            <button @click="notifyMain">通知主应用</button>
        </div>
    `,

    data() {
        return {
            userInfo: {},
            theme: 'light',
            appName: ''
        };
    },

    mounted() {
        // 获取主应用传递的 props
        const mainProps = this.$mainProps || {};

        // 获取自定义 props
        this.appName = mainProps.appName || '子应用';

        // 监听全局状态
        mainProps.onGlobalStateChange && mainProps.onGlobalStateChange((state) => {
            this.userInfo = state.user || {};
            this.theme = state.config?.theme || 'light';
        });
    },

    methods: {
        /**
         * 切换主题并同步到主应用
         */
        changeTheme() {
            const newTheme = this.theme === 'light' ? 'dark' : 'light';

            // 通过 setGlobalState 通知主应用
            if (this.$mainProps && this.$mainProps.setGlobalState) {
                this.$mainProps.setGlobalState({
                    config: {theme: newTheme}
                });
            }
        },

        /**
         * 主动通知主应用
         */
        notifyMain() {
            if (this.$mainProps && this.$mainProps.setGlobalState) {
                this.$mainProps.setGlobalState({
                    notification: {
                        type: 'info',
                        message: '来自子应用的通知',
                        timestamp: Date.now()
                    }
                });
            }
        }
    }
};

// ==================== 8. Webpack 配置 ====================

/**
 * vue.config.js 配置示例
 */
const vueConfig = {
    // 开发服务器配置
    devServer: {
        port: 8081,  // 子应用端口
        headers: {
            // 允许跨域访问
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type, Authorization'
        }
    },

    // 打包配置
    configureWebpack: {
        output: {
            // 打包成 umd 格式，支持 qiankun 加载
            library: 'user-app',      // 子应用名称
            libraryTarget: 'umd',      // 打包格式
            jsonpFunction: `webpackJsonp_user-app`  // 避免冲突
        }
    }
};

// ==================== 9. 独立运行支持 ====================

/**
 * 支持子应用独立运行（非 qiankun 环境）
 */
if (!isQiankun) {
    // 独立运行时直接渲染应用
    renderApp({
        container: '#app',
        props: {
            // 模拟主应用传递的 props
            user: JSON.parse(localStorage.getItem('user') || '{}'),
            config: {theme: 'light', language: 'zh-CN'},
            onGlobalStateChange: () => {
            },
            setGlobalState: () => {
            }
        }
    });
}

// ==================== 10. 导出 ====================

export {
    bootstrap,
    mount,
    unmount,
    update,
    vueConfig
};

export default {bootstrap, mount, unmount, update};
